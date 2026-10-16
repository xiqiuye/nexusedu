package com.xqy.nexusedu.domain.ai.model;

import java.util.ArrayList;
import java.util.List;

import com.xqy.nexusedu.domain.ai.model.AiExecutionContext;

/**
 * A request to execute one registered AI tool.
 */
public class AiTask {

    private AiExecutionContext executionContext;
    private String domain;
    private String toolName;
    private String reason;
    private String subQuestion;
    private QueryCriteria query;
    private List<String> missingArgs = new ArrayList<>();
    /**
     * Optional pre-computed embedding supplied by the planner/vectorizer.
     */
    private List<Float> embedding = new ArrayList<>();

    public AiTask() {
    }

    public String getRequestId() {
        return executionContext == null ? null : executionContext.getRequestId();
    }

    public AiExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(AiExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * Compatibility projections for existing tool executors and stored task JSON.
     */
    public String getTenantId() {
        return executionContext == null ? null : executionContext.getTenantId();
    }

    public void setTenantId(String tenantId) {
        context().setTenantId(tenantId);
    }

    public String getUserId() {
        return executionContext == null || executionContext.getUserId() == null ? null : executionContext.getUserId().toString();
    }

    public void setUserId(String userId) {
        context().setUserId(userId == null ? null : Long.valueOf(userId));
    }

    public String getSessionId() {
        return executionContext == null || executionContext.getSessionId() == null ? null : executionContext.getSessionId().toString();
    }

    public void setSessionId(String sessionId) {
        context().setSessionId(sessionId == null ? null : Long.valueOf(sessionId));
    }

    public void setRequestId(String requestId) {
        context().setRequestId(requestId);
    }

    private AiExecutionContext context() {
        if (executionContext == null) executionContext = new AiExecutionContext();
        return executionContext;
    }

    public AiTask(String domain, String toolName, String reason, QueryCriteria query,
                  List<String> missingArgs) {
        this.domain = domain;
        this.toolName = toolName;
        this.reason = reason;
        this.query = query;
        this.missingArgs = missingArgs;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getToolName() {
        return toolName;
    }

    public String getSubQuestion() {
        return subQuestion;
    }

    public void setSubQuestion(String subQuestion) {
        this.subQuestion = subQuestion;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public QueryCriteria getQuery() {
        return query;
    }

    public void setQuery(QueryCriteria query) {
        this.query = query;
    }

    public List<String> getMissingArgs() {
        return missingArgs;
    }

    public void setMissingArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    /**
     * Backward-compatible aliases for callers using the original misspelling.
     */
    public List<String> getMissngArgs() {
        return missingArgs;
    }

    public void setMissngArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
}
