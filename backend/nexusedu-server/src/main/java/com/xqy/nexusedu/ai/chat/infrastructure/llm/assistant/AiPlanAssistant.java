package com.xqy.nexusedu.ai.chat.infrastructure.llm.assistant;

import dev.langchain4j.service.UserMessage;

public interface AiPlanAssistant {


    String plan(@UserMessage String prompt);

}
