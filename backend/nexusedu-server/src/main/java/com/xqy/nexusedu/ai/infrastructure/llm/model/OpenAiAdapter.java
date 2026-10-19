package com.xqy.nexusedu.ai.infrastructure.llm.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.Locale;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiModelDefinition;
import com.xqy.nexusedu.ai.domain.constant.AiConstants;

/**
 * Adapter for OpenAI and OpenAI-compatible providers such as DeepSeek.
 */
@Component
public class OpenAiAdapter implements ModelAdapter {

    private final Environment environment;

    public OpenAiAdapter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String provider() {
        return AiConstants.MODEL_PROVIDER.OPENAI;
    }

    @Override
    public ModelHandle create(AiModelDefinition modelDefinition) {
        return create(modelDefinition, 4096);
    }

    @Override
    public ModelHandle create(AiModelDefinition modelDefinition, int maxOutputTokens) {
        if (modelDefinition == null) {
            throw new IllegalArgumentException("modelDefinition must not be null");
        }
        if (!provider().equals(normalize(modelDefinition.getProvider()))
                && !AiConstants.MODEL_PROVIDER.DEEPSEEK.equals(normalize(modelDefinition.getProvider()))) {
            throw new IllegalArgumentException("Unsupported OpenAI-compatible provider: "
                    + modelDefinition.getProvider());
        }
        String modelName = required(modelDefinition.getModelName(), "modelName");
        String apiKey = resolveApiKey(required(modelDefinition.getApiKey(), "apiKey"));
        String baseUrl = required(modelDefinition.getBaseUrl(), "baseUrl");
        return new ModelHandle(
                OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        // Retries are performed by the observed caller so every attempt is recorded.
                        .maxRetries(0)
                        .maxCompletionTokens(maxOutputTokens)
                        .build(),
                OpenAiStreamingChatModel.builder()
                        .maxCompletionTokens(maxOutputTokens)
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build(), normalize(modelDefinition.getProvider()), modelName);
    }

    private String resolveApiKey(String value) {
        String resolved = environment.getProperty(value);
        if (resolved == null || resolved.isBlank()) resolved = System.getenv(value);
        return required(resolved, "resolved apiKey");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
