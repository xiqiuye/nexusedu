package com.xqy.nexusedu.interfaces.web.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.interfaces.web.rest.dto.student.StudentRequest;
import com.xqy.nexusedu.interfaces.web.rest.dto.student.StudentResponse;
import com.xqy.nexusedu.application.student.model.StudentCommand;
import com.xqy.nexusedu.application.student.service.StudentService;
import com.xqy.nexusedu.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody StudentRequest request) {
        return ApiResponse.success(studentService.saveStudent(toCommand(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody StudentRequest request) {
        return ApiResponse.success(studentService.updateStudent(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(studentService.deleteStudent(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(StudentResponse.from(studentService.getStudentById(id)));
    }

    @GetMapping("/getStudentList")
    public ApiResponse<List<StudentResponse>> list() {
        List<StudentResponse> students = studentService.listStudents().stream()
                .map(StudentResponse::from).collect(Collectors.toList());
        return ApiResponse.success(students);
    }

    private static StudentCommand toCommand(StudentRequest request) {
        if (request == null) throw new IllegalArgumentException("student request must not be null");
        return new StudentCommand(request.getStudentNo(), request.getName(), request.getAge(), request.getGender(),
                request.getBirthday(), request.getEmail(), request.getPhone(), request.getEnrolledAt());
    }
}
