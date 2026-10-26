package com.xqy.nexusedu.knowledge.catalog.service;

import java.util.Map;

import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface KnowledgeManagementService {

    PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller);

    Map<String, Object> save(String resource, Long id, Map<String, Object> values, InternalCallContext caller);

    void delete(String resource, Long id, InternalCallContext caller);

    Map<String, Object> upload(Long knowledgeBaseId, Long documentId, String filename, byte[] bytes, InternalCallContext caller);

    void retryJob(Long jobId, InternalCallContext caller);

    void cancelJob(Long jobId, InternalCallContext caller);
}
