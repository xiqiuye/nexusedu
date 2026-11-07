package com.xqy.nexusedu.education.assessment.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateExamRequest(String name, Long academicYearId, LocalDate examDate) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("academicYearId", academicYearId);
        values.put("examDate", examDate);
        return values;
    }
}
