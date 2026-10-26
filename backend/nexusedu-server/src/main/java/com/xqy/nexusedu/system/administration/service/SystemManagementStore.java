package com.xqy.nexusedu.system.administration.service;

import java.util.Map;
import org.springframework.stereotype.Repository;
import com.xqy.nexusedu.system.administration.mapper.SystemManagementMapper;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

/** Persistence operations for the system administration resources. */
@Repository
public class SystemManagementStore {
    private final SystemManagementMapper mapper;

    public SystemManagementStore(SystemManagementMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller, boolean ownSessions) {
        filters = filters == null ? Map.of() : filters;
        String search = ManagementRows.text(filters.get("search"), 128, false);
        String status = filters.get("status");
        Long menuId = "buttons".equals(resource) && filters.get("menuId") != null && !filters.get("menuId").isBlank()
                ? ManagementRows.id(filters.get("menuId")) : null;
        Long userId = ownSessions ? caller.userId() : null;
        current = Math.max(1, current);
        size = Math.max(1, Math.min(200, size));
        Long total = mapper.countPage(resource, caller.tenantId(), userId, status, menuId, search);
        var rows = ManagementRows.publicRows(mapper.selectPage(resource, caller.tenantId(), userId, status, menuId, search,
                size, ((long) current - 1) * size));
        return new PageResult(rows, current, size, total == null ? 0 : total);
    }

    public Map<String, Object> owned(String resource, Long id, InternalCallContext caller) {
        ManagementRows.id(id);
        Map<String, Object> row = mapper.selectOwned(resource, id, caller.tenantId());
        if (row == null) throw new ManagementNotFoundException();
        return row;
    }

    public Map<String, Object> find(String resource, Long id) {
        return mapper.selectById(resource, id);
    }

    public void insert(String resource, Map<String, Object> values) {
        mapper.insertResource(resource, values);
    }

    public void update(String resource, Long id, Map<String, Object> values) {
        mapper.updateResource(resource, id, values);
    }

}
