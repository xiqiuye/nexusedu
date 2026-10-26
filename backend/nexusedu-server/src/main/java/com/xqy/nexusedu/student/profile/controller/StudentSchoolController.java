package com.xqy.nexusedu.student.profile.controller;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.student.profile.service.StudentSchoolService;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;

@RestController
@RequestMapping("/api/school")
public class StudentSchoolController {
    private final StudentSchoolService school;
    private final RpcCaller rpc;
    public StudentSchoolController(StudentSchoolService school,RpcCaller rpc){this.school=school;this.rpc=rpc;}

    @GetMapping("/campuses") public ApiResponse<?> campuses(Authentication a){return ApiResponse.success(school.campuses(context(a)));}
    @PostMapping("/campuses") public ApiResponse<?> campus(@RequestBody Map<String,Object> body,Authentication a){return ApiResponse.success(school.createCampus(body,context(a)));}
    @GetMapping("/academic-years") public ApiResponse<?> years(Authentication a){return ApiResponse.success(school.academicYears(context(a)));}
    @PostMapping("/academic-years") public ApiResponse<?> year(@RequestBody Map<String,Object> body,Authentication a){return ApiResponse.success(school.createAcademicYear(body,context(a)));}
    @GetMapping("/classes") public ApiResponse<?> classes(@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) Long campusId,Authentication a){return ApiResponse.success(school.classes(academicYearId,campusId,context(a)));}
    @PostMapping("/classes") public ApiResponse<?> createClass(@RequestBody Map<String,Object> body,Authentication a){return ApiResponse.success(school.createClass(body,context(a)));}
    @PutMapping("/classes/{classId}/homeroom") public ApiResponse<Void> homeroom(@PathVariable long classId,@RequestBody Map<String,Object> body,Authentication a){school.assignHomeroom(classId,Long.parseLong(body.get("userId").toString()),body.get("startsOn")==null?LocalDate.now():LocalDate.parse(body.get("startsOn").toString()),context(a));return ApiResponse.success(null);}
    @PutMapping("/campuses/{campusId}/admins") public ApiResponse<Void> campusAdmin(@PathVariable long campusId,@RequestBody Map<String,Object> body,Authentication a){school.assignCampusAdmin(campusId,Long.parseLong(body.get("userId").toString()),context(a));return ApiResponse.success(null);}
    @PutMapping("/classes/{classId}/teachers") public ApiResponse<Void> teacher(@PathVariable long classId,@RequestBody Map<String,Object> body,Authentication a){school.assignTeacher(classId,Long.parseLong(body.get("userId").toString()),context(a));return ApiResponse.success(null);}
    @GetMapping("/students") public ApiResponse<PageResult> students(@RequestParam Map<String,String> filters,@RequestParam(defaultValue="1") int current,@RequestParam(defaultValue="20") int size,Authentication a){return ApiResponse.success(school.students(filters,current,size,context(a)));}
    @PostMapping("/students") public ApiResponse<?> createStudent(@RequestBody Map<String,Object> body,Authentication a){return ApiResponse.success(school.createStudent(body,context(a)));}
    @PostMapping("/students/import") public ApiResponse<?> importStudents(@RequestBody java.util.List<Map<String,Object>> rows,Authentication a){return ApiResponse.success(school.importStudents(rows,context(a)));}
    @PostMapping("/students/{studentId}/move") public ApiResponse<Void> move(@PathVariable long studentId,@RequestBody Map<String,Object> body,Authentication a){school.moveStudent(studentId,body,context(a));return ApiResponse.success(null);}
    @PutMapping("/students/{studentId}/status") public ApiResponse<Void> status(@PathVariable long studentId,@RequestBody Map<String,Object> body,Authentication a){school.changeEnrollmentStatus(studentId,body,context(a));return ApiResponse.success(null);}
    private InternalCallContext context(Authentication a){return rpc.context(a);}
}
