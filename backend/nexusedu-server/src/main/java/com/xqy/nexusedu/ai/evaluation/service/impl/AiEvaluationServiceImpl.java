package com.xqy.nexusedu.ai.evaluation.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.ai.evaluation.infrastructure.evaluation.AiConfiguration;
import com.xqy.nexusedu.ai.evaluation.mapper.AiEvaluationMapper;
import com.xqy.nexusedu.ai.evaluation.service.AiEvaluationService;
import com.xqy.nexusedu.ai.governance.infrastructure.governance.AiGovernanceRuntime;
import com.xqy.nexusedu.ai.governance.mapper.AiGovernanceMapper;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class AiEvaluationServiceImpl implements AiEvaluationService {
    @Autowired private AuthorizationPolicy authorization;
    private final AiEvaluationMapper mapper;
    private final AiGovernanceMapper governanceMapper;

    public AiEvaluationServiceImpl(AiEvaluationMapper mapper, AiGovernanceMapper governanceMapper) { this.mapper = mapper; this.governanceMapper = governanceMapper; }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        authorization.authorize("ai:evaluation:read", caller);
        if (!Set.of("cases", "batches", "results", "schedules").contains(resource)) throw new IllegalArgumentException("未知的评估资源");
        filters = filters == null ? Map.of() : filters;
        String dataset = filters.get("dataset");
        if (dataset != null && !dataset.isBlank() && !resource.equals("results")) dataset = ManagementRows.text(dataset, 128, true);
        Long batchId = resource.equals("results") ? ManagementRows.id(filters.get("batchId")) : null;
        current = Math.max(current, 1); size = Math.clamp(size, 1, 200);
        long total = mapper.count(resource, caller.tenantId(), dataset, batchId);
        var rows = ManagementRows.publicRows(mapper.list(resource, caller.tenantId(), dataset, batchId, size, (long) (current - 1) * size));
        for (Map<String,Object> row : rows) for (String key : List.of("expectedPlan", "expectedTools", "expectedSources", "caseSnapshot", "costsJson")) if (row.get(key) != null) row.put(key, AiConfiguration.json(row.get(key).toString()));
        if (resource.equals("results")) for (Map<String,Object> row : rows) row.put("releases", ManagementRows.publicRows(mapper.releasesForRun(caller.tenantId(), String.valueOf(row.get("runId")))));
        return new PageResult(rows, current, size, total);
    }

    @Override
    public Map<String, Object> addCase(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:evaluation:write", caller);
        String dataset = ManagementRows.text(input.get("dataset"), 128, true); String key = ManagementRows.text(input.get("caseKey"), 128, true); String question = ManagementRows.text(input.get("question"), 4000, true);
        Object plan = array(input.get("expectedPlan")); Object tools = array(input.get("expectedTools")); Object sources = array(input.get("expectedSources")); JSONArray planArray = JSONUtil.parseArray(plan);
        if (planArray.isEmpty() || planArray.size() > 20) throw new IllegalArgumentException("计划至少包含 1 项，最多 20 项任务");
        for (Object task : planArray) { Map<String,Object> obj = AiConfiguration.object(task); if (!(Set.of("domain", "toolName", "query").containsAll(obj.keySet()) && Set.of("chat", "student", "knowledge").contains(obj.get("domain")))) throw new IllegalArgumentException("计划项格式无效"); }
        for (Object tool : JSONUtil.parseArray(tools)) if (!Set.of("query_student", "query_knowledge").contains(tool.toString())) throw new IllegalArgumentException("预期工具无效");
        for (Object source : JSONUtil.parseArray(sources)) if (!source.toString().matches("[^\\s#\\[\\]]{1,128}#[^\\s#\\[\\]]{1,64}")) throw new IllegalArgumentException("来源格式必须为 documentNo#chunkNo");
        if (!(input.getOrDefault("expectedRefusal", false) instanceof Boolean)) throw new IllegalArgumentException("expectedRefusal 必须为布尔值");
        Long id = IdWorker.getId(); int version = AiConfiguration.integer(input.getOrDefault("version", 1), 1, 1_000_000);
        try { mapper.insertCase(id, caller.tenantId(), dataset, key, version, question, JSONUtil.toJsonStr(plan), JSONUtil.toJsonStr(tools), JSONUtil.toJsonStr(sources), Boolean.TRUE.equals(input.get("expectedRefusal")) ? 1 : 0, caller.userId().toString()); }
        catch (DuplicateKeyException error) { throw new IllegalArgumentException("评估用例已存在"); }
        return Map.of("id", id.toString());
    }

    private Object array(Object value) { Object array = value instanceof String text ? JSONUtil.parse(text) : value; if (!(array instanceof Collection<?> collection) || collection.size() > 100) throw new IllegalArgumentException("数组参数必须是 JSON 数组，且最多包含 100 项"); return array; }

    @Override public void enableCase(Long id, boolean enabled, InternalCallContext caller) { authorization.authorize("ai:evaluation:write", caller); if (mapper.updateCaseEnabled(ManagementRows.id(id), caller.tenantId(), enabled ? 1 : 0) != 1) throw new ManagementNotFoundException(); }

    @Override @Transactional
    public Map<String, Object> run(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:evaluation:run", caller); String dataset = ManagementRows.text(input.get("dataset"), 128, true); String alias = AiConfiguration.key(input.getOrDefault("modelAlias", "balanced"));
        Long model = input.get("modelVersionId") == null ? null : ManagementRows.id(input.get("modelVersionId")); Long prompt = input.get("promptVersionId") == null ? null : ManagementRows.id(input.get("promptVersionId"));
        if (model != null) requireVersion(model, "MODEL", alias, caller); if (prompt != null) requireVersion(prompt, "PROMPT", "AI_PLAN", caller);
        List<Map<String,Object>> cases = mapper.latestCases(caller.tenantId(), dataset); if (cases.isEmpty() || cases.size() > 200) throw new IllegalArgumentException("数据集必须包含 1 到 200 个已启用的最新样本");
        ArrayList<Map<String,Object>> releases = new ArrayList<>();
        for (Map<String,Object> rollout : governanceMapper.listResources("rollouts", caller.tenantId(), null, null, 10_000, 0)) { String type = rollout.get("resource_type").toString(); String key = rollout.get("resource_key").toString(); Object version = rollout.get("stable_version_id"); if (rollout.get("candidate_version_id") != null && AiGovernanceRuntime.bucket(caller.tenantId(), caller.userId(), type, key) < ((Number) rollout.get("canary_percent")).intValue()) version = rollout.get("candidate_version_id"); releases.add(release(ManagementRows.id(version), type, key, caller)); }
        if (model != null) { releases.removeIf(r -> r.get("resourceType").equals("MODEL") && r.get("resourceKey").equals(alias)); releases.add(release(model, "MODEL", alias, caller)); }
        if (prompt != null) { releases.removeIf(r -> r.get("resourceType").equals("PROMPT") && r.get("resourceKey").equals("AI_PLAN")); releases.add(release(prompt, "PROMPT", "AI_PLAN", caller)); }
        Long id = IdWorker.getId(); mapper.insertBatch(id, caller.tenantId(), dataset, alias, model, prompt, JSONUtil.toJsonStr(releases), caller.userId().toString(), caller.sessionId());
        for (Map<String,Object> sample : cases) { Map<String,Object> snapshot = ManagementRows.publicRow(sample); snapshot.put("expectedPlan", JSONUtil.parse(sample.get("expected_plan").toString())); snapshot.put("expectedTools", JSONUtil.parse(sample.get("expected_tools").toString())); snapshot.put("expectedSources", JSONUtil.parse(sample.get("expected_sources").toString())); snapshot.put("expectedRefusal", ((Number) sample.get("expected_refusal")).intValue() == 1); mapper.insertResult(IdWorker.getId(), id, caller.tenantId(), sample.get("id"), JSONUtil.toJsonStr(snapshot)); }
        return Map.of("id", id.toString(), "status", "PENDING", "sampleCount", cases.size());
    }

    private void requireVersion(Long id, String type, String key, InternalCallContext caller) { if (mapper.validVersion(id, caller.tenantId(), type, key) != 1) throw new ManagementNotFoundException(); }
    private Map<String,Object> release(Long id, String type, String key, InternalCallContext caller) { Map<String,Object> row = mapper.resourceVersion(id, caller.tenantId(), type, key); if (row == null) throw new ManagementNotFoundException(); return Map.of("resourceType", type, "resourceKey", key, "versionId", id.toString(), "config", AiConfiguration.object(row.get("config_json"))); }

    @Override public Map<String, Object> summary(Long id, InternalCallContext caller) { authorization.authorize("ai:evaluation:read", caller); long batchId = ManagementRows.id(id); if (mapper.batchExists(batchId, caller.tenantId()) != 1) throw new ManagementNotFoundException(); Map<String,Object> result = ManagementRows.publicRow(mapper.summary(batchId, caller.tenantId())); result.replaceAll((key, value) -> value instanceof BigDecimal ? value.toString() : value); result.put("unknownCostCalls", mapper.unknownCosts(batchId, caller.tenantId())); result.put("costs", mapper.costs(batchId, caller.tenantId()).stream().map(r -> Map.of("currency", r.get("currency"), "amount", r.get("amount").toString())).toList()); result.put("releases", ManagementRows.publicRows(mapper.releaseSummary(batchId, caller.tenantId()))); return result; }

    @Override public void schedule(Map<String, Object> input, InternalCallContext caller) { authorization.authorize("ai:evaluation:run", caller); String dataset = ManagementRows.text(input.get("dataset"), 128, true); int hours = AiConfiguration.integer(input.getOrDefault("intervalHours", 24), 1, 8760); mapper.upsertSchedule(caller.tenantId(), dataset, hours, Boolean.FALSE.equals(input.get("enabled")) ? 0 : 1, caller.userId(), caller.sessionId()); }
}
