package com.xqy.nexusedu.knowledge.retrieval.controller;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xqy.nexusedu.knowledge.ingestion.dto.KnowledgeIngestionCommand;
import com.xqy.nexusedu.knowledge.ingestion.dto.KnowledgeIngestionResult;
import com.xqy.nexusedu.system.authentication.dto.AuthDtos;
import com.xqy.nexusedu.knowledge.ingestion.service.AiKnowledgeCommandService;
import com.xqy.nexusedu.shared.kernel.application.dto.ApiResponse;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

/** Authenticated Web BFF edge for knowledge indexing commands. */
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {
    @Autowired
    private AiKnowledgeCommandService knowledgeService;
    private final InternalCallSigner internalCallSigner;

    public AiKnowledgeController(@Value("${nexusedu.internal-rpc.secret}") String internalRpcSecret) {
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    @PostMapping("/ingestions")
    public ApiResponse<KnowledgeIngestionResult> ingest(@RequestBody(required = false) KnowledgeIngestionCommand command,
                                                          Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthDtos.AuthenticatedUser user)) {
            throw new IllegalArgumentException("unauthenticated request");
        }
        if (user.tenantId() == null || user.tenantId().isBlank()) throw new IllegalArgumentException("tenant is required");
        if (command != null && command.limit() != null && (command.limit() <= 0 || command.limit() > 10_000)) {
            throw new IllegalArgumentException("limit must be between 1 and 10000");
        }
        String requestId = UUID.randomUUID().toString();
        KnowledgeIngestionCommand trusted = new KnowledgeIngestionCommand(command == null ? null : command.knowledgeBaseId(),
                command == null ? null : command.documentVersionId(), command == null ? null : command.limit(),
                command == null ? null : command.indexRevision(), user.tenantId(),
                internalCallSigner.sign("nexusedu-web-bff", user.tenantId(), user.userId(), user.sessionId(), requestId));
        return ApiResponse.success(knowledgeService.ingest(trusted));
    }
}
