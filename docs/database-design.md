# 数据库设计

MySQL 8.4、InnoDB、utf8mb4，Flyway 管理结构和开发数据。主键采用 BIGINT 并由业务层生成；健康指标逐行存入 `indicator_value`，不塞入单个 JSON。JSON 仅保存 OCR、规则和 AI 结果快照。

所有核心表有 `tenant_id`、审计时间、逻辑删除和版本字段。报告文件存入 MinIO 私有 Bucket，MySQL 只记录对象路径、MIME、大小与哈希。

