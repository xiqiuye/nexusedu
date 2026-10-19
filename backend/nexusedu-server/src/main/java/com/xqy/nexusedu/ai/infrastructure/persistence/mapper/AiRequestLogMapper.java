package com.xqy.nexusedu.ai.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiRequestLog;

@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {
}
