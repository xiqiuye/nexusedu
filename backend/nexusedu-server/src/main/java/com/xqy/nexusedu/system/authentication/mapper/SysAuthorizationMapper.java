package com.xqy.nexusedu.system.authentication.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.system.authentication.dto.SystemAuthModels;
import com.xqy.nexusedu.system.administration.entity.SysMenu;

@Mapper
public interface SysAuthorizationMapper {
    List<String> findRoleCodes(@Param("userId") Long userId);

    List<String> findButtonAuthorities(@Param("userId") Long userId);

    List<SysMenu> findMenus(@Param("userId") Long userId);

    List<SystemAuthModels.Button> findMenuButtons(@Param("userId") Long userId, @Param("menuId") Long menuId);
}
