package com.xqy.nexusedu.education.organization.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateAcademicYearRequest(String name, LocalDate startsOn, LocalDate endsOn, Boolean current) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("startsOn", startsOn == null ? null : startsOn.toString());
        values.put("endsOn", endsOn == null ? null : endsOn.toString());
        values.put("current", current);
        return values;
    }
}
