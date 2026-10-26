package com.xqy.nexusedu.knowledge.catalog.mapper;

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

    Map<String, Object> selectPublic(@Param("resource") String resource, @Param("id") Long id,
                                     @Param("tenantId") String tenantId);

    int insertResource(@Param("resource") String resource, @Param("values") Map<String, Object> values);

    int updateResource(@Param("resource") String resource, @Param("id") Long id,
                       @Param("tenantId") String tenantId, @Param("values") Map<String, Object> values);

    int countDocumentsForBase(@Param("tenantId") String tenantId, @Param("baseId") Long baseId);
    int countPendingJobsForDocument(@Param("tenantId") String tenantId, @Param("documentId") Long documentId);
    int countActiveVersion(@Param("tenantId") String tenantId, @Param("versionId") Long versionId);
    int deleteBase(@Param("tenantId") String tenantId, @Param("baseId") Long baseId);
    int softDeleteDocument(@Param("tenantId") String tenantId, @Param("documentId") Long documentId, @Param("modifyBy") String modifyBy);
    int retryJob(@Param("tenantId") String tenantId, @Param("jobId") Long jobId);
    int retryVersion(@Param("tenantId") String tenantId, @Param("versionId") Long versionId, @Param("revision") Integer revision);
    int cancelJob(@Param("tenantId") String tenantId, @Param("jobId") Long jobId);
}
