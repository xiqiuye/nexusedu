package com.xqy.nexusedu.ai.chat.application.gateway;

/** Publishes an AI run event without depending on an HTTP transport or consumer. */
public interface AiStreamEventPublisher {
    void publish(String streamKey, String type, Object data);
}
