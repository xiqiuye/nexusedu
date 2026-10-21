package com.xqy.nexusedu.knowledge.retrieval.application.gateway;

import java.util.List;

import com.xqy.nexusedu.knowledge.retrieval.application.model.KnowledgeVectorQuery;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeBase;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeDocument;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeDocumentVersion;
import com.xqy.nexusedu.knowledge.ingestion.infrastructure.persistence.entity.AiKnowledgeIngestionJob;
import com.xqy.nexusedu.knowledge.retrieval.infrastructure.persistence.entity.AiknowledgeChunk;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;


public interface KnowledgeService {
    boolean saveKnowledgeBase(AiKnowledgeBase value, InternalCallContext context);

    boolean updateKnowledgeBase(AiKnowledgeBase value, InternalCallContext context);

    boolean deleteKnowledgeBase(Long id, InternalCallContext context);

    AiKnowledgeBase getKnowledgeBaseById(Long id, InternalCallContext context);

    List<AiKnowledgeBase> listKnowledgeBases(InternalCallContext context);

    boolean saveDocument(AiKnowledgeDocument value, InternalCallContext context);

    boolean updateDocument(AiKnowledgeDocument value, InternalCallContext context);

    boolean deleteDocument(Long id, InternalCallContext context);

    AiKnowledgeDocument getDocumentById(Long id, InternalCallContext context);

    List<AiKnowledgeDocument> listDocuments(Long knowledgeBaseId, InternalCallContext context);

    boolean saveDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context);

    boolean updateDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context);

    AiKnowledgeDocumentVersion getDocumentVersionById(Long id, InternalCallContext context);

    List<AiKnowledgeDocumentVersion> listDocumentVersions(Long documentId, InternalCallContext context);

    boolean saveIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context);

    boolean updateIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context);

    AiKnowledgeIngestionJob getIngestionJobById(Long id, InternalCallContext context);

    List<AiKnowledgeIngestionJob> listIngestionJobs(Long documentVersionId, InternalCallContext context);

    /**
     * Performs a top-K vector search in the active Milvus index.
     */
    List<AiknowledgeChunk> queryVector(KnowledgeVectorQuery query);

    default List<AiknowledgeChunk> queryKnowledge(KnowledgeVectorQuery query) {
        return queryVector(query);
    }
}
