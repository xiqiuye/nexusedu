package com.xqy.nexusedu.ai.application.service.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiChatConversation;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiChatMessage;
import com.xqy.nexusedu.ai.infrastructure.persistence.entity.AiTaskRun;
import com.xqy.nexusedu.shared.exception.ManagementNotFoundException;
import com.xqy.nexusedu.ai.infrastructure.persistence.mapper.AiChatConversationMapper;
import com.xqy.nexusedu.ai.infrastructure.persistence.mapper.AiChatMessageMapper;

/**
 * Persists the user and assistant turns of an AI chat.
 */
@Service
public class AiChatHistoryService {
    @Autowired
    private JdbcTemplate jdbc;
    private final AiChatConversationMapper conversationMapper;
    private final AiChatMessageMapper messageMapper;

    public AiChatHistoryService(AiChatConversationMapper conversationMapper, AiChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public Long recordUserMessage(AiTaskRun run) {
        validateOwner(run);
        if (blank(run.getQuestion())) throw new IllegalArgumentException("chat question must not be blank");
        return persist(run, "user", run.getQuestion());
    }

    @Transactional
    public void recordAssistantMessage(AiTaskRun run, String content) {
        validateOwner(run);
        if (blank(content)) throw new IllegalArgumentException("assistant response must not be blank");
        persist(run, "assistant", content);
    }

    private Long persist(AiTaskRun run, String role, String content) {
        AiChatConversation conversation = findOrCreate(run);
        run.setConversationId(conversation.getId());
        if (jdbc != null)
            jdbc.update("UPDATE ai_task_run SET conversation_id=? WHERE run_id=? AND tenant_id=? AND conversation_id IS NULL",
                    conversation.getId(), run.getRunId(), run.getTenantId());
        if (messageMapper.exists(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getConversationId, conversation.getId())
                .eq(AiChatMessage::getRunId, run.getRunId())
                .eq(AiChatMessage::getRole, role))) return conversation.getId();
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversation.getId());
        message.setTenantId(run.getTenantId());
        message.setUserId(run.getUserId());
        message.setSessionId(run.getSessionId());
        message.setRole(role);
        message.setContent(content);
        message.setRunId(run.getRunId());
        message.setSequenceNo(Math.addExact(messageMapper.lastSequenceNo(conversation.getId()), 1));
        messageMapper.insert(message);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation.getId();
    }

    public List<ChatMessage> recentMessages(AiTaskRun run, int limit) {
        validateOwner(run);
        if (run.getConversationId() == null) return List.of();
        var rows = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getConversationId, run.getConversationId())
                .eq(AiChatMessage::getTenantId, run.getTenantId()).eq(AiChatMessage::getUserId, run.getUserId())
                .ne(AiChatMessage::getRunId, run.getRunId())
                .orderByDesc(AiChatMessage::getSequenceNo).last("LIMIT " + Math.max(1, Math.min(limit, 100))));
        Collections.reverse(rows);
        return rows.stream().map(message -> (ChatMessage)
                ("assistant".equals(message.getRole()) ? AiMessage.from(message.getContent())
                        : UserMessage.from(message.getContent()))).toList();
    }

    private AiChatConversation findOrCreate(AiTaskRun run) {
        if (run.getConversationId() != null) {
            AiChatConversation selected = conversationMapper.lockByIdAndOwner(run.getConversationId(), run.getTenantId(), run.getUserId());
            if (selected == null) throw new ManagementNotFoundException();
            return selected;
        }
        AiChatConversation conversation = new AiChatConversation();
        conversation.setId(IdWorker.getId());
        conversation.setTenantId(run.getTenantId());
        conversation.setUserId(run.getUserId());
        conversation.setSessionId(run.getSessionId());
        String question = run.getQuestion() == null ? "AI chat" : run.getQuestion();
        conversation.setTitle(question.substring(0, question.offsetByCodePoints(0,
                Math.min(512, question.codePointCount(0, question.length())))));
        conversation.setStatus("ACTIVE");
        conversationMapper.createIfAbsent(conversation);
        conversation = conversationMapper.lockByOwner(run.getTenantId(), run.getUserId(), run.getSessionId());
        if (conversation == null) throw new IllegalStateException("chat conversation was not persisted");
        return conversation;
    }

    private void validateOwner(AiTaskRun run) {
        if (run == null || blank(run.getTenantId()) || blank(run.getUserId())
                || blank(run.getSessionId()) || blank(run.getRunId())) {
            throw new IllegalArgumentException("chat history requires tenant, user, session and run identifiers");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
