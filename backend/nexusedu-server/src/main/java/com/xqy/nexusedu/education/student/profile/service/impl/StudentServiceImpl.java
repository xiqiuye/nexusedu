package com.xqy.nexusedu.education.student.profile.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.education.student.profile.dto.StudentCommand;
import com.xqy.nexusedu.education.student.profile.vo.StudentData;
import com.xqy.nexusedu.education.student.profile.service.StudentService;
import com.xqy.nexusedu.education.student.profile.entity.Student;
import com.xqy.nexusedu.education.student.profile.mapper.StudentMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;

/** Persistence-backed implementation of the student application port. */
@Service
@Transactional(rollbackFor = Exception.class)
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private SysAuthorizationMapper authorizationMapper;

    @Override
    public boolean saveStudent(StudentCommand command) {
        return studentMapper.insert(toEntity(command, null)) > 0;
    }

    @Override
    public boolean updateStudent(Long id, StudentCommand command) {
        return studentMapper.updateById(toEntity(command, id)) > 0;
    }

    @Override
    public boolean deleteStudent(Long id) {
        return studentMapper.deleteById(id) > 0;
    }

    @Override
    public StudentData getStudentById(Long id) {
        return toData(studentMapper.selectById(id));
    }

    @Override
    public List<StudentData> listStudents() {
        return studentMapper.selectList(null).stream().map(StudentServiceImpl::toData).collect(Collectors.toList());
    }

    @Override
    public List<StudentData> queryStudents(String name, String gender, String studentNo, Integer limit) {
        return List.of();
    }

    @Override
    public List<StudentData> queryStudents(String name, String gender, String studentNo, Integer limit, String tenantId, Long userId) {
        if(tenantId==null||tenantId.isBlank()||userId==null)return List.of();
        int safeLimit=limit==null||limit<=0?100:Math.min(limit,100);
        List<String> roles=authorizationMapper.findRoleCodes(userId);
        boolean schoolWide=roles.contains("R_SUPER")||roles.contains("R_SCHOOL_ADMIN");
        List<StudentData> list =  listStudents();
        return list;
//        return studentMapper.selectCurrentForAi(tenantId,userId,schoolWide,blankToNull(name),blankToNull(gender),blankToNull(studentNo),safeLimit)
//                .stream().map(StudentServiceImpl::toData).collect(Collectors.toList());
    }

    private static String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}

    private static Student toEntity(StudentCommand command, Long id) {
        if (command == null) {
            throw new IllegalArgumentException("student command must not be null");
        }
        Student entity = new Student();
        entity.setId(id);
        entity.setStudentNo(command.studentNo());
        entity.setName(command.name());
        entity.setAge(command.age());
        entity.setGender(command.gender());
        entity.setBirthday(command.birthday());
        entity.setEmail(command.email());
        entity.setPhone(command.phone());
        entity.setEnrolledAt(command.enrolledAt());
        return entity;
    }

    private static StudentData toData(Student entity) {
        if (entity == null) return null;
        return new StudentData(entity.getId(), entity.getStudentNo(), entity.getName(), entity.getAge(),
                entity.getGender(), entity.getBirthday(), entity.getEmail(), entity.getPhone(), entity.getEnrolledAt());
    }
}
