package com.xqy.nexusedu.ai.chat.application.service.chat;

import cn.hutool.json.JSONUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.task.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.chat.application.gateway.AiStreamEventPublisher;
import com.xqy.nexusedu.ai.task.application.service.execution.TaskExecutionService;
import com.xqy.nexusedu.ai.task.application.service.plan.TaskPlannerService;
import com.xqy.nexusedu.ai.task.application.service.run.AiTaskRunService;
import com.xqy.nexusedu.ai.task.infrastructure.persistence.entity.AiTaskRun;
import com.xqy.nexusedu.ai.task.infrastructure.persistence.entity.AiTaskStep;
import com.xqy.nexusedu.ai.task.domain.constant.AiConstants;
import com.xqy.nexusedu.ai.task.domain.model.AiTask;
import com.xqy.nexusedu.ai.task.domain.model.AiTaskResult;
import com.xqy.nexusedu.ai.task.domain.constant.AiTaskRunStatus;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.AiSourceStore;
import com.xqy.nexusedu.ai.governance.infrastructure.security.AiSafetyPolicy;
import com.xqy.nexusedu.ai.chat.infrastructure.llm.chat.AiChatService;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.log.AiRequestLogService;

/**
 * Orchestrates the PLAN, TOOL, and COMPOSE lifecycle of a durable conversation run.
 */
@Service
public class ConversationApplicationService {
    private final AiTaskRunService runService;
    private final TaskPlannerService plannerService;
    private final TaskExecutionService executionService;
    private final AiChatService chatService;
    private final AiRequestLogService requestLogService;
    private final AiChatHistoryService chatHistoryService;
    private final StringRedisTemplate redisTemplate;
    private final AiStreamEventPublisher eventPublisher;
    private final TaskExecutor workflowExecutor;
    private final int maxAttempts;
    private final long stepTimeoutMs;
    private final long businessResultTtlMinutes;
    @Autowired
    private AiSourceStore sourceStore;

    public ConversationApplicationService(AiTaskRunService runService, TaskPlannerService plannerService,
                                          TaskExecutionService executionService, AiChatService chatService,
                                          AiRequestLogService requestLogService, StringRedisTemplate redisTemplate,
                                          AiChatHistoryService chatHistoryService,
                                          AiStreamEventPublisher eventPublisher,
                                          @Qualifier("aiWorkflowExecutor") TaskExecutor workflowExecutor,
                                          @Value("${nexusedu.ai.workflow.max-attempts}") int maxAttempts,
                                          @Value("${nexusedu.ai.workflow.step-timeout-ms}") long stepTimeoutMs,
                                          @Value("${nexusedu.ai.workflow.business-result-ttl-minutes}") long businessResultTtlMinutes) {
        this.runService = runService;
        this.plannerService = plannerService;
        this.executionService = executionService;
        this.chatService = chatService;
        this.requestLogService = requestLogService;
        this.chatHistoryService = chatHistoryService;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.workflowExecutor = workflowExecutor;
        this.maxAttempts = maxAttempts;
        this.stepTimeoutMs = stepTimeoutMs;
        this.businessResultTtlMinutes = businessResultTtlMinutes;
    }

    public AiTaskRun start(AiExecutionContext context) {
        return start(context, AiConstants.MODEL_ALIAS.BALANCED);
    }

    public AiTaskRun start(AiExecutionContext context, String modelAlias) {
        validate(context);
        AiTaskRun run = runService.createOrReuse(context, modelAlias);
        if (runService.claimPendingRun(run.getRunId())) {
            try {
                publish(context.getStreamKey(), AiConstants.STREAM_EVENT.START, Map.of("runId", context.getRunId()));
                requestLogService.start(context, "chat", run.getModelAlias());
                workflowExecutor.execute(() -> execute(context));
            } catch (RuntimeException error) {
                runService.failRun(run.getRunId(), error);
                publish(context.getStreamKey(), AiConstants.STREAM_EVENT.ERROR, "任务暂时无法启动，请重试");
            }
        }
        return run;
    }

    public void cancel(AiExecutionContext context) {
        if (context == null) throw new IllegalArgumentException("authenticated execution context must not be null");
        String runId = context.getRunId();
        if (runService.cancel(runId, context.getTenantId(), String.valueOf(context.getUserId()))) {
            executionService.cancel(runId);
            AiTaskRun run = runService.requireRun(runId);
            chatService.cancel(run.getStreamKey());
            if (run.getStreamKey() != null) publish(run.getStreamKey(), AiConstants.STREAM_EVENT.ERROR, "已停止生成");
        }
    }

    private void execute(AiExecutionContext context) {
        String runId = context.getRunId();
        String streamKey = context.getStreamKey();
        try {
            AiTaskRun run = runService.requireRun(runId);
            Long conversationId = chatHistoryService.recordUserMessage(run);
            runService.attachConversation(runId, conversationId);
            context.setConversationId(conversationId);
            publish(streamKey, AiConstants.STREAM_EVENT.PLANNING, "分析问题中");
            AiTaskStep planStep = runService.createStep(runId, 1, "PLAN", "planner", null,
                    run.getQuestion(), 1, stepTimeoutMs);
            runService.startStep(planStep, 1);
            List<AiTask> tasks = plannerService.plan(context, businessContext(run), run.getModelAlias());
            String planJson = JSONUtil.toJsonStr(tasks);
            runService.succeedStep(planStep, planJson);
            runService.savePlan(runId, planJson);
            checkCancelled(runId);

            if (isChatPlan(tasks)) {
                compose(context, run, List.of(), tasks);
                return;
            }
            publish(streamKey, AiConstants.STREAM_EVENT.EXECUTING, "查询相关数据");
            List<AiTaskResult> results = new ArrayList<>();
            int toolStepNo = 2;
            for (AiTask task : tasks) {
                checkCancelled(runId);
                if (AiConstants.TASK_DOMAIN.CHAT.equals(task.getDomain())) continue;
                task.setExecutionContext(context);
                AiTaskStep step = runService.createStep(runId, toolStepNo++, "TOOL", task.getDomain(), task.getToolName(),
                        JSONUtil.toJsonStr(task), maxAttempts, stepTimeoutMs);
                results.add(executionService.execute(context, step, task));
            }
            String resultJson = JSONUtil.toJsonStr(results);
            redisTemplate.opsForValue().set(businessKey(run), resultJson,
                    Duration.ofMinutes(businessResultTtlMinutes));
            compose(context, run, results, tasks.stream()
                    .filter(task -> AiConstants.TASK_DOMAIN.CHAT.equals(task.getDomain())).toList());
        } catch (TaskExecutionService.TaskCancelledException cancelled) {
            runService.markCancelled(runId);
            requestLogService.fail(context.getRequestId(), "CANCELLED", cancelled);
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR, "AI 任务已取消");
        } catch (Exception error) {
            runService.failRun(runId, error);
            requestLogService.fail(context.getRequestId(), error.getClass().getSimpleName(), error);
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR,
                    error.getMessage() == null ? "AI task failed" : error.getMessage());
        }
    }

    private void compose(AiExecutionContext context, AiTaskRun run, List<AiTaskResult> results,
                         List<AiTask> chatTasks) {
        String runId = context.getRunId();
        String streamKey = context.getStreamKey();
        checkCancelled(runId);
        int composeStepNo = results.isEmpty() ? 2 : results.size() + 2;
        AiTaskStep composeStep = runService.createStep(runId, composeStepNo, "COMPOSE", "chat", null,
                JSONUtil.toJsonStr(results), 1, stepTimeoutMs);
        runService.startStep(composeStep, 1);
        runService.setRunStatus(runId, AiTaskRunStatus.COMPOSING);
        var sources = AiSafetyPolicy.publicSources(results);
        if (sourceStore != null) sourceStore.save(context, sources);
        if (!sources.isEmpty()) publish(streamKey, AiConstants.STREAM_EVENT.SOURCES, sources);
        String prompt = results.isEmpty() && chatTasks.isEmpty() ? run.getQuestion()
                : "User question:\n---\n" + AiSafetyPolicy.redact(run.getQuestion()) + "\n---\n"
                + AiSafetyPolicy.promptChatTasks(chatTasks)
                + (results.isEmpty() ? "" : "\n" + AiSafetyPolicy.promptResults(results));
        chatService.answer(context, prompt, memoryKey(run), run.getModelAlias(),
                answer -> {
                    checkCancelled(runId);
                    chatHistoryService.recordAssistantMessage(run, answer);
                    runService.succeedStep(composeStep, "STREAM_COMPLETED");
                    if (!runService.completeRun(runId)) throw new TaskExecutionService.TaskCancelledException();
                }, error -> {
                    runService.failStep(composeStep, error.getClass().getSimpleName(), error.getMessage());
                    if (runService.isCancellationRequested(runId)) runService.markCancelled(runId);
                    else runService.failRun(runId, error);
                }, () -> runService.isCancellationRequested(runId), chatHistoryService.recentMessages(run, 40));
    }

    private void checkCancelled(String runId) {
        if (runService.isCancellationRequested(runId)) throw new TaskExecutionService.TaskCancelledException();
    }

    private void publish(String streamKey, String type, Object data) {
        eventPublisher.publish(streamKey, type, data);
    }

    private boolean isChatPlan(List<AiTask> tasks) {
        return tasks.stream().allMatch(task -> AiConstants.TASK_DOMAIN.CHAT.equals(task.getDomain()));
    }

    private String businessContext(AiTaskRun run) {
        return redisTemplate.opsForValue().get(businessKey(run));
    }

    private String businessKey(AiTaskRun run) {
        return AiConstants.CACHE_KEY.BUSINESS_PREFIX + run.getTenantId() + "_" + run.getUserId() + "_" + run.getConversationId();
    }

    private String memoryKey(AiTaskRun run) {
        return AiConstants.CACHE_KEY.CHAT_PREFIX + run.getTenantId() + "_" + run.getUserId() + "_" + run.getConversationId();
    }

    private void validate(AiExecutionContext context) {
        if (context == null || context.getQuestion() == null || context.getQuestion().isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (context.getQuestion().length() > 4_000)
            throw new IllegalArgumentException("question exceeds 4000 characters");
        if (context.getIdempotencyKey() != null && context.getIdempotencyKey().length() > 128)
            throw new IllegalArgumentException("idempotencyKey exceeds 128 characters");
        if (context.getTenantId() == null || context.getTenantId().isBlank()
                || context.getUserId() == null || context.getSessionId() == null)
            throw new IllegalArgumentException("authenticated identity is required");
        if (context.getRequestId() == null || context.getRequestId().isBlank()
                || context.getRunId() == null || context.getRunId().isBlank()
                || context.getStreamKey() == null || context.getStreamKey().isBlank())
            throw new IllegalArgumentException("backend execution identifiers are required");
    }
}
