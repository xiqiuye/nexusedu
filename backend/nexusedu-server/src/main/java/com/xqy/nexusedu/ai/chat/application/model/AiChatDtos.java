package com.xqy.nexusedu.ai.chat.application.model;

import java.io.Serializable;

/** AI chat HTTP request DTOs. */
public final class AiChatDtos {
    private AiChatDtos() {
    }

    public static class ChatRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long conversationId;
        private String question;
        private String idempotencyKey;

        public ChatRequest() {
        }

        public ChatRequest(Long conversationId, String question, String idempotencyKey) {
            this.conversationId = conversationId;
            this.question = question;
            this.idempotencyKey = idempotencyKey;
        }

        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public Long conversationId() { return conversationId; }
        public String question() { return question; }
        public String idempotencyKey() { return idempotencyKey; }
    }
}
