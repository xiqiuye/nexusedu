package com.xqy.nexusedu.education.enrollment.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.system.authentication.service.RpcCaller;
import com.xqy.nexusedu.education.enrollment.service.EnrollmentService;

@RestController
@RequestMapping("/api/school/promotions")
public class EnrollmentController {
    private final EnrollmentService service;
    private final RpcCaller rpc;

    public EnrollmentController(EnrollmentService service, RpcCaller rpc) {
        this.service = service;
        this.rpc = rpc;
    }

    @PostMapping("/preview")
    public ApiResponse<?> preview(@RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.previewPromotion(b, rpc.context(a)));
    }

    @PostMapping("/confirm")
    public ApiResponse<?> confirm(@RequestBody Map<String, Object> b, Authentication a) {
        return ApiResponse.success(service.confirmPromotion(b, rpc.context(a)));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<?> batch(@PathVariable long batchId, Authentication a) {
        return ApiResponse.success(service.promotionBatch(batchId, rpc.context(a)));
    }
}
