package com.xqy.nexusedu.infrastructure.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/** Synchronous and streaming models built from one database definition. */
public class ModelHandle {
    private ChatModel chatModel;
    private StreamingChatModel streamingChatModel;
    private String providerName;
    private String modelName;

    public ModelHandle() {
    }

    public ModelHandle(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this(chatModel, streamingChatModel, "unknown", "unknown");
    }

    public ModelHandle(ChatModel chatModel, StreamingChatModel streamingChatModel, String providerName, String modelName) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.providerName = providerName;
        this.modelName = modelName;
    }

    public ChatModel getChatModel() { return chatModel; }
    public void setChatModel(ChatModel chatModel) { this.chatModel = chatModel; }
    public StreamingChatModel getStreamingChatModel() { return streamingChatModel; }
    public void setStreamingChatModel(StreamingChatModel streamingChatModel) { this.streamingChatModel = streamingChatModel; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public ChatModel chatModel() { return chatModel; }
    public StreamingChatModel streamingChatModel() { return streamingChatModel; }
    public String providerName() { return providerName; }
    public String modelName() { return modelName; }
}
