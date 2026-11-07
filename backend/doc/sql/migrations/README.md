# 数据库迁移

`table_init.sql` 和 `data_init.sql` 仅用于全新 MySQL 数据卷的首次初始化，由
`docker-entrypoint-initdb.d` 执行。已有数据卷不会重新执行这两个文件。

本目录保存部署到已有环境的增量迁移，文件按 `VYYYYMMDD__description.sql`
命名并要求幂等。执行迁移前请备份数据库；迁移执行记录应由发布流程或
Flyway/Liquibase 等迁移工具维护，不要把迁移文件挂载到初始化目录。
