# RayK AI 功能医学 AI 健康系统

面向健康管理机构、体检中心、私立诊所、医生、健康管理师和普通客户的多机构健康管理 SaaS 主体框架。本仓库直接以当前 `health` 目录为项目根目录，没有额外外层目录，也没有独立 Vue 管理后台。

> **健康管理免责声明：** 该系统当前只实现开发测试用的亚健康风险评估与生活方式建议。所有模拟结果必须标注“该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。”系统不提供临床疾病诊断、药物处方或自动诊疗结论。

## 单小程序承载 B 端和 C 端

机构人员与普通客户使用同一套 uni-app 小程序和同一登录入口。账号可拥有一个或多个工作台；工作台决定首页摘要、卡片菜单、页面与按钮展示。真正的权限、租户和客户数据范围始终由 Java 服务端校验。

固定 TabBar：**首页 / 工作台 / 消息 / 我的**。业务页面按 C 端、机构业务端、机构管理端分包，避免主包膨胀。

## 系统架构

```mermaid
flowchart TB
  USER[微信小程序 / H5] --> NGINX[Nginx 容器 :80 / 本机开发 :8088]
  NGINX --> JAVA[Spring Boot 3 / Java 21 :8080]
  NGINX --> PY[FastAPI / Python 3.12 :8000]
  NGINX --> MINIO[MinIO :9000]
  JAVA --> MYSQL[(MySQL 8.4 :3306)]
  JAVA --> REDIS[(Redis 7 :6379)]
  JAVA --> MINIO
  JAVA -->|HTTP JSON + RequestId| PY
  PY -.不访问业务数据库.-> MYSQL
```

Java 是核心业务和数据一致性的唯一入口；Python 负责 PaddleOCR、指标标准化、演示规则评分和文字生成，不直接访问业务数据库。

## 角色与工作台

| 账号角色 | 默认工作台 | 一期能力 |
|---|---|---|
| `PLATFORM_ADMIN` | 平台管理工作台 | 机构、平台用户、审计基础查看 |
| `TENANT_ADMIN` | 机构管理工作台 | 本机构客户、员工、报告、评估、随访 |
| `DOCTOR` | 医生工作台 | 授权客户、AI 证据、审核、退回、发布 |
| `HEALTH_MANAGER` | 健康管理工作台 | 客户、报告、OCR 校对、指标确认、随访 |
| `CUSTOMER` | 个人健康中心 | 本人档案、检验、评估进度、已发布报告、随访 |

`doctor` 演示账号同时拥有 `DOCTOR` 和 `CUSTOMER` 工作台，用于验证工作台切换。

## 技术栈

- 小程序：uni-app、Vue 3、TypeScript、Pinia、Vite、uni-ui、ECharts 依赖、ESLint、Prettier
- 业务端：Java 21、Spring Boot 3、Spring Security、JWT、Redis、MyBatis-Plus、Flyway、WebClient、MapStruct、Actuator、Springdoc
- AI 端：Python 3.12、FastAPI、Pydantic、PaddleOCR、PaddlePaddle、NumPy、Pandas、scikit-learn、Pytest、Ruff、Black、MyPy
- 基础设施：MySQL 8.4、Redis 7、MinIO、Nginx、Docker Compose

## 目录

```text
.
├─ rayk-server/          Java 核心业务服务
├─ rayk-ai/              Python AI HTTP 服务
├─ rayk-miniapp/         B/C 端统一小程序
├─ database/             Flyway 迁移与数据库说明
├─ deploy/nginx/         统一入口配置
├─ docs/                 架构、权限、API 与路线图
├─ scripts/              启停、构建、备份和恢复脚本
├─ backups/              仓库内备用占位（实际默认备份到 E 盘）
├─ compose.yml           通用服务定义
├─ compose.dev.yml       开发端口覆盖
└─ compose.prod.yml      生产端口覆盖
```

## Docker Desktop 与 E 盘存储

本机只需 Docker Desktop、浏览器、微信开发者工具和可选编辑器。Java、Maven、Python、MySQL、Redis、MinIO 与 Nginx 均由容器提供。

因 C 盘空间有限，请先手动设置：

1. Docker Desktop → **Settings** → **Resources** → **Advanced**。
2. 将 **Disk image location** 设置为 `E:\DockerData\DockerDesktop`。
3. 应用设置并等待 Docker Desktop 完成迁移。

建议 E 盘路径：

```text
E:\DockerData\DockerDesktop             Docker 镜像、容器、缓存、命名卷
E:\DockerData\RayKA1\backups\mysql    MySQL 逻辑备份
E:\health                               项目源码
```

项目不会修改 Docker Desktop 全局设置。**不要手动移动 Docker WSL 文件，不要执行 `wsl --export/import` 来迁移本项目。** MySQL 使用 Docker 命名卷，未把 `/var/lib/mysql` 直接绑定到 Windows NTFS。

为改善国内网络下载速度，项目内 Maven 使用阿里云公共仓库、pip 使用清华 PyPI、npm 使用 npmmirror；Java 构建和运行基础镜像默认经过 DaoCloud 镜像代理。所有镜像地址都可在 `.env` 中替换，不影响宿主机全局设置。

## 环境变量

开发前复制示例文件：

```powershell
Copy-Item .env.example .env
```

至少更换 MySQL、Redis、MinIO 密码与长度不少于 32 字节的 `JWT_SECRET`。`.env` 已被 Git 忽略，不应提交生产密钥。Compose 内的默认值只用于本机框架演示。

如需启用综合 AI 解读，在本地 `.env` 设置 `DEEPSEEK_ENABLED=true`、`DEEPSEEK_API_KEY` 和 `DEEPSEEK_MODEL=deepseek-v4-flash`。密钥不得写入源码、迁移、日志或前端；关闭或调用失败时系统自动回退到规则摘要，不影响十二模型评分和医生审核主链路。

微信开发联调默认启用固定 openid 并自动绑定 `customer` 测试用户。生产覆盖文件会强制关闭模拟模式；正式部署需在本地 `.env` 配置 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`，并把 `MINIO_PUBLIC_ENDPOINT` 设置为客户端可访问的 HTTPS 对象存储域名。后端只使用微信 `code2Session` 返回的身份，不保存 `session_key`。

## 启动

### 开发环境

```powershell
.\scripts\start-dev.ps1
```

等价命令：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
```

### 生产形态配置

在完成 TLS、强密钥、备份和监控配置后执行：

```powershell
docker compose -f compose.yml -f compose.prod.yml up -d --build
```

`compose.prod.yml` 仅向宿主机暴露 Nginx，数据库和内部服务不暴露端口；它仍是主体框架，不代表已经完成完整生产加固。

### 状态、日志与停止

```powershell
docker compose -f compose.yml -f compose.dev.yml ps
docker compose -f compose.yml -f compose.dev.yml logs -f rayk-server rayk-ai
.\scripts\stop-dev.ps1
```

停止脚本只执行普通 `down`，保留命名卷和数据。**禁止执行 `docker compose down -v`。** 也不要使用会清除镜像、容器和卷的全局 prune 命令。

## 服务地址

| 服务 | 地址 |
|---|---|
| 小程序 H5 预览 / Nginx 统一入口 | <http://localhost:8088> |
| Java API | <http://localhost:8080/api/v1> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| FastAPI | <http://localhost:8000/health> |
| FastAPI Docs | <http://localhost:8000/docs> |
| MinIO API | <http://localhost:9000> |
| MinIO Console | <http://localhost:9001> |
| H5 开发服务 | <http://localhost:5173> |

Nginx 根路径展示已构建的小程序 H5 预览；路由为 `/api/` → Java、`/ai/` → Python、`/minio/` → MinIO。

## 测试账号

统一开发测试密码：`RayK@123456`。数据库只保存 BCrypt 摘要。

| 用户名 | 角色 |
|---|---|
| `platform_admin` | 平台管理员 |
| `tenant_admin` | 机构管理员 |
| `doctor` | 医生 + 客户工作台 |
| `health_manager` | 健康管理师 |
| `customer` | 普通客户 |

## 小程序开发与构建

```powershell
Set-Location rayk-miniapp
npm install
npm run dev:h5
npm run type-check
npm run build:h5
npm run build:mp-weixin
```

微信开发者工具导入 `rayk-miniapp\dist\build\mp-weixin`。当前 `manifest.json` 使用游客 AppID 供框架构建；开发工具内可直接验证微信一键登录。正式发布前替换正式 AppID，并在微信公众平台配置 API 与 MinIO 下载的 HTTPS 合法域名。开发 H5 通过 Vite 代理访问 Nginx，保留调试身份登录。

## 最小业务闭环

1. 在微信开发者工具中一键登录，或使用开发调试身份进入对应工作台。
2. 客户或健康管理师选择真实 PDF/JPG/PNG 报告上传；Java 校验文件、写入 MinIO 私有 Bucket，并在 MySQL 保存大小、MIME 与 SHA-256 元数据。
3. Java 创建异步 OCR 任务，Python 从短时预签名地址读取文件并使用 PaddleOCR 识别；小程序轮询展示进度、置信度和失败重试入口。
4. 用户核对、增删或修正指标后人工确认，Java 才允许进入后续 AI 健康评估。
5. Java 建立评估任务并通过 HTTP 调用 Python 十二模型规则引擎。规则引擎优先采用报告自带参考区间，数据不足时明确标记为“数据不足”，不伪装成低风险。
6. Python 调用 DeepSeek 生成跨模型结构化综合解读；仅传年龄、性别、指标值/单位/参考区间及规则结果，不传姓名、手机号、OpenID、原始报告或业务标识。调用失败会自动回退到规则摘要。
7. Java 保存规则结果与综合解读快照并创建待审核任务。
8. 医生查看证据、缺失指标、DeepSeek 解读和建议，审核通过或退回；AI 结果不能跳过医生审核。
9. 医生二次确认后发布，客户只可查看已发布报告。
10. 机构人员创建/处理随访，客户提交反馈。

种子数据中已有一个客户、八项指标、一份发布报告和一个待完成随访，全部标记为开发测试数据。

## MySQL 备份与恢复

默认备份目录为 `E:\DockerData\RayKA1\backups\mysql`，可用 `RAYK_BACKUP_DIR` 覆盖。

```powershell
.\scripts\backup-mysql.ps1
.\scripts\restore-mysql.ps1 -BackupFile 'E:\DockerData\RayKA1\backups\mysql\rayk_health_YYYYMMDD_HHMMSS.sql'
```

备份通过容器内 `mysqldump` 导出，不复制 MySQL 内部目录。恢复脚本要求显式输入 `RESTORE`，不会删除命名卷，也不会自动清空数据库；恢复前请先备份当前状态。

## 构建与测试

```powershell
docker compose -f compose.yml -f compose.dev.yml config
docker compose -f compose.yml -f compose.dev.yml build
docker build -f rayk-ai/Dockerfile.test -t rayk-a1-ai:test rayk-ai
docker run --rm rayk-a1-ai:test
Set-Location rayk-miniapp
npm run type-check
npm run build:h5
npm run build:mp-weixin
```

Java 单元测试在 Docker 多阶段构建的 Maven `package` 中执行。Python 使用独立测试镜像；正式运行镜像不携带测试依赖和测试源。

## 常见问题

- **端口占用：** 修改 `.env` 中 `NGINX_PORT`、`JAVA_PORT` 等宿主机端口。
- **登录失败：** 检查 Redis 和 Java 容器健康状态；JWT 登录态保存在 Redis。
- **Flyway 失败：** 查看 `rayk-server` 日志，不要通过删卷规避迁移错误。
- **AI 暂不可用：** 查看 `rayk-ai` 健康接口；Java 会把任务和报告标记为失败并返回明确错误。
- **小程序 401/403：** 401 会回登录页，403 会进入无权限页；切换工作台后重新加载首页和菜单。
- **Docker 数据仍在 C 盘：** 只在 Docker Desktop 设置中检查磁盘镜像位置，勿手动移动 WSL 数据文件。
- **MinIO Bucket：** `minio-init` 会创建私有 `rayk-reports`，不会设为公开访问。
- **微信一键登录提示未绑定：** 开发环境检查 `WECHAT_MOCK_ENABLED` 和 `WECHAT_AUTO_BIND_USERNAME`；生产环境需先通过已登录账号调用绑定接口建立 openid 关系。
- **上传失败：** 仅支持内容与扩展名一致的 PDF/JPG/PNG，最大 20MB；同时检查 Nginx、Java 和 MinIO 健康状态。

## 文档与下一阶段

详细设计见 [docs](./docs)。微信 `code2Session` 身份绑定、JWT 登录、真实 MinIO 上传、PaddleOCR 异步识别、任务进度与失败重试、指标人工校对和确认均已完成。下一阶段建议优先完成可配置健康评估规则、医生审核和 PDF 健康报告，再逐步接入订阅消息、会员、支付、商城和机构分润。

在引入任何真实医学规则或健康数据前，应补齐隐私授权、数据加密、保留策略、审计、规则验证、人工复核和适用地区合规评估。
