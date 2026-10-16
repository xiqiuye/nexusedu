package com.xqy.nexusedu.application.ai.service.execution;

import com.xqy.nexusedu.domain.ai.model.AiTask;
import com.xqy.nexusedu.domain.ai.model.AiTaskResult;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
