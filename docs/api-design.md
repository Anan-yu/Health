# API 设计

业务接口统一前缀 `/api/v1`，响应统一为 `code/message/data/requestId/timestamp`。核心资源包括认证、工作台、客户、检验报告、指标、AI 评估、医生审核、健康报告与随访。

Java 调用 Python 的 `/api/v1/assessments/evaluate`，透传 `X-Request-Id`，设置连接/读取超时且不无限重试。详细契约可在启动后的 Swagger 与 FastAPI Docs 查看。

