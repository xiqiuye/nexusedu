package com.xqy.nexusedu.ai.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiConversationLifecycleMapper {
    int insertConversation(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId, @Param("sessionId") String sessionId, @Param("title") String title);
    Integer retentionDays(@Param("tenantId") String tenantId, @Param("userId") String userId);
    int upsertRetention(@Param("tenantId") String tenantId, @Param("userId") String userId, @Param("days") int days);
    int updateKeepUntil(@Param("id") Long id, @Param("date") LocalDateTime date, @Param("modifyBy") String modifyBy, @Param("tenantId") String tenantId, @Param("userId") String userId);
    Map<String,Object> lockActive(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId);
    int activeRuns(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    List<String> streamKeys(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int deleteSources(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int deleteMessages(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int markDeleted(@Param("id") Long id);
    int eraseSteps(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int eraseRequestLogs(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int eraseToolLogs(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    int eraseRuns(@Param("tenantId") String tenantId, @Param("conversationId") Long conversationId);
    List<Map<String,Object>> expiredConversations(@Param("limit") int limit);
    int expiredOwned(@Param("id") Long id);
}
