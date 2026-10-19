package com.xqy.nexusedu.knowledge.application.port.in;

import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionCommand;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionResult;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;

public interface AiKnowledgeCommandService {

    void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller);

    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
