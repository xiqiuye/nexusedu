package com.xqy.nexusedu.knowledge.catalog.service;

import java.util.Map;

public final class KnowledgeManagementResources {
    private KnowledgeManagementResources() {}

    public static final Map<String, String> TABLES = Map.of("bases", "ai_knowledge_base", "documents", "ai_knowledge_document",
            "versions", "ai_knowledge_document_version", "jobs", "ai_knowledge_ingestion_job", "chunks", "ai_knowledge_document_detail");
    public static final Map<String, String> COLUMNS = Map.of(
            "bases", "id,name,description,embedding_model_alias,chunk_size,chunk_overlap,split_strategy,topk,similarity_threshold,enabled,remark,create_time,update_time",
            "documents", "id,knowledge_base_id,document_no,document_name,source_type,version,category,keywords,status,create_time,update_time",
            "versions", "id,document_id,version,source_version,mime_type,content_hash,chunk_size,chunk_overlap,split_strategy,active_index_revision,index_status,chunk_count,error_message,create_time",
            "jobs", "id,document_version_id,index_revision,job_type,status,progress,retry_count,processed_chunks,cancel_requested,start_time,finish_time,error_message,create_time",
            "chunks", "id,document_id,document_version_id,chunk_no,content,content_type,metadata");
}
