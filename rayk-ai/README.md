# 致宇健康 AI 服务

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

## 致宇健康医疗垂直评估引擎

当前垂直引擎版本为 `ZHIYU_HEALTH_VERTICAL_1.0.0`，采用“确定性规则 + 医学知识检索
+ 大模型综合解读 + 输出安全复核”的组合架构：

1. 规则引擎使用检验机构参考区间完成十二个健康维度的可解释评分。
2. 临床上下文构建器将检验指标、健康档案与问卷整理为去标识化健康时间线，并确定性计算
   BMI、参考区间异常数、有效维度和数据缺口。
3. 医学知识检索器按指标、健康维度、档案字段和随访反馈，从版本化知识库
   `ZHIYU_MEDICAL_KB_2.1.0` 中选择本次所需知识；当前采用结构化命中、中文关键词和字符向量相似度混合检索。
4. DeepSeek 只能基于健康时间线、规则结果和检索知识生成综合解读，不得自行补充患者事实、
   检验阈值、确诊结论或药物治疗方案。
5. 输出经过指标引用、诊断证据、重复候选、确诊措辞和用药剂量安全校验；校验失败自动降级为
   `RULE_FALLBACK`，不会把不合规内容交付给业务端。

知识库条目必须记录来源、版本和适用范围。新增或更新医学知识时，应由具备相应资质的专业人员
复核，并通过回归测试后再发布；不得直接用未经审核的互联网文本替换生产知识库。
