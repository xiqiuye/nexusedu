package com.xqy.nexusedu.knowledge.catalog.infrastructure.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.knowledge.retrieval.application.model.KnowledgeVectorQuery;
import com.xqy.nexusedu.knowledge.retrieval.application.port.out.KnowledgeService;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeBase;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeDocument;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.entity.AiKnowledgeDocumentVersion;
import com.xqy.nexusedu.knowledge.ingestion.infrastructure.persistence.entity.AiKnowledgeIngestionJob;
import com.xqy.nexusedu.knowledge.retrieval.infrastructure.persistence.entity.AiknowledgeChunk;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.mapper.AiKnowledgeBaseMapper;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.mapper.AiKnowledgeDocumentMapper;
import com.xqy.nexusedu.knowledge.catalog.infrastructure.persistence.mapper.AiKnowledgeDocumentVersionMapper;
import com.xqy.nexusedu.knowledge.ingestion.infrastructure.persistence.mapper.AiKnowledgeIngestionJobMapper;
import com.xqy.nexusedu.knowledge.retrieval.infrastructure.vector.MilvusVectorStore;

@Service
@Transactional(rollbackFor = Exception.class)
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private final AiKnowledgeBaseMapper baseMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeDocumentVersionMapper versionMapper;
    private final AiKnowledgeIngestionJobMapper jobMapper;
    private final MilvusVectorStore vectorStore;
    private final InternalCallSigner internalCallSigner;
    @Autowired
    private JdbcTemplate metadata;

    public KnowledgeServiceImpl(AiKnowledgeBaseMapper baseMapper, AiKnowledgeDocumentMapper documentMapper,
                                AiKnowledgeDocumentVersionMapper versionMapper, AiKnowledgeIngestionJobMapper jobMapper,
                                MilvusVectorStore vectorStore,
                                @Value("${nexusedu.internal-rpc.secret}") String internalRpcSecret) {
        this.baseMapper = baseMapper;
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
        this.jobMapper = jobMapper;
        this.vectorStore = vectorStore;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    public boolean saveKnowledgeBase(AiKnowledgeBase value, InternalCallContext context) {
        if (value == null) return false;
        value.setTenantId(requireWebTenant(context));
        return baseMapper.insert(value) > 0;
    }

    public boolean updateKnowledgeBase(AiKnowledgeBase value, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return value != null && value.getId() != null && baseMapper.update(value,
                new LambdaQueryWrapper<AiKnowledgeBase>().eq(AiKnowledgeBase::getId, value.getId())
                        .eq(AiKnowledgeBase::getTenantId, tenant)) > 0;
    }

    public boolean deleteKnowledgeBase(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id != null && baseMapper.delete(new LambdaQueryWrapper<AiKnowledgeBase>()
                .eq(AiKnowledgeBase::getId, id).eq(AiKnowledgeBase::getTenantId, tenant)) > 0;
    }

    public AiKnowledgeBase getKnowledgeBaseById(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id == null ? null : baseMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeBase>()
                .eq(AiKnowledgeBase::getId, id).eq(AiKnowledgeBase::getTenantId, tenant));
    }

    public List<AiKnowledgeBase> listKnowledgeBases(InternalCallContext context) {
        return baseMapper.selectList(new LambdaQueryWrapper<AiKnowledgeBase>()
                .eq(AiKnowledgeBase::getTenantId, requireWebTenant(context)));
    }

    public boolean saveDocument(AiKnowledgeDocument value, InternalCallContext context) {
        if (value == null) return false;
        value.setTenantId(requireWebTenant(context));
        return documentMapper.insert(value) > 0;
    }

    public boolean updateDocument(AiKnowledgeDocument value, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return value != null && value.getId() != null && documentMapper.update(value,
                new LambdaQueryWrapper<AiKnowledgeDocument>().eq(AiKnowledgeDocument::getId, value.getId())
                        .eq(AiKnowledgeDocument::getTenantId, tenant)) > 0;
    }

    public boolean deleteDocument(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id != null && documentMapper.delete(new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getId, id).eq(AiKnowledgeDocument::getTenantId, tenant)) > 0;
    }

    public AiKnowledgeDocument getDocumentById(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id == null ? null : documentMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getId, id).eq(AiKnowledgeDocument::getTenantId, tenant));
    }

    public List<AiKnowledgeDocument> listDocuments(Long knowledgeBaseId, InternalCallContext context) {
        return documentMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getTenantId, requireWebTenant(context))
                .eq(knowledgeBaseId != null, AiKnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId));
    }

    public boolean saveDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context) {
        if (value == null) return false;
        value.setTenantId(requireWebTenant(context));
        return versionMapper.insert(value) > 0;
    }

    public boolean updateDocumentVersion(AiKnowledgeDocumentVersion value, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return value != null && value.getId() != null && versionMapper.update(value,
                new LambdaQueryWrapper<AiKnowledgeDocumentVersion>()
                        .eq(AiKnowledgeDocumentVersion::getId, value.getId())
                        .eq(AiKnowledgeDocumentVersion::getTenantId, tenant)) > 0;
    }

    public AiKnowledgeDocumentVersion getDocumentVersionById(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id == null ? null : versionMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeDocumentVersion>()
                .eq(AiKnowledgeDocumentVersion::getId, id).eq(AiKnowledgeDocumentVersion::getTenantId, tenant));
    }

    public List<AiKnowledgeDocumentVersion> listDocumentVersions(Long documentId, InternalCallContext context) {
        return versionMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocumentVersion>()
                .eq(AiKnowledgeDocumentVersion::getTenantId, requireWebTenant(context))
                .eq(documentId != null, AiKnowledgeDocumentVersion::getDocumentId, documentId));
    }

    public boolean saveIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context) {
        if (value == null) return false;
        value.setTenantId(requireWebTenant(context));
        return jobMapper.insert(value) > 0;
    }

    public boolean updateIngestionJob(AiKnowledgeIngestionJob value, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return value != null && value.getId() != null && jobMapper.update(value,
                new LambdaQueryWrapper<AiKnowledgeIngestionJob>().eq(AiKnowledgeIngestionJob::getId, value.getId())
                        .eq(AiKnowledgeIngestionJob::getTenantId, tenant)) > 0;
    }

    public AiKnowledgeIngestionJob getIngestionJobById(Long id, InternalCallContext context) {
        String tenant = requireWebTenant(context);
        return id == null ? null : jobMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeIngestionJob>()
                .eq(AiKnowledgeIngestionJob::getId, id).eq(AiKnowledgeIngestionJob::getTenantId, tenant));
    }

    public List<AiKnowledgeIngestionJob> listIngestionJobs(Long documentVersionId, InternalCallContext context) {
        return jobMapper.selectList(new LambdaQueryWrapper<AiKnowledgeIngestionJob>()
                .eq(AiKnowledgeIngestionJob::getTenantId, requireWebTenant(context))
                .eq(documentVersionId != null, AiKnowledgeIngestionJob::getDocumentVersionId, documentVersionId));
    }

    @Override
    public List<AiknowledgeChunk> queryVector(KnowledgeVectorQuery query) {
        if (query == null) return Collections.emptyList();
        internalCallSigner.verify(query.getCallerContext(), "nexusedu-ai-provider");
        if (!Objects.equals(query.getTenantId(), query.getCallerContext().tenantId())) {
            throw new InternalCallSigner.InternalCallAuthenticationException();
        }
        log.info("rpc_audit action=knowledge_query caller={} tenant={} user={} requestId={}",
                query.getCallerContext().callerService(), query.getTenantId(), query.getCallerContext().userId(),
                query.getCallerContext().requestId());
        KnowledgeVectorQuery effective = query;
        if (query.getKnowledgeBaseId() != null) {
            AiKnowledgeBase base = baseMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeBase>()
                    .eq(AiKnowledgeBase::getId, query.getKnowledgeBaseId())
                    .eq(AiKnowledgeBase::getTenantId, query.getTenantId()));
            if (base == null) return Collections.emptyList();
            {
                if (Boolean.FALSE.equals(base.getEnabled())) return Collections.emptyList();
                if (effective.getTopK() == null || effective.getTopK() <= 0) effective.setTopK(base.getTopk());
                if (effective.getSimilarityThreshold() == null)
                    effective.setSimilarityThreshold(base.getSimilarityThreshold());
            }
        }
        var activeVersions = new LinkedHashMap<Long, String>();
        String sql = "SELECT v.id,v.active_index_revision FROM ai_knowledge_document_version v JOIN ai_knowledge_document d ON d.id=v.document_id AND d.tenant_id=v.tenant_id JOIN ai_knowledge_base b ON b.id=d.knowledge_base_id AND b.tenant_id=d.tenant_id WHERE v.tenant_id=? AND b.enabled=1 AND d.status='ACTIVE' AND v.active_index_revision IS NOT NULL";
        List<Object> args = new ArrayList<>();
        args.add(query.getTenantId());
        if (query.getKnowledgeBaseId() != null) { sql += " AND b.id=?"; args.add(query.getKnowledgeBaseId()); }
        for (var row : metadata.queryForList(sql, args.toArray()))
            activeVersions.put(((Number) row.get("id")).longValue(), row.get("active_index_revision").toString());
        effective.setActiveVersions(activeVersions);
        if (activeVersions.isEmpty()) return Collections.emptyList();
        List<AiknowledgeChunk> chunks = vectorStore.search(effective);
        if (chunks == null) return Collections.emptyList();
        Double threshold = effective.getSimilarityThreshold();
        return chunks.stream()
                .filter(chunk -> chunk != null && "ACTIVE".equals(chunk.getStatus())
                        && Objects.equals(chunk.getTenantId(), query.getTenantId())
                        && activeVersions.containsKey(chunk.getDocumentVersionId())
                        && Objects.equals(activeVersions.get(chunk.getDocumentVersionId()), chunk.getIndexRevision())
                        && (query.getKnowledgeBaseId() == null || Objects.equals(chunk.getKnowledgeBaseId(), query.getKnowledgeBaseId())))
                .filter(chunk -> threshold == null || chunk.getScore() == null || chunk.getScore() >= threshold)
                .toList();
    }

    private String requireWebTenant(InternalCallContext context) {
        internalCallSigner.verify(context, "nexusedu-web-bff");
        return context.tenantId();
    }
}
