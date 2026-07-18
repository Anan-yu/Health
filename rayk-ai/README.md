# RayK AI 服务

FastAPI 服务只负责模拟 OCR、指标标准化、演示评分与报告文字生成。它通过 HTTP JSON 与 Java 通信，不连接业务数据库，不调用真实大模型，也不下载 PaddleOCR 模型。

```bash
pip install -r requirements-dev.txt
pytest
uvicorn app.main:app --reload --port 8000
```

所有评估响应均包含：**该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。**

