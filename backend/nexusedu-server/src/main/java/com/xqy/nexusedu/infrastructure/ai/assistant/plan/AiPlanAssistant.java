package com.xqy.nexusedu.infrastructure.ai.assistant.plan;

import dev.langchain4j.service.UserMessage;

public interface AiPlanAssistant {


    String plan(@UserMessage String prompt);

}
