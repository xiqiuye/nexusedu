package com.xqy.nexusedu.ai.tools.infrastructure.tool;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.tools.application.port.out.AiToolDefinitionProvider;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.entity.AiToolDefinition;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.mapper.AiToolDefinitionMapper;

/** Loads the tool definitions visible to the planner from the database. */
@Service
public class AiToolDefinitionService implements AiToolDefinitionProvider {

    private final AiToolDefinitionMapper aiToolDefinitionMapper;

    public AiToolDefinitionService(AiToolDefinitionMapper aiToolDefinitionMapper) {
        this.aiToolDefinitionMapper = aiToolDefinitionMapper;
    }

    @Override
    public List<AiToolDefinition> definitions() {
        List<AiToolDefinition> definitions = aiToolDefinitionMapper.selectList(null);
        return definitions == null ? Collections.emptyList() : definitions;
    }
}
