package com.xqy.nexusedu.ai.governance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.governance.entity.AiPromptTemplate;

/** Persistence mapper for database-managed AI prompt templates. */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {
}
