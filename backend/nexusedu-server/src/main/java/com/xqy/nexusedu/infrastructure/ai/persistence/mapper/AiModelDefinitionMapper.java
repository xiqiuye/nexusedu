package com.xqy.nexusedu.infrastructure.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.infrastructure.ai.persistence.entity.AiModelDefinition;

/** Persistence mapper for database-backed AI model definitions. */
@Mapper
public interface AiModelDefinitionMapper extends BaseMapper<AiModelDefinition> {
}
