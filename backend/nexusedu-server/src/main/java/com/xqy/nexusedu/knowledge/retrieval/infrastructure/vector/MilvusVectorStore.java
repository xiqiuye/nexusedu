package com.xqy.nexusedu.knowledge.retrieval.infrastructure.vector;

import java.util.List;
import com.xqy.nexusedu.knowledge.retrieval.application.model.KnowledgeVectorQuery;
import com.xqy.nexusedu.knowledge.retrieval.infrastructure.persistence.entity.AiknowledgeChunk;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
