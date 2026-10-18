package com.xqy.nexusedu.infrastructure.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.common.entity.ai.AiRequestLog;

@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {
}
