package com.xqy.nexusedu.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.model.entity.AiModelDefinition;

/** Persistence mapper for database-backed AI model definitions. */
@Mapper
public interface AiModelDefinitionMapper extends BaseMapper<AiModelDefinition> {
}
