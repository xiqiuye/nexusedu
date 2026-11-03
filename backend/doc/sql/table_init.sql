-- NEXUSEDU MySQL 8.0+ bootstrap schema.
-- Run this script before data_init.sql on a database created with utf8mb4.
SET NAMES utf8mb4;

-- student 表 DDL
CREATE TABLE IF NOT EXISTS student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    student_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    age INT DEFAULT NULL,
    gender VARCHAR(16) DEFAULT NULL,
    birthday DATE DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    enrolled_at DATETIME DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_tenant_no (tenant_id, student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Student';


-- Elementary school organization and enrollment schema.
CREATE TABLE IF NOT EXISTS school_campus (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, campus_code VARCHAR(32) NOT NULL,
 name VARCHAR(128) NOT NULL, address VARCHAR(255) DEFAULT NULL, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_campus_tenant_code(tenant_id,campus_code), KEY idx_campus_tenant_status(tenant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_academic_year (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, name VARCHAR(32) NOT NULL,
 starts_on DATE NOT NULL, ends_on DATE NOT NULL, is_current TINYINT(1) NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_year_tenant_name(tenant_id,name), KEY idx_year_current(tenant_id,is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_class (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, campus_id BIGINT NOT NULL,
 academic_year_id BIGINT NOT NULL, grade TINYINT NOT NULL, class_name VARCHAR(64) NOT NULL,
 capacity INT DEFAULT NULL, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 PRIMARY KEY(id), UNIQUE KEY uk_class_year_name(tenant_id,campus_id,academic_year_id,grade,class_name),
 KEY idx_class_scope(tenant_id,campus_id,academic_year_id,grade),
 CONSTRAINT fk_class_campus FOREIGN KEY(campus_id) REFERENCES school_campus(id),
 CONSTRAINT fk_class_year FOREIGN KEY(academic_year_id) REFERENCES school_academic_year(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_campus_admin (
 tenant_id VARCHAR(64) NOT NULL, campus_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,campus_id,user_id), KEY idx_campus_admin_user(tenant_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_homeroom_assignment (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, class_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 starts_on DATE NOT NULL, ends_on DATE DEFAULT NULL, PRIMARY KEY(id),
 KEY idx_homeroom_user_active(tenant_id,user_id,ends_on), KEY idx_homeroom_class_active(tenant_id,class_id,ends_on),
 CONSTRAINT fk_homeroom_class FOREIGN KEY(class_id) REFERENCES school_class(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_school_class_scope (
 tenant_id VARCHAR(64) NOT NULL, class_id BIGINT NOT NULL, user_id BIGINT NOT NULL, PRIMARY KEY(tenant_id,class_id,user_id),
 KEY idx_class_scope_user(tenant_id,user_id), CONSTRAINT fk_scope_class FOREIGN KEY(class_id) REFERENCES school_class(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_enrollment (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, student_id BIGINT NOT NULL,
 academic_year_id BIGINT NOT NULL, campus_id BIGINT NOT NULL, grade TINYINT DEFAULT NULL, class_id BIGINT DEFAULT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED', starts_on DATE NOT NULL, ends_on DATE DEFAULT NULL,
 change_reason VARCHAR(255) DEFAULT NULL, changed_by BIGINT DEFAULT NULL, PRIMARY KEY(id),
 KEY idx_enrollment_student_year(tenant_id,student_id,academic_year_id,starts_on),
 KEY idx_enrollment_scope(tenant_id,academic_year_id,campus_id,grade,class_id,status),
 CONSTRAINT fk_enrollment_student FOREIGN KEY(student_id) REFERENCES student(id),
 CONSTRAINT fk_enrollment_campus FOREIGN KEY(campus_id) REFERENCES school_campus(id),
 CONSTRAINT fk_enrollment_year FOREIGN KEY(academic_year_id) REFERENCES school_academic_year(id),
 CONSTRAINT fk_enrollment_class FOREIGN KEY(class_id) REFERENCES school_class(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_guardian_contact (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, student_id BIGINT NOT NULL,
 name VARCHAR(128) NOT NULL, relationship VARCHAR(32) NOT NULL, phone VARCHAR(32) NOT NULL,
 is_primary TINYINT(1) NOT NULL DEFAULT 0, PRIMARY KEY(id), KEY idx_guardian_student(tenant_id,student_id),
 CONSTRAINT fk_guardian_student FOREIGN KEY(student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- School staff, teaching assignments, promotion audit and assessment records.
CREATE TABLE IF NOT EXISTS school_teacher (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL,
 employee_no VARCHAR(64) NOT NULL, title VARCHAR(64) DEFAULT NULL, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_teacher_tenant_user(tenant_id,user_id), UNIQUE KEY uk_teacher_tenant_no(tenant_id,employee_no),
 KEY idx_teacher_status(tenant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_subject (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, subject_code VARCHAR(32) NOT NULL,
 name VARCHAR(64) NOT NULL, grade_min TINYINT NOT NULL DEFAULT 1, grade_max TINYINT NOT NULL DEFAULT 6,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', PRIMARY KEY(id), UNIQUE KEY uk_subject_tenant_code(tenant_id,subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_class_teacher (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, class_id BIGINT NOT NULL,
 teacher_id BIGINT NOT NULL, subject_id BIGINT DEFAULT NULL, assignment_type VARCHAR(16) NOT NULL DEFAULT 'SUBJECT',
 starts_on DATE NOT NULL, ends_on DATE DEFAULT NULL, PRIMARY KEY(id),
 KEY idx_class_teacher_active(tenant_id,class_id,ends_on), KEY idx_teacher_class_active(tenant_id,teacher_id,ends_on),
 CONSTRAINT fk_class_teacher_class FOREIGN KEY(class_id) REFERENCES school_class(id),
 CONSTRAINT fk_class_teacher_teacher FOREIGN KEY(teacher_id) REFERENCES school_teacher(id),
 CONSTRAINT fk_class_teacher_subject FOREIGN KEY(subject_id) REFERENCES school_subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_promotion_batch (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, campus_id BIGINT NOT NULL,
 source_year_id BIGINT NOT NULL, target_year_id BIGINT NOT NULL, source_grade TINYINT NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED', created_by BIGINT NOT NULL, confirmed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), KEY idx_promotion_scope(tenant_id,campus_id,source_year_id,target_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_promotion_item (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, batch_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL, source_enrollment_id BIGINT NOT NULL, target_enrollment_id BIGINT DEFAULT NULL,
 action VARCHAR(16) NOT NULL, target_grade TINYINT DEFAULT NULL, target_class_id BIGINT DEFAULT NULL,
 reason VARCHAR(255) DEFAULT NULL, PRIMARY KEY(id), KEY idx_promotion_item_batch(tenant_id,batch_id),
 KEY idx_promotion_item_student(tenant_id,student_id),
 CONSTRAINT fk_promotion_item_batch FOREIGN KEY(batch_id) REFERENCES school_promotion_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_exam (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, academic_year_id BIGINT NOT NULL,
 campus_id BIGINT NOT NULL, grade TINYINT NOT NULL, name VARCHAR(128) NOT NULL, term VARCHAR(16) NOT NULL,
 exam_date DATE NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'OPEN', created_by BIGINT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id),
 KEY idx_exam_scope(tenant_id,academic_year_id,campus_id,grade,exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS school_score (
 id BIGINT NOT NULL AUTO_INCREMENT, tenant_id VARCHAR(64) NOT NULL, exam_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL, enrollment_id BIGINT NOT NULL, class_id BIGINT NOT NULL, subject_id BIGINT NOT NULL,
 score DECIMAL(6,2) DEFAULT NULL, absent TINYINT(1) NOT NULL DEFAULT 0, remark VARCHAR(255) DEFAULT NULL,
 modified_by BIGINT NOT NULL, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_score_exam_student_subject(tenant_id,exam_id,student_id,subject_id),
 KEY idx_score_class_exam(tenant_id,class_id,exam_id),
 CONSTRAINT fk_score_exam FOREIGN KEY(exam_id) REFERENCES school_exam(id),
 CONSTRAINT fk_score_subject FOREIGN KEY(subject_id) REFERENCES school_subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Idempotent upgrade for installations whose student table predates tenant scoping.
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE student ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT ''default'' AFTER id', 'SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='student' AND column_name='tenant_id');
PREPARE student_upgrade FROM @ddl; EXECUTE student_upgrade; DEALLOCATE PREPARE student_upgrade;
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE student DROP INDEX uq_student_no', 'SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='student' AND index_name='uq_student_no');
PREPARE student_upgrade FROM @ddl; EXECUTE student_upgrade; DEALLOCATE PREPARE student_upgrade;
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE student ADD UNIQUE KEY uk_student_tenant_no (tenant_id,student_no)', 'SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='student' AND index_name='uk_student_tenant_no');
PREPARE student_upgrade FROM @ddl; EXECUTE student_upgrade; DEALLOCATE PREPARE student_upgrade;

-- AI 工具定义表
CREATE TABLE IF NOT EXISTS ai_tool_definition (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    domain VARCHAR(128) NOT NULL COMMENT '工具所属领域',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    description TEXT DEFAULT NULL COMMENT '工具描述',
    argument_specification TEXT DEFAULT NULL COMMENT '参数定义',
    keywords TEXT DEFAULT NULL COMMENT '关键词',
    enable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    version VARCHAR(20) DEFAULT NULL COMMENT '版本号',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_tool_definition_domain_name (domain, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具定义表';

-- AI 模型定义表
CREATE TABLE IF NOT EXISTS ai_model_definition (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    provider VARCHAR(64) NOT NULL COMMENT '模型厂商，例如 openai、deepseek',
    model_name VARCHAR(128) NOT NULL COMMENT '厂商模型名称',
    base_url VARCHAR(500) DEFAULT NULL COMMENT '模型服务基础地址',
    api_key VARCHAR(128) NOT NULL COMMENT 'API key 环境变量名，例如 OPENAI_API_KEY',
    alias VARCHAR(64) NOT NULL COMMENT '模型别名，例如 balanced、strong、fast',
    capabilities VARCHAR(255) DEFAULT NULL COMMENT '模型能力，逗号分隔，例如 chat,stream',
    fallback_alias VARCHAR(64) DEFAULT NULL COMMENT '当前模型不可用时的回退模型别名',
    enabled VARCHAR(16) NOT NULL DEFAULT '1' COMMENT '是否启用，支持 1、true、yes',
    remark VARCHAR(500) DEFAULT NULL COMMENT '中文备注',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '记录创建者',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '记录修改者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_model_definition_alias (alias),
    UNIQUE KEY uq_ai_model_definition_provider_name (provider, model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型定义表';

-- AI prompt template table
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id BIGINT NOT NULL COMMENT 'primary key',
    prompt_code VARCHAR(128) NOT NULL COMMENT 'prompt template code',
    prompt_name VARCHAR(128) NOT NULL COMMENT 'prompt template name',
    prompt_content TEXT NOT NULL COMMENT 'prompt template content',
    version VARCHAR(32) DEFAULT NULL COMMENT 'prompt template version',
    enabled VARCHAR(16) NOT NULL DEFAULT '1' COMMENT 'whether the template is enabled',
    remark VARCHAR(500) DEFAULT NULL COMMENT 'remark',
    create_by VARCHAR(64) DEFAULT NULL COMMENT 'creator',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT 'modifier',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_prompt_template_code (prompt_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI prompt templates';

-- AI request invocation log
CREATE TABLE IF NOT EXISTS ai_request_log (
    id BIGINT NOT NULL COMMENT 'primary key',
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    request_id VARCHAR(64) NOT NULL COMMENT 'request trace id',
    user_id VARCHAR(64) DEFAULT NULL,
    session_id VARCHAR(128) DEFAULT NULL,
    request_type VARCHAR(32) DEFAULT NULL,
    question TEXT DEFAULT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    duration_time BIGINT DEFAULT NULL COMMENT 'duration in milliseconds',
    input_token_count INT DEFAULT NULL,
    output_token_count INT DEFAULT NULL,
    total_token_count INT DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_request_log_request_id (request_id),
    KEY idx_ai_request_log_tenant_session (tenant_id, session_id),
    KEY idx_ai_request_log_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI request invocation log';

-- AI task run lifecycle. A run groups planning, tool execution and compose steps.
CREATE TABLE IF NOT EXISTS ai_task_run (
    id BIGINT NOT NULL COMMENT 'primary key',
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    run_id VARCHAR(64) NOT NULL COMMENT 'run trace id',
    request_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) DEFAULT NULL,
    session_id VARCHAR(128) DEFAULT NULL,
    run_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    stream_key VARCHAR(128) DEFAULT NULL,
    conversation_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    question TEXT NOT NULL,
    model_alias VARCHAR(64) DEFAULT NULL,
    plan_json LONGTEXT DEFAULT NULL,
    idempotency_key VARCHAR(128) DEFAULT NULL,
    current_step_no INT NOT NULL DEFAULT 0,
    cancel_request TINYINT(1) NOT NULL DEFAULT 0,
    cancel_request_time DATETIME(3) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_task_run_run_id (run_id),
    UNIQUE KEY uq_ai_task_run_tenant_idempotency (tenant_id, idempotency_key),
    KEY idx_ai_task_run_request (request_id),
    KEY idx_ai_task_run_status (status),
    KEY idx_ai_task_run_tenant_user (tenant_id, user_id),
    KEY idx_ai_task_run_tenant_session (tenant_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task run lifecycle';

-- AI task step lifecycle. A step is a plan, tool or compose operation.
CREATE TABLE IF NOT EXISTS ai_task_step (
    id BIGINT NOT NULL COMMENT 'primary key',
    step_id VARCHAR(64) NOT NULL COMMENT 'step trace id',
    task_run_id VARCHAR(64) NOT NULL,
    step_no INT NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    domain VARCHAR(128) DEFAULT NULL,
    tool_name VARCHAR(128) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_json LONGTEXT DEFAULT NULL,
    output_json LONGTEXT DEFAULT NULL,
    attempt INT NOT NULL DEFAULT 0,
    max_attempt INT NOT NULL DEFAULT 3,
    timeout_ms BIGINT NOT NULL DEFAULT 30000,
    next_retry_time DATETIME(3) DEFAULT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    lease_expire_time DATETIME(3) DEFAULT NULL,
    start_time DATETIME(3) DEFAULT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_task_step_step_id (step_id),
    UNIQUE KEY uq_ai_task_step_run_no (task_run_id, step_no),
    UNIQUE KEY uq_ai_task_step_idempotency (idempotency_key),
    KEY idx_ai_task_step_status_lease (status, lease_expire_time),
    KEY idx_ai_task_step_status_retry (status, next_retry_time),
    KEY idx_ai_task_step_run (task_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task step lifecycle';

-- AI tool execution log
CREATE TABLE IF NOT EXISTS ai_tool_execute_log (
    id BIGINT NOT NULL COMMENT 'primary key',
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    tool_execute_id VARCHAR(64) NOT NULL COMMENT 'tool execution trace id',
    request_id VARCHAR(64) DEFAULT NULL,
    domain VARCHAR(128) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    question TEXT DEFAULT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    start_time DATETIME(3) NOT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    duration_time BIGINT DEFAULT NULL COMMENT 'duration in milliseconds',
    input_token_count INT DEFAULT NULL,
    output_token_count INT DEFAULT NULL,
    total_token_count INT DEFAULT NULL,
    result_count INT DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    error_code VARCHAR(64) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_tool_execute_log_execute_id (tool_execute_id),
    KEY idx_ai_tool_execute_log_tenant_request (tenant_id, request_id),
    KEY idx_ai_tool_execute_log_start_time (start_time),
    KEY idx_ai_tool_execute_log_tool (domain, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI tool execution log';

-- Knowledge base metadata used by the RAG ingestion and retrieval services.
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    embedding_model_alias VARCHAR(128) DEFAULT NULL,
    chunk_size INT NOT NULL DEFAULT 800,
    chunk_overlap INT NOT NULL DEFAULT 120,
    split_strategy VARCHAR(32) NOT NULL DEFAULT 'PARAGRAPH',
    topk INT NOT NULL DEFAULT 5,
    similarity_threshold DOUBLE DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_base_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG knowledge bases';

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    knowledge_base_id BIGINT NOT NULL,
    document_no VARCHAR(128) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version VARCHAR(64) DEFAULT NULL,
    category VARCHAR(128) DEFAULT NULL,
    keywords TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_document_tenant_no (tenant_id, knowledge_base_id, document_no),
    KEY idx_ai_knowledge_document_base (tenant_id, knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG documents';

-- Persistent staging details used as the source for vector database ingestion.
CREATE TABLE IF NOT EXISTS ai_knowledge_document_detail (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    document_id BIGINT NOT NULL,
    document_version_id BIGINT NOT NULL,
    chunk_no INT NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(64) DEFAULT 'text/plain',
    metadata JSON DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_detail_tenant_chunk (tenant_id, document_version_id, chunk_no),
    KEY idx_ai_knowledge_document_detail_document (tenant_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document content staging details';

CREATE TABLE IF NOT EXISTS ai_knowledge_document_version (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    document_id BIGINT NOT NULL,
    version VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1000) DEFAULT NULL,
    source_version VARCHAR(128) DEFAULT NULL,
    mime_type VARCHAR(128) DEFAULT NULL,
    content_hash VARCHAR(128) DEFAULT NULL,
    chunk_size INT NOT NULL DEFAULT 800,
    chunk_overlap INT NOT NULL DEFAULT 120,
    split_strategy VARCHAR(32) NOT NULL DEFAULT 'PARAGRAPH',
    active_index_revision VARCHAR(64) DEFAULT NULL,
    index_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    chunk_count INT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_knowledge_version_tenant (tenant_id, document_id, version),
    KEY idx_ai_knowledge_document_version_status (tenant_id, document_id, index_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document versions';

CREATE TABLE IF NOT EXISTS ai_knowledge_ingestion_job (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    document_version_id BIGINT NOT NULL,
    index_revision VARCHAR(64) NOT NULL,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    processed_chunks INT NOT NULL DEFAULT 0,
    cancel_requested TINYINT(1) NOT NULL DEFAULT 0,
    worker_token VARCHAR(64) DEFAULT NULL,
    lease_expire_time DATETIME(3) DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    start_time DATETIME(3) DEFAULT NULL,
    finish_time DATETIME(3) DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_knowledge_ingestion_job_version (tenant_id, document_version_id),
    UNIQUE KEY uq_ai_knowledge_job_revision (tenant_id, document_version_id, index_revision, job_type),
    KEY idx_ai_knowledge_job_claim (status, lease_expire_time),
    KEY idx_ai_knowledge_ingestion_job_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG ingestion jobs';
-- System identity and authorization (RBAC)
-- AI observations and immutable per-tenant price versions. No external metrics stack is required.
CREATE TABLE IF NOT EXISTS ai_model_price_version (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    currency CHAR(3) NOT NULL,
    input_price DECIMAL(20,8) DEFAULT NULL COMMENT 'Price per million input tokens',
    cached_input_price DECIMAL(20,8) DEFAULT NULL,
    output_price DECIMAL(20,8) DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    effective_from DATETIME(3) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_price_lookup (tenant_id,provider,model_name,enabled,effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable model token price versions';

CREATE TABLE IF NOT EXISTS ai_model_call_log (
    id BIGINT NOT NULL,
    call_id VARCHAR(64) NOT NULL,
    logical_call_id VARCHAR(64) NOT NULL,
    attempt INT NOT NULL DEFAULT 1,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    run_id VARCHAR(64) DEFAULT NULL,
    job_id VARCHAR(64) DEFAULT NULL,
    operation VARCHAR(32) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    requested_model VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    response_id VARCHAR(128) DEFAULT NULL,
    observation_scope VARCHAR(32) NOT NULL DEFAULT 'UPSTREAM_ATTEMPT',
    outcome VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    error_category VARCHAR(64) DEFAULT NULL,
    started_at DATETIME(3) NOT NULL,
    finished_at DATETIME(3) DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL,
    ttft_ms BIGINT DEFAULT NULL,
    input_tokens BIGINT DEFAULT NULL,
    cached_input_tokens BIGINT DEFAULT NULL,
    output_tokens BIGINT DEFAULT NULL,
    usage_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    price_version_id BIGINT DEFAULT NULL,
    currency CHAR(3) DEFAULT NULL,
    input_price DECIMAL(20,8) DEFAULT NULL,
    cached_input_price DECIMAL(20,8) DEFAULT NULL,
    output_price DECIMAL(20,8) DEFAULT NULL,
    estimated_cost DECIMAL(24,12) DEFAULT NULL,
    cost_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN_USAGE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_model_call (call_id),
    KEY idx_ai_model_call_window (tenant_id,started_at,outcome),
    KEY idx_ai_model_call_run (tenant_id,run_id),
    KEY idx_ai_model_call_job (tenant_id,job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Model invocation usage and estimated cost ledger';

CREATE TABLE IF NOT EXISTS ai_execution_observation (
    id BIGINT NOT NULL,
    observation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    run_id VARCHAR(64) DEFAULT NULL,
    step_id VARCHAR(64) DEFAULT NULL,
    kind VARCHAR(32) NOT NULL COMMENT 'RAG or TOOL_ATTEMPT',
    tool_name VARCHAR(128) DEFAULT NULL,
    attempt INT DEFAULT NULL,
    outcome VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    error_category VARCHAR(64) DEFAULT NULL,
    started_at DATETIME(3) NOT NULL,
    finished_at DATETIME(3) DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL,
    result_count INT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_execution_observation (observation_id),
    KEY idx_ai_observation_window (tenant_id,kind,started_at,outcome),
    KEY idx_ai_observation_run (tenant_id,run_id,step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG retrievals and authoritative tool attempt outcomes';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    real_name VARCHAR(64) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    avatar VARCHAR(512) DEFAULT NULL,
    user_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(64) DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System user';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    role_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code),
    KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System role';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-role relationship';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    path VARCHAR(256) DEFAULT NULL,
    route_name VARCHAR(128) DEFAULT NULL,
    component VARCHAR(256) DEFAULT NULL,
    redirect VARCHAR(256) DEFAULT NULL,
    title VARCHAR(128) NOT NULL,
    icon VARCHAR(128) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    keep_alive TINYINT(1) NOT NULL DEFAULT 0,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    hide_tab TINYINT(1) NOT NULL DEFAULT 0,
    full_page TINYINT(1) NOT NULL DEFAULT 0,
    external_link VARCHAR(512) DEFAULT NULL,
    iframe_flag TINYINT(1) NOT NULL DEFAULT 0,
    active_path VARCHAR(256) DEFAULT NULL,
    status VARCHAR(16) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System menu';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-menu relationship';

CREATE TABLE IF NOT EXISTS sys_button (
    id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    button_name VARCHAR(64) NOT NULL,
    auth_remark VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_button_menu_auth (menu_id, auth_remark),
    KEY idx_sys_button_menu_id (menu_id),
    KEY idx_sys_button_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System button permission';

CREATE TABLE IF NOT EXISTS sys_role_button (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    button_id BIGINT NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_button (role_id, button_id),
    KEY idx_sys_role_button_button_id (button_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-button relationship';

CREATE TABLE IF NOT EXISTS sys_user_session (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL,
    access_token CHAR(64) NOT NULL COMMENT 'SHA-256 hash of the access token',
    refresh_token CHAR(64) NOT NULL COMMENT 'SHA-256 hash of the refresh token',
    access_jti VARCHAR(64) DEFAULT NULL,
    refresh_jti VARCHAR(64) DEFAULT NULL,
    login_ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(1024) DEFAULT NULL,
    device_type VARCHAR(32) NOT NULL,
    login_time DATETIME NOT NULL,
    expire_time DATETIME NOT NULL,
    refresh_expire_time DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    logout_time DATETIME DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_user_session_tenant_user (tenant_id, user_id),
    KEY idx_sys_user_session_status_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User login session';

CREATE TABLE IF NOT EXISTS ai_chat_conversation (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    title VARCHAR(512) DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_message_at DATETIME DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_chat_conversation_owner (tenant_id, user_id, session_id),
    KEY idx_ai_chat_conversation_last_message (tenant_id, user_id, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI chat conversation';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    sequence_no INT NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    modify_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_chat_message_sequence (conversation_id, sequence_no),
    UNIQUE KEY uk_ai_chat_message_run_role (tenant_id, run_id, role),
    KEY idx_ai_chat_message_tenant_session (tenant_id, user_id, session_id, create_time),
    KEY idx_ai_chat_message_run (run_id),
    CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_chat_conversation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI chat message';

-- Repeatable upgrade: a nullable default slot preserves session-based defaults
-- while allowing arbitrarily many explicitly created conversations in the same JWT session.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ai_chat_conversation' AND column_name='default_slot')=0,
 'ALTER TABLE ai_chat_conversation ADD COLUMN default_slot VARCHAR(64) NULL, ADD COLUMN retain_until DATETIME NULL', 'SELECT 1');
PREPARE ai_upgrade FROM @ddl;
EXECUTE ai_upgrade;
DEALLOCATE PREPARE ai_upgrade;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='ai_chat_conversation' AND index_name='uk_ai_chat_conversation_owner')>0,
 'UPDATE ai_chat_conversation SET default_slot=session_id WHERE status=''ACTIVE''', 'SELECT 1');
PREPARE ai_upgrade FROM @ddl;
EXECUTE ai_upgrade;
DEALLOCATE PREPARE ai_upgrade;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='ai_chat_conversation' AND index_name='uk_ai_chat_conversation_owner')>0,
 'ALTER TABLE ai_chat_conversation DROP INDEX uk_ai_chat_conversation_owner, ADD UNIQUE KEY uk_ai_chat_default_slot(tenant_id,user_id,default_slot)', 'SELECT 1');
PREPARE ai_upgrade FROM @ddl;
EXECUTE ai_upgrade;
DEALLOCATE PREPARE ai_upgrade;

CREATE TABLE IF NOT EXISTS ai_chat_retention (
 tenant_id VARCHAR(64) NOT NULL, user_id VARCHAR(64) NOT NULL,
 retention_days INT NOT NULL DEFAULT 90, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,user_id), CHECK(retention_days BETWEEN 1 AND 3650)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_run_sources (
 tenant_id VARCHAR(64) NOT NULL, run_id VARCHAR(64) NOT NULL, conversation_id BIGINT NOT NULL,
 sources_json JSON NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,run_id), KEY idx_sources_conversation(conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_resource_version (
 id BIGINT NOT NULL, tenant_id VARCHAR(64) NOT NULL, resource_type VARCHAR(16) NOT NULL,
 resource_key VARCHAR(128) NOT NULL, version VARCHAR(32) NOT NULL, config_json JSON NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT', create_by VARCHAR(64) NOT NULL,
 reviewed_by VARCHAR(64) NULL, review_note VARCHAR(500) NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, reviewed_at DATETIME NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_resource_version(tenant_id,resource_type,resource_key,version),
 KEY idx_resource_status(tenant_id,status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_resource_rollout (
 tenant_id VARCHAR(64) NOT NULL, resource_type VARCHAR(16) NOT NULL, resource_key VARCHAR(128) NOT NULL,
 stable_version_id BIGINT NOT NULL, candidate_version_id BIGINT NULL, canary_percent INT NOT NULL DEFAULT 0,
 modify_by VARCHAR(64) NOT NULL, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,resource_type,resource_key), CHECK(canary_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_resource_policy (
 tenant_id VARCHAR(64) NOT NULL, resource_type VARCHAR(16) NOT NULL, resource_key VARCHAR(128) NOT NULL,
 enabled TINYINT NOT NULL DEFAULT 1, requests_per_minute INT NOT NULL DEFAULT 0,
 daily_budget DECIMAL(24,12) NULL, reserve_per_call DECIMAL(24,12) NOT NULL DEFAULT 0,
 currency VARCHAR(8) NOT NULL DEFAULT 'USD', permission_code VARCHAR(128) NULL,
 modify_by VARCHAR(64) NOT NULL, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,resource_type,resource_key), CHECK(requests_per_minute BETWEEN 0 AND 1000000),
 CHECK(daily_budget IS NULL OR daily_budget>=0), CHECK(reserve_per_call>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Database row locks make limits effective across provider replicas. Unknown or
-- cancelled model calls keep their reservation until authoritative usage arrives.
CREATE TABLE IF NOT EXISTS ai_resource_usage (
 tenant_id VARCHAR(64) NOT NULL, resource_type VARCHAR(16) NOT NULL, resource_key VARCHAR(128) NOT NULL,
 window_key VARCHAR(32) NOT NULL, request_count BIGINT NOT NULL DEFAULT 0, charged_cost DECIMAL(24,12) NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,resource_type,resource_key,window_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_cost_reservation (
 call_id VARCHAR(64) NOT NULL, tenant_id VARCHAR(64) NOT NULL, resource_key VARCHAR(128) NOT NULL,
 window_key VARCHAR(32) NOT NULL, currency VARCHAR(8) NOT NULL, reserved_cost DECIMAL(24,12) NOT NULL,
 settled TINYINT NOT NULL DEFAULT 0, PRIMARY KEY(call_id,resource_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_execution_release (
 tenant_id VARCHAR(64) NOT NULL, run_id VARCHAR(64) NOT NULL, resource_type VARCHAR(16) NOT NULL,
 resource_key VARCHAR(128) NOT NULL, version_id BIGINT NOT NULL, config_json JSON NOT NULL,
 PRIMARY KEY(tenant_id,run_id,resource_type,resource_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_eval_case (
 id BIGINT NOT NULL, tenant_id VARCHAR(64) NOT NULL, dataset VARCHAR(128) NOT NULL,
 case_key VARCHAR(128) NOT NULL, version INT NOT NULL, question VARCHAR(4000) NOT NULL,
 expected_plan JSON NOT NULL, expected_tools JSON NOT NULL, expected_sources JSON NOT NULL,
 expected_refusal TINYINT NOT NULL DEFAULT 0, enabled TINYINT NOT NULL DEFAULT 1,
 create_by VARCHAR(64) NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_eval_case(tenant_id,dataset,case_key,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_eval_batch (
 id BIGINT NOT NULL, tenant_id VARCHAR(64) NOT NULL, dataset VARCHAR(128) NOT NULL,
 model_alias VARCHAR(64) NOT NULL DEFAULT 'balanced', model_version_id BIGINT NULL, prompt_version_id BIGINT NULL, release_snapshot JSON NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING', create_by VARCHAR(64) NOT NULL, session_id BIGINT NOT NULL,
 error_message VARCHAR(500) NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finish_time DATETIME NULL, PRIMARY KEY(id), KEY idx_eval_batch(tenant_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_eval_result (
 id BIGINT NOT NULL, batch_id BIGINT NOT NULL, tenant_id VARCHAR(64) NOT NULL, case_id BIGINT NOT NULL,
 case_snapshot JSON NOT NULL, execution_context JSON NULL, run_id VARCHAR(64) NULL, conversation_id BIGINT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING', plan_correct TINYINT NULL, tool_correct TINYINT NULL,
 rag_hit TINYINT NULL, cited_count INT NULL, correct_cited_count INT NULL, refused TINYINT NULL,
 refusal_correct TINYINT NULL, costs_json JSON NULL, unknown_cost_calls INT NOT NULL DEFAULT 0,
 answer MEDIUMTEXT NULL, error_message VARCHAR(500) NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_eval_batch_case(batch_id,case_id), KEY idx_eval_pending(status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_eval_schedule (
 tenant_id VARCHAR(64) NOT NULL, dataset VARCHAR(128) NOT NULL, interval_hours INT NOT NULL,
 enabled TINYINT NOT NULL DEFAULT 1, user_id BIGINT NOT NULL, session_id BIGINT NOT NULL,
 next_run_at DATETIME NOT NULL, last_error VARCHAR(500) NULL,
 PRIMARY KEY(tenant_id,dataset), CHECK(interval_hours BETWEEN 1 AND 8760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
