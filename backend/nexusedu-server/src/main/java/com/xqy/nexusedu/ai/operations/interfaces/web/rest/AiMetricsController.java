package com.xqy.nexusedu.ai.operations.interfaces.web.rest;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.ai.operations.application.AiMetricsService;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.system.authentication.infrastructure.RpcCaller;

@RestController
@RequestMapping("/api/ai/metrics")
public class AiMetricsController {
    @Autowired private AiMetricsService metrics;
    private final RpcCaller caller;
    public AiMetricsController(RpcCaller caller) { this.caller = caller; }
    @GetMapping
    public ApiResponse<Map<String, Object>> overview(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, Authentication auth) {
        return ApiResponse.success(metrics.overview(startDate, endDate, caller.context(auth)));
    }
    @GetMapping("/calls")
    public ApiResponse<PageResult> calls(@RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(metrics.calls(filters, current, size, caller.context(auth)));
    }
    @GetMapping("/prices")
    public ApiResponse<PageResult> prices(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(metrics.prices(current, size, caller.context(auth)));
    }
    @PostMapping("/prices")
    public ApiResponse<Map<String, Object>> createPrice(@RequestBody Map<String, Object> input, Authentication auth) {
        return ApiResponse.success(metrics.createPrice(input, caller.context(auth)));
    }
}
