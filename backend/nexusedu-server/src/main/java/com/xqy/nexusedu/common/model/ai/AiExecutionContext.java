package com.xqy.nexusedu.common.model.ai;

import java.io.Serializable;
import java.util.UUID;

/**
 * Trusted execution metadata shared across RPC and asynchronous AI stages.
 */
public class AiExecutionContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long conversationId;
    private String question;
    private String idempotencyKey;
    private String tenantId;
    private Long userId;
    private Long sessionId;
    private String requestId;
    private String runId;
    private String streamKey;

    public AiExecutionContext() {
    }

    /**
     * Called only at the authenticated backend boundary.
     */
    public static AiExecutionContext create(Long conversationId, String question, String idempotencyKey,
                                            String tenantId, Long userId, Long sessionId) {
        AiExecutionContext context = new AiExecutionContext();
        context.setConversationId(conversationId);
        context.setQuestion(question);
        context.setIdempotencyKey(idempotencyKey);
        context.setTenantId(tenantId);
        context.setUserId(userId);
        context.setSessionId(sessionId);
        context.setRequestId(UUID.randomUUID().toString());
        context.setRunId(UUID.randomUUID().toString());
        context.setStreamKey("nexusedu:ai:sse:" + UUID.randomUUID());
        return context;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public Long conversationId() {
        return conversationId;
    }

    public String question() {
        return question;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String tenantId() {
        return tenantId;
    }

    public Long userId() {
        return userId;
    }

    public Long sessionId() {
        return sessionId;
    }

    public String requestId() {
        return requestId;
    }

    public String runId() {
        return runId;
    }

    public String streamKey() {
        return streamKey;
    }
}
