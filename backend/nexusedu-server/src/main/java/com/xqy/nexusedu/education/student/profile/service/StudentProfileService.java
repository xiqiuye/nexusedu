package com.xqy.nexusedu.education.student.profile.service;

import java.util.List;
import java.util.Map;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface StudentProfileService {
    PageResult students(Map<String, String> filter, int current, int size, InternalCallContext caller);
    Map<String, Object> createStudent(Map<String, Object> input, InternalCallContext caller);
    Map<String, Object> importStudents(List<Map<String, Object>> input, InternalCallContext caller);
    void moveStudent(Long studentId, Map<String, Object> input, InternalCallContext caller);
    void changeEnrollmentStatus(long studentId, Map<String, Object> input, InternalCallContext caller);
}
