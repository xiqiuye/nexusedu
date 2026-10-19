package com.xqy.nexusedu.ai.application.model;

import java.io.Serializable;

/** Request DTOs for AI governance. */
public final class AiGovernanceDtos {
    private AiGovernanceDtos() {
    }

    public static class Review implements Serializable {
        private static final long serialVersionUID = 1L;
        private String note;
        public Review() { }
        public Review(String note) { this.note = note; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String note() { return note; }
    }
}
