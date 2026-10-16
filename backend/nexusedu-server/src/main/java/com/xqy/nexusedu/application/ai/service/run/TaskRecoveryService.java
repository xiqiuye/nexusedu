package com.xqy.nexusedu.application.ai.service.run;

import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.application.ai.service.execution.TaskExecutionService;
import com.xqy.nexusedu.infrastructure.ai.persistence.entity.AiTaskStep;
import com.xqy.nexusedu.domain.ai.model.AiTask;

/**
 * Reclaims expired leases and due retries after a process failure.
 */
@Service
public class TaskRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryService.class);
    private final AiTaskRunService runService;
    private final TaskExecutionService executionService;

    public TaskRecoveryService(AiTaskRunService runService, TaskExecutionService executionService) {
        this.runService = runService;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "${nexusedu.ai.task-recovery-delay-ms}")
    public void recover() {
        for (AiTaskStep step : runService.listRecoverableSteps()) {
            try {
                runService.markRecoverableStepPending(step);
                if (!"TOOL".equals(step.getStepType())) continue;
                AiTask task = JSONUtil.toBean(step.getInputJson(), AiTask.class);
                executionService.execute(runService.executionContext(runService.requireRun(step.getTaskRunId())), step, task);
            } catch (Exception error) {
                // The execution service persists retry/failure details; the next sweep can claim due work.
                log.warn("Unable to recover AI task step {}", step.getStepId(), error);
            }
        }
    }
}
