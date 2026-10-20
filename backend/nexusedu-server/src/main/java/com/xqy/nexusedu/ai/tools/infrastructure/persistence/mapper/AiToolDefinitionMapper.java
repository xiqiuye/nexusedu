package com.xqy.nexusedu.ai.tools.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.entity.AiToolDefinition;

/** Persistence mapper for AI tool definitions. */
@Mapper
public interface AiToolDefinitionMapper extends BaseMapper<AiToolDefinition> {
}
