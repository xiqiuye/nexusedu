package com.xqy.nexusedu.ai.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.ai.chat.entity.AiChatConversation;
import com.xqy.nexusedu.ai.chat.entity.AiChatMessage;
import com.xqy.nexusedu.ai.task.entity.AiTaskRun;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.ai.chat.mapper.AiChatConversationMapper;
import com.xqy.nexusedu.ai.chat.mapper.AiChatMessageMapper;
import com.xqy.nexusedu.ai.task.mapper.AiTaskRunMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

/**
 * Persists the user and assistant turns of an AI chat.
 */
@Service
public class AiChatHistoryService {
    private final AiChatConversationMapper conversationMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiTaskRunMapper runMapper;

    public AiChatHistoryService(AiChatConversationMapper conversationMapper, AiChatMessageMapper messageMapper,
                                AiTaskRunMapper runMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.runMapper = runMapper;
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
        runMapper.update(null, new LambdaUpdateWrapper<AiTaskRun>()
                .set(AiTaskRun::getConversationId, conversation.getId())
                .eq(AiTaskRun::getRunId, run.getRunId())
                .eq(AiTaskRun::getTenantId, run.getTenantId())
                .isNull(AiTaskRun::getConversationId));
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
