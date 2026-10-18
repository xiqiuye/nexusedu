package com.xqy.nexusedu.infrastructure.ai.persistence;

import cn.hutool.json.JSONUtil;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.common.model.ai.AiExecutionContext;

@Service
public class AiSourceStore {
    private final JdbcTemplate jdbc;
    public AiSourceStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void save(AiExecutionContext context,List<Map<String,Object>> sources) {
        jdbc.update("INSERT INTO ai_run_sources(tenant_id,run_id,conversation_id,sources_json) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE sources_json=VALUES(sources_json)",context.getTenantId(),context.getRunId(),context.getConversationId(),JSONUtil.toJsonStr(sources));
    }
}
