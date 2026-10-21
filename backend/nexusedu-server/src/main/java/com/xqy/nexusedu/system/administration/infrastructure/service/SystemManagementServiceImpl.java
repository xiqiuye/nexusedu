package com.xqy.nexusedu.system.administration.infrastructure.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.system.administration.application.SystemManagementService;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.system.administration.infrastructure.persistence.SystemManagementStore;
import com.xqy.nexusedu.system.authentication.infrastructure.service.SystemAuthServiceImpl;

/**
 * RBAC writes and device sessions live in the identity provider.
 */
@Service
public class SystemManagementServiceImpl implements SystemManagementService {
    private final JdbcTemplate jdbc;
    private final SystemManagementStore store;
    private final SysAuthorizationMapper authorization;
    private final SystemAuthServiceImpl auth;
    private final AuthorizationPolicy authorizationPolicy;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public SystemManagementServiceImpl(JdbcTemplate jdbc, SystemManagementStore store, SysAuthorizationMapper authorization,
                                       SystemAuthServiceImpl auth, AuthorizationPolicy authorizationPolicy) {
        this.jdbc = jdbc;
        this.store = store;
        this.authorization = authorization;
        this.auth = auth;
        this.authorizationPolicy = authorizationPolicy;
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        filters = filters == null ? Map.of() : filters;
        boolean ownSessions = "sessions".equals(resource) && !"true".equals(filters.get("all"));
        authorizationPolicy.authorize(ownSessions ? "authenticated" : permission(resource, "read"), caller);
        PageResult result = store.list(resource, filters, current, size, caller, ownSessions);
        List<Map<String, Object>> rows = result.getRecords();
        if ("sessions".equals(resource))
            rows.forEach(row -> row.put("currentSession", String.valueOf(caller.sessionId()).equals(row.get("id"))));
        if ("users".equals(resource))
            rows.forEach(row -> row.put("roleCodes", authorization.findRoleCodes(Long.valueOf(row.get("id").toString()))));
        return new PageResult(rows, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> input, InternalCallContext caller) {
        authorizationPolicy.authorize(permission(resource, id == null ? "create" : "update"), caller);
        if (!SystemManagementResources.FIELDS.containsKey(resource) || input == null) throw new IllegalArgumentException("不支持的写操作");
        globalWrite(resource, caller);
        Map<String, Object> previous = id == null ? Map.of() : owned(resource, id, caller);
        Map<String, Object> values = validated(resource, input, id == null);
        if ("users".equals(resource) && id != null)
            protectAdministrator(id, caller, "DISABLED".equals(values.get("status")));
        if ("users".equals(resource) && (Objects.equals(id, caller.userId()) || "SYSTEM".equals(previous.get("user_type")))
                && "DISABLED".equals(values.get("status")))
            throw new ManagementConflictException("不能禁用当前用户或内置管理员");
        if ("roles".equals(resource) && "SYSTEM".equals(previous.get("role_type"))) {
            if ((values.containsKey("role_code") && !Objects.equals(values.get("role_code"), previous.get("role_code")))
                    || "DISABLED".equals(values.get("status")))
                throw new ManagementConflictException("内置角色不能改名或禁用");
        }
        if ("menus".equals(resource)) validateParent(id, values.get("parent_id"));
        if ("buttons".equals(resource)) {
            Long menuId = input.containsKey("menuId") ? ManagementRows.id(input.get("menuId")) : ((Number) previous.get("menu_id")).longValue();
            requireExists("sys_menu", menuId);
        }
        if (id == null) {
            id = IdWorker.getId();
            values.put("id", id);
            values.put("create_by", String.valueOf(caller.userId()));
            if ("users".equals(resource)) {
                values.put("tenant_id", caller.tenantId());
                values.put("user_type", "NORMAL");
            }
            if ("roles".equals(resource)) values.put("role_type", "CUSTOM");
        }
        values.put("modify_by", String.valueOf(caller.userId()));
        try {
            if (previous.isEmpty()) store.insert(resource, values);
            else store.update(resource, id, values);
        } catch (DataIntegrityViolationException error) {
            throw new ManagementConflictException("名称、路由或授权标识重复，或字段不符合表结构");
        }
        if ("menus".equals(resource) || "buttons".equals(resource)) grantSuper(resource, id);
        if ("users".equals(resource) && (values.containsKey("password") || "DISABLED".equals(values.get("status"))))
            terminateUserSessions(id);
        return ManagementRows.publicRow(store.find(resource, id));
    }

    @Override
    @Transactional
    public void delete(String resource, Long id, InternalCallContext caller) {
        authorizationPolicy.authorize(permission(resource, "delete"), caller);
        globalWrite(resource, caller);
        Map<String, Object> row = owned(resource, id, caller);
        switch (resource) {
            case "users" -> {
                if (Objects.equals(id, caller.userId()) || "SYSTEM".equals(row.get("user_type")))
                    throw new ManagementConflictException("不能删除当前用户或内置管理员");
                protectAdministrator(id, caller, true);
                terminateUserSessions(id);
                jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", id);
            }
            case "roles" -> {
                if ("SYSTEM".equals(row.get("role_type"))) throw new ManagementConflictException("不能删除内置角色");
                if (count("SELECT COUNT(*) FROM sys_user_role WHERE role_id=?", id) > 0)
                    throw new ManagementConflictException("请先移除角色的用户关联");
                jdbc.update("DELETE FROM sys_role_button WHERE role_id=?", id);
                jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", id);
            }
            case "menus" -> {
                if (count("SELECT COUNT(*) FROM sys_menu WHERE parent_id=?", id) > 0 || count("SELECT COUNT(*) FROM sys_button WHERE menu_id=?", id) > 0)
                    throw new ManagementConflictException("请先删除子菜单和按钮");
                jdbc.update("DELETE FROM sys_role_menu WHERE menu_id=?", id);
            }
            case "buttons" -> jdbc.update("DELETE FROM sys_role_button WHERE button_id=?", id);
            default -> throw new IllegalArgumentException("不支持的删除操作");
        }
        jdbc.update("DELETE FROM " + table(resource) + " WHERE id=?", id);
    }

    @Override
    public Map<String, Object> assignments(String resource, Long id, InternalCallContext caller) {
        authorizationPolicy.authorize(permission(resource, "read"), caller);
        owned(resource, id, caller);
        if ("users".equals(resource))
            return Map.of("roleIds", ids("SELECT role_id FROM sys_user_role WHERE user_id=?", id));
        if ("roles".equals(resource))
            return Map.of("menuIds", ids("SELECT menu_id FROM sys_role_menu WHERE role_id=?", id),
                    "buttonIds", ids("SELECT button_id FROM sys_role_button WHERE role_id=?", id));
        throw new IllegalArgumentException("不支持的授权对象");
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds, InternalCallContext caller) {
        authorizationPolicy.authorize("system:user:grant", caller);
        owned("users", userId, caller);
        protectAdministrator(userId, caller, false);
        if (Objects.equals(userId, caller.userId())) throw new ManagementConflictException("不能修改当前用户的角色");
        List<Long> roles = validIds(roleIds);
        for (Long id : roles) {
            requireExists("sys_role", id);
            if (!superUser(caller) && "R_SUPER".equals(jdbc.queryForObject("SELECT role_code FROM sys_role WHERE id=?", String.class, id)))
                throw new ManagementAccessDeniedException();
        }
        // Serialize changes to administrator membership before checking the last remaining account.
        Long superRole = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code='R_SUPER' FOR UPDATE", Long.class);
        if (!roles.contains(superRole) && count("SELECT COUNT(*) FROM sys_user_role WHERE user_id=? AND role_id=?", userId, superRole) > 0) {
            if (!superUser(caller)) throw new ManagementAccessDeniedException();
            if (count("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE ur.role_id=? AND u.status='ENABLED'", superRole) <= 1)
                throw new ManagementConflictException("必须保留至少一个启用的超级管理员");
        }
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", userId);
        for (Long role : roles)
            jdbc.update("INSERT INTO sys_user_role (id,user_id,role_id,create_by,modify_by) VALUES (?,?,?,?,?)",
                    IdWorker.getId(), userId, role, caller.userId().toString(), caller.userId().toString());
    }

    @Override
    @Transactional
    public void grantRole(Long roleId, List<Long> menuIds, List<Long> buttonIds, InternalCallContext caller) {
        authorizationPolicy.authorize("system:role:grant", caller);
        globalWrite("roles", caller);
        Map<String, Object> role = owned("roles", roleId, caller);
        if ("R_SUPER".equals(role.get("role_code")))
            throw new ManagementConflictException("超级管理员自动拥有全部权限");
        List<Long> menus = validIds(menuIds), buttons = validIds(buttonIds);
        for (Long menu : menus) requireExists("sys_menu", menu);
        for (Long button : buttons) {
            requireExists("sys_button", button);
            Long menu = jdbc.queryForObject("SELECT menu_id FROM sys_button WHERE id=?", Long.class, button);
            if (!menus.contains(menu)) throw new IllegalArgumentException("按钮对应的菜单必须同时授权");
        }
        for (Long menu : menus) {
            Long parent = jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id=?", Long.class, menu);
            if (parent != null && !menus.contains(parent))
                throw new IllegalArgumentException("请同时选择菜单的全部父级");
        }
        jdbc.update("DELETE FROM sys_role_button WHERE role_id=?", roleId);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", roleId);
        for (Long menu : menus)
            jdbc.update("INSERT INTO sys_role_menu (id,role_id,menu_id) VALUES (?,?,?)", IdWorker.getId(), roleId, menu);
        for (Long button : buttons)
            jdbc.update("INSERT INTO sys_role_button (id,role_id,button_id) VALUES (?,?,?)", IdWorker.getId(), roleId, button);
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId, InternalCallContext caller) {
        authorizationPolicy.authorize("authenticated", caller);
        List<Map<String, Object>> sessions = jdbc.queryForList("SELECT user_id FROM sys_user_session WHERE id=? AND tenant_id=?", sessionId, caller.tenantId());
        if (sessions.isEmpty()) throw new ManagementNotFoundException();
        if (!Objects.equals(((Number) sessions.getFirst().get("user_id")).longValue(), caller.userId()))
            authorizationPolicy.authorize("system:session:revoke", caller);
        auth.kick(sessionId);
    }

    private Map<String, Object> validated(String resource, Map<String, Object> input, boolean create) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String field : input.keySet())
            if (!SystemManagementResources.FIELDS.get(resource).contains(field)) throw new IllegalArgumentException("不允许写入字段: " + field);
        if (create) {
            values.put("status", "ENABLED");
            if (resource.equals("menus")) {
                values.put("visible", true);
                values.put("sort_no", 0);
            }
            if (resource.equals("buttons")) values.put("sort_no", 0);
        }
        for (var entry : input.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if ("password".equals(field)) {
                if (value == null || value.toString().isBlank()) {
                    if (create) throw new IllegalArgumentException("新用户必须设置密码");
                    continue;
                }
                String password = value.toString();
                if (password.length() < 6 || password.getBytes(StandardCharsets.UTF_8).length > 72)
                    throw new IllegalArgumentException("密码至少 6 个字符且不超过 72 字节");
                value = passwords.encode(password);
            } else if (field.endsWith("Id"))
                value = value == null || value.toString().isBlank() ? null : ManagementRows.id(value);
            else if ("sortNo".equals(field)) {
                try {
                    value = Integer.valueOf(value.toString());
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("排序必须为整数");
                }
            } else if (List.of("keepAlive", "visible", "hideTab", "fullPage", "iframeFlag").contains(field)) {
                if (!(value instanceof Boolean)) throw new IllegalArgumentException("开关字段必须为布尔值");
            } else {
                int maximum = switch (field) {
                    case "username", "nickname", "realName", "roleCode", "roleName" -> 64;
                    case "phone" -> 32;
                    case "email", "title", "icon", "routeName", "buttonName", "authRemark" -> 128;
                    case "description", "avatar", "externalLink" -> 512;
                    case "status" -> 16;
                    default -> 256;
                };
                value = ManagementRows.text(value, maximum, false);
                if ("status".equals(field) && !List.of("ENABLED", "DISABLED").contains(value))
                    throw new IllegalArgumentException("无效状态");
            }
            values.put(snake(field), value);
        }
        List<String> required = switch (resource) {
            case "users" -> List.of("username", "password");
            case "roles" -> List.of("role_code", "role_name");
            case "menus" -> List.of("title");
            default -> List.of("menu_id", "button_name", "auth_remark");
        };
        for (String field : required)
            if ((create || values.containsKey(field)) && (values.get(field) == null || values.get(field).toString().isBlank()))
                throw new IllegalArgumentException("必填字段不能为空: " + field);
        return values;
    }

    private Map<String, Object> owned(String resource, Long id, InternalCallContext caller) {
        return store.owned(resource, id, caller);
    }

    private void validateParent(Long id, Object value) {
        if (value == null) return;
        Long parent = ManagementRows.id(value);
        Set<Long> visited = new HashSet<>();
        while (parent != null) {
            if (Objects.equals(parent, id) || !visited.add(parent))
                throw new IllegalArgumentException("菜单不能循环引用");
            requireExists("sys_menu", parent);
            parent = jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id=?", Long.class, parent);
        }
    }

    private void globalWrite(String resource, InternalCallContext caller) {
        if (!"users".equals(resource) && !superUser(caller)) throw new ManagementAccessDeniedException();
    }

    private boolean superUser(InternalCallContext caller) {
        return authorization.findRoleCodes(caller.userId()).contains("R_SUPER");
    }

    private void protectAdministrator(Long userId, InternalCallContext caller, boolean removing) {
        Long role = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code='R_SUPER' FOR UPDATE", Long.class);
        if (count("SELECT COUNT(*) FROM sys_user_role WHERE user_id=? AND role_id=?", userId, role) == 0) return;
        if (!superUser(caller)) throw new ManagementAccessDeniedException();
        if (removing && count("SELECT COUNT(*) FROM sys_user WHERE id=? AND status='ENABLED'", userId) > 0
                && count("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE ur.role_id=? AND u.status='ENABLED'", role) <= 1)
            throw new ManagementConflictException("必须保留至少一个启用的超级管理员");
    }

    private String table(String resource) {
        String table = SystemManagementResources.TABLES.get(resource);
        if (table == null) throw new IllegalArgumentException("未知管理对象");
        return table;
    }

    private String permission(String resource, String action) {
        table(resource);
        return "system:" + resource.substring(0, resource.length() - 1) + ":" + action;
    }

    private int count(String sql, Object... args) {
        return store.count(sql, args);
    }

    private void requireExists(String table, Long id) {
        if (count("SELECT COUNT(*) FROM " + table + " WHERE id=?", id) != 1) throw new ManagementNotFoundException();
    }

    private List<String> ids(String sql, Long id) {
        return jdbc.queryForList(sql, Long.class, id).stream().map(String::valueOf).toList();
    }

    private List<Long> validIds(List<Long> ids) {
        if (ids == null || ids.size() > 1000 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new IllegalArgumentException("无效授权列表");
        return ids.stream().distinct().toList();
    }

    private String snake(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private void terminateUserSessions(Long userId) {
        jdbc.queryForList("SELECT id FROM sys_user_session WHERE user_id=? AND status='ACTIVE'", Long.class, userId).forEach(auth::kick);
    }

    private void grantSuper(String resource, Long id) {
        List<Long> roles = jdbc.queryForList("SELECT id FROM sys_role WHERE role_code='R_SUPER'", Long.class);
        for (Long role : roles)
            jdbc.update("INSERT IGNORE INTO " + (resource.equals("menus") ? "sys_role_menu (id,role_id,menu_id)" : "sys_role_button (id,role_id,button_id)") + " VALUES (?,?,?)", IdWorker.getId(), role, id);
    }

}
