# Changelog

## 2026-11-04
- 为 MySQL、PostgreSQL、Redis 添加仅绑定服务器回环地址的可配置端口映射，补充本地 SSH 隧道连接和配置生效说明；移除已合并删除的 `system_data_init.sql` 挂载。
- 修复科目初始化 SQL 中 `CROSS JOIN` 后直接接 `ON DUPLICATE KEY UPDATE` 的解析歧义，添加 `WHERE TRUE`；通过 MySQL 临时表复现原错误并验证多租户初始化、重复执行和更新行为。
- 修复 Docker 前端构建使用 pnpm 9.15.5 安装依赖时因工作区缺少 `packages` 字段而失败的问题，显式声明当前前端目录为工作区包。

## 2026-11-03
- 后端学生档案与 AI 学生查询从旧 `student/profile` 包迁入 `education/student/profile`，学校管理适配与持久化代码迁入 `education`；保持 `/api/school` 路由不变，并加入路由契约测试。班级、组织、教师、考评的应用服务拆分仍待后续分域重构。

## 2026-10-26
- 修复后端 MyBatis 扫描范围过宽导致 `ModelAdapter` 被误注册为 Mapper、项目启动失败的问题。
- Consolidated system_data_init.sql into data_init.sql, retained its legacy RBAC links, and fixed invalid role_id references in student role initialization.
- 用Spring管理的任务和SSE线程池取代了非托管线程创建，包括可配置的大小、队列容量、线程名称和正常关闭。
- 删除了AiStreamEventPublisher接口，并将RedisStreamEventPublisher直接注入聊天工作流服务。
- 将AI评估、治理、模型、操作、任务和工具模块以及知识和系统业务模块从DDD包层迁移到传统的控制器/服务/dto/实体/映射器包，更新导入和MyBatis XML名称空间。

## 2026-10-22
- 删除 AI 聊天应用服务接口，控制器直接注入保留的 `AiChatCommandService` 实现类。
- 将 AI 聊天持久化中的会话历史更新改为 MyBatis-Plus 条件构造器，并将聊天 Mapper 注解 SQL 迁移到 XML。
- 将 AI 聊天请求 DTO 重命名为 `AiChatRequest`，并将原内部 `ChatRequest` 字段移至顶层。

## 2026-10-21
- 新增面向小学的多校区、学年、年级班级、学生学年学籍记录与校内角色管理，支持名册分页筛选、建档、调班、状态流转和逐行反馈的批量导入。
- 新增学生管理前后端页面、Excel 导入模板与当前页导出；将教师和班主任的名册及 AI 查询限制到已分配任教班级，并按校区管理员关系过滤数据。

## 2026-10-20
- 重构后端包结构，统一按“业务域 → 能力 → DDD 分层”组织 AI、知识、系统、学生及共享内核代码。
- 更新 MyBatis Mapper 扫描配置，使其遵循新的业务域包根路径。
- 新增共享授权策略端口，并将系统 RBAC 强制逻辑移至系统域适配器中，解除 AI 和知识运行时对系统管理服务的依赖。
- 将系统管理资源分页及基础持久化操作提取到 `SystemManagementStore`；同时将知识文档上传、解析和分块处理与知识目录管理持久化逻辑分离。
- 通过领域 MyBatis XML Mapper 处理系统和知识管理资源分页，以及通用的插入、更新和所有者行持久化；资源与表的选择继续限制在显式资源映射范围内。
- 将知识向量 PostgreSQL 配置统一到 `nexusedu.knowledge.postgres` 下，并移除重复的 AI 摄取 PostgreSQL 配置块；删除未使用的 `nexusedu.web.jwt` 配置块，保留 `nexusedu.system.jwt` 作为唯一 JWT 配置来源。

## 2026-10-19
- Matched the /docs/zh navigation width to the same 80% responsive container as the page content.

## 2026-10-16
- 恢复后端 Java 包结构，统一为 DDD 分层优先、业务上下文其次。
- 曾按业务模块合并后端目录并调整 Java 包引用与 Mapper 扫描配置。

## 2026-10-18
- 将 AI 领域模型迁移到 `common/model/ai`，将 AI 常量迁移到 `common/constant/ai`，并同步后端包引用。

## 2026-10-19
- 按业务上下文优先重组后端包：新增 `ai`、`knowledge`、`student`、`system`、`shared` 顶层业务目录，并同步 Java 包引用与 MyBatis Mapper 扫描路径。
- 修复 AI 观测类包声明与目录不一致导致的重复 Bean：统一 `AiObservationService`、`AiMetricsServiceImpl`、`AiOperationsServiceImpl` 到 `ai.infrastructure.observability.observation`，并清理旧编译产物。
- 开发项目介绍长页面与管理后台共存：根路径 / 用于公开介绍页，/dashboard 保留为登录后的管理后台；开发端口使用 3006，生产环境可由 80 端口的反向代理承载，评估 Hash/History 路由部署要求。
