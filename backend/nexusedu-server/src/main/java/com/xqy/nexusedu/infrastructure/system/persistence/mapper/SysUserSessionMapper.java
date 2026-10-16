package com.xqy.nexusedu.infrastructure.system.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xqy.nexusedu.infrastructure.system.persistence.entity.SysUserSession;

@Mapper
public interface SysUserSessionMapper extends BaseMapper<SysUserSession> {
    @Select("SELECT * FROM sys_user_session WHERE id = #{id} FOR UPDATE")
    SysUserSession selectByIdForUpdate(@Param("id") Long id);
}
