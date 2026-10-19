package com.xqy.nexusedu.system.interfaces.web.rest;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xqy.nexusedu.system.application.model.SystemAuthModels;
import com.xqy.nexusedu.system.application.model.AuthDtos;
import com.xqy.nexusedu.system.application.port.in.SystemManagementService;
import com.xqy.nexusedu.system.application.auth.AuthService;
import com.xqy.nexusedu.shared.dto.ApiResponse;
import com.xqy.nexusedu.shared.dto.PageResult;
import com.xqy.nexusedu.system.infrastructure.RpcCaller;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    @Autowired
    private SystemManagementService management;
    private final RpcCaller rpcCaller;

    public AuthController(AuthService authService, RpcCaller rpcCaller) {
        this.authService = authService;
        this.rpcCaller = rpcCaller;
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthDtos.TokenPair> login(@RequestBody AuthDtos.LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || blank(request.userName()) || blank(request.password())) {
            return new ApiResponse<>(400, "用户名和密码不能为空", null);
        }
        try {
            return ApiResponse.success(authService.login(request.userName().trim(), request.password(),
                    clientIp(httpRequest), httpRequest.getHeader("User-Agent")));
        } catch (AuthService.InvalidCredentialsException exception) {
            return new ApiResponse<>(401, "用户名或密码错误", null);
        }
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthDtos.TokenPair> refresh(@RequestBody AuthDtos.RefreshRequest request) {
        if (request == null || blank(request.refreshToken())) return new ApiResponse<>(400, "刷新令牌不能为空", null);
        try {
            return ApiResponse.success(authService.refresh(request.refreshToken()));
        } catch (RuntimeException exception) {
            return new ApiResponse<>(401, "登录已失效，请重新登录", null);
        }
    }

    @GetMapping("/user/info")
    public ApiResponse<AuthDtos.UserInfo> userInfo(Authentication authentication) {
        return ApiResponse.success(authService.userInfo(principal(authentication).userId()));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        AuthDtos.AuthenticatedUser user = principal(authentication);
        authService.logout(user.sessionId(), user.tokenId());
        return ApiResponse.success(null);
    }

    @PostMapping("/system/sessions/{sessionId}/kick")
    public ApiResponse<Void> kick(@PathVariable Long sessionId, Authentication authentication) {
        management.revokeSession(sessionId, rpcCaller.context(authentication));
        return ApiResponse.success(null);
    }

    @GetMapping("/user/list")
    public ApiResponse<PageResult> users(String userName, String status, Integer current, Integer size, Authentication authentication) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (userName != null) filters.put("search", userName);
        if (status != null) filters.put("status", status);
        var page = management.list("users", filters, current == null ? 1 : current, size == null ? 10 : size, rpcCaller.context(authentication));
        // Preserve the legacy UI field names without using the old unscoped RPC query.
        page.setRecords(page.getRecords().stream().map(row -> {
            Map<String, Object> mapped = new LinkedHashMap<>(row);
            mapped.put("userName", row.get("username"));
            mapped.put("nickName", row.get("nickname"));
            mapped.put("userPhone", row.get("phone"));
            mapped.put("userEmail", row.get("email"));
            mapped.put("userGender", row.get("gender"));
            mapped.put("userRoles", row.getOrDefault("roleCodes", List.of()));
            return mapped;
        }).toList());
        return ApiResponse.success(page);
    }

    @GetMapping("/v3/system/menus")
    public ApiResponse<List<Map<String, Object>>> menus(Authentication authentication) {
        return ApiResponse.success(menuTree(authService.menus(principal(authentication).userId())));
    }

    private AuthDtos.AuthenticatedUser principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user)
            return user;
        throw new AuthService.InvalidCredentialsException();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> menuTree(List<SystemAuthModels.Menu> menus) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (SystemAuthModels.Menu menu : menus) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", menu.path());
            item.put("name", menu.name());
            item.put("component", menu.component());
            item.put("redirect", menu.redirect());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", menu.title());
            meta.put("icon", menu.icon());
            meta.put("keepAlive", Boolean.TRUE.equals(menu.keepAlive()));
            meta.put("isHide", !Boolean.TRUE.equals(menu.visible()));
            meta.put("isHideTab", Boolean.TRUE.equals(menu.hideTab()));
            meta.put("isFullPage", Boolean.TRUE.equals(menu.fullPage()));
            meta.put("link", menu.link());
            meta.put("isIframe", Boolean.TRUE.equals(menu.iframe()));
            meta.put("authList", menu.authList());
            meta.put("activePath", menu.activePath());
            item.put("meta", meta);
            item.put("children", new ArrayList<Map<String, Object>>());
            byId.put(menu.id(), item);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SystemAuthModels.Menu menu : menus) {
            Map<String, Object> item = byId.get(menu.id());
            Map<String, Object> parent = byId.get(menu.parentId());
            if (parent == null) roots.add(item);
            else ((List<Map<String, Object>>) parent.get("children")).add(item);
        }
        return roots;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

}
