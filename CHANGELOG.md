# Changelog

## 2026-10-16
- 恢复后端 Java 包结构，统一为 DDD 分层优先、业务上下文其次。
- 曾按业务模块合并后端目录并调整 Java 包引用与 Mapper 扫描配置。

## 2026-10-18
- 将 AI 领域模型迁移到 `common/model/ai`，将 AI 常量迁移到 `common/constant/ai`，并同步后端包引用。

## 2026-10-19
- 按业务上下文优先重组后端包：新增 `ai`、`knowledge`、`student`、`system`、`shared` 顶层业务目录，并同步 Java 包引用与 MyBatis Mapper 扫描路径。
- 修复 AI 观测类包声明与目录不一致导致的重复 Bean：统一 `AiObservationService`、`AiMetricsServiceImpl`、`AiOperationsServiceImpl` 到 `ai.infrastructure.observability.observation`，并清理旧编译产物。
- 开发项目介绍长页面与管理后台共存：根路径 / 用于公开介绍页，/dashboard 保留为登录后的管理后台；开发端口使用 3006，生产环境可由 80 端口的反向代理承载，评估 Hash/History 路由部署要求。
