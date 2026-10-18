package com.xqy.nexusedu.common.dto.system;

import java.io.Serializable;
import java.util.List;

/** Authentication request and response DTOs. */
public final class AuthDtos {
    private AuthDtos() {
    }

    public static class LoginRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private String userName;
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String userName() { return userName; }
        public String password() { return password; }
    }

    public static class RefreshRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private String refreshToken;

        public RefreshRequest() {
        }

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String refreshToken() { return refreshToken; }
    }

    public static class TokenPair implements Serializable {
        private static final long serialVersionUID = 1L;
        private String token;
        private String refreshToken;

        public TokenPair() {
        }

        public TokenPair(String token, String refreshToken) {
            this.token = token;
            this.refreshToken = refreshToken;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String token() { return token; }
        public String refreshToken() { return refreshToken; }
    }

    public static class AuthenticatedUser implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long userId;
        private Long sessionId;
        private String tokenId;
        private String tenantId;
        private List<String> roles;
        private List<String> authorities;

        public AuthenticatedUser() {
        }

        public AuthenticatedUser(Long userId, Long sessionId, String tokenId, String tenantId,
                                 List<String> roles, List<String> authorities) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.tokenId = tokenId;
            this.tenantId = tenantId;
            this.roles = roles;
            this.authorities = authorities;
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
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getAuthorities() { return authorities; }
        public void setAuthorities(List<String> authorities) { this.authorities = authorities; }
        public Long userId() { return userId; }
        public Long sessionId() { return sessionId; }
        public String tokenId() { return tokenId; }
        public String tenantId() { return tenantId; }
        public List<String> roles() { return roles; }
        public List<String> authorities() { return authorities; }
    }

    public static class UserInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<String> buttons;
        private List<String> roles;
        private Long userId;
        private String userName;
        private String email;
        private String avatar;

        public UserInfo() {
        }

        public UserInfo(List<String> buttons, List<String> roles, Long userId, String userName, String email, String avatar) {
            this.buttons = buttons;
            this.roles = roles;
            this.userId = userId;
            this.userName = userName;
            this.email = email;
            this.avatar = avatar;
        }

        public List<String> getButtons() { return buttons; }
        public void setButtons(List<String> buttons) { this.buttons = buttons; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public List<String> buttons() { return buttons; }
        public List<String> roles() { return roles; }
        public Long userId() { return userId; }
        public String userName() { return userName; }
        public String email() { return email; }
        public String avatar() { return avatar; }
    }
}
