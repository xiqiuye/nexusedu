package com.xqy.nexusedu.infrastructure.ai.service.knowledge;

import java.util.Locale;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.application.ai.port.out.AiToolExecutor;
import com.xqy.nexusedu.common.model.ai.AiTask;
import com.xqy.nexusedu.common.model.ai.AiTaskResult;
import com.xqy.nexusedu.infrastructure.knowledge.service.KnowledgeBusinessService;

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
