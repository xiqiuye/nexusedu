package com.xqy.nexusedu.ai.interfaces.web.rest;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.ai.application.port.in.AiGovernanceService;
import com.xqy.nexusedu.shared.dto.ApiResponse;
import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.ai.application.model.AiGovernanceDtos;
import com.xqy.nexusedu.system.infrastructure.RpcCaller;

@RestController @RequestMapping("/api/ai/config")
public class AiGovernanceController {
    @Autowired private AiGovernanceService service;
    private final RpcCaller caller;
    public AiGovernanceController(RpcCaller caller) {this.caller=caller;}
    @GetMapping("/{resource}") public ApiResponse<PageResult> list(@PathVariable String resource,@RequestParam Map<String,String> filters,@RequestParam(defaultValue="1") int current,@RequestParam(defaultValue="20") int size,Authentication auth) {return ApiResponse.success(service.list(resource,filters,current,size,caller.context(auth)));}
    @PostMapping("/versions") public ApiResponse<Map<String,Object>> draft(@RequestBody Map<String,Object> input,Authentication auth) {return ApiResponse.success(service.draft(input,caller.context(auth)));}
    @PostMapping("/versions/{id}/{action}") public ApiResponse<Void> transition(@PathVariable Long id,@PathVariable String action,@RequestBody AiGovernanceDtos.Review input,Authentication auth) {service.transition(id,action,input.note(),caller.context(auth));return ApiResponse.success(null);}
    @PutMapping("/rollouts") public ApiResponse<Void> rollout(@RequestBody Map<String,Object> input,Authentication auth) {service.rollout(input,caller.context(auth));return ApiResponse.success(null);}
    @PutMapping("/policies") public ApiResponse<Void> policy(@RequestBody Map<String,Object> input,Authentication auth) {service.policy(input,caller.context(auth));return ApiResponse.success(null);}
}
