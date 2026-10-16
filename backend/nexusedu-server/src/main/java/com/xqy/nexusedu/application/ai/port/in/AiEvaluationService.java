package com.xqy.nexusedu.application.ai.port.in;

import java.util.Map;

import com.xqy.nexusedu.common.dto.PageResult;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;

public interface AiEvaluationService {
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);

    Map<String, Object> addCase(Map<String, Object> input, InternalCallContext caller);

    void enableCase(Long id, boolean enabled, InternalCallContext caller);

    Map<String, Object> run(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> summary(Long batchId, InternalCallContext caller);

    void schedule(Map<String, Object> input, InternalCallContext caller);
}
