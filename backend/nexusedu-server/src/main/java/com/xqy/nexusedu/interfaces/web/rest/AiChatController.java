package com.xqy.nexusedu.interfaces.web.rest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.xqy.nexusedu.common.dto.ai.AiChatCommand;
import com.xqy.nexusedu.common.dto.ai.AiChatDtos;
import com.xqy.nexusedu.common.dto.system.AuthDtos;
import com.xqy.nexusedu.common.model.ai.AiExecutionContext;
import com.xqy.nexusedu.common.dto.ai.AiRunView;
import com.xqy.nexusedu.application.ai.port.in.AiChatCommandService;
import com.xqy.nexusedu.application.ai.port.in.AiOperationsService;
import com.xqy.nexusedu.common.dto.ApiResponse;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;
import com.xqy.nexusedu.common.security.rpc.InternalCallSigner;

/**
 * HTTP edge that adapts provider-published AI events to SSE.
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private static final MediaType EVENT_TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);
    @Autowired
    private AiChatCommandService chatService;
    @Autowired
    private AiOperationsService operations;
    private final StringRedisTemplate redis;
    private final InternalCallSigner internalCallSigner;

    public AiChatController(StringRedisTemplate redis, @Value("${nexusedu.internal-rpc.secret}") String internalRpcSecret) {
        this.redis = redis;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter chat(@RequestBody AiChatDtos.ChatRequest request, Authentication authentication) {
        AuthDtos.AuthenticatedUser user = user(authentication);
        Map<String, Object> receipt = submit(request, user);
        return events(String.valueOf(receipt.get("runId")), "0-0", authentication);
    }

    @PostMapping("/chat/runs")
    public ApiResponse<Map<String, Object>> start(@RequestBody AiChatDtos.ChatRequest request, Authentication authentication) {
        return ApiResponse.success(submit(request, user(authentication)));
    }

    private Map<String, Object> submit(AiChatDtos.ChatRequest request, AuthDtos.AuthenticatedUser user) {
        if (request == null || request.question() == null || request.question().isBlank())
            throw new IllegalArgumentException("question must not be blank");
        if (request.question().length() > 4_000) throw new IllegalArgumentException("question exceeds 4000 characters");
        if (request.idempotencyKey() != null && request.idempotencyKey().length() > 128)
            throw new IllegalArgumentException("idempotencyKey exceeds 128 characters");
        AiExecutionContext context = AiExecutionContext.create(request.conversationId(), request.question().trim(),
                request.idempotencyKey(), requiredTenant(user), user.userId(), user.sessionId());
        AiChatCommand command = new AiChatCommand(context, internalCall(user, context.getRequestId()));
        return chatService.submitRun(command);
    }

    @GetMapping(value = "/runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter events(@PathVariable String runId, @RequestParam(defaultValue = "0-0") String after, Authentication authentication) {
        if (after.length() > 50 || !after.matches("[0-9]+-[0-9]+"))
            throw new IllegalArgumentException("无效的事件游标");
        AuthDtos.AuthenticatedUser user = user(authentication);
        Map<String, Object> delivery = operations.delivery(runId, internalCall(user, UUID.randomUUID().toString()));
        SseEmitter emitter = new SseEmitter(120_000L);
        AtomicBoolean closed = new AtomicBoolean();
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(error -> closed.set(true));
        Thread.ofVirtual().start(() -> relay(runId, delivery.get("streamKey") == null ? null : delivery.get("streamKey").toString(), after, emitter, closed, user));
        return emitter;
    }

    @PostMapping("/runs/{runId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String runId, Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user))
            throw new IllegalArgumentException("unauthenticated request");
        if (runId == null || runId.isBlank() || runId.length() > 64)
            throw new IllegalArgumentException("invalid runId");
        chatService.cancel(runId, internalCall(user, UUID.randomUUID().toString()));
        return ApiResponse.success(null);
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AiRunView> getRun(@PathVariable String runId, Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user)) {
            throw new IllegalArgumentException("unauthenticated request");
        }
        if (runId == null || runId.isBlank() || runId.length() > 64)
            throw new IllegalArgumentException("invalid runId");
        return ApiResponse.success(chatService.getRun(runId,
                internalCall(user, UUID.randomUUID().toString())));
    }

    @SuppressWarnings("unchecked")
    private void relay(String runId, String key, String cursor, SseEmitter emitter, AtomicBoolean closed, AuthDtos.AuthenticatedUser user) {
        String lastId = cursor;
        long deadline = System.currentTimeMillis() + 115_000L;
        try {
            while (!closed.get() && System.currentTimeMillis() < deadline) {
                List<MapRecord<String, Object, Object>> records = key == null ? null : redis.opsForStream().read(
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(100),
                        StreamOffset.create(key, ReadOffset.from(lastId)));
                if (records != null) for (MapRecord<String, Object, Object> record : records) {
                    lastId = record.getId().getValue();
                    Map<Object, Object> body = record.getValue();
                    String event = String.valueOf(body.get("event"));
                    emitter.send(utf8Event(event, body.get("data"), lastId));
                    if ("done".equals(event) || "error".equals(event)) {
                        emitter.complete();
                        return;
                    }
                }
                if (records == null || records.isEmpty()) {
                    Map<String, Object> state = operations.delivery(runId, internalCall(user, UUID.randomUUID().toString()));
                    String status = String.valueOf(state.get("status"));
                    if (List.of("SUCCEEDED", "FAILED", "CANCELLED").contains(status)) {
                        if (state.get("answer") != null) emitter.send(utf8Event("snapshot", state.get("answer"), null));
                        emitter.send(utf8Event("SUCCEEDED".equals(status) ? "done" : "error",
                                state.get("errorMessage") == null ? status : state.get("errorMessage"), null));
                        emitter.complete();
                        return;
                    }
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                    if (key == null) throw new IllegalStateException("当前任务没有可恢复的事件流");
                }
            }
            emitter.complete();
        } catch (Exception error) {
            emitter.completeWithError(error);
        }
    }

    // Explicit UTF-8 bytes also work with MVC's default ISO-8859-1 String converter.
    // Repeated data fields preserve multiline tokens and snapshots as one SSE frame.
    private SseEmitter.SseEventBuilder utf8Event(String name, Object data, String id) {
        var event = SseEmitter.event().name(name);
        if (id != null) event.id(id);
        String text = data == null ? "" : data.toString();
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1))
            event.data(line.getBytes(StandardCharsets.UTF_8), EVENT_TEXT);
        return event;
    }

    private AuthDtos.AuthenticatedUser user(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user)
            return user;
        throw new IllegalArgumentException("unauthenticated request");
    }

    private InternalCallContext internalCall(AuthDtos.AuthenticatedUser user, String requestId) {
        return internalCallSigner.sign("nexusedu-web-bff", requiredTenant(user), user.userId(), user.sessionId(), requestId);
    }

    private String requiredTenant(AuthDtos.AuthenticatedUser user) {
        if (user.tenantId() == null || user.tenantId().isBlank())
            throw new IllegalArgumentException("tenant is required");
        return user.tenantId();
    }
}
