package com.xqy.nexusedu.education.teacher.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;
import com.xqy.nexusedu.education.teacher.service.TeacherService;

@RestController
@RequestMapping("/api/school")
public class TeacherController {
    private final TeacherService service;
    private final RpcCaller rpc;

    public TeacherController(TeacherService service, RpcCaller rpc) {
        this.service = service;
        this.rpc = rpc;
    }

    @GetMapping("/teachers")
    public ApiResponse<?> teachers(@RequestParam(required = false) String search, Authentication a) {
        return ApiResponse.success(service.teachers(search, rpc.context(a)));
    }

    @GetMapping("/teacher-candidates")
    public ApiResponse<?> candidates(@RequestParam(required = false) String search, Authentication a) {
        return ApiResponse.success(service.teacherCandidates(search, rpc.context(a)));
    }

    @PostMapping("/teachers")
    public ApiResponse<?> create(@RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.createTeacher(b, rpc.context(a)));
    }

    @GetMapping("/subjects")
    public ApiResponse<?> subjects(@RequestParam(required = false) Long grade, Authentication a) {
        return ApiResponse.success(service.subjects(grade, rpc.context(a)));
    }

    @PostMapping("/subjects")
    public ApiResponse<?> createSubject(@RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.createSubject(b, rpc.context(a)));
    }
}
