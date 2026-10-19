package com.xqy.nexusedu.knowledge.infrastructure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.xqy.nexusedu.ai.infrastructure.observability.observation.AiObservationService;

/** Optional OpenAI-compatible embedding client used before calling Milvus. */
@Component
public class KnowledgeEmbeddingService {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;
    private final String model;
    private final int dimensions;
    @Autowired
    private AiObservationService observations;
    @Value("${nexusedu.ai.embedding.provider:openai}")
    private String provider = "openai";

    public KnowledgeEmbeddingService(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${nexusedu.ai.embedding.endpoint}") String endpoint,
                                     @Value("${nexusedu.ai.embedding.token}") String token,
                                     @Value("${nexusedu.ai.embedding.model}") String model,
                                     @Value("${nexusedu.ai.embedding.dimensions}") int dimensions) {
        this.client = builder.build();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.token = token == null ? "" : token.trim();
        this.model = model;
        if (dimensions <= 0) throw new IllegalArgumentException("Embedding dimensions must be positive");
        this.dimensions = dimensions;
    }

    public List<Float> embed(String text) {
        return embed(text, null);
    }

    public List<Float> embed(String text, AiObservationService.Scope scope) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<List<Float>> embeddings = embedAll(List.of(text), scope, "RAG_EMBEDDING");
        return embeddings.isEmpty() ? Collections.emptyList() : embeddings.getFirst();
    }

    /** Creates embeddings in one OpenAI-compatible request and preserves input order. */
    public List<List<Float>> embedAll(List<String> texts) {
        return embedAll(texts, null, "INGESTION_EMBEDDING");
    }

    public List<List<Float>> embedAll(List<String> texts,
            AiObservationService.Scope scope, String operation) {
        if (endpoint.isBlank() || texts == null || texts.isEmpty()) return Collections.emptyList();
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Embedding input must not be blank");
        }
        var call = observations == null ? null : observations.model(scope, operation, provider, model);
        try {
            String response = client.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> { if (!token.isBlank()) headers.setBearerAuth(token); })
                    .body(Map.of("model", model, "input", texts, "dimensions", dimensions))
                    .retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            if (call != null) {
                JsonNode usage = root.path("usage");
                JsonNode cached = usage.path("prompt_tokens_details").path("cached_tokens");
                call.embedding(root.path("model").asText(model), root.path("id").asText(null),
                        usage.path("prompt_tokens").isIntegralNumber() ? usage.path("prompt_tokens").longValue()
                                : usage.path("total_tokens").isIntegralNumber() ? usage.path("total_tokens").longValue() : null,
                        cached.isIntegralNumber() ? cached.longValue() : null);
            }
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() != texts.size()) {
                throw new IllegalStateException("Embedding response count does not match request count");
            }
            List<List<Float>> result = new ArrayList<>(Collections.nCopies(texts.size(), null));
            for (JsonNode item : data) {
                int index = item.path("index").asInt(-1);
                JsonNode embedding = item.path("embedding");
                if (index < 0 || index >= texts.size() || !embedding.isArray() || embedding.size() != dimensions) {
                    throw new IllegalStateException("Embedding response has an invalid index or dimension");
                }
                List<Float> vector = new ArrayList<>(dimensions);
                for (JsonNode value : embedding) vector.add((float) value.asDouble());
                result.set(index, vector);
            }
            if (result.stream().anyMatch(Objects::isNull)) {
                throw new IllegalStateException("Embedding response is missing an input result");
            }
            return result;
        } catch (Exception exception) {
            if (call != null) call.fail(exception);
            throw new IllegalStateException("Unable to create knowledge embedding", exception);
        }
    }
}
