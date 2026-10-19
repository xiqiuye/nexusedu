package com.xqy.nexusedu.knowledge.application.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionCommand;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionResult;
import com.xqy.nexusedu.knowledge.application.port.in.AiKnowledgeCommandService;
import com.xqy.nexusedu.shared.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.security.rpc.InternalCallSigner;
import com.xqy.nexusedu.knowledge.infrastructure.service.KnowledgeIngestionQueue;

@Service
public class AiKnowledgeCommandServiceImpl implements AiKnowledgeCommandService {
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeCommandServiceImpl.class);
    private final KnowledgeIngestionQueue ingestionService;
    private final InternalCallSigner internalCallSigner;

    public AiKnowledgeCommandServiceImpl(KnowledgeIngestionQueue ingestionService,
                                         @Value("${nexusedu.internal-rpc.secret}") String internalRpcSecret) {
        this.ingestionService = ingestionService;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    @Override
    public KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command) {
        if (command == null) throw new IllegalArgumentException("ingestion command must not be null");
        InternalCallContext context = command.callerContext();
        internalCallSigner.verify(context, "nexusedu-web-bff");
        if (!Objects.equals(context.tenantId(), command.tenantId())) {
            throw new InternalCallSigner.InternalCallAuthenticationException();
        }
        log.info("rpc_audit action=knowledge_ingest caller={} tenant={} user={} requestId={}", context.callerService(),
                context.tenantId(), context.userId(), context.requestId());
        return ingestionService.enqueue(command);
    }

    @Override
    public void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller) {
        internalCallSigner.verify(caller, "nexusedu-web-bff");
        ingestionService.activateIndex(documentVersionId, indexRevision, caller);
    }
}
