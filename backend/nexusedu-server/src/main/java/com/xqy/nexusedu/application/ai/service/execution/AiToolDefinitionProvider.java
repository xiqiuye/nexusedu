package com.xqy.nexusedu.application.ai.service.execution;

import java.util.List;
import com.xqy.nexusedu.infrastructure.ai.persistence.entity.AiToolDefinition;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }



}
