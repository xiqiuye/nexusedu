package com.xqy.nexusedu.infrastructure.ai.service.tool;

import java.util.Locale;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.application.ai.port.out.AiToolExecutor;
import com.xqy.nexusedu.common.model.ai.AiTask;
import com.xqy.nexusedu.common.model.ai.AiTaskResult;

/** Executes tools belonging to the student domain. */
@Component
public class StudentToolExecutor implements AiToolExecutor {

    private String domain = "student";
    private final StudentBusinessService studentBusinessService;

    public StudentToolExecutor(StudentBusinessService studentBusinessService) {
        this.studentBusinessService = studentBusinessService;
    }

    @Override
    public String domain() {
        return domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public AiTaskResult execute(AiTask aiTask) {
        if (aiTask == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        String toolName = aiTask.getToolName();
        if (!"query_student".equalsIgnoreCase(toolName)) {
            throw new IllegalArgumentException("Unsupported student tool: " + toolName);
        }
        AiTaskResult result = studentBusinessService.queryStudent(aiTask);
        result.setDomain(domain);
        result.setToolName(toolName == null ? null : toolName.trim().toLowerCase(Locale.ROOT));
        return result;
    }
}
