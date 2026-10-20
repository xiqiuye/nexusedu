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
}
