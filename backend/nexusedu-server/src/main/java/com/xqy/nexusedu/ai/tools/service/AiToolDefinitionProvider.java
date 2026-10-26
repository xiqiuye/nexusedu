package com.xqy.nexusedu.ai.tools.service;

import java.util.List;
import com.xqy.nexusedu.ai.tools.entity.AiToolDefinition;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }



}
