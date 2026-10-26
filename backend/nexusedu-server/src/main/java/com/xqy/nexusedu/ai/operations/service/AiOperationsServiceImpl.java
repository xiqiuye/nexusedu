package com.xqy.nexusedu.ai.operations.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xqy.nexusedu.ai.chat.service.AiChatLifecycleService;
import com.xqy.nexusedu.ai.evaluation.service.AiConfiguration;
import com.xqy.nexusedu.ai.operations.mapper.AiOperationsMapper;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class AiOperationsServiceImpl implements AiOperationsService {
    @Autowired private AiChatLifecycleService lifecycle;
    @Autowired private AuthorizationPolicy authorization;
    private final AiOperationsMapper mapper;

    public AiOperationsServiceImpl(AiOperationsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void requireCaller(InternalCallContext caller) {
        authorization.authorize("authenticated", caller);
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        requireCaller(caller);
        filters = filters == null ? Map.of() : filters;
        String runId = null;
        String requestId = null;
        switch (resource) {
            case "conversations" -> authorization.authorize("authenticated", caller);
            case "runs" -> authorization.authorize("ai:run:read", caller);
            case "steps" -> {
                authorization.authorize("ai:run:read", caller);
                runId = requiredRunId(filters.get("runId"));
            }
            case "logs" -> {
                authorization.authorize("ai:log:read", caller);
                requestId = filters.get("requestId");
            }
            default -> throw new IllegalArgumentException("unknown query resource");
        }
        String search = ManagementRows.text(filters.get("search"), 128, false);
        return page(resource, caller.tenantId(), caller.userId().toString(), runId, requestId, search, current, size);
    }

    @Override
    public PageResult messages(Long conversationId, int current, int size, InternalCallContext caller) {
        requireConversation(conversationId, caller);
        current = Math.max(1, current);
        size = Math.max(1, Math.min(size, 200));
        Long total = mapper.countMessages(conversationId, caller.tenantId(), caller.userId().toString());
        List<Map<String, Object>> rows = mapper.selectMessages(conversationId, caller.tenantId(), caller.userId().toString(),
                size, ((long) current - 1) * size);
        for (Map<String, Object> row : rows) {
            Object json = row.remove("sourcesJson");
            row.put("sources", json == null ? List.of() : AiConfiguration.json(json.toString()));
        }
        return new PageResult(ManagementRows.publicRows(rows), current, size, total == null ? 0 : total);
    }

    @Override
    public Map<String, Object> delivery(String runId, InternalCallContext caller) {
        authorization.authorize("authenticated", caller);
        Map<String, Object> row = mapper.findDelivery(requiredRunId(runId), caller.tenantId(), caller.userId().toString());
        if (row == null) throw new ManagementNotFoundException();
        Map<String, Object> result = ManagementRows.publicRow(row);
        if (result.get("conversationId") != null) requireConversation(Long.valueOf(result.get("conversationId").toString()), caller);
        List<String> sources = mapper.findSources(caller.tenantId(), runId);
        result.put("sources", sources.isEmpty() ? List.of() : AiConfiguration.json(sources.getFirst()));
        List<String> answers = mapper.findAnswers(caller.tenantId(), caller.userId().toString(), runId);
        if (!answers.isEmpty()) result.put("answer", answers.getFirst());
        return result;
    }

    @Override
    public void requireConversation(Long id, InternalCallContext caller) {
        authorization.authorize("authenticated", caller);
        ManagementRows.id(id);
        if (mapper.countConversation(id, caller.tenantId(), caller.userId().toString()) != 1) throw new ManagementNotFoundException();
    }

    @Override public Map<String, Object> createConversation(String title, InternalCallContext caller) { requireCaller(caller); return lifecycle.create(title, caller); }
    @Override public void deleteConversation(Long id, InternalCallContext caller) { requireConversation(id, caller); lifecycle.delete(id, caller); }
    @Override public Map<String, Object> retention(InternalCallContext caller) { requireCaller(caller); return lifecycle.retention(caller); }
    @Override public void updateRetention(int days, InternalCallContext caller) { requireCaller(caller); lifecycle.retention(days, caller); }
    @Override public void keepConversation(Long id, String until, InternalCallContext caller) { requireConversation(id, caller); lifecycle.keepUntil(id, until, caller); }

    private PageResult page(String resource, String tenantId, String userId, String runId, String requestId,
                            String search, int current, int size) {
        current = Math.max(1, current);
        size = Math.max(1, Math.min(size, 200));
        Long total = mapper.countPage(resource, tenantId, userId, runId, requestId, search);
        List<Map<String, Object>> rows = mapper.selectPage(resource, tenantId, userId, runId, requestId, search,
                size, ((long) current - 1) * size);
        return new PageResult(ManagementRows.publicRows(rows), current, size, total == null ? 0 : total);
    }

    private String requiredRunId(String id) {
        if (id == null || id.isBlank() || id.length() > 64) throw new IllegalArgumentException("invalid runId");
        return id;
    }
}
