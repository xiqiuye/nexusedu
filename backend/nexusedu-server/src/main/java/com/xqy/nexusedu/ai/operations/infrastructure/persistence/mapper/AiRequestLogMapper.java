package com.xqy.nexusedu.ai.operations.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.ai.operations.infrastructure.persistence.entity.AiRequestLog;

@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {
}
