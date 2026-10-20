package com.xqy.nexusedu.ai.model.infrastructure.llm.model;

import dev.langchain4j.internal.RetryUtils;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.xqy.nexusedu.ai.task.domain.model.AiExecutionContext;
import com.xqy.nexusedu.ai.operations.infrastructure.observability.observation.AiObservationService;

/**
 * A per-execution wrapper; the cached underlying model holds no request identity.
 */
public final class ObservedChatModel implements ChatModel {
    private final ModelHandle handle;
    private final AiObservationService observations;
    private final AiObservationService.Scope scope;
    private final String operation;

    public ObservedChatModel(ModelHandle handle, AiObservationService observations, AiExecutionContext context, String operation) {
        this.handle = handle;
        this.observations = observations;
        this.scope = AiObservationService.Scope.chat(context);
        this.operation = operation;
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        String logicalCallId = UUID.randomUUID().toString();
        var attempt = new AtomicInteger();
        // Preserve the installed SDK's default retry/backoff policy, with one ledger row per attempt.
        return RetryUtils.withRetry(() -> {
            var call = observations.model(scope, operation, handle.providerName(), handle.modelName(), logicalCallId, attempt.incrementAndGet());
            try {
                ChatResponse response = handle.chatModel().chat(request);
                if (call != null) call.success(response);
                return response;
            } catch (RuntimeException error) {
                if (call != null) call.fail(error);
                throw error;
            }
        }, 2);
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return handle.chatModel().defaultRequestParameters();
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return handle.chatModel().supportedCapabilities();
    }

    @Override
    public ModelProvider provider() {
        return handle.chatModel().provider();
    }
}
