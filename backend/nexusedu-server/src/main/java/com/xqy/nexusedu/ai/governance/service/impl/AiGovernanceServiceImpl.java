package com.xqy.nexusedu.ai.governance.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.ai.evaluation.infrastructure.evaluation.AiConfiguration;
import com.xqy.nexusedu.ai.governance.mapper.AiGovernanceMapper;
import com.xqy.nexusedu.ai.governance.service.AiGovernanceService;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class AiGovernanceServiceImpl implements AiGovernanceService {
    @Autowired private AuthorizationPolicy authorization;
    private final AiGovernanceMapper mapper;

    public AiGovernanceServiceImpl(AiGovernanceMapper mapper) { this.mapper = mapper; }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        authorization.authorize("ai:config:read", caller);
        if (!Set.of("versions", "rollouts", "policies", "usage").contains(resource)) throw new IllegalArgumentException("未知配置资源");
        String type = filters == null || filters.get("type") == null || filters.get("type").isBlank() ? null : AiConfiguration.type(filters.get("type"));
        String key = filters == null || filters.get("key") == null || filters.get("key").isBlank() ? null : AiConfiguration.key(filters.get("key"));
        current = Math.max(current, 1); size = Math.clamp(size, 1, 200);
        long total = mapper.countResources(resource, caller.tenantId(), type, key);
        var rows = ManagementRows.publicRows(mapper.listResources(resource, caller.tenantId(), type, key, size, (long) (current - 1) * size));
        rows.forEach(row -> { if (row.containsKey("configJson")) row.put("config", AiConfiguration.object(row.remove("configJson"))); row.replaceAll((column, value) -> value instanceof BigDecimal ? value.toString() : value); });
        return new PageResult(rows, current, size, total);
    }

    @Override
    public Map<String, Object> draft(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:config:write", caller);
        String type = AiConfiguration.type(input.get("resourceType")); String key = AiConfiguration.key(input.get("resourceKey"));
        if ("*".equals(key)) throw new IllegalArgumentException("版本不支持通配符");
        var config = AiConfiguration.validate(type, key, input.get("config")); Long id = IdWorker.getId();
        try { mapper.insertVersion(id, caller.tenantId(), type, key, ManagementRows.text(input.get("version"), 32, true), JSONUtil.toJsonStr(config), caller.userId().toString()); }
        catch (org.springframework.dao.DuplicateKeyException error) { throw new ManagementConflictException("该资源版本已存在，请使用新版本号"); }
        return Map.of("id", id.toString(), "status", "DRAFT");
    }

    @Override @Transactional
    public void transition(Long id, String action, String note, InternalCallContext caller) {
        authorization.authorize(Set.of("approve", "reject").contains(action) ? "ai:config:approve" : "ai:config:write", caller);
        var version = owned(id, caller); String status = String.valueOf(version.get("status"));
        switch (action) {
            case "submit" -> { if (!"DRAFT".equals(status)) throw new ManagementConflictException("只能提交草稿"); mapper.updatePending(id); }
            case "approve", "reject" -> { if (!"PENDING".equals(status)) throw new ManagementConflictException("只能审批待审版本"); if (caller.userId().toString().equals(version.get("create_by"))) throw new ManagementConflictException("版本作者不能审批自己的版本"); mapper.updateReview(id, "approve".equals(action) ? "APPROVED" : "REJECTED", caller.userId().toString(), ManagementRows.text(note, 500, true)); }
            default -> throw new IllegalArgumentException("未知审批操作");
        }
    }

    @Override @Transactional
    public void rollout(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:config:publish", caller); String type = AiConfiguration.type(input.get("resourceType")); String key = AiConfiguration.key(input.get("resourceKey"));
        Long stable = ManagementRows.id(input.get("stableVersionId")); Long candidate = input.get("candidateVersionId") == null ? null : ManagementRows.id(input.get("candidateVersionId"));
        requireRelease(stable, type, key, caller); if (candidate != null) requireRelease(candidate, type, key, caller);
        int percent = AiConfiguration.integer(input.getOrDefault("canaryPercent", 0), 0, 100);
        if (percent > 0 && (candidate == null || candidate.equals(stable))) throw new IllegalArgumentException("灰度需要不同的候选版本");
        mapper.upsertRollout(caller.tenantId(), type, key, stable, candidate, percent, caller.userId().toString());
    }

    @Override @Transactional
    public void policy(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:policy:write", caller); String type = AiConfiguration.type(input.get("resourceType")); String key = AiConfiguration.key(input.get("resourceKey"));
        int rpm = AiConfiguration.integer(input.getOrDefault("requestsPerMinute", 0), 0, 1_000_000); BigDecimal budget = decimal(input.get("dailyBudget")); BigDecimal reserve = decimal(input.getOrDefault("reservePerCall", "0"));
        if (reserve == null) throw new IllegalArgumentException("预占金额不能为空");
        if (budget != null && (!type.equals("MODEL") || reserve.signum() <= 0 || reserve.compareTo(budget) > 0)) throw new IllegalArgumentException("模型预算必须配置正确的单次预占金额且不超过预算");
        String currency = ManagementRows.text(input.getOrDefault("currency", "USD"), 8, true); if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("货币必须是三位大写代码");
        String permission = ManagementRows.text(input.get("permissionCode"), 128, false); if (permission != null && !permission.isBlank() && !permission.matches("[a-zA-Z0-9_:.-]+")) throw new IllegalArgumentException("无效权限标识");
        if (!(input.getOrDefault("enabled", true) instanceof Boolean)) throw new IllegalArgumentException("enabled 必须为布尔值");
        String previous = mapper.selectPolicyCurrency(caller.tenantId(), type, key); if (previous != null && !currency.equals(previous)) throw new ManagementConflictException("已有策略不可修改货币，避免混合预算账户");
        mapper.upsertPolicy(caller.tenantId(), type, key, Boolean.FALSE.equals(input.get("enabled")) ? 0 : 1, rpm, budget, reserve, currency, permission, caller.userId().toString());
    }

    private BigDecimal decimal(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { BigDecimal number = new BigDecimal(value.toString()); if (number.signum() < 0 || number.scale() > 12 || number.precision() - number.scale() > 12) throw new ArithmeticException(); return number; }
        catch (Exception error) { throw new IllegalArgumentException("金额必须为非负十进制数，最多十二位整数及十二位小数"); }
    }

    private void requireRelease(Long id, String type, String key, InternalCallContext caller) {
        var row = owned(id, caller); if (!"APPROVED".equals(row.get("status")) || !type.equals(row.get("resource_type")) || !key.equals(row.get("resource_key"))) throw new ManagementConflictException("只能发布同一资源的已批准版本");
    }

    private Map<String, Object> owned(Long id, InternalCallContext caller) {
        var row = mapper.lockVersion(ManagementRows.id(id), caller.tenantId()); if (row == null || row.isEmpty()) throw new ManagementNotFoundException(); return row;
    }
}
