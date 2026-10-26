package com.xqy.nexusedu.ai.operations.mapper;

import java.time.LocalDateTime;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiObservationMapper {
    Map<String, Object> findPrice(@Param("tenantId") String tenantId, @Param("provider") String provider,
                                  @Param("modelName") String modelName, @Param("at") LocalDateTime at);

    int insertModelCall(@Param("id") long id, @Param("callId") String callId, @Param("logicalCallId") String logicalCallId,
                        @Param("attempt") int attempt, @Param("tenantId") String tenantId, @Param("requestId") String requestId,
                        @Param("runId") String runId, @Param("jobId") String jobId, @Param("operation") String operation,
                        @Param("provider") String provider, @Param("requestedModel") String requestedModel,
                        @Param("modelName") String modelName, @Param("startedAt") LocalDateTime startedAt,
                        @Param("priceVersionId") Object priceVersionId, @Param("currency") Object currency,
                        @Param("inputPrice") Object inputPrice, @Param("cachedInputPrice") Object cachedInputPrice,
                        @Param("outputPrice") Object outputPrice);

    int updateLateModelCall(@Param("callId") String callId, @Param("tenantId") String tenantId,
                            @Param("modelName") String modelName, @Param("responseId") String responseId,
                            @Param("inputTokens") Long inputTokens, @Param("cachedInputTokens") Long cachedInputTokens,
                            @Param("outputTokens") Long outputTokens, @Param("usageStatus") String usageStatus,
                            @Param("priceVersionId") Object priceVersionId, @Param("currency") Object currency,
                            @Param("inputPrice") Object inputPrice, @Param("cachedInputPrice") Object cachedInputPrice,
                            @Param("outputPrice") Object outputPrice, @Param("estimatedCost") Object estimatedCost,
                            @Param("costStatus") String costStatus);

    int finishModelCall(@Param("callId") String callId, @Param("tenantId") String tenantId, @Param("outcome") String outcome,
                        @Param("errorCategory") String errorCategory, @Param("finishedAt") LocalDateTime finishedAt,
                        @Param("durationMs") long durationMs, @Param("ttftMs") Long ttftMs, @Param("modelName") String modelName,
                        @Param("responseId") String responseId, @Param("inputTokens") Long inputTokens,
                        @Param("cachedInputTokens") Long cachedInputTokens, @Param("outputTokens") Long outputTokens,
                        @Param("usageStatus") String usageStatus, @Param("priceVersionId") Object priceVersionId,
                        @Param("currency") Object currency, @Param("inputPrice") Object inputPrice,
                        @Param("cachedInputPrice") Object cachedInputPrice, @Param("outputPrice") Object outputPrice,
                        @Param("estimatedCost") Object estimatedCost, @Param("costStatus") String costStatus);

    int insertObservation(@Param("id") long id, @Param("observationId") String observationId, @Param("tenantId") String tenantId,
                          @Param("requestId") String requestId, @Param("runId") String runId, @Param("stepId") String stepId,
                          @Param("kind") String kind, @Param("toolName") String toolName, @Param("attempt") Integer attempt,
                          @Param("startedAt") LocalDateTime startedAt);

    int finishObservation(@Param("observationId") String observationId, @Param("tenantId") String tenantId,
                          @Param("outcome") String outcome, @Param("errorCategory") String errorCategory,
                          @Param("resultCount") Integer resultCount, @Param("finishedAt") LocalDateTime finishedAt,
                          @Param("durationMs") long durationMs);
}
