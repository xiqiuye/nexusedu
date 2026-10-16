package com.xqy.nexusedu.application.student.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application command used by student create/update use cases.
 */
public record StudentCommand(
        String studentNo,
        String name,
        Integer age,
        String gender,
        LocalDate birthday,
        String email,
        String phone,
        LocalDateTime enrolledAt) {
}
