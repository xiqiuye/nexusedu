package com.xqy.nexusedu.education.assessment.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;
import com.xqy.nexusedu.education.assessment.service.AssessmentService;
import com.xqy.nexusedu.education.assessment.dto.CreateExamRequest;
import com.xqy.nexusedu.education.assessment.dto.SaveScoreRequest;

@RestController
@RequestMapping("/api/school")
public class AssessmentController {
    private final AssessmentService service;
    private final RpcCaller rpc;

    public AssessmentController(AssessmentService service, RpcCaller rpc) {
        this.service = service;
        this.rpc = rpc;
    }

    @GetMapping("/exams")
    public ApiResponse<?> exams(@RequestParam Map<String, String> filters, Authentication a) {
        return ApiResponse.success(service.exams(filters, rpc.context(a)));
    }

    @PostMapping("/exams")
    public ApiResponse<?> createExam(@RequestBody CreateExamRequest request, Authentication a) {
        return ApiResponse.success(service.createExam(request.toMap(), rpc.context(a)));
    }

    @GetMapping("/scores")
    public ApiResponse<?> scores(@RequestParam long examId, @RequestParam(required = false) Long classId, Authentication a) {
        return ApiResponse.success(service.scores(examId, classId, rpc.context(a)));
    }

    @PostMapping("/scores")
    public ApiResponse<Void> saveScore(@RequestBody SaveScoreRequest request, Authentication a) {
        service.saveScore(request.toMap(), rpc.context(a));
        return ApiResponse.success(null);
    }
}
