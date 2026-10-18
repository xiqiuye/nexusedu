package com.xqy.nexusedu.application.ai.port.out;

import com.xqy.nexusedu.common.model.ai.AiTask;
import com.xqy.nexusedu.common.model.ai.AiTaskResult;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
