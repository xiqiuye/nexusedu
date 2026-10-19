package com.xqy.nexusedu.system.infrastructure;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.xqy.nexusedu.system.application.model.AuthDtos;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.security.rpc.InternalCallSigner;

@Component
public class RpcCaller {
    private final InternalCallSigner signer;

    public RpcCaller(@Value("${nexusedu.internal-rpc.secret}") String secret) {
        signer = new InternalCallSigner(secret);
    }

    public InternalCallContext context(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user))
            throw new AccessDeniedException("Unauthenticated");
        return signer.sign("nexusedu-web-bff", user.tenantId(), user.userId(), user.sessionId(), UUID.randomUUID().toString());
    }
}
