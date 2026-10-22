package com.xqy.nexusedu.ai.chat.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.ai.chat.infrastructure.persistence.entity.AiChatConversation;

@Mapper
public interface AiChatConversationMapper extends BaseMapper<AiChatConversation> {
    int createIfAbsent(AiChatConversation conversation);

    AiChatConversation lockByOwner(@Param("tenantId") String tenantId, @Param("userId") String userId,
                                   @Param("sessionId") String sessionId);

    AiChatConversation lockByIdAndOwner(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId);
}
