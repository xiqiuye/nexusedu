package com.xqy.nexusedu.application.ai.port.out;

import java.util.List;
import com.xqy.nexusedu.common.entity.ai.AiToolDefinition;

/** Supplies the tool definitions visible to an AI model. */
public interface AiToolDefinitionProvider {

    List<AiToolDefinition> definitions();

    default List<AiToolDefinition> getDefinitions() {
        return definitions();
    }



}
