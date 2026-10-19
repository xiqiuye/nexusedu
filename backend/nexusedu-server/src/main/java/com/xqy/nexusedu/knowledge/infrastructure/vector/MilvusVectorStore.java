package com.xqy.nexusedu.knowledge.infrastructure.vector;

import java.util.List;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeVectorQuery;
import com.xqy.nexusedu.knowledge.infrastructure.persistence.entity.AiknowledgeChunk;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
