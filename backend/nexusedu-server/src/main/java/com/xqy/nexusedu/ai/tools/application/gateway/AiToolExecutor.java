package com.xqy.nexusedu.ai.tools.application.gateway;

import com.xqy.nexusedu.ai.task.domain.model.AiTask;
import com.xqy.nexusedu.ai.task.domain.model.AiTaskResult;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
