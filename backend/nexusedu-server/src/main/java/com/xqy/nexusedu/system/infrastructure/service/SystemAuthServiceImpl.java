package com.xqy.nexusedu.system.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.system.application.model.SystemAuthModels;
import com.xqy.nexusedu.system.application.port.in.SystemAuthService;
import com.xqy.nexusedu.system.infrastructure.persistence.entity.SysMenu;
import com.xqy.nexusedu.system.infrastructure.persistence.entity.SysUser;
import com.xqy.nexusedu.system.infrastructure.persistence.entity.SysUserSession;
import com.xqy.nexusedu.system.application.exception.SystemAuthenticationException;
import com.xqy.nexusedu.system.infrastructure.persistence.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.system.infrastructure.persistence.mapper.SysUserMapper;
import com.xqy.nexusedu.system.infrastructure.persistence.mapper.SysUserSessionMapper;
import com.xqy.nexusedu.system.infrastructure.security.web.JwtTokenService;
import com.xqy.nexusedu.system.infrastructure.security.web.JwtUserContext;

@Service
public class SystemAuthServiceImpl implements SystemAuthService {
    private static final String ACTIVE = "ACTIVE";
    private static final String LOGGED_OUT = "LOGGED_OUT";
    private static final String ENABLED = "ENABLED";
    private static final String SESSION_KEY = "nexusedu:session:";
    private static final String REVOKED_KEY = "nexusedu:token:revoked:";
    private final SysUserMapper userMapper;
    private final SysUserSessionMapper sessionMapper;
    private final SysAuthorizationMapper authorizationMapper;
    private final JwtTokenService jwt;
    private final StringRedisTemplate redis;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SystemAuthServiceImpl(SysUserMapper userMapper, SysUserSessionMapper sessionMapper,
                                 SysAuthorizationMapper authorizationMapper, JwtTokenService jwt,
                                 StringRedisTemplate redis) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.authorizationMapper = authorizationMapper;
        this.jwt = jwt;
        this.redis = redis;
    }

    @Override
    @Transactional
    public SystemAuthModels.TokenPair login(String username, String password, String ip, String userAgent) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (user == null || !ENABLED.equals(user.getStatus()) || !encoder.matches(password, user.getPassword()))
            throw invalid();
        LocalDateTime now = LocalDateTime.now();
        SysUserSession session = new SysUserSession();
        session.setUserId(user.getId());
        session.setTenantId(requiredTenant(user.getTenantId()));
        session.setLoginIp(ip);
        session.setUserAgent(userAgent);
        session.setDeviceType(deviceType(userAgent));
        session.setLoginTime(now);
        session.setStatus(ACTIVE);
        session.setAccessToken("");
        session.setRefreshToken("");
        session.setExpireTime(now);
        session.setRefreshExpireTime(now);
        sessionMapper.insert(session);
        SystemAuthModels.TokenPair pair = issue(session, now);
        user.setLastLoginTime(now);
        user.setLastLoginIp(ip);
        userMapper.updateById(user);
        return pair;
    }

    @Override
    @Transactional
    public SystemAuthModels.TokenPair refresh(String refreshToken) {
        JwtUserContext context = jwt.verify(refreshToken);
        if (!JwtTokenService.REFRESH_TOKEN.equals(context.tokenType()) || revoked(context.tokenId())) throw invalid();
        SysUserSession session = sessionMapper.selectByIdForUpdate(context.sessionId());
        if (session == null || !ACTIVE.equals(session.getStatus()) || !context.userId().equals(session.getUserId())
                || !Objects.equals(context.tenantId(), session.getTenantId())
                || !context.tokenId().equals(session.getRefreshJti()) || expired(session.getRefreshExpireTime()))
            throw invalid();
        SysUser user = userMapper.selectById(context.userId());
        if (user == null || !ENABLED.equals(user.getStatus())) throw invalid();
        return issue(session, LocalDateTime.now());
    }

    @Override
    public SystemAuthModels.Principal authenticate(Long userId, Long sessionId, String tokenId) {
        if (revoked(tokenId)) throw invalid();
        SysUserSession session = sessionMapper.selectById(sessionId);
        String activeJti = redis.opsForValue().get(SESSION_KEY + sessionId);
        if (session == null || !ACTIVE.equals(session.getStatus()) || !userId.equals(session.getUserId())
                || !tokenId.equals(session.getAccessJti()) || !tokenId.equals(activeJti)
                || expired(session.getExpireTime())) throw invalid();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !ENABLED.equals(user.getStatus())) throw invalid();
        if (!Objects.equals(requiredTenant(user.getTenantId()), session.getTenantId())) throw invalid();
        return new SystemAuthModels.Principal(user.getId(), session.getId(), tokenId, session.getTenantId(),
                authorizationMapper.findRoleCodes(user.getId()), authorizationMapper.findButtonAuthorities(user.getId()));
    }

    @Override
    public SystemAuthModels.UserInfo userInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw invalid();
        return new SystemAuthModels.UserInfo(authorizationMapper.findButtonAuthorities(userId), authorizationMapper.findRoleCodes(userId),
                user.getId(), user.getUsername(), user.getEmail(), user.getAvatar());
    }

    @Override
    @Transactional
    public void logout(Long sessionId, String tokenId) {
        terminate(sessionMapper.selectByIdForUpdate(sessionId), tokenId);
    }

    @Override
    @Transactional
    public void kick(Long sessionId) {
        SysUserSession session = sessionMapper.selectByIdForUpdate(sessionId);
        terminate(session, session == null ? null : session.getAccessJti());
    }

    @Override
    @Transactional
    public int clearExpiredSessions() {
        List<SysUserSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<SysUserSession>().eq(SysUserSession::getStatus, ACTIVE)
                .and(q -> q.lt(SysUserSession::getRefreshExpireTime, LocalDateTime.now())));
        sessions.forEach(candidate -> {
            SysUserSession session = sessionMapper.selectByIdForUpdate(candidate.getId());
            terminate(session, session == null ? null : session.getAccessJti());
        });
        return sessions.size();
    }

    @Override
    public List<SystemAuthModels.Menu> menus(Long userId) {
        return authorizationMapper.findMenus(userId).stream().map(menu -> menu(userId, menu)).toList();
    }

    @Override
    public SystemAuthModels.UserPage users(String username, String status, long current, long size) {
        String databaseStatus = "1".equals(status) ? ENABLED : "2".equals(status) ? "DISABLED" : status;
        Page<SysUser> page = userMapper.selectPage(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)),
                new LambdaQueryWrapper<SysUser>().like(username != null && !username.isBlank(), SysUser::getUsername, username)
                        .eq(databaseStatus != null && !databaseStatus.isBlank(), SysUser::getStatus, databaseStatus).orderByDesc(SysUser::getId));
        List<SystemAuthModels.User> records = page.getRecords().stream().map(user -> new SystemAuthModels.User(
                user.getId(), user.getUsername(), "", user.getNickname(), user.getPhone(), user.getEmail(), user.getAvatar(),
                ENABLED.equals(user.getStatus()) ? "1" : "2", authorizationMapper.findRoleCodes(user.getId()),
                user.getCreateBy(), text(user.getCreateTime()), user.getModifyBy(), text(user.getUpdateTime()))).toList();
        return new SystemAuthModels.UserPage(records, page.getCurrent(), page.getSize(), page.getTotal());
    }

    private SystemAuthModels.TokenPair issue(SysUserSession session, LocalDateTime now) {
        revoke(session.getAccessJti(), secondsUntil(session.getExpireTime()));
        revoke(session.getRefreshJti(), secondsUntil(session.getRefreshExpireTime()));
        JwtUserContext base = new JwtUserContext(session.getUserId(), session.getId(), "pending", requiredTenant(session.getTenantId()),
                authorizationMapper.findRoleCodes(session.getUserId()), authorizationMapper.findButtonAuthorities(session.getUserId()), List.of(), JwtTokenService.ACCESS_TOKEN);
        String access = jwt.createAccessToken(base);
        String refresh = jwt.createRefreshToken(base);
        JwtUserContext accessContext = jwt.verifyAccessToken(access);
        JwtUserContext refreshContext = jwt.verify(refresh);
        session.setAccessToken(sha256(access));
        session.setRefreshToken(sha256(refresh));
        session.setAccessJti(accessContext.tokenId());
        session.setRefreshJti(refreshContext.tokenId());
        session.setExpireTime(now.plusSeconds(jwt.accessTokenTtlSeconds()));
        session.setRefreshExpireTime(now.plusSeconds(jwt.refreshTokenTtlSeconds()));
        sessionMapper.updateById(session);
        redis.opsForValue().set(SESSION_KEY + session.getId(), accessContext.tokenId(), Duration.ofSeconds(jwt.refreshTokenTtlSeconds()));
        return new SystemAuthModels.TokenPair(access, refresh);
    }

    private SystemAuthModels.Menu menu(Long userId, SysMenu menu) {
        List<SystemAuthModels.Button> buttons = authorizationMapper.findMenuButtons(userId, menu.getId());
        SystemAuthModels.Menu result = new SystemAuthModels.Menu(menu.getId(), menu.getParentId(), menu.getPath(), menu.getRouteName(), menu.getComponent(), menu.getRedirect(), menu.getTitle(), menu.getIcon(), menu.getSortNo(), menu.getKeepAlive(), menu.getVisible(), menu.getHideTab(), menu.getFullPage(), menu.getExternalLink(), menu.getIframeFlag(), buttons);
        result.setActivePath(menu.getActivePath());
        return result;
    }

    private boolean revoked(String jti) {
        return jti == null || Boolean.TRUE.equals(redis.hasKey(REVOKED_KEY + jti));
    }

    private void revoke(String jti, long seconds) {
        if (jti != null && !jti.isBlank() && seconds > 0)
            redis.opsForValue().set(REVOKED_KEY + jti, "1", Duration.ofSeconds(seconds));
    }

    private void terminate(SysUserSession session, String tokenId) {
        if (session == null) return;
        revoke(tokenId, secondsUntil(session.getExpireTime()));
        revoke(session.getRefreshJti(), secondsUntil(session.getRefreshExpireTime()));
        redis.delete(SESSION_KEY + session.getId());
        session.setStatus(LOGGED_OUT);
        session.setLogoutTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private long secondsUntil(LocalDateTime time) {
        return time == null ? 0 : Math.max(1, Duration.between(LocalDateTime.now(), time).toSeconds());
    }

    private boolean expired(LocalDateTime time) {
        return time == null || !time.isAfter(LocalDateTime.now());
    }

    private String text(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private String deviceType(String ua) {
        if (ua == null) return "UNKNOWN";
        String value = ua.toLowerCase();
        return value.contains("mobile") || value.contains("android") || value.contains("iphone") ? "MOBILE" : "DESKTOP";
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private SystemAuthenticationException invalid() {
        return new SystemAuthenticationException();
    }

    private String requiredTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) throw invalid();
        return tenantId.trim();
    }
}
