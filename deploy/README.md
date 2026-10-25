# NexusEdu 一键部署

本目录按仓库当前结构部署：后端是 `backend/nexusedu-server`（Spring Boot 3.5、Java 21、Maven），前端是 `frontend/nexusedu-web`（Vue 3、TypeScript、Vite、pnpm）。Compose 同时管理 MySQL 8.4、PostgreSQL/pgvector、Redis、后端和 Nginx 前端。

## 目录

```text
deploy/
├── docker-compose.yml
├── backend.Dockerfile
├── frontend.Dockerfile
├── nginx.conf
├── .env.example
├── deploy.sh
├── deploy.ps1
└── README.md
```

Compose 直接挂载仓库已有的初始化脚本：`backend/doc/sql/table_init.sql`、`system_data_init.sql`、`data_init.sql` 和 `postgres_knowledge_chunk.sql`。这些脚本只会在对应数据库卷第一次创建时执行。

## Windows 本地开发

要求 Docker Desktop（启用 Linux containers）和 Git。PowerShell 中执行：

```powershell
Copy-Item deploy/.env.example deploy/.env
notepad deploy/.env
Set-ExecutionPolicy -Scope Process Bypass
.\deploy\deploy.ps1 deploy
```

访问 `http://localhost`。常用命令：

```powershell
.\deploy\deploy.ps1 status
.\deploy\deploy.ps1 logs backend
.\deploy\deploy.ps1 down
```

前端源码本地热更新仍使用项目自身命令：

```powershell
cd frontend/nexusedu-web
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

## Ubuntu Linux 服务器

建议 Ubuntu 22.04/24.04、4 vCPU、8 GB RAM、50 GB 可用磁盘。安装 Docker Engine 和 Compose Plugin 后：

```bash
git clone <repository-url> /opt/nexusedu
cd /opt/nexusedu
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
editor deploy/.env
chmod +x deploy/deploy.sh
./deploy/deploy.sh deploy
```

开放防火墙时只暴露 `HTTP_PORT`（默认 80）；MySQL、PostgreSQL 和 Redis 没有发布到宿主机端口，只在 Compose 内网通信。生产环境建议在宿主机或云负载均衡器上配置 HTTPS，再把 80 重定向到 443。

## 环境变量

至少需要填写：

- `MYSQL_ROOT_PASSWORD`、`POSTGRES_PASSWORD`、`REDIS_PASSWORD`
- `INTERNAL_RPC_SECRET`、`JWT_SECRET`，使用不少于 32 字节的随机值
- `OPENAI_API_KEY`
- `EMBEDDING_ENDPOINT`、`EMBEDDING_TOKEN`

使用 OpenAI 兼容服务时，可把 `EMBEDDING_ENDPOINT` 和数据库中的模型 `baseUrl` 指向兼容服务。`VITE_API_URL=/` 保持前端通过 Nginx 同源访问后端，避免把内部后端地址编译进浏览器。

生成随机密钥示例：

```bash
openssl rand -base64 48
```

不要把 `deploy/.env`、访问令牌、模型密钥或数据库备份提交到 Git。

## 发布与回滚

每次 `deploy` 会用 UTC 时间生成版本号，构建并标记两张本地镜像，例如 `nexusedu/backend:20261025213000` 和 `nexusedu/frontend:20261025213000`。保留最近几个版本的镜像即可支持快速回滚：

```bash
./deploy/deploy.sh rollback 20261025213000
```

Windows：

```powershell
.\deploy\deploy.ps1 rollback 20261025213000
```

回滚只切换应用镜像，不回滚数据库卷。数据库结构变更必须采用向后兼容的迁移脚本，并在发布前备份；当前仓库的初始化 SQL 不是增量迁移工具。回滚前先停止流量或确认旧应用兼容当前数据库结构。

## 备份与恢复

MySQL：

```bash
docker compose --env-file deploy/.env --env-file deploy/.deploy.env -f deploy/docker-compose.yml exec -T mysql \
  mysqldump -uroot -p"$(grep '^MYSQL_ROOT_PASSWORD=' deploy/.env | cut -d= -f2-)" --single-transaction nexus-edu > backup-$(date -u +%Y%m%d%H%M%S).sql
```

PostgreSQL：

```bash
docker compose --env-file deploy/.env --env-file deploy/.deploy.env -f deploy/docker-compose.yml exec -T postgres \
  pg_dump -U nexusedu -d nexus_edu_knowledge > knowledge-$(date -u +%Y%m%d%H%M%S).sql
```

恢复前停止后端写入，确认备份文件权限和目标数据库，再执行对应的 `mysql` 或 `psql` 导入。Redis 使用 AOF 持久化，但它不是数据库备份的替代品。

## 安全基线

1. `.env` 权限设为 `600`，密钥通过密码管理器或服务器 Secret 机制分发。
2. 不发布数据库和 Redis 端口，不使用默认密码，不在日志中打印密钥。
3. 首次登录后立即修改初始化账号 `Admin / 123456` 的密码；该账号来自仓库的开发种子数据。
4. 生产使用 HTTPS、限制 SSH 来源、启用自动安全更新，并定期验证备份恢复。
5. 镜像构建使用非 root 用户运行后端；Compose 使用固定的大版本镜像标签，升级前先在预发布环境验证。
