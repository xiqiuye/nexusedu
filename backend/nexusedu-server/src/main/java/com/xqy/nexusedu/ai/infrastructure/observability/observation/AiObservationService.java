package com.xqy.nexusedu.ai.infrastructure.observability.observation;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.infrastructure.governance.AiAdmissionService;

/** Durable observations. Failure to write telemetry must never change a business result. */
@Service
public class AiObservationService {
    @Autowired
    private AiAdmissionService admission;
    private static final Logger log = LoggerFactory.getLogger(AiObservationService.class);
    private final JdbcTemplate jdbc;
    private final AtomicLong writeFailures = new AtomicLong();

    public AiObservationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public static class Scope {
        private String tenantId;
        private String requestId;
        private String runId;
        private String jobId;
        private Long userId;
        private Long sessionId;
        public Scope() { }
        public Scope(String tenantId,String requestId,String runId,String jobId) {this(tenantId,requestId,runId,jobId,null,null);}
        public Scope(String tenantId, String requestId, String runId, String jobId, Long userId, Long sessionId) {
            this.tenantId = tenantId; this.requestId = requestId; this.runId = runId; this.jobId = jobId;
            this.userId = userId; this.sessionId = sessionId;
        }
        public static Scope chat(AiExecutionContext context) {
            return context == null ? null : new Scope(context.getTenantId(), context.getRequestId(), context.getRunId(), null,context.getUserId(),context.getSessionId());
        }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String tenantId() { return tenantId; }
        public String requestId() { return requestId; }
        public String runId() { return runId; }
        public String jobId() { return jobId; }
        public Long userId() { return userId; }
        public Long sessionId() { return sessionId; }
    }

    public Call model(Scope scope, String operation, String provider, String model) {
        return model(scope, operation, provider, model, UUID.randomUUID().toString(), 1);
    }

    public Call model(Scope scope, String operation, String provider, String model, String logicalCallId, int attempt) {
        if (scope == null || scope.tenantId() == null || scope.tenantId().isBlank()) return null;
        Call call = new Call(scope, provider, model);
        call.price = price(scope.tenantId(), provider, model, call.startedAt);
        if(admission!=null)admission.model(scope,provider,model,call.id,call.price);
        safe(() -> requiredUpdate("""
                INSERT INTO ai_model_call_log
                (id,call_id,logical_call_id,attempt,tenant_id,request_id,run_id,job_id,operation,provider,requested_model,model_name,started_at,
                 price_version_id,currency,input_price,cached_input_price,output_price)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, IdWorker.getId(), call.id, logicalCallId, attempt, scope.tenantId(), scope.requestId(), scope.runId(), scope.jobId(),
                operation, provider, model, model, call.startedAt, call.price.get("id"), call.price.get("currency"),
                call.price.get("input_price"), call.price.get("cached_input_price"), call.price.get("output_price")));
        // Model latency starts after observer setup; database/price lookup overhead is not upstream time.
        call.startNanos = System.nanoTime();
        return call;
    }

    public Observation observation(AiExecutionContext context, String kind, String tool, String stepId, Integer attempt) {
        Scope scope = Scope.chat(context);
        if (scope == null || scope.tenantId() == null || scope.tenantId().isBlank()) return null;
        Observation observation = new Observation(scope);
        safe(() -> requiredUpdate("""
                INSERT INTO ai_execution_observation
                (id,observation_id,tenant_id,request_id,run_id,step_id,kind,tool_name,attempt,started_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, IdWorker.getId(), observation.id, scope.tenantId(), scope.requestId(), scope.runId(), stepId,
                kind, tool, attempt, observation.startedAt));
        return observation;
    }

    public long writeFailures() { return writeFailures.get(); }

    private Map<String, Object> price(String tenant, String provider, String model, LocalDateTime at) {
        try {
            var rows = jdbc.queryForList("""
                    SELECT id,currency,input_price,cached_input_price,output_price FROM ai_model_price_version
                    WHERE tenant_id=? AND provider=? AND model_name=? AND enabled=1 AND effective_from<=?
                    ORDER BY effective_from DESC,id DESC LIMIT 1
                    """, tenant, provider, model, at);
            return rows.isEmpty() ? Map.of() : rows.getFirst();
        } catch (Exception error) { failed(error); return Map.of(); }
    }

    private void safe(Runnable action) {
        try { action.run(); } catch (Exception error) { failed(error); }
    }

    private void requiredUpdate(String sql, Object... args) {
        if (jdbc.update(sql, args) != 1) throw new IllegalStateException("Observation row was not persisted");
    }

    private void failed(Exception error) {
        writeFailures.incrementAndGet();
        // Exception messages can contain SQL arguments. Never log credentials, prompts or raw responses.
        log.warn("AI observation persistence failed: {}", error.getClass().getSimpleName());
    }

    public static String outcome(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof java.util.concurrent.CancellationException || current instanceof InterruptedException
                    || current.getClass().getSimpleName().equals("TaskCancelledException")) return "CANCELLED";
            if (current instanceof java.util.concurrent.TimeoutException || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.http.HttpTimeoutException || current instanceof dev.langchain4j.exception.TimeoutException) return "TIMEOUT";
        }
        return "ERROR";
    }

    public static BigDecimal cost(Long input, Long cached, Long output, Map<String, Object> price) {
        if (input == null || output == null || input < 0 || output < 0 || price.isEmpty()) return null;
        BigDecimal in = decimal(price.get("input_price")), cache = decimal(price.get("cached_input_price")), out = decimal(price.get("output_price"));
        if (in == null || cache == null || out == null) return null;
        // Missing cached usage is safe only when cache and normal input have the same price.
        if (cached == null && in.compareTo(cache) != 0) return null;
        long cachedCount = cached == null ? 0 : cached;
        if (cachedCount < 0 || cachedCount > input) return null;
        return in.multiply(BigDecimal.valueOf(input - cachedCount)).add(cache.multiply(BigDecimal.valueOf(cachedCount)))
                .add(out.multiply(BigDecimal.valueOf(output))).divide(BigDecimal.valueOf(1_000_000), 12, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object value) { return value == null ? null : new BigDecimal(value.toString()); }
    private static Long count(Integer value) { return value == null ? null : value.longValue(); }

    public final class Call {
        private final String id = UUID.randomUUID().toString();
        private final Scope scope;
        private final String provider;
        private final String requestedModel;
        private final LocalDateTime startedAt = LocalDateTime.now();
        private volatile long startNanos = System.nanoTime();
        private Map<String, Object> price = Map.of();
        private Long ttft;
        private boolean terminal;

        private Call(Scope scope, String provider, String model) {
            this.scope = scope; this.provider = provider; this.requestedModel = model;
        }

        public synchronized void firstToken(String text) {
            if (!terminal && ttft == null && text != null && !text.isEmpty()) ttft = elapsed();
        }

        public void success(ChatResponse response) {
            TokenUsage usage = response == null ? null : response.tokenUsage();
            Long cached = usage instanceof OpenAiTokenUsage openAi && openAi.inputTokensDetails() != null
                    ? count(openAi.inputTokensDetails().cachedTokens()) : null;
            complete("SUCCESS", null, response == null ? null : response.modelName(), response == null ? null : response.id(),
                    usage == null ? null : count(usage.inputTokenCount()), cached,
                    usage == null ? null : count(usage.outputTokenCount()));
        }

        public void embedding(String model, String responseId, Long input, Long cached) {
            complete("SUCCESS", null, model, responseId, input, cached, 0L);
        }

        public void fail(Throwable error) { complete(outcome(error), error == null ? null : error.getClass().getSimpleName(), null, null, null, null, null); }
        public void cancel() { complete("CANCELLED", null, null, null, null, null, null); }

        private synchronized void complete(String outcome, String category, String resolvedModel, String responseId,
                                           Long input, Long cached, Long output) {
            long duration = elapsed();
            LocalDateTime finishedAt = LocalDateTime.now();
            String actual = resolvedModel == null || resolvedModel.isBlank() ? requestedModel : resolvedModel;
            if (!actual.equals(requestedModel)) price = price(scope.tenantId(), provider, actual, startedAt);
            BigDecimal amount = cost(input, cached, output, price);
            String usageStatus = input == null || output == null ? "UNKNOWN" : "REPORTED";
            String costStatus = amount != null ? "ESTIMATED" : price.isEmpty() ? "UNKNOWN_PRICE" : "UNKNOWN_USAGE";
            if (terminal) {
                // Late reported usage can settle a cancelled call, but cannot replace its outcome or duration.
                if (input != null && output != null) safe(() -> jdbc.update("""
                        UPDATE ai_model_call_log SET model_name=?,response_id=?,input_tokens=?,cached_input_tokens=?,output_tokens=?,
                        usage_status=?,price_version_id=?,currency=?,input_price=?,cached_input_price=?,output_price=?,estimated_cost=?,cost_status=?
                        WHERE call_id=? AND tenant_id=? AND usage_status='UNKNOWN'
                        """, actual, responseId, input, cached, output, usageStatus, price.get("id"), price.get("currency"),
                        price.get("input_price"), price.get("cached_input_price"), price.get("output_price"), amount, costStatus, id, scope.tenantId()));
                if(admission!=null)safe(()->admission.settle(id,amount,(String)price.get("currency")));
                return;
            }
            terminal = true;
            safe(() -> requiredUpdate("""
                    UPDATE ai_model_call_log SET outcome=?,error_category=?,finished_at=?,duration_ms=?,ttft_ms=?,model_name=?,response_id=?,
                    input_tokens=?,cached_input_tokens=?,output_tokens=?,usage_status=?,price_version_id=?,currency=?,input_price=?,cached_input_price=?,output_price=?,estimated_cost=?,cost_status=?
                    WHERE call_id=? AND tenant_id=? AND outcome='IN_PROGRESS'
                    """, outcome, category, finishedAt, duration, ttft, actual, responseId, input, cached, output, usageStatus,
                    price.get("id"), price.get("currency"), price.get("input_price"), price.get("cached_input_price"), price.get("output_price"), amount, costStatus, id, scope.tenantId()));
            if(admission!=null)safe(()->admission.settle(id,amount,(String)price.get("currency")));
        }

        private long elapsed() { return (System.nanoTime() - startNanos) / 1_000_000; }
    }

    public final class Observation {
        private final String id = UUID.randomUUID().toString();
        private final Scope scope;
        private final LocalDateTime startedAt = LocalDateTime.now();
        private final long startNanos = System.nanoTime();
        private boolean terminal;
        private Observation(Scope scope) { this.scope = scope; }
        public synchronized void finish(String outcome, Integer resultCount, Throwable error) {
            if (terminal) return;
            terminal = true;
            safe(() -> requiredUpdate("""
                    UPDATE ai_execution_observation SET outcome=?,error_category=?,result_count=?,finished_at=?,duration_ms=?
                    WHERE observation_id=? AND tenant_id=? AND outcome='IN_PROGRESS'
                    """, outcome, error == null ? null : error.getClass().getSimpleName(), resultCount, LocalDateTime.now(),
                    (System.nanoTime() - startNanos) / 1_000_000, id, scope.tenantId()));
        }
    }
}
