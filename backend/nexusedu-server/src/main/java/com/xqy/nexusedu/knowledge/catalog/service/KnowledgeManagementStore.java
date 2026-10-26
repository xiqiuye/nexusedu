package com.xqy.nexusedu.knowledge.catalog.service;

import java.util.Map;
import org.springframework.stereotype.Repository;
import com.xqy.nexusedu.knowledge.catalog.mapper.KnowledgeManagementMapper;
import com.xqy.nexusedu.knowledge.catalog.service.KnowledgeManagementResources;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Repository
public class KnowledgeManagementStore {
    private final KnowledgeManagementMapper mapper;
    public KnowledgeManagementStore(KnowledgeManagementMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        filters = filters == null ? Map.of() : filters;
        if (!KnowledgeManagementResources.TABLES.containsKey(resource)) throw new IllegalArgumentException("未知管理对象");
        String parentField = switch (resource) {
            case "documents" -> "knowledgeBaseId";
            case "versions" -> "documentId";
            case "jobs", "chunks" -> "documentVersionId";
            default -> null;
        };
        Long parentId = parentField == null || filters.get(parentField) == null || filters.get(parentField).isBlank()
                ? null : ManagementRows.id(filters.get(parentField));
        String search = ManagementRows.text(filters.get("search"), 128, false);
        current = Math.max(1, current);
        size = Math.max(1, Math.min(200, size));
        Long total = mapper.countPage(resource, caller.tenantId(), parentId, search);
        return new PageResult(ManagementRows.publicRows(mapper.selectPage(resource, caller.tenantId(), parentId, search,
                size, ((long) current - 1) * size)), current, size, total == null ? 0 : total);
    }

    public Map<String, Object> owned(String resource, Long id, String tenantId) {
        ManagementRows.id(id);
        if (!KnowledgeManagementResources.TABLES.containsKey(resource)) throw new IllegalArgumentException("未知管理对象");
        Map<String, Object> row = mapper.selectOwned(resource, id, tenantId);
        if (row == null) throw new ManagementNotFoundException();
        return row;
    }

    public String table(String resource) {
        String table = KnowledgeManagementResources.TABLES.get(resource);
        if (table == null) throw new IllegalArgumentException("未知管理对象");
        return table;
    }

    public Map<String, Object> publicRow(String resource, Long id, String tenantId) {
        return mapper.selectPublic(resource, id, tenantId);
    }

    public int countDocumentsForBase(String tenantId, Long baseId) { return mapper.countDocumentsForBase(tenantId, baseId); }
    public int countPendingJobsForDocument(String tenantId, Long documentId) { return mapper.countPendingJobsForDocument(tenantId, documentId); }
    public int countActiveVersion(String tenantId, Long versionId) { return mapper.countActiveVersion(tenantId, versionId); }
    public int deleteBase(String tenantId, Long baseId) { return mapper.deleteBase(tenantId, baseId); }
    public int softDeleteDocument(String tenantId, Long documentId, String modifyBy) { return mapper.softDeleteDocument(tenantId, documentId, modifyBy); }
    public int retryJob(String tenantId, Long jobId) { return mapper.retryJob(tenantId, jobId); }
    public int retryVersion(String tenantId, Long versionId, Integer revision) { return mapper.retryVersion(tenantId, versionId, revision); }
    public int cancelJob(String tenantId, Long jobId) { return mapper.cancelJob(tenantId, jobId); }

    public void insert(String table, Map<String, Object> values) {
        mapper.insertResource(tableResource(table), values);
    }

    public void update(String table, Long id, String tenant, Map<String, Object> values) {
        mapper.updateResource(tableResource(table), id, tenant, values);
    }

    private String tableResource(String table) {
        return KnowledgeManagementResources.TABLES.entrySet().stream().filter(entry -> entry.getValue().equals(table))
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalArgumentException("未知管理对象"));
    }

}
