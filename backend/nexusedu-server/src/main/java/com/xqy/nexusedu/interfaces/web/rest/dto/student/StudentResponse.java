package com.xqy.nexusedu.interfaces.web.rest.dto.student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.xqy.nexusedu.application.student.model.StudentData;

/** HTTP response contract for a student. */
public record StudentResponse(
        Long id,
        String studentNo,
        String name,
        Integer age,
        String gender,
        LocalDate birthday,
        String email,
        String phone,
        LocalDateTime enrolledAt) {
    public static StudentResponse from(StudentData student) {
        if (student == null) return null;
        return new StudentResponse(student.id(), student.studentNo(), student.name(), student.age(),
                student.gender(), student.birthday(), student.email(), student.phone(), student.enrolledAt());
    }
}
