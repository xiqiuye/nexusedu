package com.xqy.nexusedu.application.knowledge.port.in;

import com.xqy.nexusedu.common.dto.knowledge.KnowledgeIngestionCommand;
import com.xqy.nexusedu.common.dto.knowledge.KnowledgeIngestionResult;
import com.xqy.nexusedu.common.security.rpc.InternalCallContext;

public interface AiKnowledgeCommandService {

    void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller);

    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
