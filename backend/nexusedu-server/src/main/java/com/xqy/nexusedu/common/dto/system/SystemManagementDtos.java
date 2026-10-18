package com.xqy.nexusedu.common.dto.system;

import java.io.Serializable;
import java.util.List;

/** Request DTOs for system role and permission management. */
public final class SystemManagementDtos {
    private SystemManagementDtos() {
    }

    public static class UserRoles implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Long> roleIds;

        public UserRoles() {
        }

        public UserRoles(List<Long> roleIds) { this.roleIds = roleIds; }
        public List<Long> getRoleIds() { return roleIds; }
        public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
        public List<Long> roleIds() { return roleIds; }
    }

    public static class RolePermissions implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Long> menuIds;
        private List<Long> buttonIds;

        public RolePermissions() {
        }

        public RolePermissions(List<Long> menuIds, List<Long> buttonIds) {
            this.menuIds = menuIds;
            this.buttonIds = buttonIds;
        }

        public List<Long> getMenuIds() { return menuIds; }
        public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
        public List<Long> getButtonIds() { return buttonIds; }
        public void setButtonIds(List<Long> buttonIds) { this.buttonIds = buttonIds; }
        public List<Long> menuIds() { return menuIds; }
        public List<Long> buttonIds() { return buttonIds; }
    }
}
