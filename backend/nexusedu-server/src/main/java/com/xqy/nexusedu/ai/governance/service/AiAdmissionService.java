package com.xqy.nexusedu.ai.governance.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.ai.governance.mapper.AiGovernanceMapper;
import com.xqy.nexusedu.ai.operations.service.AiObservationService.Scope;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

/** Authoritative, transactional admission; unlike telemetry this must fail closed. */
@Service
public class AiAdmissionService {
    @Autowired private AuthorizationPolicy authorization;
    private final AiGovernanceMapper mapper;
    private final InternalCallSigner signer;
    private final TransactionTemplate transactions;

    public AiAdmissionService(AiGovernanceMapper mapper, PlatformTransactionManager manager, @Value("${nexusedu.internal-rpc.secret}") String secret) {
        this.mapper = mapper; this.transactions = new TransactionTemplate(manager); this.signer = new InternalCallSigner(secret);
    }

    public void model(Scope scope, String provider, String model, String callId, Map<String,Object> price) {
        String key = provider + ":" + model;
        if (scope.runId() != null) { var keys = mapper.releasedModelKeys(scope.tenantId(), scope.runId(), provider, model); if (!keys.isEmpty()) key = keys.getFirst(); }
        for (var policy : mapper.modelPolicies(scope.tenantId(), key)) { Object permission = policy.get("permission_code"); if (permission == null || permission.toString().isBlank()) continue; if (scope.userId() == null || scope.sessionId() == null) throw new ManagementAccessDeniedException(); authorization.authorize(permission.toString(), signer.sign("nexusedu-web-bff", scope.tenantId(), scope.userId(), scope.sessionId(), scope.requestId() == null ? callId : scope.requestId())); }
        admit(scope.tenantId(), "MODEL", key, callId, price);
    }
    public void tool(String tenant, String key) { admit(tenant, "TOOL", key, null, Map.of()); }
    public void prompt(String tenant, String key) { admit(tenant, "PROMPT", key, null, Map.of()); }

    private void admit(String tenant, String type, String key, String callId, Map<String,Object> price) {
        transactions.executeWithoutResult(tx -> { for (String target : List.of("*", key)) { var policies = mapper.lockPolicies(tenant, type, target); if (policies.isEmpty()) continue; var policy = policies.getFirst(); if (((Number) policy.get("enabled")).intValue() != 1) throw new ManagementConflictException("AI 资源已被策略禁用：" + target);
            String minute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); String day = LocalDateTime.now().toLocalDate().toString(); int rpm = ((Number) policy.get("requests_per_minute")).intValue();
            if (rpm > 0) { mapper.ensureUsage(tenant, type, target, minute); long count = mapper.usageCount(tenant, type, target, minute); if (count >= rpm) throw new ManagementConflictException("AI 请求频率超限"); mapper.incrementUsage(tenant, type, target, minute); }
            BigDecimal budget = (BigDecimal) policy.get("daily_budget"); if (budget != null && callId != null) { if (price.get("input_price") == null || price.get("output_price") == null || !policy.get("currency").equals(price.get("currency"))) throw new ManagementConflictException("模型价格信息不完整或货币不匹配"); BigDecimal reserve = (BigDecimal) policy.get("reserve_per_call"); if (reserve.signum() <= 0) throw new ManagementConflictException("模型预占金额无效"); mapper.ensureUsage(tenant, type, target, day); BigDecimal charged = mapper.chargedCost(tenant, type, target, day); if (charged.add(reserve).compareTo(budget) > 0) throw new ManagementConflictException("AI 日预算超限"); mapper.incrementCharged(reserve, tenant, type, target, day); mapper.insertReservation(callId, tenant, target, day, policy.get("currency").toString(), reserve); }
        } });
    }

    public void settle(String callId, BigDecimal amount, String currency) {
        if (amount == null || currency == null) return;
        transactions.executeWithoutResult(tx -> { for (var row : mapper.reservations(callId)) { if (!currency.equals(row.get("currency"))) continue; BigDecimal delta = amount.subtract((BigDecimal) row.get("reserved_cost")); mapper.incrementCharged(delta, row.get("tenant_id").toString(), "MODEL", row.get("resource_key").toString(), row.get("window_key").toString()); mapper.markSettled(callId, row.get("resource_key").toString()); } });
    }
}
