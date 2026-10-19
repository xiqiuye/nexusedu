package com.xqy.nexusedu.knowledge.application.model;

import java.io.Serializable;

/** DTOs used by the document-to-vector ingestion adapter. */
public final class KnowledgeIngestionDtos {
    private KnowledgeIngestionDtos() {
    }

    public static class IngestionRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long knowledgeBaseId;
        private Long documentVersionId;
        private Integer limit;
        private Integer indexRevision;
        private String tenantId;
        private String jobId;

        public IngestionRequest() {
        }

        public IngestionRequest(Long knowledgeBaseId, Long documentVersionId, Integer limit, Integer indexRevision,
                                String tenantId) {
            this(knowledgeBaseId, documentVersionId, limit, indexRevision, tenantId, null);
        }

        public IngestionRequest(Long knowledgeBaseId, Long documentVersionId, Integer limit, Integer indexRevision,
                                String tenantId, String jobId) {
            this.knowledgeBaseId = knowledgeBaseId;
            this.documentVersionId = documentVersionId;
            this.limit = limit;
            this.indexRevision = indexRevision;
            this.tenantId = tenantId;
            this.jobId = jobId;
        }

        public Long getKnowledgeBaseId() { return knowledgeBaseId; }
        public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
        public Long getDocumentVersionId() { return documentVersionId; }
        public void setDocumentVersionId(Long documentVersionId) { this.documentVersionId = documentVersionId; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public Integer getIndexRevision() { return indexRevision; }
        public void setIndexRevision(Integer indexRevision) { this.indexRevision = indexRevision; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }

        public Long knowledgeBaseId() { return knowledgeBaseId; }
        public Long documentVersionId() { return documentVersionId; }
        public Integer limit() { return limit; }
        public Integer indexRevision() { return indexRevision; }
        public String tenantId() { return tenantId; }
        public String jobId() { return jobId; }
    }

    public static class IngestionResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private int selected;
        private int imported;
        private int indexRevision;

        public IngestionResult() {
        }

        public IngestionResult(int selected, int imported, int indexRevision) {
            this.selected = selected;
            this.imported = imported;
            this.indexRevision = indexRevision;
        }

        public int getSelected() { return selected; }
        public void setSelected(int selected) { this.selected = selected; }
        public int getImported() { return imported; }
        public void setImported(int imported) { this.imported = imported; }
        public int getIndexRevision() { return indexRevision; }
        public void setIndexRevision(int indexRevision) { this.indexRevision = indexRevision; }

        public int selected() { return selected; }
        public int imported() { return imported; }
        public int indexRevision() { return indexRevision; }
    }
}
