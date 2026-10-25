package com.xqy.nexusedu.ai.evaluation.service;

import java.util.Map;

import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface AiEvaluationService {
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);

    Map<String, Object> addCase(Map<String, Object> input, InternalCallContext caller);

    void enableCase(Long id, boolean enabled, InternalCallContext caller);

    Map<String, Object> run(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> summary(Long batchId, InternalCallContext caller);

    void schedule(Map<String, Object> input, InternalCallContext caller);
}
