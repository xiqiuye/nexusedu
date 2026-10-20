package com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeManagementMapper {
    Long countPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                   @Param("parentId") Long parentId, @Param("search") String search);

    List<Map<String, Object>> selectPage(@Param("resource") String resource, @Param("tenantId") String tenantId,
                   @Param("parentId") Long parentId, @Param("search") String search,
                   @Param("limit") int limit, @Param("offset") long offset);

    Map<String, Object> selectOwned(@Param("resource") String resource, @Param("id") Long id,
                                    @Param("tenantId") String tenantId);

    int insertResource(@Param("resource") String resource, @Param("values") Map<String, Object> values);

    int updateResource(@Param("resource") String resource, @Param("id") Long id,
                       @Param("tenantId") String tenantId, @Param("values") Map<String, Object> values);
}
