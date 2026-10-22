package com.xqy.nexusedu.ai.chat.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.ai.chat.infrastructure.persistence.entity.AiChatMessage;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
    int lastSequenceNo(@Param("conversationId") Long conversationId);
}
