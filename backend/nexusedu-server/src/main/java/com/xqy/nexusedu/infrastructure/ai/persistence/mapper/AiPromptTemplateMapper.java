package com.xqy.nexusedu.infrastructure.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.common.entity.ai.AiPromptTemplate;

/** Persistence mapper for database-managed AI prompt templates. */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
