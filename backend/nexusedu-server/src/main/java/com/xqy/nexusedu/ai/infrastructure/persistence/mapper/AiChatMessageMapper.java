package com.xqy.nexusedu.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiChatMessage;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
    @Select("SELECT COALESCE(MAX(sequence_no), 0) FROM ai_chat_message WHERE conversation_id = #{conversationId}")
    int lastSequenceNo(@Param("conversationId") Long conversationId);
}
