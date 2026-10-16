package com.xqy.nexusedu.application.web.auth;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.application.system.model.SystemAuthModels;
import com.xqy.nexusedu.application.system.port.in.SystemAuthService;
import com.xqy.nexusedu.common.exception.SystemAuthenticationException;
import com.xqy.nexusedu.infrastructure.web.security.JwtTokenService;
import com.xqy.nexusedu.infrastructure.web.security.JwtUserContext;

@Service
public class AuthService {
    @Autowired
    private SystemAuthService systemAuthService;
    private final JwtTokenService jwtTokenService;

    public AuthService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public TokenPair login(String username, String password, String ip, String userAgent) {
        try {
            SystemAuthModels.TokenPair pair = systemAuthService.login(username, password, ip, userAgent);
            return new TokenPair(pair.token(), pair.refreshToken());
        } catch (SystemAuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    public TokenPair refresh(String token) {
        SystemAuthModels.TokenPair pair = systemAuthService.refresh(token);
        return new TokenPair(pair.token(), pair.refreshToken());
    }

    public AuthenticatedUser authenticate(String token) {
        JwtUserContext context = jwtTokenService.verifyAccessToken(token);
        SystemAuthModels.Principal principal = systemAuthService.authenticate(context.userId(), context.sessionId(), context.tokenId());
        if (!Objects.equals(context.tenantId(), principal.tenantId())
                || !Objects.equals(context.userId(), principal.userId())
                || !Objects.equals(context.sessionId(), principal.sessionId())) throw new InvalidCredentialsException();
        return new AuthenticatedUser(context.userId(), context.sessionId(), context.tokenId(), context.tenantId(),
                principal.roles(), principal.authorities());
    }

    public UserInfo userInfo(Long userId) {
        SystemAuthModels.UserInfo info = systemAuthService.userInfo(userId);
        return new UserInfo(info.buttons(), info.roles(), info.userId(), info.userName(), info.email(), info.avatar());
    }

    public void logout(Long sessionId, String tokenId) {
        systemAuthService.logout(sessionId, tokenId);
    }

    public void kick(Long sessionId) {
        systemAuthService.kick(sessionId);
    }

    public SystemAuthModels.UserPage users(String username, String status, long current, long size) {
        return systemAuthService.users(username, status, current, size);
    }

    public List<SystemAuthModels.Menu> menus(Long userId) {
        return systemAuthService.menus(userId);
    }

    public record TokenPair(String token, String refreshToken) {
    }

    public record AuthenticatedUser(Long userId, Long sessionId, String tokenId, String tenantId,
                                    List<String> roles, List<String> authorities) {
    }

    public record UserInfo(List<String> buttons, List<String> roles, Long userId, String userName, String email,
                           String avatar) {
    }

    public static class InvalidCredentialsException extends RuntimeException {
    }
}
