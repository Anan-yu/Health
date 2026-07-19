# API 设计

业务接口统一前缀 `/api/v1`，响应统一为 `code/message/data/requestId/timestamp`。核心资源包括认证、工作台、客户、检验报告、指标、AI 评估、医生审核、健康报告与随访。

报告上传成功后，Java 创建 OCR 任务并异步调用 Python 的 `/api/v1/ocr/recognize`。小程序通过 `GET /api/v1/lab-reports/{id}/ocr-task` 查询进度，通过 `POST /api/v1/lab-reports/{id}/ocr-task/retry` 重试失败任务；识别结果必须经过 `PUT /api/v1/lab-reports/{id}/indicators` 校对保存和 `POST /api/v1/lab-reports/{id}/confirm` 人工确认。

Java 调用 Python 时透传 `X-Request-Id`，设置连接/读取超时且不无限重试。详细契约可在启动后的 Swagger 与 FastAPI Docs 查看。
