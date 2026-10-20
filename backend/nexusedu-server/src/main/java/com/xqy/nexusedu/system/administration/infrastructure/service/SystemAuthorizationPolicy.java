package com.xqy.nexusedu.system.administration.infrastructure.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.port.out.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper.SysAuthorizationMapper;

@Service
public class SystemAuthorizationPolicy implements AuthorizationPolicy {
    private final JdbcTemplate jdbc;
    private final SysAuthorizationMapper authorization;
    private final InternalCallSigner signer;

    public SystemAuthorizationPolicy(JdbcTemplate jdbc, SysAuthorizationMapper authorization,
                                     @Value("${nexusedu.internal-rpc.secret}") String secret) {
        this.jdbc = jdbc;
        this.authorization = authorization;
        this.signer = new InternalCallSigner(secret);
    }

    @Override
    public void authorize(String authority, InternalCallContext caller) {
        signer.verify(caller, "nexusedu-web-bff");
        Integer valid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user_session s JOIN sys_user u ON u.id=s.user_id
                WHERE s.id=? AND s.user_id=? AND s.tenant_id=? AND u.tenant_id=s.tenant_id
                  AND s.status='ACTIVE' AND s.expire_time>CURRENT_TIMESTAMP AND u.status='ENABLED'
                """, Integer.class, caller.sessionId(), caller.userId(), caller.tenantId());
        if (valid == null || valid != 1) throw new ManagementAccessDeniedException();
        if ("authenticated".equals(authority) || authorization.findRoleCodes(caller.userId()).contains("R_SUPER")) return;
        if (!authorization.findButtonAuthorities(caller.userId()).contains(authority))
            throw new ManagementAccessDeniedException();
    }
}
