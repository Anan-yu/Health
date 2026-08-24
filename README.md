# 三羊健康

三羊健康是一套面向体检后健康管理的智能化平台。系统把分散在体检报告、个人档案、健康问卷、日常体征和随访反馈中的信息连接起来，通过结构化 OCR、医学知识检索、AI 综合评估和持续健康随访，形成从“一次体检”到“长期健康行动”的数字化闭环。

平台不是简单的报告阅读器，而是一套可持续演进的健康智能基础设施：它保留原始医学证据，建立可追踪的个人健康画像，将复杂指标转化为清晰、温暖、可执行的行动建议，并为医生提供 AI 健康报告和趋势参考。

> 系统用于健康管理与辅助参考，不替代临床诊断、医生面诊或医疗设备测量。

## 产品角色

系统当前只包含三个角色：

| 角色 | 主要能力 |
| --- | --- |
| 平台管理员 | 管理合作医院和医生预录入；查看全平台健康报告、原体检报告及随访动态；回复问题反馈 |
| 医生 | 按姓名或手机号筛选体检者；查看档案、体检报告、AI 评估、健康报告和随访；下载 PDF 报告 |
| 普通客户 | 维护个人健康资料；上传报告；查看评估、报告和趋势；进行健康检测；完成随访和提醒设置 |

当前试运行阶段医生为全平台体检者只读；正式上线前可按业务要求调整为医院隔离。普通客户始终只能访问本人数据。

## 核心业务闭环

```text
微信身份进入系统（企业主体通过微信授权手机号匹配客户、预录入医生和平台管理员）。本地开发包的微信手机号登录也走真实微信校验；H5 三角色调试请使用开发调试入口，不要把固定 Mock 手机号当作真实身份测试。
  → 完善健康档案与问卷
  → 上传 PDF 或图片体检报告
  → OCR 保留原分类、原顺序和原内容
  → 指标标准化与 12 维健康评估
→ RAG 医学知识检索 + Qwen3.7-Flash-2026-07-15 图片直读/后台选定的 DeepSeek 模型综合解读
  → 生成 AI 健康报告和 PDF
  → AI 制定健康随访计划
  → 客户逐项反馈完成情况、感受与困难
  → 自动继续、调整或终止下一期任务
  → 趋势、健康检测和再次评估
  → 健康助手结合本人资料进行健康问答
```

## 已有功能

### 体检报告数字化

- 上传 PDF、JPG、PNG，文件保存到 MinIO 私有存储。
- 检验报告支持同一份报告连续添加多张图片；每张图片会作为独立页面保存并合并进入同一份 OCR 结果，选择器达到平台单次上限后可继续点击“添加”上传，不设应用层张数上限。PDF 和单文件上传流程保持不变。
- PDF 原生表格/文本解析与扫描 PDF 回退。
- 图片单栏、双栏识别及左右分栏合并去重。
- 保留数值结果、非数值结果、参考范围、单位、异常标识和检查小结。
- 过滤姓名、电话、门诊号、床位号、打印日期等非体检结果。
- 异步 OCR、状态查询、失败恢复、原报告查看。
- 图片和 PDF 识别详情页会实时轮询服务端处理状态，展示阶段性进度百分比和当前处理阶段；百分比是按识别、整理、评估和报告生成阶段推进的可观测进度，不代表模型内部 token 级进度。
- OCR 成功而后续 AI 评估失败时，报告会明确显示“评估未完成”并保留全部识别内容；评估入口保留在检验报告详情页，不在已发布健康报告详情页重复提供无效的 AI 重生成按钮。
- 健康报告详情页的“查看原检验报告”会按当前评估关联的检验报告编号跳转到对应检验报告详情页，便于查看完整分类结果和原始报告入口。
- 图片报告即使 OCR 质量不足或 OCR 服务异常，只要原始图片已保存，详情页也会提供“直接用图片生成健康报告”；该操作由 `qwen3.7-flash-2026-07-15` 重新阅读全部图片，再由平台管理员当前选定的 DeepSeek 模型结合图片事实、健康档案、健康拍和 RAG 生成报告。

### 智能健康评估

- 结合体检结果、健康档案、问卷、既往史、家族史、生活方式综合描述、过敏史、当前用药和最近一次成功的面部健康检测体征。
- 体检报告按原分类向评估提供结构化指标、检查所见和检查小结，不直接使用未经清洗的整份原始 OCR 文本。
- 图片体检报告启用 Qwen Vision 后，Java 为已存储图片生成短时签名地址，Python 先按批次将原始图片交给 `qwen3.7-flash-2026-07-15` 直读为逐页结构化事实，再把图片分析结果、健康档案、健康拍结果和 RAG 证据交给平台管理员当前选定的 DeepSeek 模型生成现有综合报告格式；图片内容优先，OCR 结构化结果只作低可信线索。PDF 仍保持独立解析路径，不进入图片直读链路。
- PDF 启用云 OCR 时按页级联：先用 `qwen3.7-flash-2026-07-15`，只有页面请求失败或未提取出可用检验内容时才用 `qwen3.5-ocr` 重试该页；两者仍失败才整体降级到 PDF 原生解析与 PaddleOCR。电子 PDF 的原生文本、表格和检查小结继续作为校验基线，不因模型切换被覆盖。
- 图片直读事实会同步投影到既有健康维度规则、RAG 上下文和 PDF 报告；即使 DeepSeek 汇总超时或校验失败，规则降级也会基于已保存的图片事实生成可追溯内容，不会把图片报告误判为 0 项数据。
- 健康报告详情页的“下载 PDF 健康报告”在 H5 和微信端均通过鉴权接口下载；微信端优先按当前用户名保存为“XXX健康报告.pdf”后打开，基础库不支持自定义路径时回退到临时文件，下载或打开失败会明确提示。
- 综合解读校验按“整份报告安全边界”和“单个可选诊断候选”分开处理：证据不足的疾病候选会被单独移除，不再拖垮 DeepSeek 已生成的摘要、异常解释和建议；只有确诊、剂量和自行调药等硬边界仍会触发整体降级。规则降级文案也会按具体指标给出对应的可能影响和复查动作，避免重复泛化话术。
- 12 个健康维度规则评估与健康指数仪表盘。
- 已确认的异常指标按原报告参考范围形成可追溯事实，即使完整健康维度数据不足也不会遗漏单项异常。
- 医学知识库 RAG 聚焦异常事实、重点关注维度和原报告检查小结，并由 DeepSeek 生成综合解读。
- DeepSeek 综合解读默认显式关闭思考模式，超时时间为 60 秒、输出上限为 32K，最多尝试 3 次并对网络超时、429 和 5xx 递增退避重试；医疗安全校验保留确诊、处方剂量和自行调药等硬边界，允许带限定语的风险说明，失败时继续使用可追溯的规则降级结果。
- 平台管理员工作台提供“AI模型管理”卡片，可在 `deepseek-v4-flash`（DeepSeek-V4-Flash-0731）与 `deepseek-v4-pro`（DeepSeek-V4-Pro-0813）之间切换。选择会持久化到数据库，并从下一次新生成的 AI 综合评估开始生效；图片模式也遵循该运行时选择，不会被 `DEEPSEEK_MODEL` 环境默认值覆盖；API 凭据仍只从 `.env` 读取。
- 每次健康评估都会在 Java 发起请求日志和 Python 最终生成日志中记录实际模型代码与图片数量，不记录密钥或健康原文。验证 15 张图片评估时应看到 `model=deepseek-v4-pro imageCount=15` 和 `visionMode=True`（其他图片数量同理）；验证 PDF 评估时应看到 `imageCount=0`、`visionMode=False`，并在评估结果快照的 `interpretation.model` 中核对实际模型。
- AI 模型管理页提供思考模式开关，默认关闭；开启会增加推理 token、响应时间和潜在费用，设置持久化后从下一次新的评估或随访请求生效，历史结果不变。
- 展示整体健康状态、重点发现、对应建议、待补充数据和当前结论边界；只有证据条件充分时才展示需要进一步确认的健康方向。
- 报告在重点发现之后提供逐项异常结果解释，分别说明异常含义、可能涉及的器官或系统及下一步建议；每项解释绑定结构化异常事实和医学知识证据，并保留健康管理参考边界。
- 疾病推断参考逐项提供西医治疗思路、西医药物治疗参考、中医治疗思路和中医药物/治法参考。版本化知识库 `ZHIYU_MEDICAL_KB_2.2.0` 命中对应证据时，可列出半夏泻心汤类方、血脂康胶囊或化滞柔肝颗粒等代表性讨论项；这些内容仅用于辨证就诊沟通和复查参考，不输出剂量、疗程、处方或自行购药、停药、调药建议。
- 对历史报告或模型返回旧占位语的情况，报告服务和健康报告详情页会按疾病方向补充上述证据支持的参考项；没有匹配知识证据的疾病仍保留“证据不足”提示，不强行编造具体方药。
- 大模型未完成时会明确标记为保守规则结果，已发布健康报告仍可正常查看和下载；仅对首次评估失败或尚未完成的报告提供继续生成入口，已发布报告不再支持 AI 重新解读。
- 健康拍摄像头估算明确标记为补充趋势证据，不能替代医疗设备测量或单独用于疾病判断。
- 同一份报告供客户与医生查看，支持 PDF 生成、版本保存和下载；H5 下载通过鉴权文件接口生成带文件名的本地下载，不直连 MinIO。
- PDF 报告按 A4 打印版式排版：用户报告标题 20pt、一级标题 15pt、二级标题 13pt、正文 12pt，并使用适合打印的行距、页边距和分页规则。

### 自适应健康随访

- 根据健康报告生成饮食、运动、作息、监测和营养行动。
- 提供微量营养建议和一周营养食谱。
- 客户逐项选择完成、部分完成或未完成。
- 结合完成率、文字反馈、身体感受和困难原因调整下一期计划。
- 支持继续、调整、终止、逾期和任务趋势查看。

### 健康检测与温暖提醒

- 健康拍面部健康检测接入层和结果界面。
- 心率、血压、血氧、呼吸、心率变异性、压力参考等指标展示。
- 健康指数、指标范围、异常状态、指标解读与趋势。
- 腾讯云 TTS 吃饭和睡觉提醒，支持动态文案和性别音色策略。
- 平台问题反馈、状态追踪和管理员回复。

### 健康助手

- 客户入口为小程序 `pages-customer/medical-assistant/index`，也可从“我的”和客户快捷菜单进入。
- 当前使用 `qwen3.7-flash-2026-07-15` 进行文字问答；模型名称不展示在客户端，也不影响平台管理员对健康评估/报告模型的切换。
- 对话通过 SSE 分块返回，助手回复会逐步显示；流式服务异常时保留一次性接口作为兼容降级。
- 助手 JSON 与 SSE 响应统一显式使用 UTF-8；客户端同时兼容修复历史数据或旧链路产生的 UTF-8/Latin-1 乱码，避免中文动态消息出现乱码。
- 对话历史按独立会话保存，首屏内容区的“对话记录”入口可展开/收起历史侧栏；入口避开微信右上角原生操作区。新对话欢迎卡片中的提示点击后会直接发送给大模型，不会填入输入框；回答后的追问提示仅作参考展示，不会自动发送或进入输入框。可新建对话，也可删除本人不再需要的历史对话。
- 助手头像使用压缩后的羊头像资源，位于 `rayk-miniapp/src/pages-customer/static/assistant`；聊天输入框与发送按钮保持同高，适配移动端操作。
- 页面按中老年用户的可读性优化：提高导航、正文、快捷问题、输入框和操作按钮字号与行距；首屏绿色介绍卡片采用右侧头像视觉锚点的紧凑排版，使用“健康报告”上下文标签和更短的说明文案减少换行与无效留白；第一圈半圆环已调整为以头像中心为圆心，头像保持在卡片内部，避免贴边或超出环形装饰；输入区不再显示占位提示和底部小字，历史侧栏的删除操作改为更易点击的按钮。
- 助手消息会将模型返回的 `**标题**` 转换为实际加粗文本并移除星号，流式增量内容也按同一规则渲染；对话接口、会话存储和后端安全边界不变。
- 输入框已关闭微信小程序原生确认栏，并将键盘确认动作设为发送；若 iOS/微信系统仍显示输入法自带的“完成”工具条，该部分属于系统控件，应用只能控制小程序确认栏，不能通过页面样式改写。
- “现在几点/当前时间”等纯时间问题由 AI 服务按服务端北京时间（`Asia/Shanghai`）直接回答，不再交给模型猜测；助手当前没有联网、定位或天气工具，天气问题会明确提示能力边界，不会编造实时天气。接入天气服务仍需单独配置供应商、城市来源和用户授权。
- 每次回答会在后端按本人数据范围组装健康档案、最近一次健康评估、最近一次健康拍体征和本次对话上下文；不会接受前端传入的任意 patientId 作为授权依据。
- 新增 `AI_MEDICAL_ASSISTANT` 会员权益：免费客户 3 次对话，年度会员期内不限次数；调用前预占额度，成功落库后确认，AI 失败会释放额度。免费额度用尽时，客户端会提示开通年度会员并可直接跳转开通页。
- 对胸痛、明显呼吸困难、意识不清、突发单侧无力、大出血、严重过敏和自伤风险等描述先触发急症安全提示；后台保留依据、风险级别和下一步等结构化信息，聊天界面不再展示“本次参考”标签，仍保留健康管理免责声明，不输出诊断、处方或自行调药建议。
- 首版为文字对话，不接收图片/文件，也不替代体检报告的 OCR、图片直读、健康评估和 PDF 报告生成链路。

## 技术架构

```text
UniApp 小程序 / H5
        │
      Nginx
        │
  Java Spring Boot ───── MySQL
        │               Redis
        │               MinIO
        │
  Python FastAPI AI 服务
        ├─ PDF 原生解析 / PaddleOCR
├─ Qwen3.7-flash-2026-07-15 → Qwen3.5-OCR PDF 页级联 / 图片直读
        ├─ DeepSeek + RAG
        ├─ 健康助手（Qwen3.7 Flash）
        └─ 报告与随访智能处理
```

| 层级 | 技术 |
| --- | --- |
| 客户端 | UniApp、Vue 3、TypeScript |
| 业务后端 | Java、Spring Boot、MyBatis、Flyway |
| AI 服务 | Python、FastAPI、PaddleOCR、PDF 结构解析 |
| 外部 AI | Qwen3.7-flash-2026-07-15、Qwen3.5-OCR、DeepSeek |
| 外部健康与语音 | 健康拍、腾讯云 TTS |
| 数据 | MySQL、Redis、MinIO |
| 网关与部署 | Nginx、Docker Compose |

## 项目结构

```text
E:\health
├─ rayk-miniapp/       UniApp 小程序与 H5
├─ rayk-server/        Java 业务服务
├─ rayk-ai/            Python AI、OCR、RAG 与报告服务
├─ nginx/              网关配置
├─ scripts/            启停、构建、备份和验证脚本
├─ compose.yml         通用 Docker 编排
├─ compose.dev.yml     本地开发覆盖配置
├─ compose.prod.yml    生产部署覆盖配置
├─ .env.example        环境变量模板
├─ AGENTS.md           永久工程规则
└─ handoff.md          当前状态与交接说明
```

## 运行要求

- Windows 10/11 + Docker Desktop（WSL2 后端）
- PowerShell 5.1 或更高版本
- Node.js 与 npm（前端本地检查和构建）
- Git
- 建议至少 16 GB 内存

项目根目录固定为 `E:\health`。Docker Desktop 的磁盘镜像位置和项目持久化数据应放在 E 盘，避免占满系统盘。

## 快速启动

### 1. 配置环境变量

```powershell
Set-Location E:\health
Copy-Item .env.example .env
```

在本地 `.env` 中填写数据库、微信及可选外部服务配置。`.env` 已被 Git 忽略，不得提交或复制到文档。

### 2. 启动开发环境

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

等价命令：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
docker compose -f compose.yml -f compose.dev.yml up -d --force-recreate nginx
```

启动脚本会在业务服务启动后自动重建一次 Nginx 网关，并通过宿主机端口执行 `/health` 检查。Docker Desktop 在电脑休眠、切换 WLAN 或重建 Java/AI 容器后，旧网关偶尔会出现 8088 建立连接后直接断开的情况；该操作只重建网关，不会删除或重置任何数据卷。

### 3. 查看状态

```powershell
docker compose -f compose.yml -f compose.dev.yml ps
```

本地入口：

- H5：<http://127.0.0.1:8088/>
- Java API：<http://127.0.0.1:8080/>
- MinIO 控制台：<http://127.0.0.1:9001/>

端口可通过 `.env` 调整。手机真机不能访问 `127.0.0.1`，局域网验收需使用电脑的 LAN 地址。

本地 H5 需要显示三角色开发调试入口时，在构建前端后执行：

```powershell
Set-Location E:\health\rayk-miniapp
npm run build:h5:dev
```

刷新 <http://localhost:8088/> 后可选择平台管理员、医生或客户。H5 在 localhost 下会自动请求同一 Docker 环境的 API；正式构建仍使用 `npm run build:h5`。

新同事从 Git 拉取项目后的完整步骤见 [本地开发启动指南](docs/local-development-guide.md)。

## 常用 Docker 命令

查看日志：

```powershell
docker compose -f compose.yml -f compose.dev.yml logs -f rayk-server rayk-ai nginx
```

重建单个服务：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build rayk-server
docker compose -f compose.yml -f compose.dev.yml up -d --build rayk-ai
docker compose -f compose.yml -f compose.dev.yml up -d --build nginx
```

重新构建全部服务：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
```

停止并保留数据：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

日常操作不要使用 `docker compose down -v`，该命令会删除持久化数据卷。

## 前端开发与三端同步构建

```powershell
Set-Location E:\health\rayk-miniapp
npm ci
npm run type-check
npm run lint
npm run build:h5
npm run build:mp-weixin:dev
npm run build:mp-weixin
```

每次前端修改后必须同步维护三个输出：

| 用途 | 目录 |
| --- | --- |
| H5 调试界面 | `E:\health\rayk-miniapp\dist\build\h5` |
| 微信开发包 | `E:\health\rayk-miniapp\dist\release\mp-weixin-dev` |
| 微信生产局域网验收包 | `E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan` |

微信开发者工具分别导入两个 release 目录。`mp-weixin-prod-lan` 是局域网验收包，不是可直接提交审核的正式互联网生产包。

旧的 `dist\release\mp-weixin-internal` 内测包已从当前 release 目录移出，不会再被开发者工具作为现用包读取；为便于误删恢复，原包暂存于 `E:\health-archive\mp-weixin-internal-20260822`。上传新内测版本时使用本次构建生成的 `mp-weixin-dev`，不要重新导入旧目录。

### 发布版本一致性校验

正式发布必须从干净的 Git 提交构建。发布脚本会读取当前提交 SHA，生成唯一 `releaseId`，把版本信息注入 Java、Python 和小程序请求头，并在三个前端包根目录写入不含密钥的 `release-manifest.json`：

```powershell
Set-Location E:\health
node .\scripts\release\build-release.mjs --release-id release-<版本标识>
```

脚本默认拒绝有未提交修改的工作区；只做本地构建验证时才使用 `--allow-dirty`。输出位于 `E:\health\build\release`：`release-manifest.json` 是本次源码、数据库迁移和三个前端产物的指纹，`release.env` 可作为 Docker Compose 的环境文件。构建服务时使用同一份环境文件，避免镜像标签、Java/Python 运行版本和前端包来自不同提交：

```powershell
docker compose --env-file .\build\release\release.env -f compose.yml -f compose.prod.yml build rayk-server rayk-ai
docker compose --env-file .\build\release\release.env -f compose.yml -f compose.prod.yml up -d rayk-server rayk-ai nginx
```

线上服务提供公开的版本校验接口 `GET /api/system/version`，AI 服务对应 `GET /ai/version`。部署同一份 `release.env` 和镜像后，可用本地清单校验公网服务：

```powershell
node .\scripts\release\verify-release.mjs https://xingxuyuan.com
```

校验失败时不要只重新上传小程序包：先比较线上 `releaseId`、Git SHA、数据库迁移版本和前端产物 SHA256，再决定是重新构建镜像、执行 Flyway，还是重新导入正确的微信包。

## 测试与质量检查

平台管理员会员管理：平台管理员可在平台工作台的“会员管理”页面按客户手机号查找 `CUSTOMER` 账号，查看脱敏手机号和当前会员状态，并直接开通 365 天年度健康会员或取消现有年度会员。后端接口为 `GET/PUT /api/v1/platform/customer-membership`，服务端同时校验平台管理员权限、客户角色和目标客户租户；开通来源记录为 `PLATFORM_ADMIN`，不经过支付订单。页面路由为 `/pages-platform/membership/index`。

Java（项目未提供 Maven Wrapper，使用 Dockerfile 中的 Maven 构建链）：

```powershell
Set-Location E:\health
docker compose -f compose.yml -f compose.dev.yml build --build-arg MAVEN_SKIP_TESTS=false rayk-server
```

Python：

```powershell
Set-Location E:\health\rayk-ai
python -m pytest
```

当前 AI 健康评估回归测试覆盖知识库检索、异常事实引用、疾病参考证据约束、Qwen 图片分批直读、最终报告汇总和历史报告中医药物占位语兼容处理；相关定向回归测试为 29 项通过。Java 全量测试应以 Docker 构建输出为准，若仅需更新运行镜像而暂时跳过测试，可将 `MAVEN_SKIP_TESTS` 改为 `true`。

前端：

```powershell
Set-Location E:\health\rayk-miniapp
npm run type-check
npm run lint
```

OCR 修改必须分别验证 PDF、扫描 PDF、单栏图片和双栏图片，且保证 PDF 原分类、原顺序和原内容不回归。

## 外部服务配置

所有服务都通过 `.env` 开关和参数配置：

- DeepSeek：文字结构化结果的综合评估、报告和随访调整；平台管理员可切换 V4 Flash/Pro，当前选择通过后端受保护接口管理。
- Qwen3.7-flash-2026-07-15：图片体检报告分批直读；DeepSeek负责最终报告汇总。Qwen 由 `QWEN_VISION_ENABLED` 和服务端密钥显式启用，失败时保留规则降级边界。图片阶段按 `QWEN_VISION_BATCH_SIZE` 分批，并按 `QWEN_VISION_MAX_IMAGE_BYTES`、`QWEN_VISION_MAX_TOTAL_IMAGE_BYTES` 和 `QWEN_VISION_MAX_IMAGE_PIXELS` 做安全压缩与大小控制；Qwen HTTP 状态、超时和响应格式错误会区分记录，不记录带签名的 MinIO 地址。Java 到 AI 的本地开发读取超时为 600 秒，覆盖分批分析和 DeepSeek 综合解读的完整链路。
- Qwen3.7-flash-2026-07-15 → Qwen3.5-OCR：PDF 渲染页按页级联识别；电子 PDF 原生文本/表格解析作为校验基线，两种云模型失败后由本地 PaddleOCR 兜底。图片报告直读仍单独使用 Qwen Vision。
- 健康拍：面部健康检测。
- 腾讯云 TTS：吃饭和睡觉提醒试听。
- 微信：企业主体手机号授权登录、预录入医生/管理员手机号匹配和订阅消息。

仓库只提供变量名和安全默认值，不保存真实密钥。正式环境建议使用部署平台的密钥管理服务，并在上线前轮换曾经通过聊天、截图或临时文件暴露的凭据。

## 生产部署

生产编排入口：

```powershell
docker compose -f compose.yml -f compose.prod.yml up -d --build
```

正式上线前至少完成：

- 企业主体小程序上线：配置真实 AppID/Secret，开通手机号快速验证能力；客户手机号可自动创建账户，医生手机号在医院管理页预录入，管理员手机号在平台工作台预录入，另需 HTTPS 合法域名和微信服务器域名白名单。首次管理员登录若数据库尚无手机号，可在服务部署环境临时设置 `WECHAT_PLATFORM_ADMIN_PHONE`（不写入 Git），登录后再从平台工作台维护。
- HTTPS 合法域名、Nginx TLS 和微信服务器域名白名单。
- 关闭 Mock/开发登录并使用强密码。
- 健康拍正式环境、插件、回调和计费验收。
- DeepSeek、Qwen OCR、Qwen Vision、腾讯云 TTS 的额度、限流、超时和降级验证。
- MySQL/MinIO 备份恢复演练、日志留存、监控和告警。
- 隐私政策、敏感个人信息授权、医疗与数据安全合规审查。

当前腾讯云生产实例使用 `/opt/zhiyu-health`，域名为 `xingxuyuan.com`（`www` 同域名），由 Nginx 终止 HTTPS 并反向代理 H5、Java、AI 和 MinIO 报告路径。证书文件只挂载到服务器的本地密钥目录，生产 `.env` 仅保存在服务器，不纳入 Git。微信小程序包仍需在微信开发者工具中导入 `rayk-miniapp/dist/release/mp-weixin-prod-lan` 后由具备权限的账号上传审核；该目录不是服务器静态网页包。

生产登录会话有效期由 `JWT_EXPIRE_SECONDS` 控制，当前 `compose.prod.yml` 固定为 `604800` 秒（7 天），Redis 会话 TTL 与 JWT 保持一致。调整该值后需要重建 `rayk-server`；已签发的旧令牌不会自动延长，用户重新登录后才会获得新的有效期。该配置与年度会员 365 天有效期相互独立。

生产 `compose.prod.yml` 会显式开启真实微信虚拟支付；支付 AppKey、ProductID 和回调配置仍只能从服务器 `.env` 或密钥管理注入，不能写入源码。若生产页面显示“模拟开通会员”，先核对 Java 容器实际生效的 `MEMBERSHIP_PAYMENT_ENABLED` 和支付配置，再重建/重启 `rayk-server`。

健康助手发布时必须同时更新后端：只上传 `mp-weixin-prod-lan` 小程序包不会把 Java 接口、Flyway 数据库迁移或 Python 助手服务发布到服务器。当前健康助手依赖 Java 服务中的助手模块以及数据库迁移 V43/V44；生产部署应在服务器项目目录执行：

```powershell
Set-Location /opt/zhiyu-health
docker compose -f compose.yml -f compose.prod.yml up -d --build rayk-server rayk-ai nginx
docker compose -f compose.yml -f compose.prod.yml ps
docker compose -f compose.yml -f compose.prod.yml logs --tail=100 rayk-server rayk-ai nginx
```

使用发布清单时，将本次构建的 `build/release/release.env` 安全复制到服务器项目目录（不提交 Git），并按同一文件执行：

```bash
docker compose --env-file build/release/release.env -f compose.yml -f compose.prod.yml up -d --build rayk-server rayk-ai nginx
docker compose --env-file build/release/release.env -f compose.yml -f compose.prod.yml ps
```

服务器的 `/api/system/version` 应与本地 `release-manifest.json` 完全一致；服务器没有完成这一步之前，不要把对应小程序包当作线上验收版本。

部署后再用真实客户账号打开健康助手。若页面显示“系统内部错误”，先检查 `rayk-server` 日志中的数据库异常，并确认 Flyway 已执行到最新版本；不要反复上传同一个前端包。生产 `.env` 中的 Qwen 助手密钥只影响发送消息，不影响健康助手首屏初始化。

## 数据备份

仓库提供 MySQL 备份和恢复脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\backup-mysql.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\restore-mysql.ps1 -BackupFile E:\path\to\backup.sql
```

执行恢复前必须确认目标环境、备份文件和停机窗口。MinIO 原报告与生成报告也应纳入独立备份策略。

## 安全约束

- 不提交 `.env`、API Key、数据库密码、微信密钥或供应商密钥。
- 不公开 MinIO 存储桶。
- 不修改已经执行过的 Flyway 迁移，只新增后续版本。
- 数据库 `BIGINT` ID 在前端按字符串处理。
- 前端隐藏不等于授权，权限和数据范围由 Java 后端校验。
- AI 结论必须保留证据来源和辅助参考边界。

更详细的当前状态、已知未完成事项和不可重复的工程坑见 [handoff.md](handoff.md)。永久开发规则见 [AGENTS.md](AGENTS.md)。

开发环境真实支付验收使用 `compose.real-payment-dev.yml`，它会强制开启支付、使用新域名 HTTPS 配置，并在缺少 `WECHAT_VIRTUAL_APP_KEY` 或 `WECHAT_VIRTUAL_PRODUCT_ID` 时拒绝启动。确认域名已指向当前服务器后执行：`docker compose -f compose.yml -f compose.real-payment-dev.yml up -d --build`。该配置会占用 80/443 端口，只能在具备公网 DNS、证书和防火墙权限的验收服务器上使用；普通本地开发继续使用 `compose.dev.yml`。

## 普通客户会员体系（V1）

会员能力只面向 `CUSTOMER`，平台管理员和医生不显示会员入口。免费客户和年度会员都可以永久保存、随时查看健康档案，并支持体检报告上传、保存和查看。当前产品权益为：免费客户 AI 健康评估 3 次、AI 健康报告 3 次（两项分别计数）、首次健康随访 1 次、健康拍 3 次、吃饭/睡眠语音提醒各 3 次；年度健康会员有效期 365 天，AI 健康评估和 AI 健康报告分别在会员期内不限次数，支持持续随访、每天 1 次健康拍以及会员期内持续使用两类语音提醒。免费客户的健康拍历史和检验趋势按近 3 天基础范围查看，年度会员可查看完整历史趋势。年度会员方案和权益由数据库配置，开发环境默认关闭真实支付并支持模拟订单开通；健康报告发布不因随访权益配置或生成失败而回滚。

AI 健康评估和 AI 健康报告在一次完整评估流程中分别预占、确认或释放，确保两项次数独立统计且外部 AI 失败不会扣除额度。历史综合评估产生的旧 `AI_HEALTH_REPORT` 流水会兼容计入评估额度，避免拆分权益后遗漏既有使用记录。

后端接口位于 `/api/client/membership`：`summary`、`plans`、`benefits`、`usage`、`orders` 和订单支付接口。所有消耗通过 `membership_usage` 的 `RESERVED` → `CONFIRMED`/`RELEASED` 两阶段流水记录，外部 AI、健康拍或 TTS 调用失败不会扣除权益。健康拍会员权益当前仅保留每日额度，不再配置滚动 30 天额度；“报告重新解读”不再作为会员权益提供。健康拍历史和指标趋势的查看范围也由 Java 后端按会员状态校验，不能只靠前端隐藏。数据库迁移为 `V32__customer_membership.sql` 和后续会员权益调整迁移（含 `V39__remove_ai_report_regeneration.sql`、`V40__align_membership_rights_with_product_table.sql`、`V41__split_ai_assessment_and_report_entitlements.sql`）。支付确认页仅保留订单状态、支付通知和操作按钮，不再展示支付后权益卡片或底部免责声明。

开发包会员调试：使用 `compose.yml` + `compose.dev.yml` 启动后端，并导入 `dist\release\mp-weixin-dev`。客户进入“健康会员”页面会看到“开发调试”卡片，可切换“恢复免费客户”或“模拟年度会员”来验证两套权益。该接口由后端 `MEMBERSHIP_DEVELOPMENT_MODE` 保护，`compose.dev.yml` 才会开启，生产环境默认关闭；切勿将开发包用于正式发布。

会员前端包含普通客户中心、年度会员中心和开通会员三种状态页面，统一使用真实会员 `summary/plans` 接口渲染权益、剩余次数、有效期和支付入口；开通页新增与产品表一致的 14 项权益对比表（包含“AI健康助手”，不单独展示会员有效期行），表格字号和换行按中老年用户阅读场景放大。会员中心的健康分动态读取当前客户最近一次成功 AI 评估，并按有有效分数的健康维度计算平均值，没有有效评估时显示“待评估”。会员权益和健康分仪表盘采用双指标卡片与横向进度条展示，避免圆环同时承载多个含义造成阅读负担。会员 SVG 资源位于 `rayk-miniapp/src/pages-customer/static/member/`，随 `pages-customer` 分包发布；复杂插画保留 SVG 封装但已压缩内嵌位图，通用图标统一复用，避免约 27MB 的重复资源进入微信主包。

本地环境可通过以下变量控制：`MEMBERSHIP_ENABLED`、`MEMBERSHIP_PAYMENT_ENABLED` 以及六项 `MEMBERSHIP_FREE_*_TRIAL`（AI 评估、AI 报告、首次随访、健康拍、吃饭提醒、睡眠提醒）。本地和开发包默认保持 `MEMBERSHIP_PAYMENT_ENABLED=false`，如需在开发环境进行真实支付测试，可在未提交的 `.env` 中显式设置为 `true`，同时配置虚拟支付 AppKey、ProductID 和公网 HTTPS 回调。当前年度会员真实支付按小程序虚拟支付“道具直购”模式接入：需配置 `WECHAT_VIRTUAL_APP_ID`、虚拟支付商户号 `WECHAT_VIRTUAL_MERCHANT_ID`、`WECHAT_VIRTUAL_OFFER_ID`、现网 `WECHAT_VIRTUAL_APP_KEY`、`WECHAT_VIRTUAL_PRODUCT_ID`，并将 `WECHAT_VIRTUAL_ENV=0`、`WECHAT_VIRTUAL_MODE=short_series_goods`。AppKey 只能通过服务器密钥管理或未纳入 Git 的 `.env` 提供，不能写入源码或前端；商品 ID 和价格必须与微信虚拟支付后台已发布的道具一致。当前回调地址为 `https://xingxuyuan.com/api/payments/wechat/virtual/notify`，需在小程序虚拟支付后台订阅 JSON 推送、完成 DNS/HTTPS/公网 443 转发并进行真实小额验收。服务端使用 `wx.requestVirtualPayment` 所需的 `signData`、`paySig` 和 `signature`，支付成功以后端发货通知校验并开通会员为准。
