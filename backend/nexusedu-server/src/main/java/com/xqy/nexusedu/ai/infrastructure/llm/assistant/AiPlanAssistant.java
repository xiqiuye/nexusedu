package com.xqy.nexusedu.ai.infrastructure.llm.assistant;

import dev.langchain4j.service.UserMessage;

public interface AiPlanAssistant {


    String plan(@UserMessage String prompt);

}
