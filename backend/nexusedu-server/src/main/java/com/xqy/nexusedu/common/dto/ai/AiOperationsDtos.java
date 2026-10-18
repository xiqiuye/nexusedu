package com.xqy.nexusedu.common.dto.ai;

import java.io.Serializable;

/** Request DTOs for AI conversation and knowledge operations. */
public final class AiOperationsDtos {
    private AiOperationsDtos() {
    }

    public static class ConversationInput implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        public ConversationInput() { }
        public ConversationInput(String title) { this.title = title; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String title() { return title; }
    }

    public static class RetentionInput implements Serializable {
        private static final long serialVersionUID = 1L;
        private int retentionDays;
        public RetentionInput() { }
        public RetentionInput(int retentionDays) { this.retentionDays = retentionDays; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public int retentionDays() { return retentionDays; }
    }

    public static class KeepInput implements Serializable {
        private static final long serialVersionUID = 1L;
        private String retainUntil;
        public KeepInput() { }
        public KeepInput(String retainUntil) { this.retainUntil = retainUntil; }
        public String getRetainUntil() { return retainUntil; }
        public void setRetainUntil(String retainUntil) { this.retainUntil = retainUntil; }
        public String retainUntil() { return retainUntil; }
    }

    public static class IndexRevision implements Serializable {
        private static final long serialVersionUID = 1L;
        private int indexRevision;
        public IndexRevision() { }
        public IndexRevision(int indexRevision) { this.indexRevision = indexRevision; }
        public int getIndexRevision() { return indexRevision; }
        public void setIndexRevision(int indexRevision) { this.indexRevision = indexRevision; }
        public int indexRevision() { return indexRevision; }
    }
}
