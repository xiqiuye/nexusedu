package com.xqy.nexusedu.system.infrastructure.security.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtTokenService jwtTokenService(@Value("${nexusedu.system.jwt.secret}") String secret,
                                    @Value("${nexusedu.system.jwt.access-token-ttl-seconds}") long accessTtl,
                                    @Value("${nexusedu.system.jwt.refresh-token-ttl-seconds}") long refreshTtl) {
        return new JwtTokenService(secret, accessTtl, refreshTtl);
    }
}
