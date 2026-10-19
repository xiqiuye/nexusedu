package com.xqy.nexusedu.ai.infrastructure.observability.observation;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.ai.application.port.in.AiOperationsService;
import com.xqy.nexusedu.system.application.port.in.SystemManagementService;
import com.xqy.nexusedu.ai.application.service.chat.AiConversationLifecycle;
import com.xqy.nexusedu.shared.dto.ManagementRows;
import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.shared.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;
import com.xqy.nexusedu.ai.infrastructure.evaluation.AiConfiguration;

@Service
public class AiOperationsServiceImpl implements AiOperationsService {
    @Autowired private AiConversationLifecycle lifecycle;
    @Autowired private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    public AiOperationsServiceImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void requireCaller(InternalCallContext caller) { authorization.authorize("authenticated", caller); }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        requireCaller(caller);
        filters = filters == null ? Map.of() : filters;
        String from, columns, where; List<Object> args = new ArrayList<>(); args.add(caller.tenantId());
        switch (resource) {
            case "conversations" -> {
                authorization.authorize("authenticated", caller);
                from = "ai_chat_conversation c"; columns = "c.id,c.title,c.session_id,c.status,c.last_message_at,c.create_time,c.retain_until";
                where = " WHERE c.tenant_id=? AND c.user_id=? AND c.status='ACTIVE'"; args.add(caller.userId().toString());
            }
            case "runs" -> {
                authorization.authorize("ai:run:read", caller);
                from = "ai_task_run c"; columns = "c.id,c.run_id,c.request_id,c.conversation_id,c.user_id,c.question,c.model_alias,c.status,c.current_step_no,c.cancel_request,c.start_time,c.finish_time,c.error_code,c.error_message";
                where = " WHERE c.tenant_id=?";
            }
            case "steps" -> {
                authorization.authorize("ai:run:read", caller);
                from = "ai_task_step c JOIN ai_task_run r ON r.run_id=c.task_run_id";
                columns = "c.id,c.task_run_id,c.step_id,c.step_no,c.step_type,c.domain,c.tool_name,c.status,c.attempt,c.max_attempt,c.start_time,c.finish_time,c.error_code,c.error_message,c.input_json,c.output_json";
                where = " WHERE r.tenant_id=? AND r.run_id=?"; args.add(requiredRunId(filters.get("runId")));
            }
            case "logs" -> {
                authorization.authorize("ai:log:read", caller);
                from = "ai_tool_execute_log c"; columns = "c.id,c.tool_execute_id,c.request_id,c.domain,c.tool_name,c.start_time,c.finish_time,c.duration_time,c.success,c.error_code,c.error_message";
                where = " WHERE c.tenant_id=?";
                if (filters.get("requestId") != null && !filters.get("requestId").isBlank()) { where += " AND c.request_id=?"; args.add(filters.get("requestId")); }
            }
            default -> throw new IllegalArgumentException("未知查询对象");
        }
        String search = ManagementRows.text(filters.get("search"), 128, false);
        if (search != null && !search.isBlank()) {
            String column = resource.equals("conversations") ? "c.title" : resource.equals("runs") ? "c.run_id" : "c.tool_name";
            where += " AND " + column + " LIKE ?"; args.add("%" + search + "%");
        }
        return page(from, columns, where, args, current, size, "c.id DESC");
    }

    @Override
    public PageResult messages(Long conversationId, int current, int size, InternalCallContext caller) {
        requireConversation(conversationId, caller);
        PageResult result = page("ai_chat_message c LEFT JOIN ai_task_run r ON r.run_id=c.run_id AND r.tenant_id=c.tenant_id LEFT JOIN ai_run_sources s ON s.run_id=c.run_id AND s.tenant_id=c.tenant_id AND c.role='assistant'",
                "c.id,c.conversation_id,c.role,c.content,c.run_id,c.sequence_no,c.create_time,r.status AS run_status,s.sources_json",
                " WHERE c.conversation_id=? AND c.tenant_id=? AND c.user_id=?",
                new ArrayList<>(List.of(conversationId, caller.tenantId(), caller.userId().toString())), current, size, "c.sequence_no ASC");
        for(Map<String,Object> row:result.getRecords()) { Object json=row.remove("sourcesJson"); row.put("sources",json==null?List.of():AiConfiguration.json(json.toString())); }
        return result;
    }

    @Override
    public Map<String, Object> delivery(String runId, InternalCallContext caller) {
        authorization.authorize("authenticated", caller);
        List<Map<String, Object>> runs = jdbc.queryForList("SELECT run_id,status,stream_key,conversation_id,question,error_message FROM ai_task_run WHERE run_id=? AND tenant_id=? AND user_id=?", requiredRunId(runId), caller.tenantId(), caller.userId().toString());
        if (runs.isEmpty()) throw new ManagementNotFoundException();
        Map<String, Object> result = ManagementRows.publicRow(runs.getFirst());
        if(result.get("conversationId")!=null) requireConversation(Long.valueOf(result.get("conversationId").toString()),caller);
        var sources=jdbc.queryForList("SELECT sources_json FROM ai_run_sources WHERE tenant_id=? AND run_id=?",String.class,caller.tenantId(),runId);
        result.put("sources",sources.isEmpty()?List.of():AiConfiguration.json(sources.getFirst()));
        List<String> answer = jdbc.queryForList("SELECT content FROM ai_chat_message WHERE tenant_id=? AND user_id=? AND run_id=? AND role='assistant'", String.class, caller.tenantId(), caller.userId().toString(), runId);
        if (!answer.isEmpty()) result.put("answer", answer.getFirst());
        return result;
    }

    @Override
    public void requireConversation(Long id, InternalCallContext caller) {
        authorization.authorize("authenticated", caller); ManagementRows.id(id);
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM ai_chat_conversation WHERE id=? AND tenant_id=? AND user_id=? AND status='ACTIVE'", Integer.class, id, caller.tenantId(), caller.userId().toString());
        if (found == null || found != 1) throw new ManagementNotFoundException();
    }
    @Override public Map<String,Object> createConversation(String title,InternalCallContext caller) { requireCaller(caller);return lifecycle.create(title,caller); }
    @Override public void deleteConversation(Long id,InternalCallContext caller) { requireConversation(id,caller);lifecycle.delete(id,caller); }
    @Override public Map<String,Object> retention(InternalCallContext caller) { requireCaller(caller);return lifecycle.retention(caller); }
    @Override public void updateRetention(int days,InternalCallContext caller) { requireCaller(caller);lifecycle.retention(days,caller); }
    @Override public void keepConversation(Long id,String until,InternalCallContext caller) { requireConversation(id,caller);lifecycle.keepUntil(id,until,caller); }
    private PageResult page(String from, String columns, String where, List<Object> args, int current, int size, String order) {
        current = Math.max(1, current); size = Math.max(1, Math.min(size, 200));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + from + where, Long.class, args.toArray());
        args.add(size); args.add(((long) current - 1) * size);
        return new PageResult(ManagementRows.publicRows(jdbc.queryForList("SELECT " + columns + " FROM " + from + where + " ORDER BY " + order + " LIMIT ? OFFSET ?", args.toArray())), current, size, total == null ? 0 : total);
    }
    private String requiredRunId(String id) { if (id == null || id.isBlank() || id.length() > 64) throw new IllegalArgumentException("无效 runId"); return id; }
}
