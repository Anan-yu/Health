# 智能三羊

智能三羊是一套面向体检后健康管理的智能化平台。系统把分散在体检报告、个人档案、健康问卷、日常体征和随访反馈中的信息连接起来，通过结构化 OCR、医学知识检索、AI 综合评估和持续健康随访，形成从“一次体检”到“长期健康行动”的数字化闭环。

平台不是简单的报告阅读器，而是一套可持续演进的健康智能基础设施：它保留原始医学证据，建立可追踪的个人健康画像，将复杂指标转化为清晰、温暖、可执行的行动建议，并为医生提供统一的健康报告和趋势参考。

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
微信身份进入系统（个人主体客户使用 OpenID 登录；医生由平台管理员发放一次性绑定码）
  → 完善健康档案与问卷
  → 上传 PDF 或图片体检报告
  → OCR 保留原分类、原顺序和原内容
  → 指标标准化与 12 维健康评估
  → RAG 医学知识检索 + DeepSeek 综合解读
  → 生成统一健康报告和 PDF
  → AI 制定健康随访计划
  → 客户逐项反馈完成情况、感受与困难
  → 自动继续、调整或终止下一期任务
  → 趋势、健康检测和再次评估
```

## 已有功能

### 体检报告数字化

- 上传 PDF、JPG、PNG，文件保存到 MinIO 私有存储。
- PDF 原生表格/文本解析与扫描 PDF 回退。
- 图片单栏、双栏识别及左右分栏合并去重。
- 保留数值结果、非数值结果、参考范围、单位、异常标识和检查小结。
- 过滤姓名、电话、门诊号、床位号、打印日期等非体检结果。
- 异步 OCR、状态查询、失败恢复、原报告查看。
- OCR 成功而后续 AI 评估失败时，报告会明确显示“评估未完成”，保留全部识别内容，并支持只重试 AI 评估与健康报告生成，不重复执行 OCR。

### 智能健康评估

- 结合体检结果、健康档案、问卷、既往史、家族史、生活方式综合描述、过敏史、当前用药和最近一次成功的面部健康检测体征。
- 体检报告按原分类向评估提供结构化指标、检查所见和检查小结，不直接使用未经清洗的整份原始 OCR 文本。
- 12 个健康维度规则评估与健康指数仪表盘。
- 已确认的异常指标按原报告参考范围形成可追溯事实，即使完整健康维度数据不足也不会遗漏单项异常。
- 医学知识库 RAG 聚焦异常事实、重点关注维度和原报告检查小结，并由 DeepSeek 生成综合解读。
- DeepSeek 综合解读默认显式关闭思考模式，超时时间为 60 秒、输出上限为 16K，最多尝试 3 次并对网络超时、429 和 5xx 递增退避重试；医疗安全校验保留确诊、处方剂量和自行调药等硬边界，允许带限定语的风险说明，失败时继续使用可追溯的规则降级结果。
- 展示整体健康状态、重点发现、对应建议、待补充数据和当前结论边界；只有证据条件充分时才展示需要进一步确认的健康方向。
- 报告在重点发现之后提供逐项异常结果解释，分别说明异常含义、可能涉及的器官或系统及下一步建议；每项解释绑定结构化异常事实和医学知识证据，并保留健康管理参考边界。
- 疾病推断参考逐项提供西医治疗思路、西医药物治疗参考、中医治疗思路和中医药物/治法参考；知识库命中对应证据时可列出半夏泻心汤类方、血脂康胶囊或化滞柔肝颗粒等代表性讨论项，内容仅作辨证就诊沟通与复查参考，不输出剂量或自行购药、停药、调药建议。
- 大模型未完成时会明确标记为保守规则结果，客户可从报告详情重新生成 AI 解读；旧报告在重试失败时保持可用。
- 健康拍摄像头估算明确标记为补充趋势证据，不能替代医疗设备测量或单独用于疾病判断。
- 同一份报告供客户与医生查看，支持 PDF 生成、版本保存和下载；H5 下载通过鉴权文件接口生成带文件名的本地下载，不直连 MinIO。

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
        ├─ Qwen3.5-OCR
        ├─ DeepSeek + RAG
        └─ 报告与随访智能处理
```

| 层级 | 技术 |
| --- | --- |
| 客户端 | UniApp、Vue 3、TypeScript |
| 业务后端 | Java、Spring Boot、MyBatis、Flyway |
| AI 服务 | Python、FastAPI、PaddleOCR、PDF 结构解析 |
| 外部 AI | Qwen3.5-OCR、DeepSeek |
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
```

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

## 测试与质量检查

Java：

```powershell
Set-Location E:\health\rayk-server
.\mvnw.cmd test
```

Python：

```powershell
Set-Location E:\health\rayk-ai
python -m pytest
```

前端：

```powershell
Set-Location E:\health\rayk-miniapp
npm run type-check
npm run lint
```

OCR 修改必须分别验证 PDF、扫描 PDF、单栏图片和双栏图片，且保证 PDF 原分类、原顺序和原内容不回归。

## 外部服务配置

所有服务都通过 `.env` 开关和参数配置：

- DeepSeek：综合评估、报告和随访调整。
- Qwen3.5-OCR：复杂图片和扫描内容识别。
- 健康拍：面部健康检测。
- 腾讯云 TTS：吃饭和睡觉提醒试听。
- 微信：真实登录、个人主体 OpenID 登录、可选手机号身份匹配和订阅消息。

仓库只提供变量名和安全默认值，不保存真实密钥。正式环境建议使用部署平台的密钥管理服务，并在上线前轮换曾经通过聊天、截图或临时文件暴露的凭据。

## 生产部署

生产编排入口：

```powershell
docker compose -f compose.yml -f compose.prod.yml up -d --build
```

正式上线前至少完成：

- 个人主体小程序上线：真实微信 AppID/Secret、OpenID 登录；平台管理员首次使用系统预置账号密码绑定当前微信，医生由管理员生成一次性绑定码，另需 HTTPS 合法域名和微信服务器域名白名单；后续切换企业主体后再启用手机号快速验证。
- HTTPS 合法域名、Nginx TLS 和微信服务器域名白名单。
- 关闭 Mock/开发登录并使用强密码。
- 健康拍正式环境、插件、回调和计费验收。
- DeepSeek、Qwen OCR、腾讯云 TTS 的额度、限流、超时和降级验证。
- MySQL/MinIO 备份恢复演练、日志留存、监控和告警。
- 隐私政策、敏感个人信息授权、医疗与数据安全合规审查。

当前腾讯云生产实例使用 `/opt/zhiyu-health`，域名为 `xingxuyuantech.com`（`www` 同域名），由 Nginx 终止 HTTPS 并反向代理 H5、Java、AI 和 MinIO 报告路径。证书文件只挂载到服务器的本地密钥目录，生产 `.env` 仅保存在服务器，不纳入 Git。微信小程序包仍需在微信开发者工具中导入 `rayk-miniapp/dist/release/mp-weixin-prod-lan` 后由具备权限的账号上传审核；该目录不是服务器静态网页包。

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
