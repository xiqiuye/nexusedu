package com.xqy.nexusedu.ai.chat.application.model;

import java.io.Serializable;

import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.ai.task.domain.model.AiExecutionContext;

public class AiChatCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private AiExecutionContext executionContext;
    private InternalCallContext callerContext;

    public AiChatCommand() {
    }

    public AiChatCommand(AiExecutionContext executionContext, InternalCallContext callerContext) {
        this.executionContext = executionContext;
        this.callerContext = callerContext;
    }

    public AiExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(AiExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public InternalCallContext getCallerContext() {
        return callerContext;
    }

    public void setCallerContext(InternalCallContext callerContext) {
        this.callerContext = callerContext;
    }

    /**
     * Record-style accessors for RPC consumers.
     */
    public AiExecutionContext executionContext() {
        return executionContext;
    }

    public InternalCallContext callerContext() {
        return callerContext;
    }
}
