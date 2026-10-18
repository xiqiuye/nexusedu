package com.xqy.nexusedu.infrastructure.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.common.entity.ai.AiToolDefinition;

/** Persistence mapper for AI tool definitions. */
@Mapper
public interface AiToolDefinitionMapper extends BaseMapper<AiToolDefinition> {
}
