package com.xqy.nexusedu.ai.evaluation.infrastructure.evaluation;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xqy.nexusedu.ai.evaluation.application.AiEvaluationService;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.ai.evaluation.infrastructure.evaluation.AiConfiguration;
import com.xqy.nexusedu.ai.governance.infrastructure.governance.AiGovernanceRuntime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiEvaluationServiceImpl
implements AiEvaluationService {
    @Autowired
    private AuthorizationPolicy authorization;
    private final JdbcTemplate jdbc;

    public AiEvaluationServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        this.authorization.authorize("ai:evaluation:read", caller);
        filters = filters == null ? Map.of() : filters;
        String table = switch (resource) {
            case "cases" -> "ai_eval_case";
            case "batches" -> "ai_eval_batch";
            case "results" -> "ai_eval_result";
            case "schedules" -> "ai_eval_schedule";
            default -> throw new IllegalArgumentException("未知的评估资源。");
        };
        String where = " WHERE tenant_id=?";
        ArrayList<Object> args = new ArrayList<>();
        args.add(caller.tenantId());
        if (filters.containsKey("dataset") && !filters.get("dataset").isBlank() && !resource.equals("results")) {
            where += " AND dataset=?";
            args.add(ManagementRows.text(filters.get("dataset"), 128, true));
        }
        if (resource.equals("results")) {
            where += " AND batch_id=?";
            args.add(ManagementRows.id(filters.get("batchId")));
        }
        current = Math.max(1, current);
        size = Math.clamp((long)size, 1, 200);
        Long total = (Long)this.jdbc.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        args.add(size);
        args.add((long)(current - 1) * size);
        String columns = resource.equals("results") ? "id,batch_id,case_id,case_snapshot,run_id,conversation_id,status,plan_correct,tool_correct,rag_hit,cited_count,correct_cited_count,refused,refusal_correct,costs_json,unknown_cost_calls,answer,error_message,create_time" : "*";
        List<Map<String, Object>> rows = ManagementRows.publicRows(this.jdbc.queryForList("SELECT " + columns + " FROM " + table + where + " ORDER BY " + (resource.equals("schedules") ? "next_run_at DESC" : "create_time DESC,id DESC") + " LIMIT ? OFFSET ?", args.toArray()));
        for (Map<String, Object> row : rows) {
            for (String key : List.of("expectedPlan", "expectedTools", "expectedSources", "caseSnapshot", "costsJson")) {
                if (row.get(key) == null) continue;
                row.put(key, AiConfiguration.json(row.get(key).toString()));
            }
        }
        if (resource.equals("results")) {
            for (Map<String, Object> row : rows) {
                row.put("releases", ManagementRows.publicRows(this.jdbc.queryForList("SELECT resource_type,resource_key,version_id FROM ai_execution_release WHERE tenant_id=? AND run_id=? ORDER BY resource_type,resource_key", new Object[]{caller.tenantId(), row.get("runId")})));
            }
        }
        return new PageResult(rows, current, size, total == null ? 0L : total);
    }

    @Override
    public Map<String, Object> addCase(Map<String, Object> input, InternalCallContext caller) {
        this.authorization.authorize("ai:evaluation:write", caller);
        String dataset = ManagementRows.text(input.get("dataset"), 128, true);
        String key = ManagementRows.text(input.get("caseKey"), 128, true);
        String question = ManagementRows.text(input.get("question"), 4000, true);
        Object plan = this.array(input.get("expectedPlan"));
        Object tools = this.array(input.get("expectedTools"));
        Object sources = this.array(input.get("expectedSources"));
        JSONArray planArray = JSONUtil.parseArray((Object)plan);
        if (planArray.isEmpty() || planArray.size() > 20) {
            throw new IllegalArgumentException("计划至少包含 1 项，至多 20 项任务。");
        }
        for (Object task : planArray) {
            Map<String, Object> obj = AiConfiguration.object(task);
            if (Set.of("domain", "toolName", "query").containsAll(obj.keySet()) && Set.of("chat", "student", "knowledge").contains(obj.get("domain"))) continue;
            throw new IllegalArgumentException("计划项仅允许 domain、toolName 和 query 字段，且 domain 必须是 chat、student 或 knowledge。");
        }
        for (Object tool : JSONUtil.parseArray((Object)tools)) {
            if (Set.of("query_student", "query_knowledge").contains(tool.toString())) continue;
            throw new IllegalArgumentException("预期工具仅支持 query_student 和 query_knowledge。");
        }
        for (Object source : JSONUtil.parseArray((Object)sources)) {
            if (source.toString().matches("[^\\s#\\[\\]]{1,128}#[^\\s#\\[\\]]{1,64}")) continue;
            throw new IllegalArgumentException("来源格式必须为 documentNo#chunkNo。");
        }
        if (!(input.getOrDefault("expectedRefusal", false) instanceof Boolean)) {
            throw new IllegalArgumentException("expectedRefusal 必须为布尔值。");
        }
        Long id = IdWorker.getId();
        int version = AiConfiguration.integer(input.getOrDefault("version", 1), 1, 1000000);
        try {
            this.jdbc.update("INSERT INTO ai_eval_case(id,tenant_id,dataset,case_key,version,question,expected_plan,expected_tools,expected_sources,expected_refusal,create_by) VALUES(?,?,?,?,?,?,?,?,?,?,?)", new Object[]{id, caller.tenantId(), dataset, key, version, question, JSONUtil.toJsonStr((Object)plan), JSONUtil.toJsonStr((Object)tools), JSONUtil.toJsonStr((Object)sources), Boolean.TRUE.equals(input.get("expectedRefusal")) ? 1 : 0, caller.userId().toString()});
        }
        catch (DuplicateKeyException error) {
            throw new IllegalArgumentException("评估用例已存在。");
        }
        return Map.of("id", id.toString());
    }

    private Object array(Object value) {
        Object array = value instanceof String text ? JSONUtil.parse(text) : value;
        if (!(array instanceof Collection<?> collection) || collection.size() > 100) {
            throw new IllegalArgumentException("数组参数必须是 JSON 数组，且最多包含 100 项。");
        }
        return array;
    }

    @Override
    public void enableCase(Long id, boolean enabled, InternalCallContext caller) {
        this.authorization.authorize("ai:evaluation:write", caller);
        if (this.jdbc.update("UPDATE ai_eval_case SET enabled=? WHERE id=? AND tenant_id=?", new Object[]{enabled ? 1 : 0, ManagementRows.id(id), caller.tenantId()}) != 1) {
            throw new ManagementNotFoundException();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> run(Map<String, Object> input, InternalCallContext caller) {
        List<Map<String, Object>> cases;
        Long prompt;
        this.authorization.authorize("ai:evaluation:run", caller);
        String dataset = ManagementRows.text(input.get("dataset"), 128, true);
        String alias = AiConfiguration.key(input.getOrDefault("modelAlias", "balanced"));
        Long model = input.get("modelVersionId") == null ? null : Long.valueOf(ManagementRows.id(input.get("modelVersionId")));
        Long l = prompt = input.get("promptVersionId") == null ? null : Long.valueOf(ManagementRows.id(input.get("promptVersionId")));
        if (model != null) {
            this.requireVersion(model, "MODEL", alias, caller);
        }
        if (prompt != null) {
            this.requireVersion(prompt, "PROMPT", "AI_PLAN", caller);
        }
        if ((cases = this.jdbc.queryForList("SELECT c.* FROM ai_eval_case c WHERE c.tenant_id=? AND c.dataset=? AND c.enabled=1 AND NOT EXISTS(SELECT 1 FROM ai_eval_case newer WHERE newer.tenant_id=c.tenant_id AND newer.dataset=c.dataset AND newer.case_key=c.case_key AND newer.version>c.version) ORDER BY c.case_key LIMIT 201", new Object[]{caller.tenantId(), dataset})).isEmpty() || cases.size() > 200) {
            throw new IllegalArgumentException("数据集必须包含 1 到 200 个已启用的最新样本。");
        }
        ArrayList<Map<String, Object>> releases = new ArrayList<Map<String, Object>>();
        for (Map rollout : this.jdbc.queryForList("SELECT * FROM ai_resource_rollout WHERE tenant_id=? ORDER BY resource_type,resource_key", new Object[]{caller.tenantId()})) {
            String type = rollout.get("resource_type").toString();
            String keyName = rollout.get("resource_key").toString();
            Object version = rollout.get("stable_version_id");
            if (rollout.get("candidate_version_id") != null && AiGovernanceRuntime.bucket(caller.tenantId(), caller.userId(), type, keyName) < ((Number)rollout.get("canary_percent")).intValue()) {
                version = rollout.get("candidate_version_id");
            }
            releases.add(this.release(ManagementRows.id(version), type, keyName, caller));
        }
        if (model != null) {
            releases.removeIf(r -> r.get("resourceType").equals("MODEL") && r.get("resourceKey").equals(alias));
            releases.add(this.release(model, "MODEL", alias, caller));
        }
        if (prompt != null) {
            releases.removeIf(r -> r.get("resourceType").equals("PROMPT") && r.get("resourceKey").equals("AI_PLAN"));
            releases.add(this.release(prompt, "PROMPT", "AI_PLAN", caller));
        }
        Long id = IdWorker.getId();
        this.jdbc.update("INSERT INTO ai_eval_batch(id,tenant_id,dataset,model_alias,model_version_id,prompt_version_id,release_snapshot,create_by,session_id) VALUES(?,?,?,?,?,?,?,?,?)", new Object[]{id, caller.tenantId(), dataset, alias, model, prompt, JSONUtil.toJsonStr(releases), caller.userId().toString(), caller.sessionId()});
        for (Map<String, Object> sample : cases) {
            Map<String, Object> snapshot = ManagementRows.publicRow(sample);
            snapshot.put("expectedPlan", JSONUtil.parse((Object)sample.get("expected_plan").toString()));
            snapshot.put("expectedTools", JSONUtil.parse((Object)sample.get("expected_tools").toString()));
            snapshot.put("expectedSources", JSONUtil.parse((Object)sample.get("expected_sources").toString()));
            snapshot.put("expectedRefusal", ((Number)sample.get("expected_refusal")).intValue() == 1);
            this.jdbc.update("INSERT INTO ai_eval_result(id,batch_id,tenant_id,case_id,case_snapshot) VALUES(?,?,?,?,?)", new Object[]{IdWorker.getId(), id, caller.tenantId(), sample.get("id"), JSONUtil.toJsonStr(snapshot)});
        }
        return Map.of("id", id.toString(), "status", "PENDING", "sampleCount", cases.size());
    }

    private void requireVersion(Long id, String type, String key, InternalCallContext caller) {
        Integer count = (Integer)this.jdbc.queryForObject("SELECT COUNT(*) FROM ai_resource_version WHERE id=? AND tenant_id=? AND resource_type=? AND resource_key=? AND status<>'REJECTED'", Integer.class, new Object[]{id, caller.tenantId(), type, key});
        if (count == null || count != 1) {
            throw new ManagementNotFoundException();
        }
    }

    private Map<String, Object> release(Long id, String type, String key, InternalCallContext caller) {
        Map row = this.jdbc.queryForMap("SELECT config_json FROM ai_resource_version WHERE id=? AND tenant_id=? AND resource_type=? AND resource_key=?", new Object[]{id, caller.tenantId(), type, key});
        return Map.of("resourceType", type, "resourceKey", key, "versionId", id.toString(), "config", AiConfiguration.object(row.get("config_json")));
    }

    @Override
    public Map<String, Object> summary(Long id, InternalCallContext caller) {
        this.authorization.authorize("ai:evaluation:read", caller);
        if ((Integer)this.jdbc.queryForObject("SELECT COUNT(*) FROM ai_eval_batch WHERE id=? AND tenant_id=?", Integer.class, new Object[]{ManagementRows.id(id), caller.tenantId()}) != 1) {
            throw new ManagementNotFoundException();
        }
        Map row = this.jdbc.queryForMap("SELECT COUNT(*) AS total,SUM(status='SUCCEEDED') AS succeeded,SUM(status='FAILED') AS failed,SUM(status IN ('SUCCEEDED','FAILED')) AS completed,AVG(plan_correct) AS planning_accuracy,AVG(tool_correct) AS tool_accuracy,AVG(rag_hit) AS rag_hit_rate,SUM(cited_count) AS cited_count,SUM(correct_cited_count) AS correct_cited_count,SUM(correct_cited_count)/NULLIF(SUM(cited_count),0) AS citation_correctness,AVG(refused) AS refusal_rate,AVG(refusal_correct) AS refusal_accuracy,SUM(unknown_cost_calls) AS unknown_cost_calls FROM ai_eval_result WHERE batch_id=? AND tenant_id=?", new Object[]{id, caller.tenantId()});
        Map<String, Object> result = ManagementRows.publicRow(row);
        result.replaceAll((key, value) -> value instanceof BigDecimal ? value.toString() : value);
        result.put("unknownCostCalls", this.jdbc.queryForObject("SELECT COUNT(*) FROM ai_model_call_log l JOIN ai_eval_result e ON e.run_id=l.run_id AND e.tenant_id=l.tenant_id WHERE e.batch_id=? AND e.tenant_id=? AND l.estimated_cost IS NULL", Long.class, new Object[]{id, caller.tenantId()}));
        result.put("costs", this.jdbc.queryForList("SELECT l.currency,SUM(l.estimated_cost) AS amount FROM ai_model_call_log l JOIN ai_eval_result e ON e.run_id=l.run_id AND e.tenant_id=l.tenant_id WHERE e.batch_id=? AND e.tenant_id=? AND l.estimated_cost IS NOT NULL GROUP BY l.currency", new Object[]{id, caller.tenantId()}).stream().map(r -> Map.of("currency", r.get("currency"), "amount", r.get("amount").toString())).toList());
        result.put("releases", ManagementRows.publicRows(this.jdbc.queryForList("SELECT DISTINCT v.resource_type,v.resource_key,v.version_id FROM ai_execution_release v JOIN ai_eval_result e ON e.run_id=v.run_id AND e.tenant_id=v.tenant_id WHERE e.batch_id=? AND e.tenant_id=? ORDER BY v.resource_type,v.resource_key,v.version_id", new Object[]{id, caller.tenantId()})));
        return result;
    }

    @Override
    public void schedule(Map<String, Object> input, InternalCallContext caller) {
        this.authorization.authorize("ai:evaluation:run", caller);
        String dataset = ManagementRows.text(input.get("dataset"), 128, true);
        int hours = AiConfiguration.integer(input.getOrDefault("intervalHours", 24), 1, 8760);
        this.jdbc.update("INSERT INTO ai_eval_schedule(tenant_id,dataset,interval_hours,enabled,user_id,session_id,next_run_at) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE interval_hours=VALUES(interval_hours),enabled=VALUES(enabled),user_id=VALUES(user_id),session_id=VALUES(session_id),next_run_at=CURRENT_TIMESTAMP,last_error=NULL", new Object[]{caller.tenantId(), dataset, hours, Boolean.FALSE.equals(input.get("enabled")) ? 0 : 1, caller.userId(), caller.sessionId()});
    }
}
