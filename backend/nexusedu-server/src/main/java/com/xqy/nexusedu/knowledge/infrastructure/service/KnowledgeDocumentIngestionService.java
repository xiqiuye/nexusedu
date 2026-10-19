package com.xqy.nexusedu.knowledge.infrastructure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.ai.infrastructure.observability.observation.AiObservationService;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionDtos.IngestionRequest;
import com.xqy.nexusedu.knowledge.application.model.KnowledgeIngestionDtos.IngestionResult;

/** Reads MySQL staging details, creates embeddings, and upserts them into pgvector. */
@Service
public class KnowledgeDocumentIngestionService {
    private final JdbcTemplate sourceJdbcTemplate;
    private final JdbcTemplate vectorJdbcTemplate;
    private final KnowledgeEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final String vectorTable;
    private final int defaultLimit;
    private final int maxLimit;
    private final int defaultIndexRevision;
    private final TransactionTemplate vectorTransaction;

    public KnowledgeDocumentIngestionService(
            JdbcTemplate sourceJdbcTemplate,
            KnowledgeEmbeddingService embeddingService,
            ObjectMapper objectMapper,
            @Value("${nexusedu.ai.knowledge-ingestion.postgres.url}") String vectorUrl,
            @Value("${nexusedu.ai.knowledge-ingestion.postgres.username}") String vectorUsername,
            @Value("${nexusedu.ai.knowledge-ingestion.postgres.password}") String vectorPassword,
            @Value("${nexusedu.ai.knowledge-ingestion.postgres.table}") String vectorTable,
            @Value("${nexusedu.ai.knowledge-ingestion.default-limit}") int defaultLimit,
            @Value("${nexusedu.ai.knowledge-ingestion.max-limit}") int maxLimit,
            @Value("${nexusedu.ai.knowledge-ingestion.default-index-revision}") int defaultIndexRevision) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.vectorTable = safeIdentifier(vectorTable);
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.defaultIndexRevision = defaultIndexRevision;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(vectorUrl, vectorUsername, vectorPassword);
        dataSource.setDriverClassName("org.postgresql.Driver");
        this.vectorJdbcTemplate = new JdbcTemplate(dataSource);
        this.vectorTransaction = new TransactionTemplate(
                new DataSourceTransactionManager(dataSource));
    }

    public IngestionResult ingest(IngestionRequest request) {
        return ingestAfter(request, 0);
    }

    public IngestionResult ingestAfter(IngestionRequest request, int afterChunkNo) {
        int limit = request == null || request.limit() == null ? defaultLimit : request.limit();
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        limit = Math.min(limit, maxLimit);
        int indexRevision = request == null || request.indexRevision() == null ? defaultIndexRevision : request.indexRevision();
        if (indexRevision <= 0) throw new IllegalArgumentException("indexRevision must be positive");

        if (request == null || request.tenantId() == null || request.tenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        List<StagingDetail> details = loadDetails(request.knowledgeBaseId(),
                request.documentVersionId(), request.tenantId(), limit, afterChunkNo);
        if (details.isEmpty()) return new IngestionResult(0, 0, indexRevision);

        List<List<Float>> embeddings = embeddingService.embedAll(
                details.stream().map(StagingDetail::content).toList(),
                new AiObservationService.Scope(request.tenantId(), null, null, request.jobId()),
                "INGESTION_EMBEDDING");
        int imported = 0;
        for (int i = 0; i < details.size(); i++) {
            StagingDetail detail = details.get(i);
            upsertChunk(detail, embeddings.get(i), indexRevision, request.tenantId());
            imported++;
        }
        return new IngestionResult(details.size(), imported, indexRevision);
    }

    private List<StagingDetail> loadDetails(Long knowledgeBaseId, Long documentVersionId, String tenantId, int limit, int afterChunkNo) {
        StringBuilder sql = new StringBuilder("""
                SELECT d.knowledge_base_id, d.id AS document_id, d.document_no,
                       v.id AS document_version_id, x.chunk_no, x.content, x.metadata
                  FROM ai_knowledge_document_detail x
                  JOIN ai_knowledge_document d ON d.id = x.document_id
                  JOIN ai_knowledge_document_version v
                    ON v.id = x.document_version_id AND v.document_id = d.id AND v.tenant_id = d.tenant_id
                 WHERE d.tenant_id = ? AND x.tenant_id = d.tenant_id AND d.status='ACTIVE' AND x.chunk_no>?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(afterChunkNo);
        if (knowledgeBaseId != null) {
            sql.append(" AND d.knowledge_base_id = ?");
            args.add(knowledgeBaseId);
        }
        if (documentVersionId != null) {
            sql.append(" AND x.document_version_id = ?");
            args.add(documentVersionId);
        }
        sql.append(" ORDER BY x.document_version_id, x.chunk_no LIMIT ?");
        args.add(limit);
        return sourceJdbcTemplate.query(sql.toString(), (rs, rowNum) -> new StagingDetail(
                rs.getLong("knowledge_base_id"),
                rs.getLong("document_id"),
                rs.getString("document_no"),
                rs.getLong("document_version_id"),
                rs.getInt("chunk_no"),
                rs.getString("content"),
                rs.getString("metadata")
        ), args.toArray());
    }

    private void upsertChunk(StagingDetail detail, List<Float> embedding, int indexRevision, String tenantId) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("Embedding response is empty for chunk " + detail.chunkNo());
        }
        String sql = "INSERT INTO " + vectorTable + " "
                + "(tenant_id, knowledge_base_id, document_id, document_no, document_version_id, index_revision, "
                + "chunk_no, content, metadata, embedding, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector), 'STAGING') "
                + "ON CONFLICT (tenant_id, knowledge_base_id, document_id, document_version_id, index_revision, chunk_no) "
                + "DO UPDATE SET document_no = EXCLUDED.document_no, content = EXCLUDED.content, "
                + "metadata = EXCLUDED.metadata, embedding = EXCLUDED.embedding, "
                + "update_time = CURRENT_TIMESTAMP";
        vectorJdbcTemplate.update(sql,
                tenantId, detail.knowledgeBaseId(), detail.documentId(), detail.documentNo(), detail.documentVersionId(),
                indexRevision, detail.chunkNo(), detail.content(), metadataJson(detail), vectorLiteral(embedding));
    }

    /** Atomically switches the searchable revision after every staging chunk was imported. */
    public void activate(Long documentVersionId, int indexRevision, String tenantId) {
        List<Long> documents = sourceJdbcTemplate.queryForList("SELECT document_id FROM ai_knowledge_document_version WHERE id=? AND tenant_id=?", Long.class, documentVersionId, tenantId);
        if (documents.isEmpty()) throw new IllegalArgumentException("文档版本不存在");
        vectorTransaction.executeWithoutResult(transaction -> {
            vectorJdbcTemplate.update("UPDATE " + vectorTable + " SET status='INACTIVE',update_time=CURRENT_TIMESTAMP WHERE tenant_id=? AND document_id=? AND status='ACTIVE'", tenantId, documents.getFirst());
            int activated = vectorJdbcTemplate.update("UPDATE " + vectorTable + " SET status='ACTIVE',update_time=CURRENT_TIMESTAMP WHERE tenant_id=? AND document_version_id=? AND index_revision=?", tenantId, documentVersionId, indexRevision);
            if (activated == 0) throw new IllegalStateException("此索引没有已导入的分块");
        });
    }

    private String metadataJson(StagingDetail detail) {
        Map<String, Object> metadata = new HashMap<>();
        if (detail.metadata() != null && !detail.metadata().isBlank()) {
            try {
                metadata.putAll(objectMapper.readValue(detail.metadata(), new TypeReference<Map<String, Object>>() { }));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid metadata for document detail " + detail.chunkNo(), exception);
            }
        }
        metadata.putIfAbsent("documentId", detail.documentId());
        metadata.putIfAbsent("documentNo", detail.documentNo());
        metadata.putIfAbsent("documentVersionId", detail.documentVersionId());
        metadata.putIfAbsent("chunkNo", detail.chunkNo());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize document metadata", exception);
        }
    }

    private String vectorLiteral(List<Float> embedding) {
        StringBuilder value = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) value.append(',');
            value.append(embedding.get(i));
        }
        return value.append(']').toString();
    }

    private String safeIdentifier(String value) {
        String identifier = value == null ? "" : value.trim();
        if (!identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid PostgreSQL vector table name");
        }
        return identifier;
    }

    private static class StagingDetail {
        private Long knowledgeBaseId;
        private Long documentId;
        private String documentNo;
        private Long documentVersionId;
        private Integer chunkNo;
        private String content;
        private String metadata;

        StagingDetail(Long knowledgeBaseId, Long documentId, String documentNo, Long documentVersionId,
                      Integer chunkNo, String content, String metadata) {
            this.knowledgeBaseId = knowledgeBaseId;
            this.documentId = documentId;
            this.documentNo = documentNo;
            this.documentVersionId = documentVersionId;
            this.chunkNo = chunkNo;
            this.content = content;
            this.metadata = metadata;
        }

        public Long getKnowledgeBaseId() { return knowledgeBaseId; }
        public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
        public Long getDocumentVersionId() { return documentVersionId; }
        public void setDocumentVersionId(Long documentVersionId) { this.documentVersionId = documentVersionId; }
        public Integer getChunkNo() { return chunkNo; }
        public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
        public Long knowledgeBaseId() { return knowledgeBaseId; }
        public Long documentId() { return documentId; }
        public String documentNo() { return documentNo; }
        public Long documentVersionId() { return documentVersionId; }
        public Integer chunkNo() { return chunkNo; }
        public String content() { return content; }
        public String metadata() { return metadata; }
    }
}
