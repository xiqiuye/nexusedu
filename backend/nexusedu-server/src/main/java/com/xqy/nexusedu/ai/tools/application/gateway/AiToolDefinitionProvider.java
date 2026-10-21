package com.xqy.nexusedu.ai.tools.application.gateway;

import java.util.List;
import com.xqy.nexusedu.ai.tools.infrastructure.persistence.entity.AiToolDefinition;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }



}
