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

Compose 直接挂载仓库已有的初始化脚本：`backend/doc/sql/table_init.sql`、`data_init.sql` 和 `postgres_knowledge_chunk.sql`。这些脚本只会在对应数据库卷第一次创建时执行。

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

开放防火墙时只暴露 `HTTP_PORT`（默认 80）；MySQL、PostgreSQL 和 Redis 端口仅绑定宿主机 `127.0.0.1`，本地客户端通过 SSH 隧道连接。生产环境建议在宿主机或云负载均衡器上配置 HTTPS，再把 80 重定向到 443。

## 从本地连接服务器数据库

更新配置后，在服务器项目根目录执行以下命令应用端口映射，无需重建镜像。容器重建时会短暂中断数据库连接，保留已有数据卷。

```bash
docker compose --env-file deploy/.env --env-file deploy/.deploy.env -f deploy/docker-compose.yml up -d mysql postgres redis
```

默认服务器回环端口为 MySQL `3306`、PostgreSQL `5432`、Redis `6379`。若端口已被占用，在服务器 `deploy/.env` 中设置 `MYSQL_PORT`、`POSTGRES_PORT`、`REDIS_PORT`，再执行上述命令。容器内服务端口不受这些映射影响。

在本地电脑终端执行（替换服务器 IP 和 SSH 用户）：

```bash
ssh -N -o ExitOnForwardFailure=yes -L 127.0.0.1:13306:127.0.0.1:3306 -L 127.0.0.1:15432:127.0.0.1:5432 -L 127.0.0.1:16379:127.0.0.1:6379 root@服务器IP
```

保持终端打开，无输出通常表示隧道已建立。自定义 SSH 端口可添加 `-p 端口号`；若修改了服务器映射端口，也需同步修改各 `-L` 参数最后的端口。本地使用不同端口，避免与本机数据库冲突。

| 服务 | 本地客户端地址 | 数据库/用户 | 密码 |
| --- | --- | --- | --- |
| MySQL | `127.0.0.1:13306` | `nexus-edu` / `root` | `MYSQL_ROOT_PASSWORD` |
| PostgreSQL | `127.0.0.1:15432` | `nexus_edu_knowledge` / `POSTGRES_USER`（默认 `nexusedu`） | `POSTGRES_PASSWORD` |
| Redis | `127.0.0.1:16379` | 默认数据库 `0` / 默认用户 | `REDIS_PASSWORD` |

客户端选择普通 TCP 连接即可，SSH 已由上述命令建立。也可以使用客户端自带的 SSH 隧道，此时数据库地址填写服务器的 `127.0.0.1` 和对应服务器映射端口。数据库密码使用服务器实际配置；已有数据库的密码不会因修改 `.env` 自动改变。

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
2. 数据库和 Redis 端口仅绑定服务器回环地址，通过 SSH 隧道访问，不使用默认密码，不在日志中打印密钥。
3. 首次登录后立即修改初始化账号 `Admin / 123456` 的密码；该账号来自仓库的开发种子数据。
4. 生产使用 HTTPS、限制 SSH 来源、启用自动安全更新，并定期验证备份恢复。
5. 镜像构建使用非 root 用户运行后端；Compose 使用固定的大版本镜像标签，升级前先在预发布环境验证。
