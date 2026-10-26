package com.xqy.nexusedu.knowledge.ingestion.service;

import com.xqy.nexusedu.knowledge.ingestion.dto.KnowledgeIngestionCommand;
import com.xqy.nexusedu.knowledge.ingestion.dto.KnowledgeIngestionResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface AiKnowledgeCommandService {

    void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller);

    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
