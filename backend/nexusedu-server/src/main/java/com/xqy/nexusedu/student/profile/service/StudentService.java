package com.xqy.nexusedu.student.profile.service;

import java.util.List;

import com.xqy.nexusedu.student.profile.dto.StudentCommand;
import com.xqy.nexusedu.student.profile.vo.StudentData;

/**
 * Application port for student use cases.
 */
public interface StudentService {

    boolean saveStudent(StudentCommand command);

    boolean updateStudent(Long id, StudentCommand command);

    boolean deleteStudent(Long id);

    StudentData getStudentById(Long id);

    List<StudentData> listStudents();

    /**
     * Query students by the fields supported by the AI student tool.
     */
    List<StudentData> queryStudents(String name, String gender, String studentNo, Integer limit);

    List<StudentData> queryStudents(String name, String gender, String studentNo, Integer limit, String tenantId, Long userId);
}
