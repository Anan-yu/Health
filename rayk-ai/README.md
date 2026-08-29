# 三羊健康 AI 服务

FastAPI 服务负责 PaddleOCR、指标标准化、十二模型规则评分、多模态体检图片综合解读、DeepSeek 综合解读和报告文字生成。它通过 HTTP JSON 与 Java 通信，不连接业务数据库。

## 评估原则

- 十二模型规则引擎是可追溯的主评分来源，版本为 `RULE_3.0.0`。
- 优先使用报告自带参考区间；只有单位匹配时才使用内置开发阈值。
- 指标不足时返回 `INSUFFICIENT_DATA`、完整度和缺失指标，不输出虚假的低风险分数。
- DeepSeek 只负责跨模型综合解读，不修改模型分数，也不能跳过医生审核。
- 配置 Qwen Vision 后，图片体检报告走混合两阶段模式：先由 `qwen3.8-flash` 按批次直接读取原始报告图片并形成逐页结构化事实，再由 DeepSeek 将该结果与健康档案、健康拍结果和 RAG 证据汇总为现有综合报告 JSON；图片页是体检事实的第一来源，OCR 结构化结果只作低可信线索。
- 未配置 Qwen Vision 时，仍沿用文字/PDF 结构化结果的 DeepSeek 链路；PDF 原生解析路径不与图片直读路径合并。
- DeepSeek 被关闭、超时或响应校验失败时，自动返回 `RULE_FALLBACK` 规则摘要。
- 健康助手独立使用 Qwen `qwen3.8-flash`，不复用报告综合解读的 DeepSeek 模型配置；对话支持 SSE 流式输出，Qwen 未配置或流式失败时由 Java 保留安全降级和一次性接口兼容路径。
- 综合解读会先保留安全边界，再逐项校验可选 `diagnosticReferences`；单个疾病候选证据不足时只移除该候选，保留 DeepSeek 的摘要、重点发现、异常解释和建议。规则降级解释按指标类型生成具体复查动作，不再用同一段泛化描述覆盖所有异常。
- DeepSeek 请求支持 `thinkingEnabled` 覆盖运行时思考模式；未传入时使用 `DEEPSEEK_THINKING_ENABLED` 环境默认值。思考模式通常会增加推理 token 和响应时间。

## Qwen 多模态体检报告配置

图片报告直读默认关闭。启用后，Java 只为已存储的图片生成 10 分钟内有效的 MinIO 签名地址，AI 服务不接收 PDF 文件；签名地址不应写入日志。

```dotenv
QWEN_VISION_ENABLED=true
QWEN_VISION_API_KEY=
QWEN_VISION_WORKSPACE_ID=
QWEN_VISION_BASE_URL=
QWEN_VISION_MODEL=qwen3.8-flash
QWEN_VISION_TIMEOUT_SECONDS=120
QWEN_VISION_MAX_TOKENS=16000
QWEN_VISION_MAX_IMAGES=50
QWEN_VISION_BATCH_SIZE=4
QWEN_VISION_BATCH_ATTEMPTS=2
QWEN_VISION_IMAGE_ANALYSIS_MAX_TOKENS=8000
```

## PDF OCR 配置

PDF 与图片直读是两条独立路径。电子 PDF 仍优先使用原生文本/表格解析来保留原分类、顺序和内容；启用 Qwen OCR 后，仅将 PDF 渲染页交给 `qwen3.8-flash` 做视觉补充，扫描或图片型 PDF 再依赖该结果补齐文字所见，最后由本地解析和质量校验负责合并与兜底。

```dotenv
QWEN_OCR_ENABLED=true
QWEN_OCR_API_KEY=
QWEN_OCR_WORKSPACE_ID=
QWEN_OCR_BASE_URL=
QWEN_OCR_MODEL=qwen3.8-flash
QWEN_OCR_FALLBACK_MODEL=qwen3.5-ocr
QWEN_OCR_TIMEOUT_SECONDS=180
QWEN_OCR_MAX_PIXELS=16000000
QWEN_OCR_MAX_PAGES=50
QWEN_OCR_CONCURRENCY=3
```

PDF 云 OCR 按页面级联：先调用 `QWEN_OCR_MODEL`，页面请求失败或没有提取出可用检验内容时，再调用 `QWEN_OCR_FALLBACK_MODEL`；备用模型也失败后才进入本地 PDF/PaddleOCR 降级。`QWEN_OCR_MODEL` 和 `QWEN_VISION_MODEL` 当前默认均为 `qwen3.8-flash`，图片直读不会使用 PDF 的备用模型。PDF 原生文本、表格和检查小结解析始终保留为校验基线；正式效果仍需用真实电子 PDF、扫描 PDF、单栏图片和双栏图片分别回归。

## 健康助手配置

助手与报告生成使用独立模型配置，当前固定使用 `qwen3.8-flash`。密钥只放在项目根目录 `.env` 或部署平台密钥管理中，不要提交 Git。助手没有联网天气/搜索工具；纯时间问题由服务端 `Asia/Shanghai` 时钟直接回答，并向模型上下文标注当前服务端时间，避免模型凭记忆猜测日期。

```dotenv
QWEN_ASSISTANT_ENABLED=true
QWEN_ASSISTANT_API_KEY=
QWEN_ASSISTANT_WORKSPACE_ID=
QWEN_ASSISTANT_BASE_URL=
QWEN_ASSISTANT_MODEL=qwen3.8-flash
QWEN_ASSISTANT_TIMEOUT_SECONDS=120
QWEN_ASSISTANT_MAX_TOKENS=6000
```

如果未填写助手专用的密钥、工作空间或地址，服务会兼容读取已配置的 `QWEN_VISION_*`，再读取 `QWEN_OCR_*`；报告综合解读仍只读取 `DEEPSEEK_*`，两条链路互不切换。

## DeepSeek 配置

在项目根目录的 `.env` 中配置，密钥不要提交 Git：

```dotenv
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_TIMEOUT_SECONDS=60
DEEPSEEK_MAX_TOKENS=16000
DEEPSEEK_MAX_ATTEMPTS=3
DEEPSEEK_RETRY_BACKOFF_SECONDS=1
DEEPSEEK_THINKING=false
```

## OCR

Docker 开发环境默认启用 PaddleOCR CPU 模式，首次识别会下载 PP-OCRv6 小模型到独立的 `rayk_ocr_models_data` 命名卷，后续重建容器会复用模型。测试环境可将 `RAYK_OCR_MODE` 设置为 `mock`，只验证接口流程而不执行真实识别。

所有评估响应均包含：**该结果仅用于健康管理参考，不构成医学诊断。**

## 三羊健康医疗垂直评估引擎

当前垂直引擎版本为 `ZHIYU_HEALTH_VERTICAL_2.8.0`，采用“确定性规则 + 医学知识检索
+ 大模型综合解读 + 输出安全复核”的组合架构：

1. 规则引擎使用检验机构参考区间完成十二个健康维度的可解释评分。
2. 临床上下文构建器将检验指标、健康档案与问卷整理为去标识化健康时间线，并确定性计算
   BMI、参考区间异常数、有效维度和数据缺口。
3. 医学知识检索器按指标、健康维度、档案字段和随访反馈，从版本化知识库
   `ZHIYU_MEDICAL_KB_2.2.0` 中选择本次所需知识；当前采用结构化命中、中文关键词和字符向量相似度混合检索。新增幽门螺杆菌、血脂异常和脂肪性肝病的中医药物参考证据，只有命中对应证据时才展示代表性方药/中成药方向；历史报告中的旧占位语会在报告生成和详情展示层按匹配证据兼容处理。
4. 图片模式先由 Qwen Vision 基于原始报告图片生成逐页结构化事实，再由 DeepSeek 基于这些事实、健康时间线、规则结果和检索知识生成综合解读；文字/PDF 模式直接由 DeepSeek 生成综合解读。两者都不得自行补充患者事实、检验阈值、确诊结论或药物治疗方案。
   Python ReportLab 按 A4 打印版式生成最终 PDF，使用 20pt 用户报告标题、15pt 一级标题、13pt 二级标题和 12pt 正文，并对异常结果块执行整体分页，避免关键解释被拆到页间。
5. 输出经过指标引用、诊断证据、重复候选、确诊措辞和用药剂量安全校验；校验失败自动降级为
   `RULE_FALLBACK`，不会把不合规内容交付给业务端。

知识库条目必须记录来源、版本和适用范围。新增或更新医学知识时，应由具备相应资质的专业人员
复核，并通过回归测试后再发布；不得直接用未经审核的互联网文本替换生产知识库。
