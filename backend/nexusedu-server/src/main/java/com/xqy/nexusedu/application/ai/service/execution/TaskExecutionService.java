package com.xqy.nexusedu.application.ai.service.execution;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.domain.ai.model.AiExecutionContext;
import com.xqy.nexusedu.application.ai.service.run.AiTaskRunService;
import com.xqy.nexusedu.infrastructure.ai.persistence.entity.AiTaskStep;
import com.xqy.nexusedu.domain.ai.model.AiTask;
import com.xqy.nexusedu.domain.ai.model.AiTaskResult;
import com.xqy.nexusedu.infrastructure.ai.model.AiToolRegistry;
import com.xqy.nexusedu.infrastructure.ai.security.AiSafetyPolicy;
import com.xqy.nexusedu.infrastructure.ai.service.observation.AiObservationService;

/**
 * Executes one tool step with cancellation, timeout, retries, leases, and durable outcomes.
 */
@Service
public class TaskExecutionService {
    private final AiToolRegistry toolRegistry;
    private final AiTaskRunService runService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    @Autowired
    private AiObservationService observations;

    public TaskExecutionService(AiToolRegistry toolRegistry, AiTaskRunService runService) {
        this.toolRegistry = toolRegistry;
        this.runService = runService;
    }

    public AiTaskResult execute(AiExecutionContext context, AiTaskStep step, AiTask task) {
        String runId = context.getRunId();
        task.setExecutionContext(context);
        validate(task);
        int maxAttempt = Math.max(1, step.getMaxAttempt());
        for (int attempt = Math.max(1, step.getAttempt() + 1); attempt <= maxAttempt; attempt++) {
            if (runService.isCancellationRequested(runId)) {
                runService.markCancelled(runId);
                throw new TaskCancelledException();
            }
            runService.startStep(step, attempt);
            var observation = observations == null ? null : observations.observation(context, "TOOL_ATTEMPT", task.getToolName(), step.getStepId(), attempt);
            Future<AiTaskResult> future;
            try {
                future = executor.submit(() -> toolRegistry.execute(task));
            } catch (RuntimeException error) {
                if (observation != null) observation.finish("ERROR", null, error);
                handleFailure(step, attempt, maxAttempt, error);
                continue;
            }
            runningTasks.put(runId, future);
            try {
                AiTaskResult result = future.get(step.getTimeoutMs(), TimeUnit.MILLISECONDS);
                if (result == null || !result.isSuccess())
                    throw new IllegalStateException("Tool returned a failed result");
                if (observation != null)
                    observation.finish("SUCCESS", result.getItems() == null ? 0 : result.getItems().size(), null);
                runService.succeedStep(step, JSONUtil.toJsonStr(result));
                return result;
            } catch (TimeoutException exception) {
                if (observation != null) observation.finish("TIMEOUT", null, exception);
                future.cancel(true);
                handleFailure(step, attempt, maxAttempt, exception);
            } catch (TaskCancelledException exception) {
                if (observation != null) observation.finish("CANCELLED", null, exception);
                future.cancel(true);
                runService.markCancelled(runId);
                throw exception;
            } catch (CancellationException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                future.cancel(true);
                if (observation != null) observation.finish("CANCELLED", null, exception);
                runService.markCancelled(runId);
                throw new TaskCancelledException();
            } catch (Exception exception) {
                if (observation != null)
                    observation.finish(AiObservationService.outcome(unwrap(exception)), null, unwrap(exception));
                if ("CANCELLED".equals(AiObservationService.outcome(unwrap(exception)))) {
                    runService.markCancelled(runId);
                    throw new TaskCancelledException();
                }
                handleFailure(step, attempt, maxAttempt, unwrap(exception));
            } finally {
                runningTasks.remove(runId, future);
            }
        }
        throw new IllegalStateException("Task step exhausted retry attempts");
    }

    /**
     * Interrupts the active tool invocation, if its executor honors interruption.
     */
    public void cancel(String runId) {
        Future<?> future = runningTasks.get(runId);
        if (future != null) future.cancel(true);
    }

    private void handleFailure(AiTaskStep step, int attempt, int maxAttempt, Exception error) {
        String code = error.getClass().getSimpleName();
        String message = error.getMessage();
        if (attempt >= maxAttempt) {
            runService.failStep(step, code, message);
            throw new IllegalStateException("Task step failed after " + attempt + " attempts", error);
        }
        LocalDateTime retryAt = LocalDateTime.now().plusSeconds(Math.min(30, 1L << Math.min(attempt, 5)));
        runService.retryStep(step, code, message, retryAt);
        try {
            Thread.sleep(Math.max(1, Duration.between(LocalDateTime.now(), retryAt).toMillis()));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new TaskCancelledException();
        }
    }

    private void validate(AiTask task) {
        if (task == null || task.getMissingArgs() != null && !task.getMissingArgs().isEmpty()) {
            throw new IllegalArgumentException("Task is missing required arguments");
        }
        AiSafetyPolicy.validateToolTask(task);
    }

    private Exception unwrap(Exception error) {
        return error.getCause() instanceof Exception cause ? cause : error;
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    public static class TaskCancelledException extends RuntimeException {
        public TaskCancelledException() {
            super("AI task run was cancelled");
        }
    }
}
