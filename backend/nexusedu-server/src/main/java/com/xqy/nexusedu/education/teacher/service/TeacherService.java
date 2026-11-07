package com.xqy.nexusedu.education.teacher.service;

import java.util.List;
import java.util.Map;

import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface TeacherService {
    List<Map<String, Object>> teachers(String search, InternalCallContext caller);

    List<Map<String, Object>> teacherCandidates(String search, InternalCallContext caller);

    Map<String, Object> createTeacher(Map<String, Object> input, InternalCallContext caller);

    List<Map<String, Object>> subjects(Long grade, InternalCallContext caller);

    Map<String, Object> createSubject(Map<String, Object> input, InternalCallContext caller);
}
