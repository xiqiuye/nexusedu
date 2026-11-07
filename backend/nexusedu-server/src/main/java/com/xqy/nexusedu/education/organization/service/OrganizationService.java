package com.xqy.nexusedu.education.organization.service;

import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrganizationService {
    List<Map<String, Object>> campuses(InternalCallContext caller);

    Map<String, Object> createCampus(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> updateCampus(long campusId, Map<String, Object> input, InternalCallContext caller);

    void deleteCampus(long campusId, InternalCallContext caller);

    List<Map<String, Object>> academicYears(InternalCallContext caller);

    Map<String, Object> createAcademicYear(Map<String, Object> input, InternalCallContext caller);

    List<Map<String, Object>> classes(Long yearId, Long campusId, InternalCallContext caller);

    Map<String, Object> createClass(Map<String, Object> input, InternalCallContext caller);

    Map<String, Object> classDetail(long classId, InternalCallContext caller);

    Map<String, Object> updateClass(long classId, Map<String, Object> input, InternalCallContext caller);

    void updateClassStatus(long classId, String status, InternalCallContext caller);

    void assignClassTeacher(long classId, Map<String, Object> input, InternalCallContext caller);

    void removeClassTeacher(long classId, long assignmentId, InternalCallContext caller);

    Map<String, Object> importClassRoster(long classId, List<Map<String, Object>> input, InternalCallContext caller);

    void assignHomeroom(long classId, long userId, LocalDate start, InternalCallContext caller);

    void assignCampusAdmin(long campusId, long userId, InternalCallContext caller);

    void assignTeacher(long classId, long userId, InternalCallContext caller);
}
