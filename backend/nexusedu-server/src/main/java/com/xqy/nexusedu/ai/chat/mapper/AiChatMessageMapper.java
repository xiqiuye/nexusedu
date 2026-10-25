package com.xqy.nexusedu.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.ai.chat.entity.AiChatMessage;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
    int lastSequenceNo(@Param("conversationId") Long conversationId);
}
