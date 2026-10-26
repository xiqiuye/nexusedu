package com.xqy.nexusedu.ai.model.service;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.model.entity.AiModelDefinition;
import com.xqy.nexusedu.ai.model.mapper.AiModelDefinitionMapper;

/** Loads model definitions from the database. */
@Service
public class AiModelDefinitionService {

    private final AiModelDefinitionMapper aiModelDefinitionMapper;

    public AiModelDefinitionService(AiModelDefinitionMapper aiModelDefinitionMapper) {
        this.aiModelDefinitionMapper = aiModelDefinitionMapper;
    }

    public List<AiModelDefinition> definitions() {
        List<AiModelDefinition> definitions = aiModelDefinitionMapper.selectList(null);
        return definitions == null ? Collections.emptyList() : definitions;
    }
}
