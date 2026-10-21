package com.xqy.nexusedu.ai.chat.infrastructure.llm.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.ai.task.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.chat.application.gateway.AiStreamEventPublisher;
import com.xqy.nexusedu.ai.task.application.service.execution.TaskExecutionService;
import com.xqy.nexusedu.ai.task.domain.constant.AiConstants;
import com.xqy.nexusedu.ai.chat.infrastructure.llm.assistant.AiChatAssistant;
import com.xqy.nexusedu.ai.chat.infrastructure.messaging.RedisChatMemoryStore;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.ModelHandle;
import com.xqy.nexusedu.ai.model.infrastructure.llm.model.ModelRegistry;
import com.xqy.nexusedu.ai.governance.infrastructure.governance.AiGovernanceRuntime;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.log.AiRequestLogService;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.observation.AiObservationService;

@Component
public class AiChatService {

    private final ModelRegistry modelRegistry;
    private final RedisChatMemoryStore chatMemoryStore;
    private final AiRequestLogService requestLogService;
    private final AiStreamEventPublisher eventPublisher;
    private final int chatMemoryMaxMessages;
    private final ConcurrentMap<String, StreamState> streams = new ConcurrentHashMap<>();
    @Autowired
    private AiObservationService observations;
    @Autowired private AiGovernanceRuntime governance;
    private static class StreamState {
        final AtomicBoolean terminal = new AtomicBoolean();
        volatile StreamingHandle handle;
        volatile boolean cancelled;
        Runnable onCancel;
        long lastCancelCheck;
        AiObservationService.Call call;
    }

    public AiChatService(ModelRegistry modelRegistry,
                         RedisChatMemoryStore chatMemoryStore,
                         AiRequestLogService requestLogService,
                         AiStreamEventPublisher eventPublisher,
                         @Value("${nexusedu.ai.chat-memory-max-messages}") int chatMemoryMaxMessages) {
        this.modelRegistry = modelRegistry;
        this.chatMemoryStore = chatMemoryStore;
        this.requestLogService = requestLogService;
        this.eventPublisher = eventPublisher;
        this.chatMemoryMaxMessages = chatMemoryMaxMessages;
    }

    public String sampleChat(String question) {
        return createAssistant((String) null).sampleChat(question);
    }

    /** Streams a composed answer and reports terminal status to the task-run orchestrator. */
    public void answer(AiExecutionContext context, String resultJson, String chatMemoryId,
                       String alias, Consumer<String> onSuccess, Consumer<Throwable> onFailure) {
        answer(context,resultJson,chatMemoryId,alias,onSuccess,onFailure,()->false,List.of());
    }

    public void cancel(String streamKey) {
        if (streamKey == null) return;
        StreamState state = streams.get(streamKey);
        if (state == null) return;
        state.cancelled = true;
        if (state.handle != null) state.handle.cancel();
        if (state.onCancel != null) state.onCancel.run();
    }

    public void answer(AiExecutionContext context, String resultJson, String chatMemoryId,
                       String alias, Consumer<String> onSuccess, Consumer<Throwable> onFailure,
                       BooleanSupplier cancellationRequested,
                       List<ChatMessage> restoredMemory) {
        String streamKey = context.getStreamKey();
        String requestId = context.getRequestId();
        StreamState state = new StreamState();
        state.onCancel = () -> {
            if (state.terminal.compareAndSet(false,true)) {
                if (state.call != null) state.call.cancel();
                try { fail(streamKey,requestId,onFailure,new TaskExecutionService.TaskCancelledException()); }
                finally { streams.remove(streamKey,state); }
            }
        };
        streams.put(streamKey,state);
        if (resultJson == null || resultJson.isBlank()) {
            streams.remove(streamKey,state);
            fail(streamKey, requestId, onFailure, new IllegalArgumentException("工具没有返回结果"));
            return;
        }
        try {
            if (cancellationRequested.getAsBoolean()) { cancel(streamKey); return; }
            chatMemoryStore.initializeIfAbsent(chatMemoryId, restoredMemory);
            publish(streamKey, AiConstants.STREAM_EVENT.GENERATING, "生成中");
            ModelHandle model = governance==null?modelRegistry.find(normalizeAlias(alias)).orElseGet(modelRegistry::defaultModel):governance.model(context,normalizeAlias(alias),modelRegistry);
            String systemPrompt=governance==null?null:governance.prompt(context,"AI_COMPOSE",AiGovernanceRuntime.COMPOSE_PROMPT);
            var tokenStream = createAssistant(model,systemPrompt).answer(chatMemoryId, resultJson)
                    .onPartialResponseWithContext((response, responseContext) -> {
                        state.handle = responseContext.streamingHandle();
                        if (state.call != null) state.call.firstToken(response.text());
                        long now = System.currentTimeMillis();
                        if (now-state.lastCancelCheck >= 100) {
                            state.lastCancelCheck = now;
                            if (cancellationRequested.getAsBoolean()) state.cancelled = true;
                        }
                        if (state.cancelled) { state.handle.cancel(); state.onCancel.run(); return; }
                        if (!state.terminal.get()) publish(streamKey, AiConstants.STREAM_EVENT.TOKEN, response.text());
                    })
                    .onCompleteResponse(response -> {
                        if (state.call != null) state.call.success(response);
                        if (!state.terminal.compareAndSet(false,true)) return;
                        try {
                            onSuccess.accept(response == null || response.aiMessage() == null ? null : response.aiMessage().text());
                            finishRequest(requestId, response);
                            publish(streamKey, AiConstants.STREAM_EVENT.DONE, "完成");
                        } catch (Exception error) {
                            fail(streamKey, requestId, onFailure, error);
                        } finally {
                            streams.remove(streamKey,state);
                        }
                    })
                    .onError(error -> {
                        if (state.call != null) state.call.fail(error);
                        if (!state.terminal.compareAndSet(false,true)) return;
                        try { fail(streamKey, requestId, onFailure, error); }
                        finally { streams.remove(streamKey,state); }
                    });
            if (observations != null) state.call = observations.model(
                    AiObservationService.Scope.chat(context),
                    "COMPOSE", model.providerName(), model.modelName());
            if (state.cancelled || state.terminal.get()) { if (state.call != null) state.call.cancel(); return; }
            tokenStream.start();
        } catch (Exception error) {
            if (state.call != null) state.call.fail(error);
            if (state.terminal.compareAndSet(false,true)) {
                try { fail(streamKey, requestId, onFailure, error); }
                finally { streams.remove(streamKey,state); }
            }
        }
    }

    private void fail(String streamKey, String requestId, Consumer<Throwable> onFailure, Throwable error) {
        requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
        try {
            onFailure.accept(error);
        } finally {
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR, errorMessage(error));
        }
    }

    private void publish(String streamKey, String type, Object data) {
        eventPublisher.publish(streamKey, type, data);
    }

    private String errorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "AI chat failed";
        }
        return error.getMessage();
    }

    private void finishRequest(String requestId, ChatResponse response) {
        if (response == null) {
            requestLogService.success(requestId, null, null, null, null);
            return;
        }
        TokenUsage usage = response.tokenUsage();
        requestLogService.success(requestId, response.modelName(),
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount(),
                usage == null ? null : usage.totalTokenCount());
    }

    private AiChatAssistant createAssistant(String alias) {
        ModelHandle modelHandle = modelRegistry.find(normalizeAlias(alias))
                .orElseGet(modelRegistry::defaultModel);
        return createAssistant(modelHandle);
    }

    private AiChatAssistant createAssistant(ModelHandle modelHandle) {
        return createAssistant(modelHandle,null);
    }
    private AiChatAssistant createAssistant(ModelHandle modelHandle,String systemPrompt) {
        var builder=AiServices.builder(AiChatAssistant.class)
                .chatModel(modelHandle.chatModel())
                .streamingChatModel(modelHandle.streamingChatModel())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(chatMemoryMaxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build());
        if(systemPrompt!=null && !systemPrompt.isBlank())builder.systemMessageProvider(memoryId->systemPrompt);
        return builder.build();
    }

    private static String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? AiConstants.MODEL_ALIAS.BALANCED : alias.trim();
    }
}
