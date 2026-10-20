package com.xqy.nexusedu.ai.task.application.service.plan;

import java.util.List;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.ai.task.domain.constant.AiConstants;
import com.xqy.nexusedu.ai.task.domain.model.AiTask;
import com.xqy.nexusedu.ai.chat.infrastructure.llm.chat.AiChatService;

/** Compatibility facade for existing callers; lifecycle work lives in dedicated services. */
@Component
public class AiPlanService {
    private final TaskPlannerService plannerService;
    private final AiChatService chatService;

    public AiPlanService(TaskPlannerService plannerService, AiChatService chatService) {
        this.plannerService = plannerService;
        this.chatService = chatService;
    }

    public String sampleChat(String question) { return chatService.sampleChat(question); }
    public List<AiTask> planTasks(String question, String businessContext) {
        return planTasks(question, businessContext, AiConstants.MODEL_ALIAS.STRONG);
    }
    public List<AiTask> planTasks(String question, String businessContext, String alias) {
        return plannerService.plan(question, businessContext, alias);
    }
}
