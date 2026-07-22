# RayK A1 项目交接说明（Handoff）

## 0.4 前端构建产物保留与同步约定

- 仅保留三份当前可用产物：`rayk-miniapp/dist/release/mp-weixin-dev`、`rayk-miniapp/dist/release/mp-weixin-prod-lan` 与浏览器 H5 `rayk-miniapp/dist/build/h5`。
- 不再保留 `dist/dev/mp-weixin`、`dist/build/mp-weixin` 或历史构建回退目录，避免微信开发者工具导入旧包。
- 此后每次小程序代码变更，交付前必须同步重建并替换上述三份产物；不可只更新 H5 或其中一个微信包。
- 微信开发者工具应导入 `dist/release/mp-weixin-dev`；生产局域网验收时导入 `dist/release/mp-weixin-prod-lan`。


## 0.3 2026-07-21 反馈工单受理规则更新

- 可提交反馈：机构管理员、医生、健康管理师、普通客户；每位提交人只能查看自己的工单与平台回复。
- 不可提交反馈：平台管理员。服务端会直接拒绝平台管理员对创建工单接口的调用，前端也不再显示“帮助与反馈”提交入口。
- 平台管理员新增“反馈中心”：可跨机构查看未删除工单、填写回复，并将状态更新为处理中、已回复或已关闭；不向医生、健康管理师、机构管理员开放工单回复权限。
- 已验证完整权限链路：平台创建返回 403；机构管理员创建后平台可查看和回复；提交人可立即读取平台回复。Java Docker 构建共 57 项测试通过。


## 0. 2026-07-21 十二模型与 DeepSeek 更新（本节优先于下方历史快照）

- Python 规则引擎由五模型扩充为十二模型：糖代谢、血脂心血管、慢性炎症、肝脏代谢、肾脏与电解质、血液与贫血、甲状腺、性激素、HPA/肾上腺、营养与微量元素、肠道屏障、重金属暴露。
- 规则版本升级为 `RULE_3.0.0`。报告自带参考区间优先，只有单位匹配时才使用内置开发阈值；每个模型设置最小指标数，数据不足返回 `INSUFFICIENT_DATA`，不再误报为低风险。
- 已接入 `deepseek-v4-flash` 生成结构化跨模型解读。DeepSeek 只做解读，不修改可追溯规则分数，也不能绕过医生审核与正式发布流程。
- 外发数据最小化：仅传年龄、性别、指标代码/值/单位/参考区间和规则结果；不传患者 ID、姓名、手机号、OpenID、任务 ID、原始报告或 MinIO 地址。
- DeepSeek 关闭、超时、网络失败或 JSON 校验失败时自动使用 `RULE_FALLBACK`，十二模型评分、审核和发布主流程仍可继续。
- DeepSeek 密钥只配置在被 Git 忽略的根目录 `.env`，示例文件保持空值；禁止把密钥写入源码、前端、数据库迁移、日志或文档。
- Flyway 新增 `V13__twelve_model_catalog.sql`，把旧模型配置标记为历史版本并登记十二个 `3.0.0` 模型。已存在的历史评估不会自动重算，需从新确认的报告发起新评估。
- 本轮按产品方要求不补测试、不跑完整回归，只执行 Python 启动检查、一次虚构数据的 DeepSeek 连通验证、Java 跳过测试打包以及 H5/微信小程序构建。

## 0.1 2026-07-20 五角色终检更新（历史终检快照）

下方第 1～15 节保留了早期 MVP 的开发背景，其中“Docker 未运行”“多租户、数据库用户、任务恢复、审计、五模型、PDF 尚未完成”等描述已经过期。当前最新状态如下：

- 平台管理员、机构管理员、医生、健康管理师、普通客户五角色的登录、菜单、权限、数据范围和主要业务链均已完成 API 与构建回归。
- 数据库用户/RBAC、双租户隔离、任务恢复、隐私授权、审计、五模型评估、医生编辑审核、PDF 报告、趋势和随访计划已经落地。
- 客户的 `DATA_COLLECTION`、`HEALTH_ASSESSMENT`、`DATA_SHARING` 授权已经接入真实业务门禁；撤回专业协作授权后，机构人员立即无法继续访问该客户。
- 真实链路已覆盖：PDF 上传 → MinIO → PaddleOCR → 人工确认 → 五模型评估 → 医生编辑/审核 → PDF 发布/下载 → 随访计划/反馈。
- 最终 Java Docker 构建：52 项测试通过；Python：7 项测试及 Ruff、MyPy、Black 通过；小程序 TypeScript、ESLint、Prettier、H5、微信小程序构建全部通过。
- MySQL、Redis、MinIO、Java、Python、Nginx 当前均为 healthy；H5、Java 健康接口、Python 健康接口均返回 HTTP 200。
- 仍需外部环境完成的上线验收：真实微信 AppID/AppSecret、合法 HTTPS 域名、微信开发者工具与真机授权/文件预览验证，以及医学规则、隐私政策文本和生产运维合规审批。

后续维护人员应以第 0 节、当前源码、Flyway V1～V13 和最新验证记录为准，不要按下方早期“未完成列表”重复实现已经交付的功能。

> 最后更新：2026-07-20  
> 项目根目录：`E:\health`  
> 当前分支：`main`  
> 当前提交：`f47952e Implement PaddleOCR-based asynchronous report recognition`  
> Git 状态：`main` 与 `origin/main` 一致；除本次新增的 `handoff.md` 外无其他工作区改动  
> 当前运行状态：Docker Desktop 此刻未启动；下文的端到端结果是 2026-07-19 的最后一次实际验证结果。

## 1. 一句话现状

项目已经具备“单个微信小程序同时承载 B 端和 C 端”的可运行主体框架，并跑通了真实报告上传、MinIO 私有存储、PaddleOCR 异步识别、人工校对、演示健康评估、医生审核、报告发布和基础随访的最小闭环。

但是，它目前仍是开发验证版，不是可直接上线的医疗或健康 SaaS 产品。最需要优先处理的是多租户隔离、真实用户/机构体系、可靠异步任务、正式评估规则、隐私审计和测试覆盖。

## 2. 不得改变的项目约束

1. 项目根目录就是 `E:\health`，不要再套一层项目目录。
2. 只维护一个 uni-app 微信小程序，同时承载 B 端和 C 端。
3. 不创建独立 Vue 管理后台；机构业务也在同一个小程序中完成。
4. Java 是业务规则、权限、事务和业务数据的唯一入口。
5. Python 只负责 OCR、指标标准化、评估计算和文本生成，不直接访问 MySQL。
6. MySQL、Redis、MinIO、Java、Python、Nginx 全部 Docker 化。
7. Docker 镜像、容器和命名卷应通过 Docker Desktop 的磁盘镜像位置落到 E 盘。
8. MySQL 使用 Docker 命名卷，不直接把 `/var/lib/mysql` 绑定到 Windows NTFS。
9. 报告原文件只能放 MinIO 私有 Bucket；MySQL 只保存元数据和对象路径。
10. OCR 结果必须经过人工确认，不能直接进入健康评估或对客户发布。
11. 当前系统只能输出健康管理参考，不得输出疾病诊断、药物处方或自动诊疗结论。
12. 所有外显评估结果必须保留免责声明：`该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。`

## 3. 当前架构

```text
微信小程序 / H5
        |
      Nginx
        |
        +---- Spring Boot / Java 21 ---- MySQL 8.4
        |              |                Redis 7
        |              +--------------- MinIO
        |
        +---- FastAPI / Python 3.12 / PaddleOCR
```

- Java：模块化单体，负责认证、数据范围、客户、报告、OCR 任务编排、评估工作流、审核、发布和随访。
- Python：FastAPI 服务，负责 PaddleOCR、有限指标归一化、演示规则计算和演示文字生成。
- 小程序：uni-app + Vue 3 + TypeScript + Pinia，同一入口按工作台和权限展示 B/C 端功能。
- 数据库：Flyway 管理，当前迁移为 `V1` 至 `V4`。
- 对象存储：MinIO 私有 `rayk-reports` Bucket，下载使用短时预签名 URL。
- 异步 OCR：Spring 事务提交后发布事件，独立线程执行 Python OCR 调用。

## 4. 已实现功能

### 4.1 工程与基础设施——已完成主体框架

- Java、Python、小程序、MySQL、Redis、MinIO、Nginx 的目录和构建体系已建立。
- Docker Compose 提供基础、开发和生产形态覆盖文件。
- Java 和 Python 使用国内镜像源；Paddle 模型使用可配置模型源。
- Nginx 提供统一入口，H5 静态文件、Java API、Python API 分路由代理。
- MySQL、Redis、MinIO、Java、Python、Nginx 均有健康检查。
- Flyway 自动初始化数据库结构和开发种子数据。
- 提供 PowerShell/Shell 启停、构建、MySQL 备份和恢复脚本。
- `.gitignore` 已覆盖 Java、Python、Node、微信开发者工具、VS Code、密钥、Docker 数据和系统文件。

### 4.2 单小程序 B/C 端框架——已完成

- 固定 TabBar：首页、工作台、消息、我的。
- 主包与客户、机构业务、机构管理分包已经建立。
- 支持 `PLATFORM_ADMIN`、`TENANT_ADMIN`、`DOCTOR`、`HEALTH_MANAGER`、`CUSTOMER` 工作台。
- 支持工作台切换、菜单权限控制、401、403、网络错误和通用状态页。
- B/C 端使用同一套报告 OCR 校对组件。
- 已完成一轮视觉美化，包括卡片、状态标签、步骤条、移动表单和底部操作区。

### 4.3 身份认证——开发闭环已完成，生产体系未完成

- 支持开发账号密码登录，JWT 登录态存入 Redis。
- 支持小程序 `wx.login` 获取 code。
- Java 已实现微信 `code2Session`、openid/unionid 解析、绑定表和登录接口。
- 支持已登录用户绑定微信身份。
- 开发环境支持固定 openid 和自动绑定 `customer` 账号。
- 生产覆盖会关闭微信模拟模式。

当前限制：用户、角色、权限和工作台仍主要来自 Java 内存中的 `MockUserCatalog`，不是完整的数据库用户中心。

### 4.4 客户档案与数据范围——基础能力已完成

- 客户档案新增、列表、详情。
- 手机号入库前脱敏。
- 客户、医生、健康管理师按照当前工作台筛选可访问客户。
- 报告、评估、健康报告和随访复用客户数据范围校验。

当前限制：没有编辑、停用、转移、标签、分级、完整健康档案和批量操作；多租户过滤还存在 P0 缺口，见第 7 节。

### 4.5 真实报告上传与文件管理——已完成 MVP

- 支持 PDF、JPG、PNG，最大 20 MB。
- 同时校验扩展名、MIME 和文件内容特征。
- 文件写入 MinIO 私有 Bucket。
- MySQL 保存对象路径、原文件名、MIME、大小和 SHA-256。
- 支持文件列表和短时预签名下载。
- 小程序支持选择、上传、查看原报告和进入识别流程。

### 4.6 PaddleOCR 异步识别——已完成并真实验证

- Python 已接入 PaddleOCR 3.7.0 和 PP-OCRv6 small CPU 模型。
- PaddlePaddle 固定为 3.2.2。
- Paddle 模型缓存放在独立命名卷 `rayk_ocr_models_data`。
- Java 建立 OCR 任务，支持 `PENDING`、`PROCESSING`、`SUCCESS`、`FAILED`。
- 报告同步维护 `OCR_PENDING`、`OCR_PROCESSING`、`OCR_FAILED`、`WAITING_CONFIRMATION` 等状态。
- 支持任务查询、置信度、结果快照、失败原因、执行次数和人工重试。
- 小程序轮询展示识别进度，并提供失败重试。
- 已支持一组常用指标别名和通用行解析。
- 支持人工增删、修改名称、数值、单位和参考范围。
- 人工确认后指标统一标记为 `manuallyConfirmed=true`。

最后一次真实回归结果：

- 状态链：`PENDING -> PROCESSING -> SUCCESS -> CONFIRMED`
- 整体置信度：`97.22%`
- 成功抽取 3 项：糖化血红蛋白、低密度脂蛋白、C 反应蛋白
- OCR 引擎：`PaddleOCR-3.7.0/PP-OCRv6-small`

### 4.7 健康评估、审核、发布——演示闭环已完成

- 已确认指标可以提交 Python 评估。
- Java 保存 AI 任务、评估快照和免责声明。
- 自动创建医生待审核任务。
- 医生可以查看、通过或退回评估。
- 审核通过后可以发布健康报告。
- 客户只能查询已发布健康报告。

当前限制：Python 只有糖代谢和炎症两个演示规则，不是正式五大模型；健康报告是结构化演示数据，没有正式 PDF。

### 4.8 随访——基础闭环已完成

- 机构人员可以创建随访任务。
- 可以查询任务、标记完成。
- 客户可以提交随访反馈。

当前限制：没有自动计划、提醒、重复任务、逾期升级、量表、附件或消息通知。

### 4.9 已有测试与验证

截至 2026-07-19，最后一次完整验证结果：

- Java Docker Maven 构建成功，3 个测试通过。
- Python 6 个测试通过。
- Python Ruff、MyPy、Black 检查通过。
- 小程序 `type-check`、ESLint、Prettier 检查通过。
- H5 和 `mp-weixin` 构建通过。
- Compose 配置检查通过。
- 全部容器当时健康，H5 首页返回 200。
- 真实 MinIO 上传、PaddleOCR、状态轮询、人工保存和确认通过。

当前 2026-07-20 Docker Desktop 未运行，因此交接时没有重新启动容器。

## 5. 功能完成度估算

以下是按原始产品目标估算，不是严格的工时或 Story Point 统计：

| 范围 | 估算完成度 | 说明 |
|---|---:|---|
| 可运行技术主体框架 | 约 85% | 核心工程、Docker、单小程序和服务通信已可运行 |
| 最小业务闭环 MVP | 约 65% | 上传、OCR、确认、演示评估、审核、发布、随访已串通 |
| 原始完整经营体系 | 约 30%～35% | 五大模型、干预方案、会员、支付、商城和分润尚未实现 |
| 生产可用与合规 | 约 20% | 隔离、安全、隐私、审计、可靠任务、监控和测试仍需大量工作 |

按功能域统计：

- 相对完整或已跑通：7 个功能域。
- 已有页面/API 但仍是演示或基础版本：8 个功能域。
- 尚未开发的主要功能包：至少 13 个。

不要用“页面已经存在”判断功能完成。消息、趋势、审计、员工等页面中仍有静态演示数据。

## 6. 尚未开发或需要重做的功能

### 6.1 P0：继续扩功能前必须处理

1. **严格多租户隔离**
   - `DataScopeService.scopedPatients()` 当前没有强制添加 `tenant_id = 当前租户`。
   - 机构管理员工作台存在跨租户看到其他客户的风险。
   - 所有核心查询、更新和文件访问都要补充租户条件并增加越权测试。

2. **数据库化用户、机构、角色和权限体系**
   - 替换 `MockUserCatalog`。
   - 实现用户注册/邀请、员工管理、角色授权、账号状态、解绑和离职交接。
   - 微信身份应绑定真实数据库用户，而不是开发内存账号。

3. **可靠异步任务**
   - 当前 OCR 使用进程内 `@Async + AFTER_COMMIT`，服务宕机会留下 PENDING/PROCESSING 任务。
   - 需要任务恢复扫描、超时、幂等、并发锁和重试策略。
   - 正式环境建议使用数据库 Outbox、Redis Stream 或消息队列。

4. **隐私、授权和审计**
   - 健康数据采集授权、隐私政策版本、撤回授权、数据导出/删除流程。
   - 敏感字段加密、密钥管理、访问审计、跨租户审计。
   - 当前审计页面只是静态结构。

5. **安全与测试基线**
   - 补充租户隔离、角色权限、对象越权、文件越权和状态机测试。
   - 增加 Java 集成测试、Python OCR 边界测试和小程序端到端测试。
   - 增加限流、登录防爆破、上传恶意文件检查和依赖漏洞扫描。

### 6.2 P1：产品核心能力

1. 指标字典、别名、单位换算、性别/年龄参考区间和版本管理。
2. 复杂表格、跨行、旋转、扫描模糊、多页 PDF 的 OCR 质量提升。
3. OCR 低置信度逐字段提示，而不仅是报告整体置信度。
4. 五大正式评估模型：
   - 糖代谢失衡。
   - 慢性炎症与血管老化。
   - HPA 肾上腺压力轴。
   - 甲状腺与性激素内分泌。
   - 重金属、肝脏解毒和肠道屏障。
5. 规则配置、版本、适用人群、证据链、缺失数据和回溯计算。
6. 个性化饮食、营养、运动、作息、减压和复查计划。
7. 医生对评估结果和方案逐项编辑，不只是通过/退回。
8. 正式 PDF 健康报告生成、模板、签名、版本和下载。
9. 真实历史指标趋势图、同指标单位归一、时间筛选和异常区间。
10. 客户健康档案扩展：身高、体重、病史、过敏、用药、生活方式和问卷。
11. 自动随访计划、到期提醒、逾期处理和效果复评。
12. 微信订阅消息与真实消息中心。

### 6.3 P2：经营与平台能力

1. 机构档案、员工 CRUD、岗位、套餐、账号数和 SaaS 到期管理。
2. 平台机构管理和跨租户审计查询。
3. 检验机构接口和批量数据导入。
4. 智能穿戴数据：睡眠、HRV、心率和压力。
5. 会员套餐、订单、微信支付、退款和发票。
6. 商品、营养素、库存、供应链和配送。
7. 机构分润、渠道佣金、结算和对账。
8. 招商和培训支持模块。
9. 运营报表和业务数据看板。
10. 私有化部署工具、租户初始化和升级方案。
11. TLS、域名、CDN/WAF、日志平台、指标监控和告警。
12. 备份自动化、恢复演练、容灾和数据保留策略。
13. 灰度发布、回滚、数据库兼容性检查和 CI/CD。

## 7. 当前已知风险和技术债

### 7.1 严重：租户过滤不完整

`DataScopeService` 目前主要按客户本人、分配医生或分配健康管理师过滤，没有统一附加当前 `tenant_id`。机构管理员没有额外条件时可能拿到所有租户客户。

修复原则：

- 每个业务查询首先限定 `tenant_id` 和 `deleted=0`。
- 客户/医生/健康管理师的数据范围是在租户条件之上的二次收窄。
- 更新和删除也必须使用租户条件，不能只按全局 ID。
- 文件、报告、评估、审核、发布、随访全部增加跨租户自动化测试。

### 7.2 微信身份仍依赖开发账号目录

`code2Session` 和绑定表是真实实现，但绑定成功后仍通过 `MockUserCatalog.findByUserId()` 取得账号、角色和权限。没有数据库用户就不能算正式身份体系。

### 7.3 首页、机构和部分页面是假数据

- 首页数量是硬编码的 1 或 80。
- 机构资料和员工接口返回固定开发数据。
- 消息中心是静态两条消息。
- 趋势页是静态三个月空腹血糖。
- 审计页只有说明文字。

### 7.4 评估和报告仍是演示实现

- `DemoRuleEngine` 只有空腹血糖和 C 反应蛋白两个简单阈值规则。
- 没有正式五大模型和经过评审的规则版本。
- 发布报告只有数据库摘要，没有 PDF 文件。
- Python 的 `DemoReportService` 尚未接入 Java 发布流程。

### 7.5 异步 OCR 不是持久化队列

- 当前线程池只有当前 Java 进程内的执行能力。
- 容器重启后不会自动恢复处理中任务。
- 相同报告的并发重试只靠应用查询，缺少数据库级互斥。

### 7.6 测试数量不足

- Java 目前只有 3 个单元测试。
- Python 目前只有 6 个测试。
- 小程序没有组件测试和真实自动化 UI 测试。
- 没有完整的多租户、状态机、并发、失败恢复和安全回归套件。

### 7.7 生产 Compose 不是完整生产部署

虽然 `compose.prod.yml` 会减少宿主机暴露端口并关闭微信 mock，但仍缺 TLS、外部密钥管理、告警、集中日志、备份调度、容灾和发布回滚。

## 8. 已踩过的坑：绝对不要再踩

### 8.1 不要随意升级 PaddlePaddle 到 3.3.x

实际使用 PaddlePaddle 3.3.1 时，PP-OCRv6 CPU 推理触发 oneDNN/PIR 属性转换错误：

```text
ConvertPirAttribute2RuntimeAttribute not support
[pir::ArrayAttribute<pir::DoubleAttribute>]
```

当前已固定：

```text
paddleocr==3.7.0
paddlepaddle==3.2.2
```

升级前必须在当前 Docker CPU 环境跑真实图片和多页 PDF 回归，不能只看安装成功或健康接口。

### 8.2 不要把外部 OCR 调用包在一个长数据库事务里

最初 OCR 事件处理把状态更新和几分钟的模型调用放在同一事务中，导致小程序一直只能看到 `PENDING`，直到识别完成才一次性看到 `SUCCESS`。

正确方式：

1. 短事务提交 `PROCESSING`。
2. 事务外调用 Python/MinIO。
3. 短事务保存 `SUCCESS` 或 `FAILED`。

不要把它改回单个大 `@Transactional` 方法。

### 8.3 `@TransactionalEventListener(AFTER_COMMIT)` 的事务传播要谨慎

曾因事件监听器事务配置不合法导致 Spring Boot 启动失败。当前实现通过 `TransactionTemplate` 显式划分短事务。修改异步逻辑后必须重新启动完整 Java 容器，不能只依赖编译通过。

### 8.4 不要删除 OCR 模型命名卷

PaddleOCR 首次运行需要下载模型，耗时较长。模型缓存位于：

```text
rayk_ocr_models_data
```

不要执行：

```text
docker compose down -v
docker system prune --volumes
```

否则模型、MySQL、Redis 和 MinIO 数据都可能被删除。

### 8.5 不要直接绑定 MySQL 数据目录到 Windows NTFS

MySQL 数据文件在 Windows bind mount 上可能出现性能、权限和一致性问题。继续使用 Docker 命名卷，并通过 Docker Desktop 把整个 Docker 磁盘镜像迁到 E 盘。

### 8.6 不要用裸 `docker compose up` 代替开发启动命令

基础 `compose.yml` 本身不保证暴露所有开发端口。应使用：

```powershell
.\scripts\start-dev.ps1
```

或：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
```

裸启动或只重建单个服务可能使 Java、AI 等宿主机端口消失。

### 8.7 Nginx 404 往往不是后端坏了，而是 H5 没构建

Nginx 根目录挂载的是：

```text
rayk-miniapp\dist\build\h5
```

第一次启动或清理 `dist` 后，要先执行：

```powershell
Set-Location rayk-miniapp
npm run build:h5
```

之后再启动或重载 Nginx。

### 8.8 不要误以为 `wx.login` 必须弹微信授权界面

`wx.login` 获取临时 code 通常不会弹头像、昵称或手机号授权框。开发环境还默认启用固定 openid 和自动绑定，所以会看起来“直接登录”。

需要真实微信链路时：

- 使用微信开发者工具或真机，不要只在 H5 浏览器测试。
- 配置真实 AppID/AppSecret。
- 关闭 `WECHAT_MOCK_ENABLED`。
- 清空 `WECHAT_AUTO_BIND_USERNAME`。
- 头像、昵称、手机号授权属于另外的用户交互流程。

### 8.9 不要把 MinIO Bucket 改成公开

- Java 到 Python OCR 应使用容器网络可访问的内部短时 URL。
- 小程序预览原报告应使用客户端可访问的公共端点生成短时 URL。
- 生产必须配置 HTTPS 合法域名。
- 不要把对象永久公开，也不要把 MinIO 内部地址直接返回给小程序。

### 8.10 不要让前端决定权限或租户

小程序权限只用于隐藏按钮。服务端必须从 JWT/安全上下文获取用户和租户，并再次检查角色、资源归属和客户范围。

绝对不要接受小程序传来的 `tenantId` 作为可信条件。

### 8.11 不要在前端把数据库 BIGINT ID 转成 JavaScript Number

当前生成的 ID 已超过 JavaScript 安全整数范围。Java VO 把 ID 返回为字符串，小程序类型也使用 `string`。

新增接口必须继续使用字符串 ID，不得使用 `Number(id)`、`parseInt(id)` 或数学运算处理主键。

### 8.12 不要修改已执行的 Flyway 迁移

`V1`～`V4` 已在开发数据库执行。后续结构变更只能新增 `V5__...sql`、`V6__...sql`，不能直接改旧迁移，否则会造成 checksum 失败。

遇到 Flyway 错误先查日志和迁移内容，不要通过删除 MySQL 卷“解决”。

### 8.13 不要提交 `.env`、密码、AppSecret 或 API Key

- 仓库只提交 `.env.example`。
- 不把真实密钥写进 Compose、README、截图、日志或测试用例。
- 不要在终端命令中打印数据库和 MinIO 密码。

### 8.14 不要绕过人工确认和医生审核

OCR 有误识别风险，健康评估也只是辅助工具。任何自动识别指标必须人工确认；任何健康报告必须经过专业人员审核后才允许发布。

### 8.15 不要把当前演示规则包装成正式医学模型

当前规则没有完成医学证据评审、适用人群定义、阈值版本、回溯验证和合规审批。UI、接口和报告中必须继续标注演示和免责声明。

## 9. 关键状态机

### 9.1 检验报告

```text
UPLOADED
  -> OCR_PENDING
  -> OCR_PROCESSING
  -> WAITING_CONFIRMATION
  -> CONFIRMED
  -> AI_PROCESSING
  -> REVIEWING
  -> PUBLISHED
```

异常分支：

```text
OCR_PROCESSING -> OCR_FAILED -> retry -> OCR_PENDING
AI_PROCESSING  -> FAILED
REVIEWING      -> REJECTED -> 重新处理/再次审核
```

新增功能时必须显式限制允许的前置状态，不能允许跨步骤直接发布。

### 9.2 OCR 任务

```text
PENDING -> PROCESSING -> SUCCESS
                      -> FAILED -> 新任务重试
```

每次重试会新增任务并增加 `attemptCount`，不要覆盖历史任务。

## 10. 关键代码入口

| 领域 | 主要文件 |
|---|---|
| Java OCR 编排 | `rayk-server/src/main/java/com/rayk/health/laboratory/application/OcrTaskService.java` |
| Java 文件上传 | `rayk-server/src/main/java/com/rayk/health/laboratory/application/LabReportFileService.java` |
| Java 主业务闭环 | `rayk-server/src/main/java/com/rayk/health/assessment/application/WorkflowApplicationService.java` |
| 数据范围 | `rayk-server/src/main/java/com/rayk/health/patient/application/DataScopeService.java` |
| 微信登录 | `rayk-server/src/main/java/com/rayk/health/security/wechat/WeChatAuthService.java` |
| Python OCR | `rayk-ai/app/ocr/service.py` |
| Python 演示规则 | `rayk-ai/app/scoring/engine.py` |
| 小程序 OCR 页面 | `rayk-miniapp/src/components/OcrIndicatorEditor.vue` |
| 小程序登录状态 | `rayk-miniapp/src/stores/auth.ts` |
| Docker 编排 | `compose.yml`、`compose.dev.yml`、`compose.prod.yml` |
| 数据库迁移 | `database/migrations/` |
| Nginx | `deploy/nginx/nginx.conf` |

## 11. 关键 API

### 11.1 认证

- `POST /api/v1/auth/mock-login`
- `POST /api/v1/auth/wechat-login`
- `POST /api/v1/auth/wechat-bind`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/profile`

### 11.2 报告、OCR 和指标

- `POST /api/v1/lab-reports/upload`
- `GET /api/v1/lab-reports/{id}`
- `GET /api/v1/lab-reports/{id}/files`
- `POST /api/v1/lab-reports/{id}/files/{fileId}/download-url`
- `GET /api/v1/lab-reports/{id}/ocr-task`
- `POST /api/v1/lab-reports/{id}/ocr-task/retry`
- `PUT /api/v1/lab-reports/{id}/indicators`
- `POST /api/v1/lab-reports/{id}/confirm`
- `POST /api/v1/lab-reports/{id}/submit-ai`

### 11.3 评估、审核、发布和随访

- `GET/POST /api/v1/assessments`
- `GET /api/v1/reviews/tasks`
- `POST /api/v1/reviews/tasks/{id}/approve`
- `POST /api/v1/reviews/tasks/{id}/reject`
- `POST /api/v1/reviews/tasks/{id}/publish`
- `GET /api/v1/health-reports`
- `GET/POST /api/v1/followups`
- `PUT /api/v1/followups/{id}/complete`
- `POST /api/v1/followups/{id}/feedback`

完整契约以启动后的 Swagger 和 FastAPI Docs 为准。

## 12. 启动与验证

### 12.1 启动前

1. 启动 Docker Desktop。
2. 确认 Docker Desktop Disk image location 在 E 盘。
3. 确认根目录存在本机 `.env`，不要提交它。
4. 首次启动前构建 H5。

### 12.2 开发启动

```powershell
Set-Location E:\health\rayk-miniapp
npm install
npm run build:h5

Set-Location E:\health
.\scripts\start-dev.ps1
```

### 12.3 微信开发者工具

```powershell
Set-Location E:\health\rayk-miniapp
npm run build:mp-weixin
```

导入目录：

```text
E:\health\rayk-miniapp\dist\build\mp-weixin
```

### 12.4 常用地址

| 服务 | 地址 |
|---|---|
| H5/Nginx | `http://localhost:8088` |
| Java API | `http://localhost:8080/api/v1` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| FastAPI | `http://localhost:8000/health` |
| FastAPI Docs | `http://localhost:8000/docs` |
| MinIO Console | `http://localhost:9001` |

### 12.5 开发测试账号

统一开发密码：`RayK@123456`

```text
platform_admin
tenant_admin
doctor
health_manager
customer
```

这些仅是开发账号，不得用于生产。

### 12.6 最低验证命令

```powershell
Set-Location E:\health
docker compose -f compose.yml -f compose.dev.yml config --quiet
docker compose -f compose.yml -f compose.dev.yml build

docker build -f rayk-ai/Dockerfile.test -t rayk-a1-ai:test rayk-ai
docker run --rm rayk-a1-ai:test

Set-Location E:\health\rayk-miniapp
npm run type-check
npm run lint
npx prettier --check src
npm run build:h5
npm run build:mp-weixin
```

至少再做一次真实 PDF/JPG/PNG 上传，确认状态可观察为：

```text
PENDING -> PROCESSING -> SUCCESS -> CONFIRMED
```

## 13. 数据和环境注意事项

- 开发种子数据在 `V2__development_seed.sql`。
- 如果沿用 2026-07-19 的 Docker 命名卷，数据库中还可能保留两条 OCR 回归测试报告。
- 其中一条名为“OCR状态链回归测试”，已完成到 `CONFIRMED`。
- 清理这些业务测试数据前先备份数据库，不要删除整个 MySQL 卷。
- 默认逻辑备份位置：`E:\DockerData\RayKA1\backups\mysql`。
- Paddle 首次识别会下载模型；首次调用较慢属于正常现象。
- Java OCR 读取超时当前默认 180 秒，不要轻易降低到普通 HTTP 接口的几秒钟。

## 14. 推荐的下一阶段开发顺序

建议下一个开发周期只做以下顺序，不要先做支付或商城：

1. 修复所有查询和写操作的 `tenant_id` 隔离，并补自动化越权测试。
2. 建立数据库化用户、机构、员工、角色、权限和微信绑定体系。
3. 为 OCR/评估任务增加恢复扫描、超时、幂等和数据库级并发保护。
4. 建立指标字典、单位换算、参考区间和规则版本模型。
5. 实现五大评估模型的配置框架，但先保持“开发规则”标识。
6. 扩展医生审核为逐项编辑和确认。
7. 生成正式版式的 PDF 健康报告并存入 MinIO。
8. 接入真实趋势图和自动随访计划。
9. 补齐隐私授权、审计、加密、监控、备份恢复演练。
10. 以上稳定后再进入订阅消息、会员、支付、商城和机构分润。

## 15. 下一阶段验收标准

下一阶段不能只以“页面能打开”为完成标准。至少应满足：

- 不同租户之间的客户、文件、报告、评估和随访完全不可互访。
- 真实数据库用户可以通过微信登录并获得数据库角色和权限。
- Java 重启后能恢复遗留的 OCR/评估任务。
- 相同报告重复提交不会产生重复有效结果。
- 规则和报告能追溯到明确版本。
- 医生修改的内容有操作者、时间和前后值审计。
- PDF 报告与结构化数据一致，并保留免责声明。
- Java、Python、小程序构建通过，关键闭环有自动化回归。
- 生产配置不使用默认密码，不暴露数据库和内部服务端口。

---

交接结论：当前仓库适合继续开发和产品验证，但在完成 P0 多租户隔离、真实用户体系、可靠任务和隐私审计之前，禁止接入真实机构和真实健康数据，也禁止按生产系统对外发布。
