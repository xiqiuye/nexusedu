package com.xqy.nexusedu.infrastructure.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/** Synchronous and streaming models built from one database definition. */
public record ModelHandle(ChatModel chatModel, StreamingChatModel streamingChatModel, String providerName, String modelName) {

    public ModelHandle(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this(chatModel, streamingChatModel, "unknown", "unknown");
    }

    public ChatModel chatModel() {
        return chatModel;
    }
}
