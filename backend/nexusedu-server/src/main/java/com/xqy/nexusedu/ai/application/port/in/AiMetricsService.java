package com.xqy.nexusedu.ai.application.port.in;

import java.util.Map;

import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;

/**
 * Tenant-scoped persisted AI statistics, independent of external monitoring infrastructure.
 */
public interface AiMetricsService {
    Map<String, Object> overview(String startDate, String endDate, InternalCallContext caller);

    PageResult calls(Map<String, String> filters, int current, int size, InternalCallContext caller);

    PageResult prices(int current, int size, InternalCallContext caller);

    Map<String, Object> createPrice(Map<String, Object> input, InternalCallContext caller);
}
