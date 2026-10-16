package com.xqy.nexusedu.application.knowledge.port.in;

import com.xqy.nexusedu.application.knowledge.model.KnowledgeIngestionCommand;
import com.xqy.nexusedu.application.knowledge.model.KnowledgeIngestionResult;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;

public interface AiKnowledgeCommandService {

    void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller);

    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
