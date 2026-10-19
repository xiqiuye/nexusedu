package com.xqy.nexusedu.system.infrastructure.security.web;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Identity and authorization claims carried by a verified JWT. */
public class JwtUserContext {
    private Long userId;
    private Long sessionId;
    private String tokenId;
    private String tenantId;
    private List<String> roles;
    private List<String> authorities;
    private List<String> dataScopes;
    private String tokenType;

    public JwtUserContext() {
    }

    public JwtUserContext(Long userId, Long sessionId, String tokenId, String tenantId, List<String> roles,
                          List<String> authorities, List<String> dataScopes, String tokenType) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.tokenId = Objects.requireNonNull(tokenId, "tokenId must not be null");
        this.tenantId = tenantId;
        this.roles = immutableStrings(roles);
        this.authorities = immutableStrings(authorities);
        this.dataScopes = immutableStrings(dataScopes);
        this.tokenType = tokenType;
    }

    private static List<String> immutableStrings(Collection<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = immutableStrings(roles); }
    public List<String> getAuthorities() { return authorities; }
    public void setAuthorities(List<String> authorities) { this.authorities = immutableStrings(authorities); }
    public List<String> getDataScopes() { return dataScopes; }
    public void setDataScopes(List<String> dataScopes) { this.dataScopes = immutableStrings(dataScopes); }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long userId() { return userId; }
    public Long sessionId() { return sessionId; }
    public String tokenId() { return tokenId; }
    public String tenantId() { return tenantId; }
    public List<String> roles() { return roles; }
    public List<String> authorities() { return authorities; }
    public List<String> dataScopes() { return dataScopes; }
    public String tokenType() { return tokenType; }
}
