package com.xqy.nexusedu.ai.governance.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.xqy.nexusedu.ai.task.model.AiExecutionContext;
import com.xqy.nexusedu.ai.task.model.AiTask;
import com.xqy.nexusedu.ai.task.model.AiTaskResult;
import com.xqy.nexusedu.ai.tools.service.StudentBusinessService;
import com.xqy.nexusedu.ai.tools.service.StudentToolExecutor;
import com.xqy.nexusedu.education.student.profile.service.StudentService;
import com.xqy.nexusedu.education.student.profile.vo.StudentData;

class AiSafetyPolicyStudentResultTest {
    @Test
    void passesAuthorizedStudentDetailsFromToolToComposePrompt() {
        StudentService students = mock(StudentService.class);
        StudentData student = student();
        when(students.queryStudents(isNull(), isNull(), isNull(), eq(100), eq("school-a"), eq(7L)))
                .thenReturn(List.of(student));
        StudentBusinessService business = new StudentBusinessService();
        ReflectionTestUtils.setField(business, "studentService", students);
        AiExecutionContext context = new AiExecutionContext();
        context.setTenantId("school-a");
        context.setUserId(7L);
        AiTask task = new AiTask();
        task.setDomain("student");
        task.setToolName("query_student");
        task.setExecutionContext(context);

        AiTaskResult result = new StudentToolExecutor(business).execute(task);
        String prompt = AiSafetyPolicy.promptResults(List.of(result));

        assertTrue(prompt.contains("张三"), "Compose must receive the student's name, not just count");
        assertTrue(prompt.contains("S2026001"));
        assertTrue(prompt.contains("studentNo"));
        assertTrue(prompt.contains("2026-09-01"));
        assertFalse(prompt.contains("private@example.test"));
        assertFalse(prompt.contains("13800000000"));
        assertFalse(prompt.contains("2010-01-02"));
        verify(students).queryStudents(null, null, null, 100, "school-a", 7L);
    }

    private static StudentData student() {
        return new StudentData(1L, "S2026001", "张三", 16, "男", LocalDate.of(2010, 1, 2),
                "private@example.test", "13800000000", LocalDateTime.of(2026, 9, 1, 8, 0));
    }

    @Test
    void exposesOnlyAllowedFieldsEvenIfToolReturnsSensitiveData() {
        AiTaskResult result = result(List.of(student()));
        String prompt = AiSafetyPolicy.promptResults(List.of(result));
        assertTrue(prompt.contains("张三"));
        assertFalse(prompt.contains("private@example.test"));
        assertFalse(prompt.contains("13800000000"));
        assertFalse(prompt.contains("2010-01-02"));
        assertFalse(prompt.contains("\"age\""));
    }

    @Test
    void handlesEmptyMissingAndUnexpectedItemsWithoutLeakingOtherToolData() {
        for (List<?> items : new List<?>[] { null, List.of(), Collections.singletonList(null),
                List.of(Map.of("name", "private-name", "phone", "13800000000")) }) {
            String prompt = AiSafetyPolicy.promptResults(List.of(result(items)));
            assertFalse(prompt.contains("student="));
            assertFalse(prompt.contains("13800000000"));
        }
        AiTaskResult failed = result(List.of(student()));
        failed.setSuccess(false);
        assertFalse(AiSafetyPolicy.promptResults(List.of(failed)).contains("张三"));
        AiTaskResult otherTool = result(List.of(student()));
        otherTool.setToolName("other_tool");
        assertFalse(AiSafetyPolicy.promptResults(List.of(otherTool)).contains("张三"));
    }

    @Test
    void keepsStudentDataUntrustedRedactedAndBounded() {
        StudentData student = student();
        student.setName("password=do-not-expose");
        student.setStudentNo("S".repeat(200));
        String prompt = AiSafetyPolicy.promptResults(List.of(result(Collections.nCopies(100, student))));
        assertTrue(prompt.startsWith("UNTRUSTED_TOOL_DATA:"));
        assertTrue(prompt.contains("[REDACTED]"));
        assertFalse(prompt.contains("do-not-expose"));
        assertTrue(prompt.length() <= 12_000 + "...[truncated]".length());
        assertTrue(prompt.endsWith("...[truncated]"));
    }

    private static AiTaskResult result(List<?> items) {
        AiTaskResult result = new AiTaskResult();
        result.setDomain("student");
        result.setToolName("query_student");
        result.setSuccess(true);
        result.setItems(items);
        result.setSummary(Map.of("count", items == null ? 0 : items.size()));
        return result;
    }
}
