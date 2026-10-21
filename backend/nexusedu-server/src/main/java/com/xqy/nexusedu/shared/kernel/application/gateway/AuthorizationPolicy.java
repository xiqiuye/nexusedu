package com.xqy.nexusedu.shared.kernel.application.gateway;

import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

/** Stable authorization boundary for business capabilities. */
public interface AuthorizationPolicy {
    void authorize(String authority, InternalCallContext caller);
}
