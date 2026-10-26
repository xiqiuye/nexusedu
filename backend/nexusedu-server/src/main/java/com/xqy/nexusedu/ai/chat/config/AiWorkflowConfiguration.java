package com.xqy.nexusedu.ai.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiWorkflowConfiguration {

    @Bean("aiWorkflowExecutor")
    public TaskExecutor aiWorkflowExecutor(
            @Value("${nexusedu.ai.workflow.core-pool-size}") int corePoolSize,
            @Value("${nexusedu.ai.workflow.max-pool-size}") int maxPoolSize,
            @Value("${nexusedu.ai.workflow.queue-capacity}") int queueCapacity) {
        return executor(corePoolSize, maxPoolSize, queueCapacity, "ai-workflow-");
    }

    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor(
            @Value("${nexusedu.ai.task-executor.core-pool-size:8}") int corePoolSize,
            @Value("${nexusedu.ai.task-executor.max-pool-size:32}") int maxPoolSize,
            @Value("${nexusedu.ai.task-executor.queue-capacity:200}") int queueCapacity) {
        return executor(corePoolSize, maxPoolSize, queueCapacity, "ai-task-");
    }

    @Bean("aiSseExecutor")
    public ThreadPoolTaskExecutor aiSseExecutor(
            @Value("${nexusedu.ai.sse-executor.core-pool-size:4}") int corePoolSize,
            @Value("${nexusedu.ai.sse-executor.max-pool-size:16}") int maxPoolSize,
            @Value("${nexusedu.ai.sse-executor.queue-capacity:100}") int queueCapacity) {
        return executor(corePoolSize, maxPoolSize, queueCapacity, "ai-sse-");
    }

    private ThreadPoolTaskExecutor executor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
