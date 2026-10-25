package com.xqy.nexusedu.system.administration.infrastructure.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper.SysUserSessionMapper;

@Service
public class SystemAuthorizationPolicy implements AuthorizationPolicy {
    private final SysAuthorizationMapper authorization;
    private final SysUserSessionMapper sessions;
    private final InternalCallSigner signer;

    public SystemAuthorizationPolicy(SysAuthorizationMapper authorization, SysUserSessionMapper sessions,
                                     @Value("${nexusedu.internal-rpc.secret}") String secret) {
        this.authorization = authorization;
        this.sessions = sessions;
        this.signer = new InternalCallSigner(secret);
    }

    @Override
    public void authorize(String authority, InternalCallContext caller) {
        signer.verify(caller, "nexusedu-web-bff");
        Integer valid = sessions.countValidSession(caller.sessionId(), caller.userId(), caller.tenantId());
        if (valid == null || valid != 1) throw new ManagementAccessDeniedException();
        if ("authenticated".equals(authority) || authorization.findRoleCodes(caller.userId()).contains("R_SUPER")) return;
        if (!authorization.findButtonAuthorities(caller.userId()).contains(authority))
            throw new ManagementAccessDeniedException();
    }
}
