package com.xqy.nexusedu.knowledge.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;
import com.xqy.nexusedu.knowledge.catalog.entity.AiKnowledgeDocumentVersion;

@Mapper public interface AiKnowledgeDocumentVersionMapper extends BaseMapper<AiKnowledgeDocumentVersion> {
    List<Map<String, Object>> findActiveIndexRevisions(@Param("tenantId") String tenantId,
                                                        @Param("knowledgeBaseId") Long knowledgeBaseId);
}
