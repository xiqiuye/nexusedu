package com.xqy.nexusedu.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiPromptTemplate;

/** Persistence mapper for database-managed AI prompt templates. */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
