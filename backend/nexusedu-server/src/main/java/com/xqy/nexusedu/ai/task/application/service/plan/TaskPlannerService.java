package com.xqy.nexusedu.ai.task.application.service.plan;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.service.AiServices;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.task.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.entity.AiToolDefinition;
import com.xqy.nexusedu.ai.task.domain.constant.AiConstants;
import com.xqy.nexusedu.ai.task.domain.model.AiTask;
import com.xqy.nexusedu.ai.task.domain.model.QueryCriteria;
import com.xqy.nexusedu.ai.chat.infrastructure.llm.assistant.AiPlanAssistant;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.AiToolRegistry;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.ModelHandle;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.ModelRegistry;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.ObservedChatModel;
import com.xqy.nexusedu.ai.governance.infrastructure.governance.AiGovernanceRuntime;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.observation.AiObservationService;
import com.xqy.nexusedu.ai.governance.infrastructure.governance.prompt.AiPromptTemplateService;

/**
 * Creates and validates the structured plan; it does not execute tools or send SSE events.
 */
@Service
public class TaskPlannerService {
    private final ModelRegistry modelRegistry;
    private final AiToolRegistry toolRegistry;
    private final AiPromptTemplateService promptTemplateService;
    @Autowired
    private AiObservationService observations;
    @Autowired
    private AiGovernanceRuntime governance;

    public TaskPlannerService(ModelRegistry modelRegistry, AiToolRegistry toolRegistry,
                              AiPromptTemplateService promptTemplateService) {
        this.modelRegistry = Objects.requireNonNull(modelRegistry, "modelRegistry must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.promptTemplateService = promptTemplateService;
    }

    public List<AiTask> plan(AiExecutionContext context, String businessContext, String alias) {
        if (context == null || context.getQuestion() == null || context.getQuestion().isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        List<AiToolDefinition> definitions = governance == null ? toolRegistry.definitions() : governance.tools(context, toolRegistry.definitions());
        if (governance != null)
            governance.resolve(context, "PROMPT", "AI_COMPOSE", Map.of("content", AiGovernanceRuntime.COMPOSE_PROMPT, "enabled", true));
        String template = promptTemplateService == null ? null : promptTemplateService.findEnabledContent(AiConstants.PROMPT_CODE.PLAN);
        if (governance != null) template = governance.prompt(context, AiConstants.PROMPT_CODE.PLAN, template);
        String prompt = buildPlanPrompt(context.getQuestion(), businessContext, definitions, template);
        ModelHandle model = governance == null ? modelRegistry.find(normalizeAlias(alias)).orElseGet(modelRegistry::defaultModel) : governance.model(context, normalizeAlias(alias), modelRegistry);
        var chatModel = observations == null ? model.chatModel()
                : new ObservedChatModel(model, observations, context, "PLAN");
        List<AiTask> tasks = parsePlanJson(AiServices.builder(AiPlanAssistant.class).chatModel(chatModel).build().plan(prompt), definitions);
        // Never trust execution metadata returned by the model.
        tasks.forEach(task -> task.setExecutionContext(context));
        return tasks;
    }

    public List<AiTask> plan(String question, String businessContext, String alias) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("question must not be blank");
        List<AiToolDefinition> definitions = toolRegistry.definitions();
        String prompt = buildPlanPrompt(question, businessContext, definitions);
        return parsePlanJson(createPlanAssistant(alias).plan(prompt), definitions);
    }

    public List<AiTask> parsePlanJson(String response, List<AiToolDefinition> definitions) {
        if (response == null || response.isBlank())
            throw new IllegalArgumentException("AI planner returned an empty plan");
        String json = extractJsonArray(response);
        try {
            List<AiTask> tasks = JSONUtil.toList(json, AiTask.class);
            if (tasks == null || tasks.isEmpty()) throw new IllegalArgumentException("AI planner returned no task");
            for (AiTask task : tasks) normalizeAndValidate(task, definitions);
            return tasks;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI planner returned invalid task JSON", exception);
        }
    }

    public String buildPlanPrompt(String question, String businessContext, List<AiToolDefinition> definitions) {
        String template = promptTemplateService == null ? null
                : promptTemplateService.findEnabledContent(AiConstants.PROMPT_CODE.PLAN);
        return buildPlanPrompt(question, businessContext, definitions, template);
    }

    private String buildPlanPrompt(String question, String businessContext, List<AiToolDefinition> definitions, String template) {
        StringBuilder prompt = new StringBuilder();
        if (template != null && !template.isBlank()) {
            prompt.append(template);
            if (!template.endsWith("\n")) prompt.append('\n');
        }
        prompt.append("用户问题：\n---\n").append(question).append("\n---\n业务上下文：\n---\n")
                .append(businessContext == null || businessContext.isBlank() ? "(无)" : businessContext)
                .append("\n---\n可用工具定义：\n");
        if (definitions == null || definitions.isEmpty()) {
            prompt.append("(无；仅能生成 chat 任务)\n");
        } else {
            definitions.stream().filter(Objects::nonNull).forEach(definition -> prompt
                    .append("- domain: ").append(valueOrEmpty(definition.getDomain()))
                    .append(", toolName: ").append(valueOrEmpty(definition.getToolName()))
                    .append("\n  描述: ").append(valueOrEmpty(definition.getDescription()))
                    .append("\n  参数说明: ").append(valueOrEmpty(definition.getArgumentSpecification()))
                    .append("\n  关键词: ").append(valueOrEmpty(definition.getKeywords())).append('\n'));
        }
        return prompt.toString();
    }

    private void normalizeAndValidate(AiTask task, List<AiToolDefinition> definitions) {
        if (task == null || task.getDomain() == null || task.getDomain().isBlank()) {
            throw new IllegalArgumentException("Plan task domain must not be blank");
        }
        task.setDomain(task.getDomain().trim().toLowerCase(Locale.ROOT));
        if (task.getMissingArgs() == null) task.setMissingArgs(new ArrayList<>());
        if (AiConstants.TASK_DOMAIN.CHAT.equals(task.getDomain())) {
            task.setToolName(null);
            return;
        }
        if (task.getToolName() == null || task.getToolName().isBlank()) {
            String toolName = uniqueToolForDomain(task.getDomain(), definitions);
            if (toolName == null) throw new IllegalArgumentException("Plan task toolName must not be blank");
            task.setToolName(toolName);
        }
        if (toolRegistry.find(task.getDomain(), task.getToolName()).isEmpty() || definitions == null || definitions.stream().noneMatch(d -> task.getToolName().equals(d.getToolName()) && task.getDomain().equals(d.getDomain()))) {
            throw new IllegalArgumentException("Plan task references an unavailable tool: "
                    + task.getDomain() + "/" + task.getToolName());
        }
        if (task.getQuery() == null) task.setQuery(new QueryCriteria());
    }

    private String extractJsonArray(String result) {
        String json = result.trim();
        if (json.startsWith("```") && json.endsWith("```")) {
            int firstLineEnd = json.indexOf('\n');
            json = firstLineEnd >= 0 ? json.substring(firstLineEnd + 1, json.length() - 3).trim()
                    : json.substring(3, json.length() - 3).trim();
        }
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) throw new IllegalArgumentException("AI planner response must be a JSON array");
        return json.substring(start, end + 1);
    }

    private String uniqueToolForDomain(String domain, List<AiToolDefinition> definitions) {
        if (definitions == null) return null;
        String candidate = null;
        for (AiToolDefinition definition : definitions) {
            if (definition == null || !domain.equalsIgnoreCase(definition.getDomain())
                    || definition.getToolName() == null || definition.getToolName().isBlank()) continue;
            if (candidate != null && !candidate.equals(definition.getToolName())) return null;
            candidate = definition.getToolName();
        }
        return candidate;
    }

    private AiPlanAssistant createPlanAssistant(String alias) {
        ModelHandle model = modelRegistry.find(normalizeAlias(alias)).orElseGet(modelRegistry::defaultModel);
        return AiServices.builder(AiPlanAssistant.class).chatModel(observations == null ? model.chatModel()
                : new ObservedChatModel(model, observations, null, "PLAN")).build();
    }

    private String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? AiConstants.MODEL_ALIAS.STRONG : alias.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
