package com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.xqy.nexusedu.knowledge.catalog.application.service.KnowledgeManagementResources;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Repository
public class KnowledgeManagementStore {
    private final JdbcTemplate jdbc;
    private final KnowledgeManagementMapper mapper;
    public KnowledgeManagementStore(JdbcTemplate jdbc, KnowledgeManagementMapper mapper) {
        this.jdbc = jdbc;
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

    public int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

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
