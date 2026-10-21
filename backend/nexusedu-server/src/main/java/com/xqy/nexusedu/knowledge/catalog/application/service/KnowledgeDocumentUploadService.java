package com.xqy.nexusedu.knowledge.catalog.application.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.xqy.nexusedu.knowledge.ingestion.infrastructure.parser.DocumentChunker;
import com.xqy.nexusedu.knowledge.ingestion.infrastructure.parser.DocumentTextParser;
import com.xqy.nexusedu.shared.kernel.application.dto.ManagementRows;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class KnowledgeDocumentUploadService {
    private final JdbcTemplate jdbc;
    private final DocumentTextParser parser;
    private final DocumentChunker chunker;
    private final ObjectMapper json;
    private final AuthorizationPolicy authorization;
    private final Path uploadDirectory;

    public KnowledgeDocumentUploadService(JdbcTemplate jdbc, DocumentTextParser parser, DocumentChunker chunker,
                                          ObjectMapper json, AuthorizationPolicy authorization,
                                          @Value("${nexusedu.knowledge.upload-directory}") String uploadDirectory) {
        this.jdbc = jdbc;
        this.parser = parser;
        this.chunker = chunker;
        this.json = json;
        this.authorization = authorization;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(Long knowledgeBaseId, Long documentId, String filename, byte[] bytes, InternalCallContext caller) {
        authorization.authorize("knowledge:document:create", caller);
        Map<String, Object> base = owned("bases", knowledgeBaseId, caller);
        String text = parser.parse(filename, bytes);
        int chunkSize = ((Number) base.get("chunk_size")).intValue(), overlap = ((Number) base.get("chunk_overlap")).intValue();
        String strategy = base.get("split_strategy").toString();
        List<String> chunks = chunker.split(text, chunkSize, overlap, strategy);
        String actor = caller.userId().toString(), tenant = caller.tenantId();
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (documentId == null) {
                documentId = IdWorker.getId();
                jdbc.update("INSERT INTO ai_knowledge_document (id,tenant_id,knowledge_base_id,document_no,document_name,source_type,status,create_by,modify_by) VALUES (?,?,?,?,?,?,'ACTIVE',?,?)",
                        documentId, tenant, knowledgeBaseId, "UPLOAD-" + documentId, filename.replaceAll(".*[\\\\/]", ""), "UPLOAD", actor, actor);
            } else {
                Map<String, Object> document = owned("documents", documentId, caller);
                if (!Objects.equals(((Number) document.get("knowledge_base_id")).longValue(), knowledgeBaseId) || !"ACTIVE".equals(document.get("status")))
                    throw new IllegalArgumentException("文档不属于当前知识库或已删除");
                jdbc.queryForObject("SELECT id FROM ai_knowledge_document WHERE id=? AND tenant_id=? FOR UPDATE", Long.class, documentId, tenant);
                List<Map<String, Object>> same = jdbc.queryForList("SELECT " + KnowledgeManagementResources.COLUMNS.get("versions") + " FROM ai_knowledge_document_version WHERE tenant_id=? AND document_id=? AND content_hash=? AND chunk_size=? AND chunk_overlap=? AND split_strategy=? ORDER BY id DESC LIMIT 1", tenant, documentId, hash, chunkSize, overlap, strategy);
                if (!same.isEmpty()) return ManagementRows.publicRow(same.getFirst());
            }
            long versionId = IdWorker.getId();
            String version = "v-" + versionId;
            String storageName = UUID.randomUUID() + "." + parser.extension(filename);
            Files.createDirectories(uploadDirectory);
            Path storage = uploadDirectory.resolve(storageName).normalize();
            if (!storage.startsWith(uploadDirectory)) throw new IllegalArgumentException("无效存储路径");
            Files.write(storage, bytes);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) try {
                        Files.deleteIfExists(storage);
                    } catch (Exception ignored) {
                    }
                }
            });
            String mime = switch (parser.extension(filename)) {
                case "pdf" -> "application/pdf";
                case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "html", "htm" -> "text/html";
                default -> "text/plain";
            };
            jdbc.update("INSERT INTO ai_knowledge_document_version (id,tenant_id,document_id,version,source_uri,mime_type,content_hash,chunk_size,chunk_overlap,split_strategy,index_status,chunk_count,create_by,modify_by) VALUES (?,?,?,?,?,?,?,?,?,?,'STAGED',?,?,?)",
                    versionId, tenant, documentId, version, "upload://" + storageName, mime, hash, chunkSize, overlap, strategy, chunks.size(), actor, actor);
            for (int i = 0; i < chunks.size(); i++)
                jdbc.update("INSERT INTO ai_knowledge_document_detail (id,tenant_id,document_id,document_version_id,chunk_no,content,content_type,metadata,create_by,modify_by) VALUES (?,?,?,?,?,?,'text/plain',?,?,?)",
                        IdWorker.getId(), tenant, documentId, versionId, i + 1, chunks.get(i), json.writeValueAsString(Map.of("filename", filename, "strategy", strategy)), actor, actor);
            jdbc.update("UPDATE ai_knowledge_document SET version=?,modify_by=? WHERE id=? AND tenant_id=?", version, actor, documentId, tenant);
            return ManagementRows.publicRow(jdbc.queryForMap("SELECT " + KnowledgeManagementResources.COLUMNS.get("versions") + " FROM ai_knowledge_document_version WHERE id=? AND tenant_id=?", versionId, tenant));
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("文档保存失败", error);
        }
    }

    private Map<String, Object> owned(String resource, Long id, InternalCallContext caller) {
        ManagementRows.id(id);
        return jdbc.queryForList("SELECT * FROM " + KnowledgeManagementResources.TABLES.get(resource) + " WHERE id=? AND tenant_id=?", id, caller.tenantId())
                .stream().findFirst().orElseThrow(ManagementNotFoundException::new);
    }
}
