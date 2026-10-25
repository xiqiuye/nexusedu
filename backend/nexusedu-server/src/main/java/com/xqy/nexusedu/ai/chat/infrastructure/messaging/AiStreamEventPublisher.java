package com.xqy.nexusedu.ai.chat.infrastructure.messaging;

/** Publishes an AI run event without depending on an HTTP transport or consumer. */
public interface AiStreamEventPublisher {
    void publish(String streamKey, String type, Object data);
}
