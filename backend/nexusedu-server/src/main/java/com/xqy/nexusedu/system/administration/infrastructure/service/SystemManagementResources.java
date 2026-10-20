package com.xqy.nexusedu.system.administration.infrastructure.service;

import java.util.List;
import java.util.Map;

public final class SystemManagementResources {
    private SystemManagementResources() {}

    public static final Map<String, String> TABLES = Map.of("users", "sys_user", "roles", "sys_role",
            "menus", "sys_menu", "buttons", "sys_button", "sessions", "sys_user_session");
    public static final Map<String, String> COLUMNS = Map.of(
            "users", "id,tenant_id,username,nickname,real_name,email,phone,avatar,user_type,status,last_login_time,last_login_ip,create_time,update_time",
            "roles", "id,role_code,role_name,description,role_type,status,create_time,update_time",
            "menus", "id,parent_id,path,route_name,component,redirect,title,icon,sort_no,keep_alive,visible,hide_tab,full_page,external_link,iframe_flag,active_path,status",
            "buttons", "id,menu_id,button_name,auth_remark,description,sort_no,status",
            "sessions", "id,tenant_id,user_id,login_ip,user_agent,device_type,login_time,expire_time,refresh_expire_time,status,logout_time");
    public static final Map<String, List<String>> FIELDS = Map.of(
            "users", List.of("username", "nickname", "realName", "email", "phone", "avatar", "status", "password"),
            "roles", List.of("roleCode", "roleName", "description", "status"),
            "menus", List.of("parentId", "path", "routeName", "component", "redirect", "title", "icon", "sortNo", "keepAlive", "visible", "hideTab", "fullPage", "externalLink", "iframeFlag", "activePath", "status"),
            "buttons", List.of("menuId", "buttonName", "authRemark", "description", "sortNo", "status"));
}
