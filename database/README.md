# 数据库

Flyway 在 Spring Boot 启动时按顺序执行 `migrations/` 中的迁移。`V2` 只包含虚构的开发测试数据，测试密码使用 BCrypt 摘要保存。

核心健康数据采用逻辑删除；报告文件本体由私有 MinIO Bucket 保存，MySQL 只保留元数据。

