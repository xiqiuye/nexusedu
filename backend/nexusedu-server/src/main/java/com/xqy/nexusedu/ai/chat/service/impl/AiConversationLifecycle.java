package com.xqy.nexusedu.ai.chat.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.ai.chat.infrastructure.messaging.RedisChatMemoryStore;
import com.xqy.nexusedu.ai.chat.mapper.AiConversationLifecycleMapper;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class AiConversationLifecycle {
    @Autowired private StringRedisTemplate redis;
    private final AiConversationLifecycleMapper mapper;
    private final TransactionTemplate transactions;
    private final RedisChatMemoryStore memory;

    public AiConversationLifecycle(AiConversationLifecycleMapper mapper, PlatformTransactionManager manager, RedisChatMemoryStore memory) {
        this.mapper = mapper; this.transactions = new TransactionTemplate(manager); this.memory = memory;
    }

    public Map<String,Object> create(String title, InternalCallContext caller) {
        Long id = IdWorker.getId(); String actualTitle = title == null || title.isBlank() ? "新对话" : ManagementRows.text(title, 512, true);
        mapper.insertConversation(id, caller.tenantId(), caller.userId().toString(), caller.sessionId().toString(), actualTitle);
        return Map.of("id", id.toString(), "title", actualTitle, "status", "ACTIVE");
    }

    public Map<String,Object> retention(InternalCallContext caller) { Integer days = mapper.retentionDays(caller.tenantId(), caller.userId().toString()); return Map.of("retentionDays", days == null ? 90 : days); }
    public void retention(int days, InternalCallContext caller) { if (days < 1 || days > 3650) throw new IllegalArgumentException("留存天数应在 1-3650 之间"); mapper.upsertRetention(caller.tenantId(), caller.userId().toString(), days); }

    public void keepUntil(Long id, String until, InternalCallContext caller) {
        LocalDateTime date = until == null || until.isBlank() ? null : LocalDateTime.parse(until);
        if (date != null && (date.isBefore(LocalDateTime.now()) || date.isAfter(LocalDateTime.now().plusYears(10)))) throw new IllegalArgumentException("留存截止时间须在未来十年内");
        if (mapper.updateKeepUntil(id, date, caller.userId().toString(), caller.tenantId(), caller.userId().toString()) != 1) throw new ManagementNotFoundException();
    }

    public void delete(Long id, InternalCallContext caller) { erase(id, caller.tenantId(), caller.userId().toString(), false); }

    private void erase(Long id, String tenant, String user, boolean expiredOnly) {
        transactions.executeWithoutResult(tx -> {
            if (mapper.lockActive(id, tenant, user) == null) { if (expiredOnly) return; throw new ManagementNotFoundException(); }
            if (expiredOnly && mapper.expiredOwned(id) == 0) return;
            Integer active = mapper.activeRuns(tenant, id); if (active != null && active > 0) { if (expiredOnly) return; throw new ManagementConflictException("对话有执行中的任务，请先停止生成"); }
            memory.deleteMessages("chat_" + tenant + "_" + user + "_" + id);
            if (redis != null) { for (String stream : mapper.streamKeys(tenant, id)) redis.delete(stream); redis.delete("business_" + tenant + "_" + user + "_" + id); }
            mapper.deleteSources(tenant, id); mapper.deleteMessages(tenant, id); mapper.markDeleted(id); mapper.eraseSteps(tenant, id); mapper.eraseRequestLogs(tenant, id); mapper.eraseToolLogs(tenant, id); mapper.eraseRuns(tenant, id);
        });
    }

    @Scheduled(fixedDelayString = "${nexusedu.ai.conversation-retention.scan-ms:3600000}")
    public void sweep() { for (var row : mapper.expiredConversations(100)) erase(((Number) row.get("id")).longValue(), row.get("tenant_id").toString(), row.get("user_id").toString(), true); }
}
