package com.xqy.nexusedu.infrastructure.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.common.dto.student.StudentCommand;
import com.xqy.nexusedu.common.dto.student.StudentData;
import com.xqy.nexusedu.application.student.service.StudentService;
import com.xqy.nexusedu.common.entity.student.Student;
import com.xqy.nexusedu.infrastructure.student.persistence.mapper.StudentMapper;

/** Persistence-backed implementation of the student application port. */
@Service
@Transactional(rollbackFor = Exception.class)
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

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
        int safeLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
        LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
        query.like(name != null && !name.isBlank(), Student::getName, name)
                .eq(gender != null && !gender.isBlank(), Student::getGender, gender)
                .eq(studentNo != null && !studentNo.isBlank(), Student::getStudentNo, studentNo)
                .last("LIMIT " + safeLimit);
        return studentMapper.selectList(query).stream().map(StudentServiceImpl::toData).collect(Collectors.toList());
    }

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
