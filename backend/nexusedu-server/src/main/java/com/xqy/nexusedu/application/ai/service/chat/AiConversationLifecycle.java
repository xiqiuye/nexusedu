package com.xqy.nexusedu.application.ai.service.chat;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.common.dto.ManagementRows;
import com.xqy.nexusedu.common.exception.ManagementConflictException;
import com.xqy.nexusedu.common.exception.ManagementNotFoundException;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;
import com.xqy.nexusedu.infrastructure.ai.messaging.RedisChatMemoryStore;

@Service
public class AiConversationLifecycle {
    @Autowired
    private StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final RedisChatMemoryStore memory;

    public AiConversationLifecycle(JdbcTemplate jdbc, PlatformTransactionManager manager, RedisChatMemoryStore memory) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.memory = memory;
    }

    public Map<String, Object> create(String title, InternalCallContext caller) {
        Long id = IdWorker.getId();
        jdbc.update("INSERT INTO ai_chat_conversation(id,tenant_id,user_id,session_id,title,create_by,modify_by) VALUES(?,?,?,?,?,?,?)",
                id, caller.tenantId(), caller.userId().toString(), caller.sessionId().toString(),
                title == null || title.isBlank() ? "新对话" : ManagementRows.text(title, 512, true), caller.userId().toString(), caller.userId().toString());
        return Map.of("id", id.toString(), "title", title == null || title.isBlank() ? "新对话" : title, "status", "ACTIVE");
    }

    public Map<String, Object> retention(InternalCallContext caller) {
        var rows = jdbc.queryForList("SELECT retention_days FROM ai_chat_retention WHERE tenant_id=? AND user_id=?", caller.tenantId(), caller.userId().toString());
        return Map.of("retentionDays", rows.isEmpty() ? 90 : rows.getFirst().get("retention_days"));
    }

    public void retention(int days, InternalCallContext caller) {
        if (days < 1 || days > 3650) throw new IllegalArgumentException("留存天数应在 1–3650 之间");
        jdbc.update("INSERT INTO ai_chat_retention(tenant_id,user_id,retention_days) VALUES(?,?,?) ON DUPLICATE KEY UPDATE retention_days=VALUES(retention_days)", caller.tenantId(), caller.userId().toString(), days);
    }

    public void keepUntil(Long id, String until, InternalCallContext caller) {
        LocalDateTime date = until == null || until.isBlank() ? null : LocalDateTime.parse(until);
        if (date != null && (date.isBefore(LocalDateTime.now()) || date.isAfter(LocalDateTime.now().plusYears(10))))
            throw new IllegalArgumentException("留存截止时间须在未来十年内");
        if (jdbc.update("UPDATE ai_chat_conversation SET retain_until=?,modify_by=? WHERE id=? AND tenant_id=? AND user_id=? AND status='ACTIVE'", date, caller.userId().toString(), id, caller.tenantId(), caller.userId().toString()) != 1)
            throw new ManagementNotFoundException();
    }

    public void delete(Long id, InternalCallContext caller) {
        erase(id, caller.tenantId(), caller.userId().toString(), false);
    }

    private void erase(Long id, String tenant, String user, boolean expiredOnly) {
        transactions.executeWithoutResult(tx -> {
            var rows = jdbc.queryForList("SELECT id FROM ai_chat_conversation WHERE id=? AND tenant_id=? AND user_id=? AND status='ACTIVE' FOR UPDATE", id, tenant, user);
            if (rows.isEmpty()) {
                if (expiredOnly) return;
                throw new ManagementNotFoundException();
            }
            if (expiredOnly && jdbc.queryForObject(expiredSql() + " AND c.id=?", Integer.class, id) == 0) return;
            Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM ai_task_run WHERE tenant_id=? AND conversation_id=? AND status NOT IN ('SUCCEEDED','FAILED','CANCELLED')", Integer.class, tenant, id);
            if (active != null && active > 0) {
                if (expiredOnly) return;
                throw new ManagementConflictException("对话有执行中的任务，请先停止生成");
            }
            // Redis failure rolls the SQL transaction back, so cleanup is safely retryable.
            memory.deleteMessages("chat_" + tenant + "_" + user + "_" + id);
            if (redis != null) {
                var keys = jdbc.queryForList("SELECT stream_key FROM ai_task_run WHERE tenant_id=? AND conversation_id=? AND stream_key IS NOT NULL", String.class, tenant, id);
                for (String stream : keys) redis.delete(stream);
                redis.delete("business_" + tenant + "_" + user + "_" + id);
            }
            jdbc.update("DELETE FROM ai_run_sources WHERE tenant_id=? AND conversation_id=?", tenant, id);
            jdbc.update("DELETE FROM ai_chat_message WHERE conversation_id=? AND tenant_id=?", id, tenant);
            jdbc.update("UPDATE ai_chat_conversation SET status='DELETED',default_slot=NULL,title='已删除',retain_until=NULL WHERE id=?", id);
            // Erase content-bearing execution artifacts; accounting observations remain.
            jdbc.update("UPDATE ai_task_step s JOIN ai_task_run r ON r.run_id=s.task_run_id SET s.input_json=NULL,s.output_json=NULL,s.error_message=NULL WHERE r.tenant_id=? AND r.conversation_id=?", tenant, id);
            jdbc.update("UPDATE ai_request_log l JOIN ai_task_run r ON r.request_id=l.request_id AND r.tenant_id=l.tenant_id SET l.question=NULL,l.error_message=NULL WHERE r.tenant_id=? AND r.conversation_id=?", tenant, id);
            jdbc.update("UPDATE ai_tool_execute_log l JOIN ai_task_run r ON r.request_id=l.request_id AND r.tenant_id=l.tenant_id SET l.question=NULL,l.error_message=NULL WHERE r.tenant_id=? AND r.conversation_id=?", tenant, id);
            jdbc.update("UPDATE ai_task_run SET question='',plan_json=NULL,error_message=NULL WHERE tenant_id=? AND conversation_id=?", tenant, id);
        });
    }

    private String expiredSql() {
        return "SELECT COUNT(*) FROM ai_chat_conversation c LEFT JOIN ai_chat_retention p ON p.tenant_id=c.tenant_id AND p.user_id=c.user_id WHERE c.status='ACTIVE' AND COALESCE(c.retain_until,DATE_ADD(COALESCE(c.last_message_at,c.create_time),INTERVAL COALESCE(p.retention_days,90) DAY))<CURRENT_TIMESTAMP AND NOT EXISTS(SELECT 1 FROM ai_task_run r WHERE r.conversation_id=c.id AND r.tenant_id=c.tenant_id AND r.status NOT IN ('SUCCEEDED','FAILED','CANCELLED'))";
    }

    @Scheduled(fixedDelayString = "${nexusedu.ai.conversation-retention.scan-ms:3600000}")
    public void sweep() {
        var rows = jdbc.queryForList(expiredSql().replace("SELECT COUNT(*)", "SELECT c.id,c.tenant_id,c.user_id") + " LIMIT 100");
        for (var row : rows)
            erase(((Number) row.get("id")).longValue(), row.get("tenant_id").toString(), row.get("user_id").toString(), true);
    }
}
