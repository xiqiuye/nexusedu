package com.xqy.nexusedu.infrastructure.knowledge.vector;

import java.util.List;
import com.xqy.nexusedu.common.dto.knowledge.KnowledgeVectorQuery;
import com.xqy.nexusedu.common.entity.knowledge.AiknowledgeChunk;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
