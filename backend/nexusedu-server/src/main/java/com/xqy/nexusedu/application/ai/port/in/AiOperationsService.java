package com.xqy.nexusedu.application.ai.port.in;

import java.util.Map;

import com.xqy.nexusedu.common.dto.PageResult;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;

public interface AiOperationsService {
    void requireCaller(InternalCallContext caller);

    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);

    PageResult messages(Long conversationId, int current, int size, InternalCallContext caller);

    Map<String, Object> delivery(String runId, InternalCallContext caller);

    void requireConversation(Long id, InternalCallContext caller);

    Map<String, Object> createConversation(String title, InternalCallContext caller);

    void deleteConversation(Long id, InternalCallContext caller);

    Map<String, Object> retention(InternalCallContext caller);

    void updateRetention(int days, InternalCallContext caller);

    void keepConversation(Long id, String until, InternalCallContext caller);
}
