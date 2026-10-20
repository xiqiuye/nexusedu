# Changelog

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
