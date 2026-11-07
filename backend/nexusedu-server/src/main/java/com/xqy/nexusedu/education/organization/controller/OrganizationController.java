package com.xqy.nexusedu.education.organization.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;
import com.xqy.nexusedu.education.organization.service.OrganizationService;
import com.xqy.nexusedu.education.organization.dto.CreateAcademicYearRequest;
import com.xqy.nexusedu.education.organization.dto.CreateCampusRequest;

@RestController
@RequestMapping("/api/school")
public class OrganizationController {
    private final OrganizationService service;
    private final RpcCaller rpc;

    public OrganizationController(OrganizationService service, RpcCaller rpc) {
        this.service = service;
        this.rpc = rpc;
    }

    @GetMapping("/campuses")
    public ApiResponse<?> campuses(Authentication a) {
        return ApiResponse.success(service.campuses(rpc.context(a)));
    }

    @PostMapping("/campuses")
    public ApiResponse<?> campus(@RequestBody CreateCampusRequest request, Authentication a) {
        return ApiResponse.success(service.createCampus(request.toMap(), rpc.context(a)));
    }

    @PutMapping("/campuses/{campusId}")
    public ApiResponse<?> updateCampus(@PathVariable long campusId, @RequestBody CreateCampusRequest request, Authentication a) {
        return ApiResponse.success(service.updateCampus(campusId, request.toMap(), rpc.context(a)));
    }

    @DeleteMapping("/campuses/{campusId}")
    public ApiResponse<Void> deleteCampus(@PathVariable long campusId, Authentication a) {
        service.deleteCampus(campusId, rpc.context(a));
        return ApiResponse.success(null);
    }

    @GetMapping("/academic-years")
    public ApiResponse<?> years(Authentication a) {
        return ApiResponse.success(service.academicYears(rpc.context(a)));
    }

    @PostMapping("/academic-years")
    public ApiResponse<?> year(@RequestBody CreateAcademicYearRequest request, Authentication a) {
        return ApiResponse.success(service.createAcademicYear(request.toMap(), rpc.context(a)));
    }

    @GetMapping("/classes")
    public ApiResponse<?> classes(@RequestParam(required = false) Long academicYearId, @RequestParam(required = false) Long campusId, Authentication a) {
        return ApiResponse.success(service.classes(academicYearId, campusId, rpc.context(a)));
    }

    @PostMapping("/classes")
    public ApiResponse<?> createClass(@RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.createClass(b, rpc.context(a)));
    }

    @GetMapping("/classes/{classId}")
    public ApiResponse<?> classDetail(@PathVariable long classId, Authentication a) {
        return ApiResponse.success(service.classDetail(classId, rpc.context(a)));
    }

    @PutMapping("/classes/{classId}")
    public ApiResponse<?> updateClass(@PathVariable long classId, @RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.updateClass(classId, b, rpc.context(a)));
    }

    @PutMapping("/classes/{classId}/status")
    public ApiResponse<Void> classStatus(@PathVariable long classId, @RequestBody Map<String, Object> b, Authentication a) {
        service.updateClassStatus(classId, String.valueOf(b.get("status")), rpc.context(a));
        return ApiResponse.success(null);
    }

    @PostMapping("/classes/{classId}/students/import")
    public ApiResponse<?> rosterImport(@PathVariable long classId, @RequestBody List<Map<String, Object>> rows, Authentication a) {
        return ApiResponse.success(service.importClassRoster(classId, rows, rpc.context(a)));
    }

    @PutMapping("/classes/{classId}/homeroom")
    public ApiResponse<Void> homeroom(@PathVariable long classId, @RequestBody Map<String, Object> b, Authentication a) {
        service.assignHomeroom(classId, Long.parseLong(b.get("userId").toString()), b.get("startsOn") == null ? LocalDate.now() : LocalDate.parse(b.get("startsOn").toString()), rpc.context(a));
        return ApiResponse.success(null);
    }

    @PutMapping("/campuses/{campusId}/admins")
    public ApiResponse<Void> campusAdmin(@PathVariable long campusId, @RequestBody Map<String, Object> b, Authentication a) {
        service.assignCampusAdmin(campusId, Long.parseLong(b.get("userId").toString()), rpc.context(a));
        return ApiResponse.success(null);
    }

    @PutMapping("/classes/{classId}/teachers")
    public ApiResponse<Void> teacher(@PathVariable long classId, @RequestBody Map<String, Object> b, Authentication a) {
        service.assignClassTeacher(classId, b, rpc.context(a));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/classes/{classId}/teachers/{assignmentId}")
    public ApiResponse<Void> removeTeacher(@PathVariable long classId, @PathVariable long assignmentId, Authentication a) {
        service.removeClassTeacher(classId, assignmentId, rpc.context(a));
        return ApiResponse.success(null);
    }
}
