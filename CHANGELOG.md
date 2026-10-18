# Changelog

## 2026-10-16
- 恢复后端 Java 包结构，统一为 DDD 分层优先、业务上下文其次。
- 曾按业务模块合并后端目录并调整 Java 包引用与 Mapper 扫描配置。

## 2026-10-18
- 将 AI 领域模型迁移到 `common/model/ai`，将 AI 常量迁移到 `common/constant/ai`，并同步后端包引用。