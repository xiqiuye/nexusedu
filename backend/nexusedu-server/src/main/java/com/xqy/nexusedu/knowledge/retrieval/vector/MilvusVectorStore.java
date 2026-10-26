package com.xqy.nexusedu.knowledge.retrieval.vector;

import java.util.List;
import com.xqy.nexusedu.knowledge.retrieval.dto.KnowledgeVectorQuery;
import com.xqy.nexusedu.knowledge.retrieval.entity.AiknowledgeChunk;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
