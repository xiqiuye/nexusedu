package com.xqy.nexusedu.ai.chat.messaging;

import cn.hutool.json.JSONUtil;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Delivers AI run events through Redis Streams for independently deployed consumers. */
@Component
public class RedisStreamEventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final Duration eventTtl;

    public RedisStreamEventPublisher(StringRedisTemplate redisTemplate,
                                     @Value("${nexusedu.ai.stream-event-ttl-minutes:5}") long eventTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.eventTtl = Duration.ofMinutes(eventTtlMinutes);
    }

    public void publish(String streamKey, String type, Object data) {
        if (streamKey == null || streamKey.isBlank()) {
            throw new IllegalArgumentException("streamKey must not be blank");
        }
        redisTemplate.opsForStream().add(StreamRecords.mapBacked(
                Map.of("event", type, "data", data instanceof String ? (String) data : JSONUtil.toJsonStr(data))).withStreamKey(streamKey));
        redisTemplate.expire(streamKey, eventTtl);
    }
}
