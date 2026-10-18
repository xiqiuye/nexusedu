package com.xqy.nexusedu.interfaces.web.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xqy.nexusedu.application.ai.port.in.AiOperationsService;
import com.xqy.nexusedu.application.knowledge.port.in.AiKnowledgeCommandService;
import com.xqy.nexusedu.common.dto.ApiResponse;
import com.xqy.nexusedu.common.dto.PageResult;
import com.xqy.nexusedu.common.dto.ai.AiOperationsDtos;
import com.xqy.nexusedu.infrastructure.system.RpcCaller;

@RestController
@RequestMapping("/api/ai")
public class AiOperationsController {
    @Autowired
    private AiOperationsService queries;
    @Autowired
    private AiKnowledgeCommandService knowledge;
    private final RpcCaller caller;

    public AiOperationsController(RpcCaller caller) {
        this.caller = caller;
    }

    @PostMapping("/conversations")
    public ApiResponse<Map<String, Object>> createConversation(@RequestBody AiOperationsDtos.ConversationInput input, Authentication auth) {
        return ApiResponse.success(queries.createConversation(input.title(), caller.context(auth)));
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id, Authentication auth) {
        queries.deleteConversation(id, caller.context(auth));
        return ApiResponse.success(null);
    }

    @GetMapping("/conversations/retention")
    public ApiResponse<Map<String, Object>> retention(Authentication auth) {
        return ApiResponse.success(queries.retention(caller.context(auth)));
    }

    @PutMapping("/conversations/retention")
    public ApiResponse<Void> retention(@RequestBody AiOperationsDtos.RetentionInput input, Authentication auth) {
        queries.updateRetention(input.retentionDays(), caller.context(auth));
        return ApiResponse.success(null);
    }

    @PutMapping("/conversations/{id}/retention")
    public ApiResponse<Void> keep(@PathVariable Long id, @RequestBody AiOperationsDtos.KeepInput input, Authentication auth) {
        queries.keepConversation(id, input.retainUntil(), caller.context(auth));
        return ApiResponse.success(null);
    }

    @GetMapping("/manage/{resource}")
    public ApiResponse<PageResult> list(@PathVariable String resource, @RequestParam Map<String, String> filters,
                                        @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(queries.list(resource, filters, current, size, caller.context(auth)));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<PageResult> messages(@PathVariable Long id, @RequestParam(defaultValue = "1") int current,
                                            @RequestParam(defaultValue = "100") int size, Authentication auth) {
        return ApiResponse.success(queries.messages(id, current, size, caller.context(auth)));
    }

    @GetMapping("/runs/{runId}/snapshot")
    public ApiResponse<Map<String, Object>> snapshot(@PathVariable String runId, Authentication auth) {
        Map<String, Object> delivery = queries.delivery(runId, caller.context(auth));
        delivery.remove("streamKey");
        return ApiResponse.success(delivery);
    }

    @PostMapping("/knowledge/versions/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable Long id, @RequestBody AiOperationsDtos.IndexRevision input, Authentication auth) {
        knowledge.activateIndex(id, input.indexRevision(), caller.context(auth));
        return ApiResponse.success(null);
    }

}
