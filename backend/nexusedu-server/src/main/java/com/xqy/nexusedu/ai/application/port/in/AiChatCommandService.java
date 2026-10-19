package com.xqy.nexusedu.ai.application.port.in;

import java.util.Map;
import com.xqy.nexusedu.ai.application.model.AiChatCommand;
import com.xqy.nexusedu.ai.application.model.AiRunView;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;

public interface AiChatCommandService {
    /**
     * Accepts a chat command; provider events are appended to the command's Redis Stream key.
     */
    void submit(AiChatCommand command);

    Map<String, Object> submitRun(AiChatCommand command);

    void cancel(String runId, InternalCallContext callerContext);

    AiRunView getRun(String runId, InternalCallContext callerContext);
}
