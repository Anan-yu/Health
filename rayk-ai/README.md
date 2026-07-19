# RayK AI 服务

FastAPI 服务负责 PaddleOCR、指标标准化、演示评分与报告文字生成。它通过 HTTP JSON 与 Java 通信，不连接业务数据库，也不调用真实大模型。

Docker 开发环境默认启用 PaddleOCR CPU 模式，首次识别会下载 PP-OCRv6 小模型到独立的 `rayk_ocr_models_data` 命名卷；后续重建容器会复用模型。可在测试环境将 `RAYK_OCR_MODE` 设置为 `mock`，只验证接口流程而不执行真实识别。

```bash
pip install -r requirements-dev.txt
pytest
uvicorn app.main:app --reload --port 8000
```

所有评估响应均包含：**该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。**
