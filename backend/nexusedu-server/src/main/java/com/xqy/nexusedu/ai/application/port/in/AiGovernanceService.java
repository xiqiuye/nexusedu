package com.xqy.nexusedu.ai.application.port.in;

import java.util.Map;

import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;

public interface AiGovernanceService {
    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);

    Map<String, Object> draft(Map<String, Object> input, InternalCallContext caller);

    void transition(Long id, String action, String note, InternalCallContext caller);

    void rollout(Map<String, Object> input, InternalCallContext caller);

    void policy(Map<String, Object> input, InternalCallContext caller);
}
