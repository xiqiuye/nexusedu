package com.xqy.nexusedu.ai.evaluation.service.impl;

import cn.hutool.json.JSONUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.ai.chat.service.AiChatLifecycleService;
import com.xqy.nexusedu.ai.chat.service.AiTaskService;
import com.xqy.nexusedu.ai.evaluation.service.AiConfiguration;
import com.xqy.nexusedu.ai.evaluation.service.AiEvaluationScorer;
import com.xqy.nexusedu.ai.evaluation.mapper.AiEvaluationMapper;
import com.xqy.nexusedu.ai.evaluation.service.AiEvaluationService;
import com.xqy.nexusedu.ai.task.service.run.AiTaskRunService;
import com.xqy.nexusedu.ai.task.model.AiExecutionContext;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

/** Durable evaluation queue worker. */
@Service
public class AiEvaluationWorker {
    @Autowired private AuthorizationPolicy authorization;
    private final AiEvaluationMapper mapper;
    private final TransactionTemplate transactions;
    private final InternalCallSigner signer;
    private final AiEvaluationService evaluations;
    private final AiChatLifecycleService lifecycle;
    private final AiTaskRunService runs;
    private final AiTaskService workflow;
    @Value("${nexusedu.ai.evaluation.case-timeout-ms:300000}") private long caseTimeoutMs = 300000;

    public AiEvaluationWorker(AiEvaluationMapper mapper, PlatformTransactionManager manager, AiEvaluationService evaluations,
                              AiChatLifecycleService lifecycle, AiTaskRunService runs, AiTaskService workflow,
                              @Value("${nexusedu.internal-rpc.secret}") String secret) {
        this.mapper = mapper; this.transactions = new TransactionTemplate(manager); this.evaluations = evaluations;
        this.lifecycle = lifecycle; this.runs = runs; this.workflow = workflow; this.signer = new InternalCallSigner(secret);
    }

    private InternalCallContext caller(Map<String,Object> batch) {
        return signer.sign("nexusedu-web-bff", batch.get("tenant_id").toString(), Long.valueOf(batch.get("create_by").toString()),
                ((Number) batch.get("session_id")).longValue(), UUID.randomUUID().toString());
    }

    @Scheduled(fixedDelayString = "${nexusedu.ai.evaluation.poll-ms:5000}")
    public void tick() {
        schedules();
        for (int i = 0; i < 2; i++) {
            Long[] claimedId = {null};
            Map<String,Object> claimed;
            try {
                claimed = transactions.execute(tx -> {
                    var item = mapper.claimPendingResult(); if (item == null) return null;
                    var batch = mapper.batch(item.get("batch_id")); claimedId[0] = ((Number) item.get("id")).longValue();
                    var caller = caller(batch); authorization.authorize("ai:evaluation:run", caller);
                    var sample = AiConfiguration.object(item.get("case_snapshot"));
                    var conversation = lifecycle.create("评测：" + sample.get("caseKey"), caller);
                    var context = AiExecutionContext.create(Long.valueOf(conversation.get("id").toString()), sample.get("question").toString(), "eval:" + item.get("id"), caller.tenantId(), caller.userId(), caller.sessionId());
                    runs.createOrReuse(context, batch.get("model_alias").toString()); mapper.markTaskRunEvaluation(context.getRunId());
                    if (batch.get("release_snapshot") != null) for (Object release : (List<?>) AiConfiguration.json(batch.get("release_snapshot").toString())) { var pinned = AiConfiguration.object(release); mapper.insertRelease(context.getTenantId(), context.getRunId(), pinned.get("resourceType"), pinned.get("resourceKey"), pinned.get("versionId"), JSONUtil.toJsonStr(pinned.get("config"))); }
                    mapper.markResultRunning(item.get("id"), JSONUtil.toJsonStr(context), context.getRunId(), context.getConversationId()); mapper.markBatchRunning(batch.get("id")); return item;
                });
            } catch (Exception error) { if (claimedId[0] != null) mapper.markPendingFailed(claimedId[0], "评测启动失败：" + error.getClass().getSimpleName()); continue; }
            if (claimed == null) break;
        }
        for (var item : mapper.runningResults()) {
            var run = mapper.run(item.get("run_id"), item.get("tenant_id")); if (run == null) continue;
            var started = run.get("start_time") instanceof java.sql.Timestamp timestamp ? timestamp.toLocalDateTime() : (java.time.LocalDateTime) run.get("start_time");
            if (!Set.of("SUCCEEDED", "FAILED", "CANCELLED").contains(run.get("status")) && started.plusNanos(caseTimeoutMs * 1_000_000).isBefore(java.time.LocalDateTime.now())) { workflow.cancel(JSONUtil.toBean(item.get("execution_context").toString(), AiExecutionContext.class)); continue; }
            if ("PENDING".equals(run.get("status"))) workflow.start(JSONUtil.toBean(item.get("execution_context").toString(), AiExecutionContext.class), item.get("model_alias").toString());
            else if (Set.of("SUCCEEDED", "FAILED", "CANCELLED").contains(run.get("status"))) score(item, run);
        }
        mapper.updateBatchStatuses();
    }

    @SuppressWarnings("unchecked")
    private void score(Map<String,Object> item, Map<String,Object> run) {
        var sample = AiConfiguration.object(item.get("case_snapshot")); String tenant = item.get("tenant_id").toString(); String runId = item.get("run_id").toString();
        var answers = mapper.assistantAnswers(tenant, runId); String answer = answers.isEmpty() ? null : answers.getFirst(); var sourceJson = mapper.sourceJson(tenant, runId);
        List<Map<String,Object>> sources = sourceJson.isEmpty() ? List.of() : (List<Map<String,Object>>) (Object) JSONUtil.toList(sourceJson.getFirst(), Map.class);
        var score = AiEvaluationScorer.score(sample, Objects.toString(run.get("plan_json"), "[]"), answer, sources);
        var costs = mapper.modelCosts(tenant, runId).stream().map(r -> Map.of("currency", r.get("currency"), "amount", r.get("amount").toString())).toList();
        long unknown = mapper.unknownModelCosts(tenant, runId); mapper.updateResult(run.get("status").equals("SUCCEEDED") ? "SUCCEEDED" : "FAILED", score.planCorrect(), score.toolCorrect(), score.ragHit(), score.citedCount(), score.correctCitedCount(), score.refused(), score.refusalCorrect(), JSONUtil.toJsonStr(costs), unknown, answer, run.get("status").equals("SUCCEEDED") ? null : "执行未成功：" + run.get("status"), item.get("id"));
    }

    private void schedules() {
        var row = transactions.execute(tx -> { var schedule = mapper.claimSchedule(); if (schedule != null) mapper.advanceSchedule(schedule.get("tenant_id"), schedule.get("dataset")); return schedule; });
        if (row == null) return;
        try { var context = signer.sign("nexusedu-web-bff", row.get("tenant_id").toString(), ((Number) row.get("user_id")).longValue(), ((Number) row.get("session_id")).longValue(), UUID.randomUUID().toString()); evaluations.run(Map.of("dataset", row.get("dataset")), context); }
        catch (Exception error) { mapper.pauseSchedule("计划已暂停：" + error.getClass().getSimpleName() + "。请重新登录检查权限并保存计划。", row.get("tenant_id"), row.get("dataset")); }
    }
}
