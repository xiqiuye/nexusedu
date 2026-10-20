package com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.entity.SysUser;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
