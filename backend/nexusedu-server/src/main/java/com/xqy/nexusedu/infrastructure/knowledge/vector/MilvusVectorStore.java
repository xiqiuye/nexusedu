package com.xqy.nexusedu.infrastructure.knowledge.vector;

import java.util.List;
import com.xqy.nexusedu.application.knowledge.model.KnowledgeVectorQuery;
import com.xqy.nexusedu.infrastructure.knowledge.persistence.entity.AiknowledgeChunk;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
