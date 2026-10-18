package com.xqy.nexusedu.application.system.port.in;

import java.util.List;

import com.xqy.nexusedu.common.dto.system.SystemAuthModels;

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
