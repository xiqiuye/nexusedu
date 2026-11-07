package com.xqy.nexusedu.education.assessment.service;

import java.util.List;
import java.util.Map;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface AssessmentService {
    List<Map<String, Object>> exams(Map<String, String> filters, InternalCallContext caller);
    Map<String, Object> createExam(Map<String, Object> input, InternalCallContext caller);
    List<Map<String, Object>> scores(long examId, Long classId, InternalCallContext caller);
    void saveScore(Map<String, Object> input, InternalCallContext caller);
}
