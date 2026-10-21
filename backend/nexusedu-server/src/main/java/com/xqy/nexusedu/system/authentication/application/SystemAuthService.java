package com.xqy.nexusedu.system.authentication.application;

import java.util.List;

import com.xqy.nexusedu.system.authentication.application.model.SystemAuthModels;

public interface SystemAuthService {
    SystemAuthModels.TokenPair login(String username, String password, String loginIp, String userAgent);

    SystemAuthModels.TokenPair refresh(String refreshToken);

    SystemAuthModels.Principal authenticate(Long userId, Long sessionId, String tokenId);

    SystemAuthModels.UserInfo userInfo(Long userId);

    void logout(Long sessionId, String tokenId);

    void kick(Long sessionId);

    int clearExpiredSessions();

    List<SystemAuthModels.Menu> menus(Long userId);

    SystemAuthModels.UserPage users(String username, String status, long current, long size);
}
