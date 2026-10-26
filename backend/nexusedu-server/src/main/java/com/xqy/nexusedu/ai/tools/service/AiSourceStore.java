package com.xqy.nexusedu.ai.tools.service;

import cn.hutool.json.JSONUtil;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.task.model.AiExecutionContext;
import com.xqy.nexusedu.ai.tools.mapper.AiRunSourceMapper;

@Service
public class AiSourceStore {
    private final AiRunSourceMapper mapper;
    public AiSourceStore(AiRunSourceMapper mapper) { this.mapper=mapper; }
    public void save(AiExecutionContext context,List<Map<String,Object>> sources) {
        mapper.save(context.getTenantId(), context.getRunId(), context.getConversationId(), JSONUtil.toJsonStr(sources));
    }
}
