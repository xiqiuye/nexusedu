package com.xqy.nexusedu.system.authentication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.system.authentication.entity.SysUserSession;

@Mapper
public interface SysUserSessionMapper extends BaseMapper<SysUserSession> {
    SysUserSession selectByIdForUpdate(@Param("id") Long id);

    Integer countValidSession(@Param("sessionId") Long sessionId, @Param("userId") Long userId,
                              @Param("tenantId") String tenantId);
}
