package com.xqy.nexusedu.knowledge.ingestion.application.port.in;

import com.xqy.nexusedu.knowledge.ingestion.application.model.KnowledgeIngestionCommand;
import com.xqy.nexusedu.knowledge.ingestion.application.model.KnowledgeIngestionResult;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

public interface AiKnowledgeCommandService {

    void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller);

    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
