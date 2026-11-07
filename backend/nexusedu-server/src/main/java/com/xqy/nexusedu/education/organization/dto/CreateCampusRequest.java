package com.xqy.nexusedu.education.organization.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record CreateCampusRequest(String campusCode, String name, String address) {
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("campusCode", campusCode);
        values.put("name", name);
        values.put("address", address);
        return values;
    }
}
