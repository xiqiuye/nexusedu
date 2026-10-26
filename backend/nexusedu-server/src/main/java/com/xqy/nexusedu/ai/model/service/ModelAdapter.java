package com.xqy.nexusedu.ai.model.service;

import com.xqy.nexusedu.ai.model.entity.AiModelDefinition;

/** Provider adapter that turns a model definition into LangChain4j models. */
public interface ModelAdapter {

    String provider();

    ModelHandle create(AiModelDefinition modelDefinition);
    default ModelHandle create(AiModelDefinition definition,int maxOutputTokens) {return create(definition);}
}
