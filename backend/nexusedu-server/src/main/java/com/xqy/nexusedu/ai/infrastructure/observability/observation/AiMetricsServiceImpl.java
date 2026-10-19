package com.xqy.nexusedu.ai.infrastructure.observability.observation;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.application.port.in.AiMetricsService;
import com.xqy.nexusedu.system.application.port.in.SystemManagementService;
import com.xqy.nexusedu.shared.dto.ManagementRows;
import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;

@Service
public class AiMetricsServiceImpl implements AiMetricsService {
    @Autowired private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    private final AiObservationService observations;
    public AiMetricsServiceImpl(JdbcTemplate jdbc, AiObservationService observations) {
        this.jdbc = jdbc; this.observations = observations;
    }

    @Override public Map<String, Object> overview(String startDate, String endDate, InternalCallContext caller) {
        authorization.authorize("ai:metrics:read", caller);
        Window window = window(startDate, endDate);
        Object[] args = {caller.tenantId(), window.start(), window.end()};
        String modelWhere = " WHERE tenant_id=? AND started_at>=? AND started_at<?";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", window.start().toLocalDate().toString());
        result.put("endDate", window.end().minusDays(1).toLocalDate().toString());
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("model", row("""
                SELECT COUNT(*) AS calls,
                COALESCE(SUM(outcome='SUCCESS'),0) AS succeeded,
                COALESCE(SUM(outcome IN ('ERROR','TIMEOUT')),0) AS failed,
                COALESCE(SUM(outcome='CANCELLED'),0) AS cancelled,
                COALESCE(SUM(outcome='IN_PROGRESS'),0) AS unfinished,
                COALESCE(SUM(usage_status='UNKNOWN'),0) AS unknown_usage,
                COALESCE(SUM(cost_status='UNKNOWN_PRICE'),0) AS unknown_price,
                COALESCE(SUM(estimated_cost IS NULL),0) AS unknown_cost,
                SUM(input_tokens) AS input_tokens,SUM(output_tokens) AS output_tokens,
                SUM(cached_input_tokens) AS cached_input_tokens,
                AVG(duration_ms) AS average_ms,AVG(ttft_ms) AS average_ttft_ms,
                SUM(outcome='SUCCESS')/NULLIF(SUM(outcome IN ('SUCCESS','ERROR','TIMEOUT')),0) AS success_rate
                FROM ai_model_call_log
                """ + modelWhere, args));
        Map<String, Object> latency = percentiles("duration_ms", args);
        latency.put("ttftP95Ms", percentiles("ttft_ms", args).get("p95Ms"));
        result.put("latency", latency);
        result.put("costs", rows("SELECT currency,SUM(estimated_cost) AS estimated_cost,COUNT(*) AS known_calls FROM ai_model_call_log"
                + modelWhere + " AND estimated_cost IS NOT NULL GROUP BY currency", args));
        result.put("models", rows("""
                SELECT provider,model_name,operation,currency,COUNT(*) AS calls,
                SUM(outcome='SUCCESS') AS succeeded,SUM(outcome IN ('ERROR','TIMEOUT')) AS failed,
                SUM(outcome='CANCELLED') AS cancelled,SUM(outcome='IN_PROGRESS') AS unfinished,
                SUM(input_tokens) AS input_tokens,SUM(output_tokens) AS output_tokens,
                SUM(estimated_cost) AS estimated_cost,SUM(estimated_cost IS NULL) AS unknown_cost,
                AVG(duration_ms) AS average_ms,AVG(ttft_ms) AS average_ttft_ms
                FROM ai_model_call_log
                """ + modelWhere + " GROUP BY provider,model_name,operation,currency ORDER BY calls DESC", args));
        result.put("daily", rows("""
                SELECT DATE_FORMAT(started_at,'%Y-%m-%d') AS day,currency,COUNT(*) AS calls,
                SUM(input_tokens) AS input_tokens,SUM(output_tokens) AS output_tokens,
                SUM(estimated_cost) AS estimated_cost,SUM(estimated_cost IS NULL) AS unknown_cost
                FROM ai_model_call_log
                """ + modelWhere + " GROUP BY DATE_FORMAT(started_at,'%Y-%m-%d'),currency ORDER BY day", args));
        result.put("rag", row("""
                SELECT COUNT(*) AS retrievals,COALESCE(SUM(outcome='HIT'),0) AS hits,
                COALESCE(SUM(outcome='EMPTY'),0) AS empty_count,
                COALESCE(SUM(outcome IN ('ERROR','TIMEOUT')),0) AS errors,
                COALESCE(SUM(outcome='CANCELLED'),0) AS cancelled,
                COALESCE(SUM(outcome='IN_PROGRESS'),0) AS unfinished,
                SUM(outcome='HIT')/NULLIF(SUM(outcome IN ('HIT','EMPTY')),0) AS hit_rate,
                SUM(outcome IN ('ERROR','TIMEOUT'))/NULLIF(SUM(outcome IN ('HIT','EMPTY','ERROR','TIMEOUT')),0) AS error_rate,
                AVG(duration_ms) AS average_ms,SUM(result_count) AS returned_chunks
                FROM ai_execution_observation WHERE tenant_id=? AND started_at>=? AND started_at<? AND kind='RAG'
                """, args));
        result.put("tools", rows("""
                SELECT tool_name,COUNT(*) AS attempts,COALESCE(SUM(outcome='SUCCESS'),0) AS succeeded,
                COALESCE(SUM(outcome IN ('ERROR','TIMEOUT')),0) AS failed,
                COALESCE(SUM(outcome='TIMEOUT'),0) AS timeouts,
                COALESCE(SUM(outcome='CANCELLED'),0) AS cancelled,
                COALESCE(SUM(outcome='IN_PROGRESS'),0) AS unfinished,
                SUM(outcome IN ('ERROR','TIMEOUT'))/NULLIF(SUM(outcome IN ('SUCCESS','ERROR','TIMEOUT')),0) AS error_rate,
                AVG(duration_ms) AS average_ms
                FROM ai_execution_observation WHERE tenant_id=? AND started_at>=? AND started_at<? AND kind='TOOL_ATTEMPT'
                GROUP BY tool_name ORDER BY attempts DESC
                """, args));
        result.put("toolSteps", row("""
                SELECT COUNT(*) AS steps,COALESCE(SUM(s.status='SUCCEEDED'),0) AS succeeded,
                COALESCE(SUM(s.status='FAILED'),0) AS failed,
                SUM(s.status='FAILED')/NULLIF(SUM(s.status IN ('SUCCEEDED','FAILED')),0) AS error_rate
                FROM ai_task_step s JOIN ai_task_run r ON r.run_id=s.task_run_id
                WHERE r.tenant_id=? AND s.start_time>=? AND s.start_time<? AND s.step_type='TOOL'
                """, args));
        // Current queue snapshot is tenant-scoped and independent of the historical date filter.
        result.put("queue", row("""
                SELECT COALESCE(SUM(j.status='PENDING' AND j.cancel_requested=0),0) AS pending,
                COALESCE(SUM(j.status='RUNNING'),0) AS running,
                COALESCE(SUM(j.status IN ('PENDING','RUNNING') AND j.cancel_requested=1),0) AS cancelling,
                COALESCE(SUM(j.status='RUNNING' AND j.lease_expire_time<CURRENT_TIMESTAMP(3)),0) AS expired_leases,
                COALESCE(MAX(CASE WHEN j.status='PENDING' AND j.cancel_requested=0
                    THEN GREATEST(TIMESTAMPDIFF(SECOND,j.create_time,CURRENT_TIMESTAMP),0) END),0) AS oldest_pending_age_seconds,
                COALESCE(SUM(CASE WHEN j.status IN ('PENDING','RUNNING') AND j.cancel_requested=0
                    THEN GREATEST(v.chunk_count-j.processed_chunks,0) ELSE 0 END),0) AS remaining_chunks,
                COALESCE(SUM(j.status IN ('PENDING','RUNNING') AND j.cancel_requested=0 AND v.chunk_count IS NULL),0) AS unknown_chunk_jobs
                FROM ai_knowledge_ingestion_job j LEFT JOIN ai_knowledge_document_version v
                    ON v.id=j.document_version_id AND v.tenant_id=j.tenant_id WHERE j.tenant_id=?
                """, caller.tenantId()));
        result.put("queueHistory", row("""
                SELECT COALESCE(SUM(status='SUCCEEDED'),0) AS succeeded,
                COALESCE(SUM(status='FAILED'),0) AS failed,COALESCE(SUM(status='CANCELLED'),0) AS cancelled,
                COALESCE(SUM(retry_count),0) AS retries,COALESCE(SUM(CASE WHEN status='SUCCEEDED' THEN processed_chunks ELSE 0 END),0) AS completed_chunks,
                AVG(CASE WHEN status='SUCCEEDED' THEN TIMESTAMPDIFF(MICROSECOND,start_time,finish_time)/1000 END) AS average_execution_ms
                FROM ai_knowledge_ingestion_job WHERE tenant_id=? AND finish_time>=? AND finish_time<?
                """, args));
        result.put("queueThroughput", row("""
                SELECT COUNT(*)/900.0 AS jobs_per_second,COALESCE(SUM(processed_chunks),0)/900.0 AS chunks_per_second
                FROM ai_knowledge_ingestion_job WHERE tenant_id=? AND status='SUCCEEDED'
                AND finish_time>=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 15 MINUTE)
                """, caller.tenantId()));
        result.put("instanceWriteFailures", observations.writeFailures());
        return result;
    }

    @Override public PageResult calls(Map<String, String> filters, int current, int size, InternalCallContext caller) {
        authorization.authorize("ai:metrics:read", caller);
        filters = filters == null ? Map.of() : filters;
        Window window = window(filters.get("startDate"), filters.get("endDate"));
        List<Object> args = new ArrayList<>(List.of(caller.tenantId(), window.start(), window.end()));
        StringBuilder where = new StringBuilder(" WHERE tenant_id=? AND started_at>=? AND started_at<?");
        for (String field : List.of("provider", "operation", "outcome", "runId", "requestId", "modelName")) {
            String value = ManagementRows.text(filters.get(field), field.equals("modelName") ? 128 : 64, false);
            if (value != null && !value.isBlank()) {
                String column = switch (field) { case "runId" -> "run_id"; case "requestId" -> "request_id"; case "modelName" -> "model_name"; default -> field; };
                where.append(" AND ").append(column).append("=?"); args.add(value);
            }
        }
        return page("ai_model_call_log", "id,call_id,logical_call_id,attempt,request_id,run_id,job_id,operation,provider,requested_model,model_name,response_id,observation_scope,outcome,error_category,started_at,finished_at,duration_ms,ttft_ms,input_tokens,cached_input_tokens,output_tokens,usage_status,price_version_id,currency,estimated_cost,cost_status", where.toString(), args, current, size, "started_at DESC,id DESC");
    }

    @Override public PageResult prices(int current, int size, InternalCallContext caller) {
        authorization.authorize("ai:metrics:read", caller);
        return page("ai_model_price_version", "id,provider,model_name,currency,input_price,cached_input_price,output_price,enabled,effective_from,create_by", " WHERE tenant_id=?", new ArrayList<>(List.of(caller.tenantId())), current, size, "effective_from DESC,id DESC");
    }

    @Override public Map<String, Object> createPrice(Map<String, Object> input, InternalCallContext caller) {
        authorization.authorize("ai:price:create", caller);
        if (input == null) throw new IllegalArgumentException("价格不能为空");
        String provider = ManagementRows.text(input.get("provider"), 64, true).toLowerCase(Locale.ROOT);
        String model = ManagementRows.text(input.get("modelName"), 128, true);
        String currency = ManagementRows.text(input.get("currency"), 3, true).toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("币种须为三位字母代码");
        BigDecimal in = money(input.get("inputPrice")), out = money(input.get("outputPrice"));
        BigDecimal cached = input.get("cachedInputPrice") == null ? in : money(input.get("cachedInputPrice"));
        long id = IdWorker.getId();
        // Publish a new immutable version. Historical records keep their selected price snapshot.
        jdbc.update("""
                INSERT INTO ai_model_price_version
                (id,tenant_id,provider,model_name,currency,input_price,cached_input_price,output_price,enabled,effective_from,create_by,modify_by)
                VALUES (?,?,?,?,?,?,?,?,1,?,?,?)
                """, id, caller.tenantId(), provider, model, currency, in, cached, out, LocalDateTime.now(), caller.userId().toString(), caller.userId().toString());
        return row("SELECT id,provider,model_name,currency,input_price,cached_input_price,output_price,enabled,effective_from FROM ai_model_price_version WHERE id=? AND tenant_id=?", id, caller.tenantId());
    }

    static Window window(String startDate, String endDate) {
        try {
            LocalDate end = endDate == null || endDate.isBlank() ? LocalDate.now() : LocalDate.parse(endDate);
            LocalDate start = startDate == null || startDate.isBlank() ? end.minusDays(6) : LocalDate.parse(startDate);
            long days = ChronoUnit.DAYS.between(start, end);
            if (days < 0 || days > 30) throw new IllegalArgumentException("查询范围须为 1–31 天");
            return new Window(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        } catch (java.time.DateTimeException error) { throw new IllegalArgumentException("日期须为 yyyy-MM-dd", error); }
    }

    static BigDecimal money(Object value) {
        try {
            String text = Objects.toString(value, "");
            if (text.length() > 32) throw new NumberFormatException();
            BigDecimal amount = new BigDecimal(text);
            if (amount.signum() < 0 || amount.scale() > 8 || amount.compareTo(new BigDecimal("1000000000")) > 0) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException error) { throw new IllegalArgumentException("单价须为不超过 10 亿且最多 8 位小数的非负数字"); }
    }

    private Map<String, Object> percentiles(String column, Object[] args) {
        // column is supplied only by the two fixed call sites above, never by a request parameter.
        return row("WITH durations AS (SELECT " + column + " AS elapsed,ROW_NUMBER() OVER (ORDER BY " + column
                + ") AS rn,COUNT(*) OVER () AS n FROM ai_model_call_log WHERE tenant_id=? AND started_at>=? AND started_at<? AND "
                + column + " IS NOT NULL AND outcome IN ('SUCCESS','ERROR','TIMEOUT')) "
                + "SELECT MAX(CASE WHEN rn=CEIL(n*0.50) THEN elapsed END) AS p50_ms,"
                + "MAX(CASE WHEN rn=CEIL(n*0.95) THEN elapsed END) AS p95_ms,"
                + "MAX(CASE WHEN rn=CEIL(n*0.99) THEN elapsed END) AS p99_ms FROM durations", args);
    }
    private Map<String, Object> publicRow(Map<String, Object> source) {
        Map<String, Object> result = ManagementRows.publicRow(source);
        result.replaceAll((key, value) -> value instanceof BigDecimal decimal ? decimal.toPlainString() : value);
        return result;
    }
    private Map<String, Object> row(String sql, Object... args) { return publicRow(jdbc.queryForMap(sql, args)); }
    private List<Map<String, Object>> rows(String sql, Object... args) { return jdbc.queryForList(sql, args).stream().map(this::publicRow).toList(); }
    private PageResult page(String table, String columns, String where, List<Object> args, int current, int size, String order) {
        current = Math.max(1, current); size = Math.max(1, Math.min(size, 200));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        args.add(size); args.add(((long) current - 1) * size);
        return new PageResult(rows("SELECT " + columns + " FROM " + table + where + " ORDER BY " + order + " LIMIT ? OFFSET ?", args.toArray()), current, size, total == null ? 0 : total);
    }
    static class Window {
        private LocalDateTime start;
        private LocalDateTime end;
        Window() { }
        Window(LocalDateTime start, LocalDateTime end) { this.start = start; this.end = end; }
        LocalDateTime getStart() { return start; }
        void setStart(LocalDateTime start) { this.start = start; }
        LocalDateTime getEnd() { return end; }
        void setEnd(LocalDateTime end) { this.end = end; }
        LocalDateTime start() { return start; }
        LocalDateTime end() { return end; }
    }
}
