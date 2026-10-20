package com.xqy.nexusedu.ai.tools.infrastructure.tool.knowledge;

import java.util.Locale;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.ai.tools.application.port.out.AiToolExecutor;
import com.xqy.nexusedu.ai.task.domain.model.AiTask;
import com.xqy.nexusedu.ai.task.domain.model.AiTaskResult;
import com.xqy.nexusedu.knowledge.catalog.application.service.KnowledgeBusinessService;

/** Executes RAG tools belonging to the knowledge domain. */
@Component
public class KnowledgeToolExecutor implements AiToolExecutor {
    private final KnowledgeBusinessService knowledgeBusinessService;
    private String domain = "knowledge";

    public KnowledgeToolExecutor(KnowledgeBusinessService knowledgeBusinessService) {
        this.knowledgeBusinessService = knowledgeBusinessService;
    }
    @Override public String domain() { return domain; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    @Override
    public AiTaskResult execute(AiTask task) {
        if (task == null) throw new IllegalArgumentException("task must not be null");
        String toolName = task.getToolName();
        if (!("query_knowledge".equalsIgnoreCase(toolName) || "search_knowledge".equalsIgnoreCase(toolName))) {
            throw new IllegalArgumentException("Unsupported knowledge tool: " + toolName);
        }
        AiTaskResult result = knowledgeBusinessService.queryKnowledge(task);
        result.setDomain(domain);
        result.setToolName(toolName.trim().toLowerCase(Locale.ROOT));
        return result;
    }
}
