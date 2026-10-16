package com.xqy.nexusedu.application.student.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application-level student data. It is independent from the persistence model.
 */
public record StudentData(
        Long id,
        String studentNo,
        String name,
        Integer age,
        String gender,
        LocalDate birthday,
        String email,
        String phone,
        LocalDateTime enrolledAt) {
}
