package com.xqy.nexusedu.ai.application.port.out;

import com.xqy.nexusedu.ai.domain.model.AiTask;
import com.xqy.nexusedu.ai.domain.model.AiTaskResult;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
