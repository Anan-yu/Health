# RayK AI 服务

FastAPI 服务负责 PaddleOCR、指标标准化、十二模型规则评分、DeepSeek 综合解读和报告文字生成。它通过 HTTP JSON 与 Java 通信，不连接业务数据库。

## 评估原则

- 十二模型规则引擎是可追溯的主评分来源，版本为 `RULE_3.0.0`。
- 优先使用报告自带参考区间；只有单位匹配时才使用内置开发阈值。
- 指标不足时返回 `INSUFFICIENT_DATA`、完整度和缺失指标，不输出虚假的低风险分数。
- DeepSeek 只负责跨模型综合解读，不修改模型分数，也不能跳过医生审核。
- 发往 DeepSeek 的内容仅含年龄、性别、指标代码/值/单位/参考区间和规则结果，不含患者 ID、姓名、手机号、OpenID、任务 ID或原始报告。
- DeepSeek 被关闭、超时或响应校验失败时，自动返回 `RULE_FALLBACK` 规则摘要。

## DeepSeek 配置

在项目根目录的 `.env` 中配置，密钥不要提交 Git：

```dotenv
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_TIMEOUT_SECONDS=30
DEEPSEEK_MAX_TOKENS=2000
DEEPSEEK_THINKING=false
```

## OCR

Docker 开发环境默认启用 PaddleOCR CPU 模式，首次识别会下载 PP-OCRv6 小模型到独立的 `rayk_ocr_models_data` 命名卷，后续重建容器会复用模型。测试环境可将 `RAYK_OCR_MODE` 设置为 `mock`，只验证接口流程而不执行真实识别。

所有评估响应均包含：**该结果仅用于健康管理参考，不构成医学诊断。**
