package com.xqy.nexusedu.infrastructure.system.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.common.entity.system.SysUser;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
