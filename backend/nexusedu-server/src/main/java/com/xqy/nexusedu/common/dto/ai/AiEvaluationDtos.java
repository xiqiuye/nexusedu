package com.xqy.nexusedu.common.dto.ai;

import java.io.Serializable;

/** Request DTOs for AI evaluation. */
public final class AiEvaluationDtos {
    private AiEvaluationDtos() {
    }

    public static class Enable implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean enabled;
        public Enable() { }
        public Enable(boolean enabled) { this.enabled = enabled; }
        public boolean isEnabled() { return enabled; }
        public boolean getEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean enabled() { return enabled; }
    }
}
