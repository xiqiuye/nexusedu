package com.xqy.nexusedu.ai.tools.service;

import com.xqy.nexusedu.ai.task.model.AiTask;
import com.xqy.nexusedu.ai.task.model.AiTaskResult;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
