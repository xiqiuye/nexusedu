package com.xqy.nexusedu.interfaces.web.rest;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.xqy.nexusedu.application.knowledge.port.in.KnowledgeManagementService;
import com.xqy.nexusedu.common.dto.ApiResponse;
import com.xqy.nexusedu.common.dto.PageResult;
import com.xqy.nexusedu.infrastructure.system.RpcCaller;

@RestController
@RequestMapping("/api/knowledge/manage")
public class KnowledgeManagementController {
    @Autowired
    private KnowledgeManagementService management;
    private final RpcCaller caller;

    public KnowledgeManagementController(RpcCaller caller) {
        this.caller = caller;
    }

    @GetMapping("/{resource}")
    public ApiResponse<PageResult> list(@PathVariable String resource, @RequestParam Map<String, String> filters,
                                        @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(management.list(resource, filters, current, size, caller.context(auth)));
    }

    @PostMapping("/{resource}")
    public ApiResponse<Map<String, Object>> create(@PathVariable String resource, @RequestBody Map<String, Object> values, Authentication auth) {
        return ApiResponse.success(management.save(resource, null, values, caller.context(auth)));
    }

    @PutMapping("/{resource}/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable String resource, @PathVariable Long id, @RequestBody Map<String, Object> values, Authentication auth) {
        return ApiResponse.success(management.save(resource, id, values, caller.context(auth)));
    }

    @DeleteMapping("/{resource}/{id}")
    public ApiResponse<Void> delete(@PathVariable String resource, @PathVariable Long id, Authentication auth) {
        management.delete(resource, id, caller.context(auth));
        return ApiResponse.success(null);
    }

    @PostMapping("/documents/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam Long knowledgeBaseId, @RequestParam(required = false) Long documentId,
                                                   @RequestPart MultipartFile file, Authentication auth) throws IOException {
        if (file.isEmpty() || file.getSize() > 4 * 1024 * 1024)
            throw new IllegalArgumentException("文件大小须为 1 字节到 4MB");
        return ApiResponse.success(management.upload(knowledgeBaseId, documentId, file.getOriginalFilename(), file.getBytes(), caller.context(auth)));
    }

    @PostMapping("/jobs/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long id, Authentication auth) {
        management.retryJob(id, caller.context(auth));
        return ApiResponse.success(null);
    }

    @PostMapping("/jobs/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, Authentication auth) {
        management.cancelJob(id, caller.context(auth));
        return ApiResponse.success(null);
    }
}
