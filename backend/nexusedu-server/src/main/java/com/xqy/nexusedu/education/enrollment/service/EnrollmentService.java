package com.xqy.nexusedu.education.enrollment.service;

import java.util.Map;

import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface EnrollmentService {
    Map<String, Object> previewPromotion(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> confirmPromotion(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> promotionBatch(long batchId, InternalCallContext caller);
}
