package com.xqy.nexusedu.infrastructure.system.persistence.entity;

import com.xqy.nexusedu.infrastructure.persistence.entity.CommonEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_user_role")
public class SysUserRole extends CommonEntity {
    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
