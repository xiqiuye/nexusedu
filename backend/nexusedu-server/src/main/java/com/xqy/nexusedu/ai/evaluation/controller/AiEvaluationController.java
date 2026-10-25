package com.xqy.nexusedu.ai.evaluation.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.ai.evaluation.service.AiEvaluationService;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.ai.evaluation.dto.AiEvaluationDtos;
import com.xqy.nexusedu.system.authentication.infrastructure.RpcCaller;

@RestController
@RequestMapping("/api/ai/evaluation")
public class AiEvaluationController {
    @Autowired
    private AiEvaluationService service;
    private final RpcCaller caller;

    public AiEvaluationController(RpcCaller caller) {
        this.caller = caller;
    }

    @GetMapping("/{resource}")
    public ApiResponse<PageResult> list(@PathVariable String resource, @RequestParam Map<String, String> filters, @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(service.list(resource, filters, current, size, caller.context(auth)));
    }

    @PostMapping("/cases")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> input, Authentication auth) {
        return ApiResponse.success(service.addCase(input, caller.context(auth)));
    }

    @PutMapping("/cases/{id}/enabled")
    public ApiResponse<Void> enable(@PathVariable Long id, @RequestBody AiEvaluationDtos.Enable input, Authentication auth) {
        service.enableCase(id, input.enabled(), caller.context(auth));
        return ApiResponse.success(null);
    }

    @PostMapping("/batches")
    public ApiResponse<Map<String, Object>> run(@RequestBody Map<String, Object> input, Authentication auth) {
        return ApiResponse.success(service.run(input, caller.context(auth)));
    }

    @GetMapping("/batches/{id}/summary")
    public ApiResponse<Map<String, Object>> summary(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(service.summary(id, caller.context(auth)));
    }

    @PutMapping("/schedules")
    public ApiResponse<Void> schedule(@RequestBody Map<String, Object> input, Authentication auth) {
        service.schedule(input, caller.context(auth));
        return ApiResponse.success(null);
    }

}
