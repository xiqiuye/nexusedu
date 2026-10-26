package com.xqy.nexusedu.ai.operations.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiOperationsMapper {
    Long countPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                   @Param("userId") String userId, @Param("runId") String runId,
                   @Param("requestId") String requestId, @Param("search") String search);

    List<Map<String, Object>> selectPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                                         @Param("userId") String userId, @Param("runId") String runId,
                                         @Param("requestId") String requestId, @Param("search") String search,
                                         @Param("limit") int limit, @Param("offset") long offset);

    Map<String, Object> findDelivery(@Param("runId") String runId, @Param("tenantId") String tenantId,
                                     @Param("userId") String userId);
    List<String> findSources(@Param("tenantId") String tenantId, @Param("runId") String runId);
    List<String> findAnswers(@Param("tenantId") String tenantId, @Param("userId") String userId,
                             @Param("runId") String runId);
    int countConversation(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId);
    Long countMessages(@Param("conversationId") Long conversationId, @Param("tenantId") String tenantId, @Param("userId") String userId);
    List<Map<String, Object>> selectMessages(@Param("conversationId") Long conversationId, @Param("tenantId") String tenantId,
                                             @Param("userId") String userId, @Param("limit") int limit, @Param("offset") long offset);
}
