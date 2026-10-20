package com.xqy.nexusedu.ai.chat.infrastructure.config;

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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-workflow-");
        executor.initialize();
        return executor;
    }
}
