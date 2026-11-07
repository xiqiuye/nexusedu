package com.xqy.nexusedu.education.assessment.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record SaveScoreRequest(Long examId, Long studentId, Long subjectId, BigDecimal score) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("examId", examId);
        values.put("studentId", studentId);
        values.put("subjectId", subjectId);
        values.put("score", score);
        return values;
    }
}
