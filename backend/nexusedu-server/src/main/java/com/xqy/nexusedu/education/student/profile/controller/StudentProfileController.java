package com.xqy.nexusedu.education.student.profile.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;
import com.xqy.nexusedu.education.student.profile.service.StudentProfileService;

@RestController
@RequestMapping("/api/school/students")
public class StudentProfileController {
    private final StudentProfileService service;
    private final RpcCaller rpc;
    public StudentProfileController(StudentProfileService service, RpcCaller rpc) { this.service = service; this.rpc = rpc; }
    @GetMapping public ApiResponse<PageResult> students(@RequestParam Map<String, String> filters, @RequestParam(defaultValue="1") int current, @RequestParam(defaultValue="20") int size, Authentication a) { return ApiResponse.success(service.students(filters, current, size, rpc.context(a))); }
    @PostMapping public ApiResponse<?> create(@RequestBody Map<String, Object> body, Authentication a) { return ApiResponse.success(service.createStudent(body, rpc.context(a))); }
    @PostMapping("/import") public ApiResponse<?> importStudents(@RequestBody List<Map<String, Object>> rows, Authentication a) { return ApiResponse.success(service.importStudents(rows, rpc.context(a))); }
    @PostMapping("/{studentId}/move") public ApiResponse<Void> move(@PathVariable long studentId, @RequestBody Map<String, Object> body, Authentication a) { service.moveStudent(studentId, body, rpc.context(a)); return ApiResponse.success(null); }
    @PutMapping("/{studentId}/status") public ApiResponse<Void> status(@PathVariable long studentId, @RequestBody Map<String, Object> body, Authentication a) { service.changeEnrollmentStatus(studentId, body, rpc.context(a)); return ApiResponse.success(null); }
}
