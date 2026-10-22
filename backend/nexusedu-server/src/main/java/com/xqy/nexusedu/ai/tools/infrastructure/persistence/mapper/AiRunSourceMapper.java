package com.xqy.nexusedu.ai.tools.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiRunSourceMapper {
    int save(@Param("tenantId") String tenantId, @Param("runId") String runId,
             @Param("conversationId") Long conversationId, @Param("sourcesJson") String sourcesJson);
}
