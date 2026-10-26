package com.xqy.nexusedu.knowledge.catalog.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.util.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.xqy.nexusedu.knowledge.catalog.service.KnowledgeManagementService;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.knowledge.ingestion.parser.DocumentChunker;
import com.xqy.nexusedu.knowledge.catalog.service.KnowledgeManagementStore;

@Service
public class KnowledgeManagementServiceImpl implements KnowledgeManagementService {
    private final AuthorizationPolicy authorization;
    private final KnowledgeManagementStore store;
    private final DocumentChunker chunker;
    private final KnowledgeDocumentUploadService documentUpload;

    public KnowledgeManagementServiceImpl(DocumentChunker chunker, KnowledgeDocumentUploadService documentUpload, AuthorizationPolicy authorization, KnowledgeManagementStore store) {
        this.chunker = chunker;
        this.documentUpload = documentUpload;
        this.authorization = authorization;
        this.store = store;
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        authorize(resource, "read", caller);
        return store.list(resource, filters, current, size, caller);
    }

    @Override
    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> input, InternalCallContext caller) {
        authorize(resource, id == null ? "create" : "update", caller);
        if (input == null || !(resource.equals("bases") || resource.equals("documents")))
            throw new IllegalArgumentException("未知写操作");
        if (resource.equals("documents") && id == null) throw new IllegalArgumentException("请通过上传创建文档");
        Map<String, Object> previous = id == null ? Map.of() : store.owned(resource, id, caller.tenantId());
        Set<String> fields = resource.equals("bases") ? Set.of("name", "description", "embeddingModelAlias", "chunkSize", "chunkOverlap", "splitStrategy", "topk", "similarityThreshold", "enabled", "remark")
                : Set.of("documentName", "category", "keywords");
        Map<String, Object> values = new LinkedHashMap<>();
        if (id == null) {
            id = IdWorker.getId();
            values.put("id", id);
            values.put("tenant_id", caller.tenantId());
            values.put("create_by", caller.userId().toString());
            values.put("chunk_size", 800);
            values.put("chunk_overlap", 120);
            values.put("split_strategy", "PARAGRAPH");
            values.put("topk", 5);
            values.put("enabled", true);
        }
        for (var entry : input.entrySet()) {
            if (!fields.contains(entry.getKey()))
                throw new IllegalArgumentException("不允许写入字段: " + entry.getKey());
            Object value = entry.getValue();
            if (List.of("chunkSize", "chunkOverlap", "topk").contains(entry.getKey())) {
                try {
                    value = Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException("参数必须为整数");
                }
            } else if (entry.getKey().equals("enabled")) {
                if (!(value instanceof Boolean)) throw new IllegalArgumentException("启用状态必须为布尔值");
            } else if (entry.getKey().equals("similarityThreshold")) {
                value = value == null ? null : Double.parseDouble(value.toString());
                if (value != null && (!Double.isFinite((Double) value) || (Double) value < 0 || (Double) value > 1))
                    throw new IllegalArgumentException("相似度须为 0–1");
            } else {
                int maximum = switch (entry.getKey()) {
                    case "name", "embeddingModelAlias", "category" -> 128;
                    case "documentName" -> 255;
                    case "description", "remark" -> 500;
                    case "keywords" -> 4000;
                    default -> 32;
                };
                value = ManagementRows.text(value, maximum, entry.getKey().equals("name") || entry.getKey().equals("documentName"));
            }
            values.put(snake(entry.getKey()), value);
        }
        if (resource.equals("bases")) {
            if (id != null && previous.isEmpty() && !values.containsKey("name"))
                throw new IllegalArgumentException("名称不能为空");
            Map<String, Object> effective = new HashMap<>(previous);
            effective.putAll(values);
            chunker.validate(((Number) effective.get("chunk_size")).intValue(), ((Number) effective.get("chunk_overlap")).intValue(), effective.get("split_strategy").toString());
            int topk = ((Number) effective.get("topk")).intValue();
            if (topk < 1 || topk > 100) throw new IllegalArgumentException("检索条数须为 1–100");
        }
        values.put("modify_by", caller.userId().toString());
        try {
            if (previous.isEmpty()) store.insert(store.table(resource), values);
            else store.update(store.table(resource), id, caller.tenantId(), values);
        } catch (DataIntegrityViolationException error) {
            throw new ManagementConflictException("名称重复或字段不符合表结构");
        }
        return ManagementRows.publicRow(store.publicRow(resource, id, caller.tenantId()));
    }

    @Override
    @Transactional
    public void delete(String resource, Long id, InternalCallContext caller) {
        authorize(resource, "delete", caller);
        store.owned(resource, id, caller.tenantId());
        if (resource.equals("bases")) {
            if (store.countDocumentsForBase(caller.tenantId(), id) > 0)
                throw new ManagementConflictException("知识库仍有文档，可先禁用知识库或删除文档");
            store.deleteBase(caller.tenantId(), id);
        } else if (resource.equals("documents")) {
            if (store.countPendingJobsForDocument(caller.tenantId(), id) > 0)
                throw new ManagementConflictException("请先取消或完成此文档的入库任务");
            store.softDeleteDocument(caller.tenantId(), id, caller.userId().toString());
        } else throw new IllegalArgumentException("不能直接删除版本、分块或任务记录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(Long knowledgeBaseId, Long documentId, String filename, byte[] bytes, InternalCallContext caller) {
        return documentUpload.upload(knowledgeBaseId, documentId, filename, bytes, caller);
    }

    @Override
    @Transactional
    public void retryJob(Long id, InternalCallContext caller) {
        authorize("jobs", "retry", caller);
        Map<String, Object> job = store.owned("jobs", id, caller.tenantId());
        if (!List.of("FAILED", "CANCELLED").contains(job.get("status")))
            throw new ManagementConflictException("仅失败或取消的任务可以重试");
        Long versionId = Long.valueOf(job.get("document_version_id").toString());
        if (store.countActiveVersion(caller.tenantId(), versionId) != 1)
            throw new ManagementConflictException("文档已删除或知识库已禁用");
        store.retryJob(caller.tenantId(), id);
        store.retryVersion(caller.tenantId(), versionId, Integer.parseInt(job.get("index_revision").toString()));
    }

    @Override
    @Transactional
    public void cancelJob(Long id, InternalCallContext caller) {
        authorize("jobs", "cancel", caller);
        store.owned("jobs", id, caller.tenantId());
        store.cancelJob(caller.tenantId(), id);
    }

    private String snake(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private void authorize(String resource, String action, InternalCallContext caller) {
        String capability = switch (resource) {
            case "bases" -> "base";
            case "documents" -> "document";
            case "versions" -> "version";
            case "jobs" -> "job";
            case "chunks" -> "chunk";
            default -> throw new IllegalArgumentException("未知管理对象");
        };
        authorization.authorize("knowledge:" + capability + ":" + action, caller);
    }

}
