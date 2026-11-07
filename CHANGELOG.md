# Changelog

## 2026-11-07
- 移除教师管理页面中的“科目设置”标签、科目列表和新增科目弹窗，教师管理现在只负责教师档案登记与查询。
- 学科查询和维护统一保留在教学组织下的“学科管理”页面，避免同一业务出现两个入口。

## 2026-11-07
- 修复教师登记接口因大整数用户 ID 在浏览器 JSON 解析时发生精度丢失而返回 `RESOURCE_NOT_FOUND` 的问题。
- 教师列表、候选账号、登记回显及班级任教关系查询统一将 `userId` 按字符串返回，前端提交时可保持数据库中的完整 ID。
- 新增用户 ID 字符串序列化回归测试；后端教师路由契约测试、前端 ESLint 和 `vue-tsc --noEmit` 均通过。


- 完善校区管理页面，沿用班级管理的页面布局，补充校区名称、编码和状态查询，以及新增、编辑、删除操作、状态展示和空数据提示。
- 校区页面接入独立路由组件与 `/api/school/campuses` 的新增、更新、删除接口，关联班级等业务数据时显示后端保护提示。
- 验证：目录路由、学科表单和菜单种子回归脚本，`vue-tsc --noEmit`，相关页面 ESLint，后端 `StudentSchoolControllerContractTest` 均通过。


- 修复旧数据库菜单直接加载 `/school/catalog` 时未传入页面类型，导致校区标题下调用学科接口的问题：兼容校区、学年和学科的既有路由名称，未知页面类型禁止发起请求，并补充校区编码校验。
- 新增校区、学年和学科菜单组件的幂等增量迁移；新增运行编译后组件 setup 的回归脚本，覆盖旧菜单、新页面以及列表、保存和重新加载接口选择。
- 验证：目录路由回归、学科表单回归、菜单种子校验、`vue-tsc --noEmit`、局部 ESLint 和 `git diff --check` 均通过；迁移 SQL 未执行到数据库。


- 修复新增学科只校验名称、空科目代码仍请求后端并返回 `subjectCode is required` 的问题：增加科目代码、名称长度和年级范围的前端校验，提交前去除首尾空格，并仅提交学科所需字段。
- 新增学科表单回归脚本，覆盖空代码、非法年级范围和有效请求内容；前端类型及局部 lint 检查通过。


- 按业务边界补充前后端职责拆分基础：校区、学年、学科页面改为独立路由包装页，学生接口补充学校目录、考试、成绩和升班的明确类型。
- 考试和成绩接口、校区和学年接口新增后端请求 DTO，控制器不再直接接收无约束的请求 Map。
- 新增 `backend/doc/sql/migrations/` 版本化迁移目录与教育菜单拆分迁移，明确与首次初始化 SQL 的执行边界。
- 验证：后端 `StudentSchoolControllerContractTest` 通过；前端 `vue-tsc --noEmit` 通过。


- 修复教学组织页面体验及任课关系入口：为校区、学年、学科和考试页面补足各自的页面识别与操作布局；将“班主任/任课关系”菜单从班级管理页面拆出，新增独立关系列表、筛选和分配表单，并同步更新菜单组件映射及种子契约。
- 验证：`node scripts/check-menu-seed.cjs`、`pnpm.cmd exec vue-tsc --noEmit`、相关页面 ESLint 与 `git diff --check` 通过。
- 修复教学菜单层级与页面错配：校区/学年/学科菜单接入组织目录页，学科归入教学组织；考试管理独立展示考试列表与新建表单，成绩录入保留成绩录入页；初始化 SQL 撤销并禁用旧“考试成绩”菜单，避免旧菜单继续显示。扩展菜单种子契约检查组件映射和学科父级。
- 验证：菜单种子契约检查、Vue TypeScript 检查及前端相关文件 lint。
- 修复动态路由中教师菜单仍保留旧父子层级导致的 `TeacherManagement` 同名嵌套冲突：菜单初始化 SQL 的重复键更新现同步刷新 `parent_id`、`route_name` 等路由字段，并扩展菜单种子校验确保重复执行后菜单层级和名称与前端路由一致。服务器需执行更新后的 `backend/doc/sql/data_init.sql` 菜单初始化语句后重新登录/刷新以重新获取菜单。
- 修复教育模块拆分后新增的 10 个 Java 文件带 UTF-8 BOM 导致 `AssessmentController.java` 报非法字符的问题；补齐五个 ServiceImpl 的业务接口导入，并将 API 路由契约测试放到与 package 匹配的目录、按各 Controller 实际 base path 校验。
- 验证：`backend/nexusedu-server/mvnw.cmd '-Dtest=StudentSchoolControllerContractTest' test` 通过，覆盖主源码完整编译和路由契约测试；确认 education Java 源文件均无 UTF-8 BOM。
- 按“学生管理、教学组织、教师管理、成绩管理”调整前端菜单路由，并将后端学校管理 API 按学生档案、教学组织、教师、升班和成绩拆分 Controller/Service 包；保留原 `/api/school` 路由并更新路由契约测试。
- 修复菜单种子校验脚本对 `ON DUPLICATE KEY UPDATE` 的解析，并按当前路由/角色授权结构校验菜单和按钮种子。
- 注意：当前各业务 ServiceImpl 仍继承包含多类业务方法的 `SchoolServiceSupport`；因 Maven 不可用且后端契约测试未能运行，本次未继续迁移这些方法，服务实现层的职责拆分仍需在可验证基线上完成。
- 验证：`node scripts/check-menu-seed.cjs` 通过（94 个菜单、62 项按钮权限）；`pnpm.cmd exec vue-tsc --noEmit` 通过；`git diff --check` 通过。后端 Maven 测试未运行（环境中无 `mvn` 命令）。
- 完成前端“学生管理”菜单职责分析：确认班级、教师、学年升班和考试成绩已作为学生管理子菜单存在；建议后续按学生管理、教学组织、教师管理、学籍变更和成绩管理拆分菜单，并将学生档案页内嵌的校区、学年、班级及教师分配设置迁移到对应业务域。本次仅记录分析，未修改业务代码。

## 2026-11-06
- 修复后端 Docker 健康检查仅接受 wget 返回码 8 的问题；Spring Security 保护根路径时返回 401、wget 返回码为 6，现兼容 200、401 和 404 响应，避免正常运行的后端被误判为 unhealthy。
- 补充 Linux 服务器前端镜像构建的资源保护建议：配置持久 Swap、限制 Node 构建堆内存，并使用后台日志运行部署脚本，降低 Vite 构建阶段因内存耗尽导致实例失联的风险。

## 2026-11-06
- 排查本地登录请求的 `CommonCode` `NoClassDefFoundError`：确认源码和编译类均存在，问题来自运行中的旧/不完整 classpath；记录通过停止旧进程后执行 Maven clean compile 并重启 IDE 的修复步骤。

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
