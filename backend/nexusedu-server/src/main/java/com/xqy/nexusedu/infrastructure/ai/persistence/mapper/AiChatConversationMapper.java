package com.xqy.nexusedu.infrastructure.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xqy.nexusedu.infrastructure.ai.persistence.entity.AiChatConversation;

@Mapper
public interface AiChatConversationMapper extends BaseMapper<AiChatConversation> {
    @Insert("""
            INSERT INTO ai_chat_conversation (id, tenant_id, user_id, session_id, default_slot, title, status)
            VALUES (#{id}, #{tenantId}, #{userId}, #{sessionId}, #{sessionId}, #{title}, #{status})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int createIfAbsent(AiChatConversation conversation);

    @Select("""
            SELECT * FROM ai_chat_conversation
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND default_slot = #{sessionId} AND status='ACTIVE'
            FOR UPDATE
            """)
    AiChatConversation lockByOwner(@Param("tenantId") String tenantId, @Param("userId") String userId,
                                   @Param("sessionId") String sessionId);

    @Select("SELECT * FROM ai_chat_conversation WHERE id=#{id} AND tenant_id=#{tenantId} AND user_id=#{userId} AND status='ACTIVE' FOR UPDATE")
    AiChatConversation lockByIdAndOwner(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId);
}
