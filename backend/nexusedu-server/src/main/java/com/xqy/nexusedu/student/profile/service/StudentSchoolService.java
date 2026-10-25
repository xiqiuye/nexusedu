package com.xqy.nexusedu.student.profile.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface StudentSchoolService {
    List<Map<String, Object>> campuses(InternalCallContext caller);
    Map<String, Object> createCampus(Map<String, Object> input, InternalCallContext caller);
    List<Map<String, Object>> academicYears(InternalCallContext caller);
    Map<String, Object> createAcademicYear(Map<String, Object> input, InternalCallContext caller);
    List<Map<String, Object>> classes(Long yearId, Long campusId, InternalCallContext caller);
    Map<String, Object> createClass(Map<String, Object> input, InternalCallContext caller);
    PageResult students(Map<String, String> filter, int current, int size, InternalCallContext caller);
    Map<String, Object> createStudent(Map<String, Object> input, InternalCallContext caller);
    Map<String, Object> importStudents(List<Map<String, Object>> input, InternalCallContext caller);
    void moveStudent(Long studentId, Map<String, Object> input, InternalCallContext caller);
    void changeEnrollmentStatus(long studentId, Map<String, Object> input, InternalCallContext caller);
    void assignHomeroom(long classId, long userId, LocalDate start, InternalCallContext caller);
    void assignCampusAdmin(long campusId, long userId, InternalCallContext caller);
    void assignTeacher(long classId, long userId, InternalCallContext caller);
}
