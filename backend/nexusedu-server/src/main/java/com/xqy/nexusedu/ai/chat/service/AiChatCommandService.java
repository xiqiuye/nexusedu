package com.xqy.nexusedu.ai.chat.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.chat.dto.AiChatCommand;
import com.xqy.nexusedu.ai.task.model.AiExecutionContext;
import com.xqy.nexusedu.ai.task.dto.AiRunView;
import com.xqy.nexusedu.ai.operations.service.AiOperationsService;
import com.xqy.nexusedu.ai.task.service.run.AiTaskRunService;
import com.xqy.nexusedu.ai.task.entity.AiTaskRun;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

@Service
public class AiChatCommandService {
    private static final Logger log = LoggerFactory.getLogger(AiChatCommandService.class);
    private final AiTaskService conversation;
    private final AiTaskRunService runs;
    private final InternalCallSigner internalCallSigner;
    private final AiOperationsService operations;

    public AiChatCommandService(AiTaskService conversation, AiTaskRunService runs, @Value("${nexusedu.internal-rpc.secret}") String internalRpcSecret, AiOperationsService operations) {
        this.conversation = conversation;
        this.runs = runs;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
        this.operations = operations;
    }

    public void submit(AiChatCommand command) {
        submitRun(command);
    }

    public Map<String, Object> submitRun(AiChatCommand command) {
        if (command == null) throw new IllegalArgumentException("chat command must not be null");
        InternalCallContext context = authenticate(command.callerContext());
        AiExecutionContext execution = command.getExecutionContext();
        if (execution == null || !Objects.equals(context.userId(), execution.getUserId()) || !Objects.equals(context.sessionId(), execution.getSessionId()) || !Objects.equals(context.tenantId(), execution.getTenantId()) || !Objects.equals(context.requestId(), execution.getRequestId())) {
            throw new InternalCallSigner.InternalCallAuthenticationException();
        }
        if (execution.getConversationId() != null)
            operations.requireConversation(execution.getConversationId(), context);
        log.info("rpc_audit action=chat_submit caller={} tenant={} user={} requestId={}", context.callerService(), context.tenantId(), context.userId(), context.requestId());
        AiTaskRun run = conversation.start(execution);
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("runId", run.getRunId());
        receipt.put("status", run.getStatus());
        receipt.put("conversationId", run.getConversationId() == null ? null : run.getConversationId().toString());
        return receipt;
    }

    public void cancel(String runId, InternalCallContext callerContext) {
        InternalCallContext context = authenticate(callerContext);
        log.info("rpc_audit action=chat_cancel caller={} tenant={} user={} requestId={}", context.callerService(), context.tenantId(), context.userId(), context.requestId());
        AiExecutionContext execution = new AiExecutionContext();
        execution.setRunId(runId);
        execution.setRequestId(context.requestId());
        execution.setTenantId(context.tenantId());
        execution.setUserId(context.userId());
        execution.setSessionId(context.sessionId());
        conversation.cancel(execution);
    }

    public AiRunView getRun(String runId, InternalCallContext callerContext) {
        InternalCallContext context = authenticate(callerContext);
        AiTaskRun run = runs.requireOwnedRun(runId, context.tenantId(), String.valueOf(context.userId()));
        log.info("rpc_audit action=run_get caller={} tenant={} user={} requestId={} runId={}", context.callerService(), context.tenantId(), context.userId(), context.requestId(), runId);
        return new AiRunView(run.getRunId(), run.getStatus(), run.getCurrentStepNo(), run.getStartTime(), run.getFinishTime());
    }

    private InternalCallContext authenticate(InternalCallContext context) {
        internalCallSigner.verify(context, "nexusedu-web-bff");
        operations.requireCaller(context);
        return context;
    }
}
