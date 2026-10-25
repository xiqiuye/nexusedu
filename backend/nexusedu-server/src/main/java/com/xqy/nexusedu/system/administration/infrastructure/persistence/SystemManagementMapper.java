package com.xqy.nexusedu.system.administration.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemManagementMapper {
    Long countPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                   @Param("userId") Long userId, @Param("status") String status,
                   @Param("menuId") Long menuId, @Param("search") String search);

    List<Map<String, Object>> selectPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                   @Param("userId") Long userId, @Param("status") String status,
                   @Param("menuId") Long menuId, @Param("search") String search,
                   @Param("limit") int limit, @Param("offset") long offset);

    Map<String, Object> selectOwned(@Param("resource") String resource, @Param("id") Long id,
                                    @Param("tenantId") String tenantId);

    Map<String, Object> selectById(@Param("resource") String resource, @Param("id") Long id);

    int insertResource(@Param("resource") String resource, @Param("values") Map<String, Object> values);

    int updateResource(@Param("resource") String resource, @Param("id") Long id,
                       @Param("values") Map<String, Object> values);

    int countByTable(@Param("table") String table, @Param("id") Long id);

    int deleteUserRoles(@Param("userId") Long userId);

    int countRoleUsers(@Param("roleId") Long roleId);

    int deleteRoleButtons(@Param("roleId") Long roleId);

    int deleteRoleMenus(@Param("roleId") Long roleId);

    int countChildMenus(@Param("menuId") Long menuId);

    int countMenuButtons(@Param("menuId") Long menuId);

    int deleteMenuRoles(@Param("menuId") Long menuId);

    int deleteButtonRoles(@Param("buttonId") Long buttonId);

    int deleteResource(@Param("resource") String resource, @Param("id") Long id);

    String findRoleCode(@Param("roleId") Long roleId);

    Long lockSuperRole();

    int countUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    int countEnabledSuperUsers(@Param("roleId") Long roleId);

    int countEnabledUser(@Param("userId") Long userId);

    int insertUserRole(@Param("id") Long id, @Param("userId") Long userId, @Param("roleId") Long roleId,
                       @Param("createBy") String createBy, @Param("modifyBy") String modifyBy);

    Long findButtonMenuId(@Param("buttonId") Long buttonId);

    Long findMenuParentId(@Param("menuId") Long menuId);

    int insertRoleMenu(@Param("id") Long id, @Param("roleId") Long roleId, @Param("menuId") Long menuId);

    int insertRoleButton(@Param("id") Long id, @Param("roleId") Long roleId, @Param("buttonId") Long buttonId);

    Long findSessionUserId(@Param("sessionId") Long sessionId, @Param("tenantId") String tenantId);

    List<Long> findRoleIdsByUser(@Param("userId") Long userId);

    List<Long> findMenuIdsByRole(@Param("roleId") Long roleId);

    List<Long> findButtonIdsByRole(@Param("roleId") Long roleId);

    List<Long> findActiveSessionIds(@Param("userId") Long userId);

    List<Long> findSuperRoleIds();

    int insertSuperMenu(@Param("id") Long id, @Param("roleId") Long roleId, @Param("menuId") Long menuId);

    int insertSuperButton(@Param("id") Long id, @Param("roleId") Long roleId, @Param("buttonId") Long buttonId);
}
