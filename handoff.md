# 三羊健康项目交接说明

> 本文只记录当前代码和运行环境的真实状态。历史讨论、已废弃方案和逐次排障过程不在此保留。接手前请同时阅读根目录 `AGENTS.md`、`README.md`，并执行 `git status --short`。

## 1. 当前产品边界

- 产品名称：三羊健康。
- 项目根目录：`E:\health`。
- 客户端：一个 UniApp 微信小程序，同时提供 H5 调试界面；不建设独立 Vue 管理后台。
- 后端分工：Java 负责业务、权限、数据和流程；Python 负责 OCR、AI 评估、RAG、报告与随访智能处理。
- 基础设施：MySQL、Redis、MinIO、Java、Python、Nginx 全部 Docker 化。
- 当前仅保留三个角色：平台管理员、医生、普通客户。
- 2026-08-05 生产部署：腾讯云 Ubuntu 服务器 `62.234.44.238` 已安装 Docker/Compose 并运行生产 Compose；当前生产域名切换为 `xingxuyuan.com` 与 `www.xingxuyuan.com`，Nginx 使用 `crets/xingxuyuan.com_nginx` 证书目录并监听 80/443，H5 与 Java/Python/MinIO 经过容器健康检查。生产微信 AppSecret 只写入服务器 `/opt/zhiyu-health/.env`（未进入 Git）；该密钥曾在对话中暴露，上线后应立即在微信平台轮换。
- 2026-08-05 外部访问阻断：旧域名已废弃；新域名 `xingxuyuan.com` 需完成 DNS 解析、备案/风控放行和公网 HTTPS 验收后再进行微信真机验收。
- 2026-08-05 内部验收包：`E:\health\rayk-miniapp\dist\release\mp-weixin-internal` 使用 `https://62.234.44.238` 访问生产服务器，仅用于微信开发者工具模拟器内测；需勾选“不校验合法域名、TLS 版本及 HTTPS 证书”，不得上传或作为正式包分发。正式包使用 `https://xingxuyuan.com`。
- 2026-08-07 本地 H5 启动修复：`rayk-ai/app/main.py` 的 PaddleOCR 预热改为后台线程，避免阻塞 AI `/health`，导致 Java 与 Nginx 无法启动；Docker Desktop 启动后执行 `docker compose -f compose.yml -f compose.dev.yml up -d`，本地 H5 入口为 `http://localhost:8088/`。

## 2. 三个角色及权限

### 平台管理员 `PLATFORM_ADMIN`

- 创建、编辑、启停合作医院。
- 为医院预录入、修改和删除医生姓名及手机号。
- 查看全平台已发布健康报告、原体检报告、PDF 健康报告和健康随访动态。
- 查看健康随访任务详情及客户完成反馈。
- 查看并回复医生或客户提交的问题反馈。
- 不作为体检客户使用，不显示身份切换入口。

### 医生 `DOCTOR`

- 按姓名或手机号筛选体检者。
- 查看体检者健康档案、原体检报告、AI 评估、已发布健康报告和健康随访。
- 查看和下载 PDF 健康报告，作为健康管理与诊断参考。
- 当前试运行阶段可读取全平台体检者，属于明确的临时策略；正式上线前必须决定是否恢复医院数据隔离。
- 医生可以切换到个人客户工作台，以本人身份使用客户功能。
- 当前没有强制“医生审核后发布”的流程门禁，医生以查看和辅助判断为主。

### 普通客户 `CUSTOMER`

- 仅查看和操作本人数据。
- 维护健康档案与健康问卷，档案完整度按真实填写情况动态计算。
- 上传 PDF 或图片体检报告，查看识别进度、结构化结果和原报告。
- 查看 AI 评估、已发布健康报告、下载 PDF 健康报告。
- 查看指标趋势、进行面部健康检测。
- 执行健康随访任务，逐项提交完成情况、身体感受、困难原因和文字反馈。
- 设置吃饭与睡觉提醒，试听已配置的语音。
- 提交问题反馈并只查看本人反馈及平台回复。
- 不显示身份切换入口。

权限必须由 Java 后端强制执行，前端隐藏卡片只用于改善体验，不能替代数据鉴权。

## 3. 当前完整业务闭环

1. 用户通过微信身份进入系统；本地开发可使用 Mock 登录。
2. 用户维护姓名、手机号、性别、出生日期、身高、体重、腰围、既往史、家族史、生活方式及健康问卷。
3. 用户上传 PDF 或图片体检报告。
4. OCR 按文件类型进入独立路径，提取原分类、原顺序、原内容、数值结果、非数值结果和类目小结。
5. Python 对结果做指标标准化，结合健康档案、问卷、既往史和知识库进行规则评估、RAG 检索及 DeepSeek 综合解读。
6. 系统生成并发布同一份健康评估报告，客户、医生和平台管理员按数据范围查看；报告可下载为 PDF。
7. 系统根据报告生成健康随访计划，包含可执行的饮食、运动、作息、监测、微量营养建议及一周食谱。
8. 客户逐项反馈完成情况，并补充身体感受、困难原因和自由文字。
9. 规则护栏与大模型共同判断下一期继续、调整或终止，避免无限生成重复任务。
10. 历史体检指标、面部健康检测和随访反馈进入趋势及下一次评估上下文。

产品输出属于健康管理和辅助参考，不代替临床诊断或医疗设备测量。

## 4. 已实现功能

### 账户与组织

- 微信登录：企业主体客户、预录入医生和平台管理员都通过授权手机号识别；客户手机号未预录入时自动创建最低权限 CUSTOMER，医生手机号在医院管理页预录入，管理员手机号在平台工作台预录入。首次管理员登录若数据库尚无手机号，可在部署环境临时设置 `WECHAT_PLATFORM_ADMIN_PHONE` 与 `WECHAT_PLATFORM_ADMIN_USERNAME`，成功进入平台工作台后再维护；真实手机号只能放在部署密钥中。生产服务通过 `WECHAT_PHONE_LOGIN_REQUIRED=true` 禁止未验证手机号的 OpenID 回退；本地 compose.dev 保留兼容路径和三角色 Mock 调试。
- 2026-08-06 生产平台管理员账号已通过数据库迁移 `V31__reset_platform_admin_credentials.sql` 完成重置，账号记录（用户 ID 10001）现为 `admin` 且状态为 ACTIVE；生产 Java 容器已重建并通过健康检查。重置前数据库备份保存在服务器 `/opt/zhiyu-health/backups/`。
- 微信 `code2session` 请求参数已进行 URL 编码，避免真实登录凭证中的特殊字符被错误解析；微信接口返回的 `text/plain` JSON 现改为先读取文本再显式解析，已部署到生产 Java 服务并通过生产健康检查。登录实测仍需在微信开发者工具中使用一次新的有效凭证完成验证；本次仅跳过了与登录无关的既有语音提醒测试。
- 微信小程序登录卡片不再显示账号密码、绑定码或首次绑定入口；三类用户统一点击微信手机号授权按钮登录。H5/开发包的开发调试入口仍可使用测试账号，不代表正式登录方式。
- 微信登录按钮已增加加载态并发保护，避免重复提交同一个一次性登录凭证导致 `invalid code`。
- 2026-08-05 生产服务已验证健康；若微信开发者工具将 `wechat-login` 显示为 0 B/网络失败且 Nginx 无访问记录，问题发生在客户端 DNS/TLS 或导入了错误构建包，应先核对请求地址为 `https://xingxuyuan.com/api/v1/auth/wechat-login`。
- 登录成功提示只展示“客户、医生、管理员”三类身份；客户“我的”页面优先显示健康档案姓名。
- 退出登录会先清理本地会话，再尽力通知后端注销；退出期间完成的旧请求不再触发“登录状态已过期”跳转。若同一微信 OpenID 已自动绑定客户，再尝试绑定平台管理员或医生，后端会按安全规则拒绝并提示更换未绑定的工作人员微信。
- 三角色工作台和后端权限控制。
- 合作医院创建、编辑、启停。
- 医生预录入、编辑、删除及手机号匹配。
- 医生个人客户工作台切换。

### 健康档案与问卷

- 姓名、手机号、性别、出生日期、身高、体重、腰围、体重变化等基础信息。
- 客户首页问候语优先读取本人健康档案姓名，档案姓名为空时使用“朋友”作为兜底；医生和平台管理员仍显示登录账号名称。
- 既往史、家族史、常见慢性病、饮食、运动、睡眠、压力、情绪等信息。
- 不自动补填未填写字段；档案完整度由后端真实数据动态计算。

### 体检报告与 OCR

- PDF、JPG、PNG 上传及 MinIO 私有存储。
- OCR 异步任务、失败恢复、幂等保护、状态查询和原文件查看。
- OCR 成功与 AI 评估失败已拆分为独立状态：后者使用 `AI_FAILED`，不会再把已识别报告显示成“检验失败”；报告详情可直接重试 AI，历史上已有 OCR 内容的通用 `FAILED` 报告也可恢复。
- 数值与非数值结果、检查类目、检查小结的结构化保存。
- 体检元数据过滤，避免姓名、电话、门诊号、床位号、打印日期等被误识别为指标。
- OCR 将“姓名/性别/年龄/检查日期”等同一行政信息行拆到项目名和结果列时，Python 最终输出与 Java 历史快照读取层会组合识别并过滤，已存报告无需修改原 OCR 快照即可停止展示并避免进入后续 AI 评估。
- 按原报告分类、顺序和内容展示，不自行创造分类。

### AI 评估与健康报告

- 客户端原“AI评估结果”入口已命名为“健康总览”，入口卡片使用“总”标识，保留综合解读、健康维度仪表盘和维度卡片，并移除重复的“健康状态概览”卡片；业务端 AI 评估页面与后端接口保持不变。
- 客户端已发布健康报告列表卡片展示报告日期，报告详情页说明统一为由健康档案、检验报告和面部检测结果综合评估。
- H5 查看原检验报告改用带鉴权的 Java 文件接口生成临时浏览地址，下载 PDF 健康报告则通过同一接口读取 Blob 并以稳定文件名触发浏览器下载，不再依赖浏览器直连 MinIO 的 `127.0.0.1:9000` 预签名地址；微信端原有下载路径保持不变。

- 规则引擎和 12 个健康维度评估。
- 报告重点发现之后新增逐项“异常结果解释”：由结构化异常事实和 RAG 证据生成异常含义、可能涉及的器官或系统及下一步建议；PDF 与客户报告详情同步展示，规则降级仅输出不作诊断的保守说明。
- AI 评估上下文包含健康档案与问卷中的生活方式综合描述、过敏史和当前用药，最近一次成功的面部健康检测体征，以及当前体检报告按原分类整理的结构化指标、检查所见和检查小结。
- 检验异常会生成可追溯的 `abnormalFacts`，单项异常即使不足以完成整个健康维度，也会进入综合解读；BMI 区间互斥，未知性别或缺少腰围时不推测腹型肥胖。
- 糖代谢只以空腹血糖、糖化血红蛋白或空腹胰岛素作为核心评估依据，甘油三酯、HDL、BMI、家族史和体重变化不会单独生成糖代谢异常结论。
- 健康拍体征在 AI 上下文中标记为摄像头估算、补充证据、不可用于诊断，并保留检测时间；不得作为疾病方向的唯一证据。
- 医学知识库 RAG 优先检索异常事实、`ATTENTION/HIGH` 维度和原报告检查小结，每次最多返回 6 条最相关证据。
- DeepSeek 与规则降级共用自然中文的综合总结、重点发现、下一步建议、缺失数据和不确定性；单项轻度异常不再强制生成疾病候选，疾病方向栏目允许为空。
- DeepSeek 默认超时时间调整为 60 秒，输出上限为 32K，并支持最多 3 次尝试及递增退避；网络超时、429 和 5xx 会重试，400/422 等不可重试错误会直接降级。医疗安全校验保留确诊、处方剂量、自行调药和内部枚举等硬边界，同时允许带限定语的风险描述、BMI 解释和“不要自行停药”等安全提示；中西医治疗字段按证据择一或同时输出，不再要求无依据地填满四项。
- 规则降级不再伪装成大模型成功结果：AI 任务保留受控降级原因，报告详情显示降级提示。客户可基于已发布体检报告重新生成 AI 解读；重试失败不会把原已发布报告改成失败状态。
- 面部检测完成时间跨 Java/Python 统一使用 ISO 日期时间字符串，避免 `cameraCompletedAt` 被 Java 序列化成数组后触发 Python 422；请求校验日志只记录字段路径和错误类型，不记录健康数据。
- DeepSeek 返回不存在的指标或证据编号时，会过滤无法追溯的引用；仍有合法事实与知识证据的内容继续使用，完全失去依据的条目才丢弃，避免一处无效引用导致整份解读降级。
- 2026-08-03 已用一份 OCR 成功但评估失败的真实本地报告完成恢复验证：原 67 个结构化指标和 167 条检查所见未重新 OCR，重试后得到 `DEEPSEEK/SUCCESS` 评估、已发布健康报告和私有 PDF。
- 2026-08-03 本地验收：真实 `deepseek-v4-flash` 请求一次完成，返回 `DEEPSEEK/SUCCESS`、无降级；Python 64 项和 Java 65 项测试通过，前端类型检查、Lint、H5 与两个微信包构建通过。仍需医生对真实报告输出进行医学质量评审。
- 小程序和 PDF 优先展示同一份 `interpretation` 核心结论；疾病参考统一命名为“疾病推断参考”，报告编号仅保留为系统内部索引、不向用户展示。PDF 嵌入文泉驿中文字体，疾病名称和建议咨询科室使用真实粗体字形显示，并展示专科诊疗路径与营养干预修复方案。
- AI 疾病参考优先核对原报告检查小结的 `diagnosticSummaryFacts`，必须保留对应事实编号和支持性原文；正常或阴性小结作为反证，不得被改写为疾病线索。
- 每个疾病推断参考均要求给出“专科确认与分层—循证治疗类别—疗效复核/随访”的具体路径，不能只写“由专科决定”。知识库版本 `2.2.0` 已加入幽门螺杆菌、血脂异常和脂肪性肝病的中医药物参考证据；命中对应证据时可展示半夏泻心汤类方、血脂康胶囊或化滞柔肝颗粒等代表性讨论项，但只作辨证和就诊沟通参考，不提供剂量、处方或自行购药建议。
- 每个疾病推断参考新增“中西医结合治疗建议”，拆分西医治疗思路、西医药物治疗参考、中医治疗思路和中医药物/治法参考；已移除单独的“结合注意事项”展示。药物类别或常用药物名称只能来自本条 RAG 证据，必须保留医生评估、处方和复查边界，不生成剂量或自行购药/调药建议。无证据时明确提示未支持具体药物。
- HTML/PDF 健康报告生成、MinIO 版本保存、在线查看和下载。
- 客户和医生查看相同报告内容，平台管理员拥有全平台查看权限。

### 健康随访

- 根据健康报告自动生成可执行计划。
- 饮食、运动、作息、监测、微量营养建议和一周食谱分组展示。
- 客户逐项反馈完成、部分完成或未完成。
- 综合完成率、文字反馈、身体感受和困难原因生成或调整下一期任务。
- 继续、调整、终止和逾期状态处理；列表优先显示最近未完成计划。
- 平台管理员可查看全平台任务详情和完成情况，医生可查看体检者随访。

### 健康检测、提醒与反馈

- 健康拍接入层、面部检测入口、采集页、检测结果、健康指数仪表盘、指标详情和趋势界面。
- 心率变异性指标解读已补充睡眠、压力、恢复状态及心血管/代谢风险线索，并明确单次面部检测不能替代疾病诊断。
- 腾讯云 TTS 吃饭与睡觉提醒设置和试听；客户应用在前台运行且允许声音时，会按保存的时间自动生成并播放提醒。当前配置为女声 `601012`（爱小璟，大模型音色）、男声 `501005`（飞镜）；微信关闭或进入后台后的准时触达仍需订阅消息授权。
- 问题反馈提交、本人隔离查看、待回复/已回复状态和平台管理员回复。

## 5. OCR 的 PDF 与图片处理逻辑

PDF 和图片必须保持两条独立处理路径，任何修改都不得用一条通用流程覆盖另一条。

### PDF 路径

1. 优先读取 PDF 原生文本、表格和定位字符。
2. 按页面保留原分类、原顺序、原内容，解析数值、非数值和检查小结。
3. 原生结构足够时不渲染图片，避免速度下降和内容损失。
4. 只有扫描型或结构不足的 PDF 才逐页渲染并进入 OCR/Qwen 补充路径。
5. PDF 当前识别效果较完整，是回归保护基线；修改图片识别时不得改变此分支。

### 图片路径

1. 图片进入 Qwen OCR 或本地 PaddleOCR 路径。
2. 先对整图识别和结构化解析。
3. 检测到多栏且整图结果不完整时，按版面将图片切成带重叠区域的左、右高分辨率分片。
4. 分栏结果合并、去重，并恢复原报告阅读顺序。
5. 过滤身份、医院、科室、门诊号、住院号、床位号、设备号、条码和打印日期等非体检结果。

### 永久 OCR 约束

- 保持原顺序、原分类、原内容，不自行起分类名称，不移动项目归属。
- 同时保留数值与非数值结果、参考值、单位、异常标识和类目小结。
- 不能因质量门槛过严把可用报告判失败，或使识别项目明显减少。
- 不把未经结构化和可追溯校验的整份原始 PDF 直接交给大模型作为唯一事实来源。
- OCR 不是绝对准确；低质量、遮挡或复杂版式仍需用真实样本做回归验证。

## 6. 外部 AI 与服务接入状态

“代码已接入”不等于“生产环境已验收”。所有密钥只允许存在于本地 `.env` 或部署平台密钥管理中。

| 服务 | 当前代码状态 | 生产前仍需完成 |
| --- | --- | --- |
| DeepSeek | 已接入 AI 评估、健康报告和自适应随访；默认模型配置为 `deepseek-v4-flash`，平台管理员可在 Flash/Pro 之间切换，支持关闭和降级 | 核验正式额度、超时、限流、结构化输出稳定性和医学评审 |
| Qwen3.7-flash → Qwen3.5-OCR | 已接入 PDF 按页云视觉级联；`QWEN_OCR_MODEL` 默认为 `qwen3.7-flash`，`QWEN_OCR_FALLBACK_MODEL` 默认为 `qwen3.5-ocr`。图片继续走独立直读路径，PDF 保留原生解析并将渲染页作为视觉补充，双模型失败时本地回退 | 扩充真实电子 PDF、扫描 PDF、单栏/双栏图片回归集，验证识别质量、计费、并发和故障降级 |
| 健康拍 | Java 签名客户端、健康检测业务接口、小程序插件配置和检测结果 UI 已接入，支持 UAT/生产地址切换 | 供应商确认插件审核、正式版本、域名白名单、回调、正式环境、计费与对账，并完成真机验收 |
| 腾讯云 TTS | Java TTS 客户端、提醒设置、性别音色选择、动态文案、试听和前台运行时自动播报已接入 | 在部署环境开启配置并验收资源包；关闭小程序或进入后台时的定时送达仍需微信订阅消息授权与模板 |

此前在对话或截图中出现过的所有真实 API Key、SecretId、SecretKey 和供应商 Key，正式上线前必须轮换。文档和 Git 中不得记录其值。

## 7. Docker 启动、停止与重新构建

首次使用先在本地创建环境文件并填入自己的配置：

```powershell
Set-Location E:\health
Copy-Item .env.example .env
```

启动开发环境：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

等价命令：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
docker compose -f compose.yml -f compose.dev.yml ps
```

查看日志：

```powershell
docker compose -f compose.yml -f compose.dev.yml logs -f rayk-server rayk-ai nginx
```

只重建单个服务：

```powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build rayk-ai
docker compose -f compose.yml -f compose.dev.yml up -d --build rayk-server
docker compose -f compose.yml -f compose.dev.yml up -d --build nginx
```

停止服务但保留数据卷：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
```

严禁在日常操作中使用 `docker compose down -v`。Docker Desktop 的镜像磁盘、容器磁盘和项目持久数据必须放在 E 盘；迁移或清理前必须先核对实际路径和备份。

## 8. H5 与两个微信包的同步构建规则

每次修改 `rayk-miniapp` 前端后，交付前必须同步更新以下三个输出：

1. H5：`E:\health\rayk-miniapp\dist\build\h5`
2. 微信开发包：`E:\health\rayk-miniapp\dist\release\mp-weixin-dev`
3. 微信生产局域网验收包：`E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan`

建议顺序：

```powershell
Set-Location E:\health\rayk-miniapp
npm ci
npm run type-check
npm run lint
npm run build:h5
npm run build:mp-weixin:dev
# 将本次 dist\build\mp-weixin 完整同步到 dist\release\mp-weixin-dev
npm run build:mp-weixin
# 将本次 dist\build\mp-weixin 完整同步到 dist\release\mp-weixin-prod-lan
```

`mp-weixin-prod-lan` 是局域网真机验收包，不是可直接提交微信审核的正式互联网生产包。正式发布仍需 HTTPS 合法域名、关闭开发登录、真实微信配置和隐私合规审核。

## 9. 当前未完成事项

1. 企业主体小程序正式发布：在微信平台开通手机号快速验证，配置生产 AppSecret、HTTPS 合法域名和服务器域名白名单，并使用真实手机号预录入医生后完成真机验收。
2. 健康拍正式插件/接口生产验收、白名单、回调、计费和真机全链路测试。
3. 腾讯云 TTS 正式环境验收，以及微信关闭后的订阅消息提醒闭环。
4. 扩充真实医院 PDF、扫描 PDF、单栏图片、双栏图片、倾斜和低清图片 OCR 回归数据集。
5. 对 DeepSeek/RAG 输出做医生评审、知识来源版本治理、幻觉防护和质量监控。
6. 正式上线前确定医生数据范围：继续全平台只读，还是恢复按医院隔离。
7. 完成互联网部署、备份恢复演练、监控告警、审计留痕、隐私合规和医疗合规验收。
8. 轮换所有曾在对话或截图中暴露的外部服务凭据。

## 10. 已踩过且绝对不能重复的坑

- 不得把 PDF 与图片 OCR 合并成同一条处理路径；修图片识别不能破坏 PDF。
- 不得打乱、重命名或臆造原体检报告的分类、顺序和内容。
- 不得因过严质量策略让原本可识别的报告失败或从 31 项下降为少量项目。
- 不得把姓名、年龄、手机号、门诊号、床位号、打印日期等元数据当成体检指标。
- 不得只保留数值指标而丢失影像描述、文字结论和检查小结。
- 不得自动填充用户未填写的健康档案字段；完整度必须反映真实数据。
- 不得重新引入机构管理员、健康管理师等已取消角色或无权限卡片。
- 不得只依赖前端隐藏按钮做权限控制；所有数据范围由 Java 后端校验。
- JavaScript 接收数据库 `BIGINT` 标识时必须按字符串处理，避免精度丢失。
- Flyway 已执行迁移不得修改，只能新增后续版本迁移。
- MinIO 桶不得公开，报告和原文件使用授权下载或预签名地址。
- 不得执行 `docker compose down -v`、删除数据库卷、MinIO 卷或 OCR 模型卷。
- 不得把构建缓存、镜像和运行数据重新写回空间紧张的 C 盘。
- 不得只更新 H5 或一个微信包，导致开发者工具继续加载旧代码。
- 手机真机不得访问 `127.0.0.1`；局域网验收使用电脑 LAN 地址，正式环境使用 HTTPS 域名。
- 开发 Mock 登录、局域网 HTTP 配置不得进入正式发布包。
- 密钥不得出现在源码、日志、README、handoff、截图说明或 Git 历史中。
- 修改前必须检查 `git status` 和差异，不覆盖用户或其他 Agent 未提交的改动。
- 不得把 AI 输出描述成确诊结论；报告必须保持可追溯证据和健康管理边界。

## 11. 接手检查清单

```powershell
Set-Location E:\health
git status --short
git log -5 --oneline
docker compose -f compose.yml -f compose.dev.yml ps
```

然后依次阅读：

1. `AGENTS.md`：永久工程规则。
2. `README.md`：项目使用、启动和部署说明。
3. 本文：当前状态、未完成事项和不可重复的坑。
## 本地 H5 启动说明（2026-08-07）

- PaddleOCR 冷启动改为后台预热，避免阻塞 AI 服务和 Docker Compose 健康检查。
- Docker Desktop 启动后执行 `docker compose -f compose.yml -f compose.dev.yml up -d`，本地 H5 入口为 `http://localhost:8088/`。
- 本地 H5 三角色调试需先在 `rayk-miniapp` 执行 `npm run build:h5:dev`；开发入口提供平台管理员、医生和客户，H5 在 localhost 下自动使用同源 API。平台管理员本地调试账号为 `admin` / `123456`，医生和客户使用各自开发测试密码。
- 微信开发包构建修复：`build:mp-weixin:dev` 通过 `scripts/build-mp-weixin-dev.mjs` 强制读取 `.env.development`，避免 Uni 构建误加载 `.env.production` 导致开发包请求生产域名。
- 2026-08-13 小程序主体已切换为企业 AppID `wxf6f4549c8c962948`，源码配置位于 `rayk-miniapp/src/manifest.json`，`.env.example` 同步更新；部署环境仍需单独配置对应 AppSecret，未写入仓库。
- 2026-08-13 企业主体手机号登录已启用：生产构建默认使用微信 `getPhoneNumber` 凭证，Java 生产配置 `WECHAT_PHONE_LOGIN_REQUIRED=true` 时拒绝未携带手机号凭证的 OpenID 回退；客户手机号自动创建/匹配 CUSTOMER，平台预录入医生和管理员手机号分别匹配 DOCTOR/PLATFORM_ADMIN。登录页已移除账号密码、绑定码和首次绑定卡片；平台工作台新增管理员手机号维护卡片，并支持通过 `WECHAT_PLATFORM_ADMIN_PHONE` 完成首次管理员手机号引导；本地开发包保留三角色调试入口。
- 2026-08-13 手机号识别修复：本地 `compose.dev.yml` 不再默认启用微信 Mock，也要求手机号凭证；即使环境显式保留 Mock，只要小程序请求携带 `getPhoneNumber` 凭证，Java 也会强制走真实 `code2session` 与手机号校验，避免共享 Mock OpenID 或固定测试手机号把预录入管理员识别成 CUSTOMER。若同一微信 OpenID 之前已绑定客户，手机号确认属于预录入医生/管理员时会迁移旧绑定；若该工作人员已有另一个微信绑定则拒绝冲突。修改后必须重建 Java 服务，并重新导入 `dist\\release\\mp-weixin-dev`。
- 登录页已移除手机号授权说明小字，并在登录卡片下方增加健康档案、健康报告、持续管理三项服务概览；仅调整展示，不改变微信授权、角色识别和开发调试登录逻辑。
- 登录页已按“三羊健康”参考稿调整为浅色品牌头图、三项信任能力条、授权登录大卡片、健康服务入口和安全提示；仅调整布局与样式，不改变微信授权、角色识别和开发调试登录逻辑。
- 登录界面视觉资源统一归档到 `rayk-miniapp/src/assets/ui/login/`，包含品牌头图、微信授权、隐私安全、会员、随访、健康档案、健康评估和健康管理 PNG，以及目录说明；资源仅做归档，未改变页面功能。
- 登录页顶部品牌方块已替换为 `brand-logo-sheep.png` 羊形 Logo，并移除原头图中的两个 CSS 半圆装饰；登录逻辑保持不变。

## 2026-08-10 会员体系 V1 开发状态

- 已新增 `V32__customer_membership.sql`，包含会员方案、客户会员、权益字典、方案权益、使用流水和订单表，并预置免费客户与年度会员方案。
- 已新增 Java `/api/client/membership` 接口和 `MembershipEntitlementService`，使用流水采用 `RESERVED`、`CONFIRMED`、`RELEASED` 两阶段模型，并按客户数据边界查询。
- 已接入 AI 健康评估、AI 初始/续期随访、健康拍上传、腾讯云 TTS 预览的权益预占与确认/释放；外部调用失败不会确认消耗。健康拍年度会员当前仅按每日额度核销，不再使用滚动 30 天额度。
- 已新增客户会员中心、方案、权益、使用记录和订单结果页面；会员方案页保留脱敏会员动态滚动展示，当前不显示“示例动态”标签。
- 已接入微信 JSAPI 支付下单、`/api/payments/wechat/notify` 回调验签解密、订单幂等更新和会员激活；开发环境仍默认模拟支付。正式启用前需在部署环境配置 `WECHAT_PAY_APP_ID`、`WECHAT_PAY_MERCHANT_ID`、`WECHAT_PAY_MERCHANT_SERIAL_NUMBER`、`WECHAT_PAY_PRIVATE_KEY_PATH`、`WECHAT_PAY_API_V3_KEY`、`WECHAT_PAY_NOTIFY_URL`，并将 `MEMBERSHIP_PAYMENT_ENABLED=true`。API v3 Key 和商户私钥不进入 Git 或聊天记录，证书目录只读挂载到容器。
- 已完成 Java 编译、前端类型检查/Lint、H5 和两个微信包构建；真实支付商户开通、微信商户平台回调配置、真实金额验收和生产数据库 Flyway 执行仍需部署侧完成，不能将代码接入表述为已完成生产验收。
- 会员中心故障已修复：会员方案、权益字典和方案权益是全局配置表，已对这三张表忽略租户拦截；客户会员、订单和用量流水仍保留租户隔离。
- 修复开通会员后仍显示“免费客户”：免费会员永久有效，当前会员选择改为按最新 `start_at` 优先，避免永久免费记录遮蔽已开通的年度会员。
- 会员权益调整：新增 `V33__remove_health_shot_rolling_quota.sql`，停用健康拍滚动额度配置，保留历史流水；年度会员健康拍仅保留每日 1 次额度。
- 会员开发验收：新增开发专用 `POST /api/client/membership/dev/switch`，客户在开发包会员中心可恢复免费客户或模拟年度会员，保留历史会员记录并将旧付费记录标记为过期。仅 `compose.dev.yml` 开启 `MEMBERSHIP_DEVELOPMENT_MODE=true`，生产配置默认关闭；重新构建并导入 `dist\release\mp-weixin-dev` 后使用。
- 会员方案页的动态滚动条仅使用本地脱敏展示文案，不读取或暴露其他客户的真实会员记录。

## 2026-08-13 AI 模型管理

- 平台管理员工作台新增“AI模型管理”卡片和配置页，可切换 `deepseek-v4-flash` 与 `deepseek-v4-pro`。
- 选择持久化在 `ai_model_runtime_config`（迁移 `V34__ai_model_runtime_config.sql`）；下一次新生成评估由 Java 将当前模型传给 Python 生效，现有报告不会被改写。
- 模型配置接口仅允许 `PLATFORM_ADMIN` 访问，页面只展示模型元数据，不暴露 DeepSeek API 密钥。
- 模型管理页新增思考模式开关，状态持久化在 `ai_model_runtime_config.thinking_enabled`（迁移 `V35__ai_thinking_mode.sql`）。默认关闭；开启可能增加推理 token、响应时间和费用，下一次新评估或随访请求由 Java 将当前状态传给 Python，历史结果不变。

## 2026-08-13 平台首页指标入口

- 平台首页“合作医院”与“预录入医生”指标已拆分为不同入口：合作医院进入医院管理页，预录入医生进入按医院分组的医生目录页。
- 医生目录通过现有平台医院/医生接口加载脱敏手机号和登录状态；每个医院保留“管理”入口，可继续进入医院详情进行预录入、修改和删除。
- 前端已重新生成 H5、微信开发包和微信生产局域网验收包；Java 首页汇总接口已同步返回 `/pages-tenant/dashboard/doctors`。

## 2026-08-13 微信代码包体优化

- 修复微信开发包中旧哈希图片未清理的问题。此前新旧登录页 PNG 同时被复制到 `dist/release/mp-weixin-dev/assets`，导致主包膨胀到约 13 MB 并触发 4 MB 限制。
- 登录页资源统一保留 `rayk-miniapp/src/assets/ui/login` 中的 9 张压缩图；当前每张均小于 200 KB，三份微信产物的资源目录均只包含这一组文件，递归包体约 1 MB。
- 新增 `rayk-miniapp/scripts/sync-mp-weixin-release.mjs`，同步 release 包前会清理明确的目标目录再复制本次构建结果，避免历史哈希文件残留。`build:mp-weixin:dev` 已自动同步开发包，生产局域网包使用 `npm run sync:mp-weixin:prod`。
- `src/pages.json` 已开启 `lazyCodeLoading: requiredComponents`；`src/manifest.json` 已开启脚本、WXML、WXSS 压缩。微信开发者工具中仍需在“详情 > 本地设置”勾选上传时自动压缩脚本、WXML 和 WXSS。
- 交付前必须重新导入当次生成的 `dist/release/mp-weixin-dev`，不要继续使用已打开的旧目录；若目录被开发者工具占用，先关闭旧项目再导入或重新构建。

## 2026-08-14 登录页微信图标

- 按参考视觉稿放大微信授权图标展示区域，登录授权流程、手机号识别逻辑和其他页面功能保持不变。
- 已重新构建 H5、微信开发包和微信生产局域网验收包；若开发者工具仍显示旧尺寸，需重新导入当次 `dist\\release\\mp-weixin-dev` 目录或清除编译缓存。

## 2026-08-14 品牌名称

- 用户可见品牌名称已统一调整为“三羊健康”，已同步登录页、首页欢迎语、我的页面、导航标题、PDF 报告署名、AI 服务提示词、接口标题及项目文档。
- `rayk`/`RayK` 内部包名、数据库标识、容器名和兼容标识保持不变，仅调整品牌文案。

## 2026-08-14 医生手机号变更与会话

- 医生预录入手机号修改后，微信登录必须以本次 `getPhoneNumber` 验证手机号为准；若旧手机号已不再绑定任何系统账号，不会再沿用旧 OpenID 绑定登录为医生。
- 医生个人客户档案的手机号哈希与脱敏值会在医生手机号修改时同步更新，避免旧手机号残留在个人工作台数据中。
- JWT 默认有效期为 `JWT_EXPIRE_SECONDS=604800` 秒（1 周），Redis 会话键使用同一 TTL；部署环境可通过该环境变量覆盖。医生手机号修改后会递增该用户的会话版本，已签发的旧令牌会在下一次请求时立即失效，主动退出登录也会立即失效。
- 通过微信验证手机号登录时，手机号未命中预录入平台管理员/医生账号的，统一按普通客户自动注册；平台概览新增按手机号哈希去重的普通手机号用户数统计，不展示完整手机号。
- 2026-08-14 补充修复：手机号未命中预录入账号但该微信 OpenID 存在历史绑定时，不再返回“尚未绑定系统用户”，而是先创建普通 CUSTOMER，再将历史绑定迁移到该手机号对应的客户账号。
- 2026-08-14 平台管理员首页移除“手机号用户”快捷统计卡片；手机号用户统计仍保留在平台概览页接口和合作医院管理页。

## 2026-08-14 检验报告多图片上传

- 检验报告上传页支持连续选择/拍摄多张图片，并在提交前展示文件列表、允许单张删除和继续添加；应用层不限制总张数，受微信/系统单次选择器上限时可再次添加。
- 多张图片先创建同一份检验报告，再通过 `/api/v1/lab-reports/{id}/files` 追加文件，最后调用 `/api/v1/lab-reports/{id}/files/complete` 统一启动 OCR；OCR 会逐页识别并合并指标、检查所见、原始行和警告。
- PDF 与原有单文件上传仍走原生解析/单次 OCR 路径，未合并两条 OCR 处理链。Java 服务已通过完整 Maven 测试并在 Docker 中重建运行。
- 修复多页任务完成阶段的数据库字段溢出：多个页面的 OCR 引擎名称现在去重并限制在 `ai_task.engine_version` 的 80 字符内，避免识别已完成但任务无法落库。当前 15 张图片的实测任务已成功完成并生成报告。

## 2026-08-15 图片 OCR 泛化优化

- Qwen 图片识别结果现在兼容嵌套 JSON、Markdown/HTML 表格、制表符表格和普通文本；会递归提取分类、数值指标、参考范围、单位、异常标识以及检查小结、影像表现、印象和结论等非数值内容。
- 对没有标准表格但能读出文字的报告页，保留可追溯的原文行和“原文待核对”发现，不再因缺少数值指标直接判定整页失败；仅有文字的图片会进入待人工核对状态并给出提示。
- 多页报告在同一 OCR 任务内对全部已入库图片使用有界并行识别，再按上传顺序合并结果；单页失败会记录警告，不丢弃其他成功页面。PDF 原生解析路径保持独立，未被图片识别改动。
- 验证：Qwen OCR 测试全部 12 项通过，Python OCR 模块编译通过，前端类型检查和 lint 通过；H5、微信开发包、微信生产局域网包均已在本次构建生成并核对。Docker 构建同时完成 rayk-ai 镜像和 rayk-server 镜像，Java Maven 测试 65 项全部通过。三份前端产物分别位于 `dist\\build\\h5`、`dist\\release\\mp-weixin-dev` 和 `dist\\release\\mp-weixin-prod-lan`。
- 本次已使用新镜像重新拉起 `rayk-ai`、`rayk-server`，Docker 健康检查通过；本机 H5 `http://127.0.0.1:8088/`、Java 健康端点和 AI 健康端点均返回 200，可直接继续做多图片上传验收。

## 2026-08-15 OCR 状态与视觉结果修复

- 检验报告详情页不再把 `CONFIRMED` 误判为“解析中”；只有上传、OCR 或 AI 评估进行中的状态显示加载提示，已确认报告直接展示分类结果。
- 现有 Qwen3.5-OCR 已经是视觉识别模型，本次优先修正提示词和解析兜底：要求逐行保留表格项目、结果、单位、参考范围和异常标识，并在模型把项目名与结果分别输出为“原文”时自动按同类目相邻行恢复，避免页面出现大量“原文”占位。
- 当前数据库中已完成的旧 OCR 快照不会自动重写；要验证新的项目名/结果恢复规则，需要重新上传报告或重新触发该报告的 OCR。截图中 `CONFIRMED` 且已有 129 项的记录属于识别已完成、前端状态提示错误，不是任务仍在后台等待。
- 该阶段结论已被下方“体检图片直读综合报告改造”更新：Qwen3.5-OCR 仍保留为图片识别/结构化降级路径，但综合报告生成新增 Qwen Vision 图片直读路径。

## 2026-08-15 体检图片直读综合报告改造

- 已按当前产品决策新增 Qwen Vision 多模态直读模式：图片报告不再以 OCR 结构化结果作为体检事实唯一入口。Java 为已入库的图片页生成最多 10 分钟有效的 MinIO 内部签名地址，Python 在同一次请求中接收原始图片、健康档案和健康拍结果，要求模型直接按既有健康报告 JSON 格式生成综合解读；图片页使用 `IMAGE:PAGE:<页码>` 事实编号追溯。
- PDF 与图片保持独立：PDF 继续走原生解析/既有 OCR 路径，`reportImages` 只筛选 `image/*` 文件，不会把 PDF 传入 Qwen Vision。Qwen 未启用或未配置密钥时，图片请求不会伪装成模型已看图，而是回到原有结构化文字/规则降级链路。
- OCR 失败但原始图片已存储时，客户报告详情页可直接触发“用图片生成健康报告”，无需先让 OCR 结构化结果通过；Java 后端仍校验客户权限、隐私同意、会员权益和报告状态。
- 新增配置：`.env` 中使用 `QWEN_VISION_ENABLED`、`QWEN_VISION_API_KEY`、可选 `QWEN_VISION_WORKSPACE_ID`/`QWEN_VISION_BASE_URL`、`QWEN_VISION_MODEL=qwen3.7-flash`、超时、输出 token 和最大图片数；密钥不得进入 Git 或日志。
- 验证：`python -m compileall -q app` 通过；新增/相关 Python 测试 25 项通过；Java Maven Docker 测试 65 项全部通过；小程序 `type-check`、`lint`、H5、微信开发包和微信生产局域网包均通过并已同步到三个规定目录。已使用不含患者数据的 16×16 测试图调用 Qwen Vision，返回 HTTP 200；真实 MinIO 报告图片、医疗样本质量和生产合规仍未验收。完整 Python pytest 当前仍受本机缺少 `reportlab` 依赖影响，未宣称全量通过。

## 2026-08-15 多模态自动评估与健康报告生成修复

- 修复 OCR 异步任务完成后自动评估的身份上下文顺序：先在租户范围内读取报告和客户，再建立系统客户上下文，最后复用 `submitAi` 的客户数据权限校验；因此图片评估请求会真正进入 Python 的 Qwen Vision 直读路径。
- 修复 OCR 失败分支：图片原文件已保存时，即使 OCR 结果质量不足或 OCR 服务异常，也会继续尝试图片直读综合评估；PDF 不进入该分支，PDF 原生解析路径保持独立。
- 修复自动评估在会员、授权或其他前置条件失败时的静默问题：报告会标记为 `AI_FAILED`，并记录不含敏感正文的告警，客户可重试。
- 图片报告详情页在 `CONFIRMED`、`OCR_FAILED`、`WAITING_CONFIRMATION`、`AI_FAILED` 等可直读状态显示“直接用图片生成健康报告”，用于恢复此前已完成 OCR 但尚未生成报告的历史任务。
- 本次修复验证：Java Docker Maven 测试 65 项通过；AI 侧 `compileall` 通过，相关 Python 测试 25 项通过；前端 `type-check`、`lint`、H5、微信开发包、微信生产局域网包全部通过。此前已上传但卡在 `CONFIRMED` 的历史 15 图任务不会被代码更新自动重跑，需在详情页点击上述入口触发一次正式评估。

## 2026-08-15 上传 503 与多图 Qwen 稳定性修复

- Java 文件服务继续对用户返回统一的存储不可用提示，但现在会按操作记录报告/文件 ID、异常类型和 MinIO 错误码，不记录原始文件名、对象路径、凭据或完整异常正文；MinIO/数据库/临时签名失败可据此区分，PDF 与图片存储路径未合并。MinIO 首次建桶的并发竞态（`BucketAlreadyOwnedByYou`/`BucketAlreadyExists`）现在按成功处理，避免首个上传请求被误报为 503。
- Qwen 图片直读新增单图字节数、全部图片字节数和像素上限。超过上限时在 AI 容器内转为适合报告阅读的 JPEG，再以 Base64 Data URL 发送，保留全部图片页，不把内部 MinIO 签名 URL传给外部模型。
- Qwen 400/422 会自动尝试去除不兼容的 `response_format`/思考参数；429、5xx 和网络超时保留重试，明确的 4xx、空响应和无效响应不再重复发送同一大请求。日志仅记录状态、供应商错误码是否存在和请求 ID 是否存在。
- AI 服务已关闭 `httpx`/`httpcore` INFO 请求 URL 日志，避免把短时签名地址写入日志。新增配置：`QWEN_VISION_MAX_IMAGE_BYTES=2000000`、`QWEN_VISION_MAX_TOTAL_IMAGE_BYTES=8000000`、`QWEN_VISION_MAX_IMAGE_PIXELS=12000000`。
- 代码层验证：本机相关 Python 测试 27 项通过，Python `compileall` 通过；Java Docker 镜像构建阶段的 Maven 测试 65 项通过。Docker 已按 `compose.yml + compose.dev.yml` 重建并运行，MySQL、Redis、MinIO、AI、Java、Nginx 均健康；AI 容器确认启用 Qwen Vision 且已加载 Pillow。此前卡住的 15 图任务仍是 Docker 重启前的旧 `PROCESSING` 记录，未自动继续调用模型，需重新提交或在详情页重新触发评估。

## 2026-08-15 Qwen 两阶段图片分析与报告汇总修复

- Qwen 图片链路改为两阶段：第一阶段按 `QWEN_VISION_BATCH_SIZE`（默认 4 张）分批将原始图片直接交给 `qwen3.7-flash`，逐页输出保留分类、项目、结果、单位、参考范围、异常标识、文字所见和不确定性的结构化事实；第二阶段使用同一 `qwen3.7-flash` 的文本请求，将该 `imageAnalysis` 与健康档案、健康拍、结构化体检快照和 RAG 证据汇总成现有健康报告 JSON。
- 第一阶段严格校验页面覆盖，缺页、重复页、无效 JSON 和可重试的上游超时按批次重试；最终报告失败时只重试文本汇总，不重复发送已经完成的图片批次。最终汇总不再携带 15 张 Base64 图片，因此避免整批图片拖垮一次报告请求。
- 新增配置：`QWEN_VISION_BATCH_SIZE=4`、`QWEN_VISION_BATCH_ATTEMPTS=2`、`QWEN_VISION_IMAGE_ANALYSIS_MAX_TOKENS=8000`。PDF 仍保持原生解析路径，Qwen 图片阶段只接收 `image/*`。
- 本次验证：相关 Python 测试 29 项通过，`compileall` 通过，Java Maven 测试 65 项通过，Qwen 客户端与最终汇总的格式检查通过；Docker 已重建 AI/Java 镜像并按开发覆盖配置运行，六个服务均健康，Java 到 AI 的读取超时为 600 秒。此前改造前的旧 15 图任务已按超时失败收口；新的两阶段链路尚待重新提交真实 15 图验收。

## 2026-08-15 图片报告直读改造（跳过 OCR 结构化提取）

- 图片体检报告（全部文件为 `image/*`）上传后不再创建 OCR 任务，也不把 Qwen3.5-OCR/PaddleOCR 的结构化结果写入 `indicator_value` 或 `lab_report.ocr_snapshot`；改为上传/补传完成后直接异步进入 `qwen3.7-flash` 两阶段直读：先按批把原始图片直读为逐页结构化事实 `imageAnalysis`，再结合健康档案、健康拍和 RAG 证据汇总成健康报告。PDF 仍走原生解析/既有 OCR 路径，两条处理链未合并。
- 图片直读出的 `imageAnalysis` 通过评估响应回传 Java，落库到 `lab_report.image_analysis_snapshot`（迁移 `V37__lab_report_image_analysis_snapshot.sql`）；客户报告详情页与业务端报告详情页优先展示该逐页结构化结果（分类、项目、结果、单位、参考范围、异常标识、小结），替代原 OCR 结构化结果。
- RAG 检索顺序重排：图片直读先于 RAG 检索执行，检索查询会纳入图片直读出的异常项目、结论和不确定项，使图片报告跳过 OCR 后仍能围绕图片异常事实检索知识库证据。
- 状态机：图片报告走 `UPLOADED → AI_PROCESSING → PUBLISHED`（失败为 `AI_FAILED`）；`submitAi` 允许图片直读从 `UPLOADED` 进入。前端详情页轮询从 ocr-task 改为轮询报告状态，图片报告不再依赖 OCR 任务状态。
- 本次验证：Python 相关测试 29 项通过（含新增直读回传与图片事实 RAG 检索测试），`compileall` 通过；Java Docker 构建阶段 Maven 测试 65 项全部通过，镜像构建成功；前端 `type-check`、`lint` 通过，H5、微信开发包、微信生产局域网包三份产物已重新生成。真机/模拟器端的图片直读全链路（真实报告图）仍需在重建 Java/AI 服务后提交验收。
- 2026-08-15 已重建 `rayk-server`（新代码 + V37 迁移，数据库已生成 `image_analysis_snapshot` 列）并重启 `rayk-ai`；`.env` 将 `QWEN_VISION_TIMEOUT_SECONDS` 调至 300（qwen 直读单批读超时）、新增 `RAYK_AI_READ_TIMEOUT_SECONDS=1800`（Java 到 AI 总超时），`compose.dev.yml` 的该值改为从 `.env` 读取。原因为 Qwen Vision 直读 4 张图超过默认 120 秒读超时导致降级；调整后需重新上传图片报告验证直读不再超时。
- 2026-08-15 实测 15 图直读：batch=1/2/3 各约 3 分钟成功（合计 167 项 findings），但 batch=4 因 MinIO 预签名 URL 过期（`REPORT_IMAGE_PRESIGN_EXPIRY_SECONDS` 原为 600 秒，直读最后一批下载时已 634 秒）返回 403 触发降级。已将预签名有效期调至 1800 秒并与 `RAYK_AI_READ_TIMEOUT_SECONDS` 对齐，重建 `rayk-server`；需重新上传验收确认四个批次全部完成。

## 2026-08-16 Qwen Vision 模型切换

- 按当前产品选择，Qwen Vision 从 `qwen3.8-max` 切换为 `qwen3.7-flash`，准确 API Model ID 使用无连字符写法 `qwen3.7-flash`。图片报告仍保持两阶段直读：先分批分析原图，再结合健康档案、健康拍和 RAG 证据生成现有报告格式。
- `qwen3.7-flash` 的思考模式参数现在显式随请求发送；如果供应商接口拒绝可选思考或结构化输出参数，客户端保留原有兼容重试逻辑。
- `.env`、Compose 默认值、Python 默认值、测试和 README 已同步；AI 容器已重建并确认运行时模型为 `qwen3.7-flash`，AI/Java 健康检查均返回 200。
- 本次相关 Python 测试 32 项通过，`compileall` 和 Compose 配置检查通过；真实患者图片的模型效果仍需重新提交一份图片报告做业务验收。

## 2026-08-16 图片评估失败与会员权益修复

- 最新 15 图任务中，Qwen3.7-Flash 四个图片批次均完成；最终汇总一次连接超时、后续输出校验失败后按既有策略返回规则降级结果。Java 随后创建健康报告成功，但创建首次健康随访时发现年度会员方案缺少 `AI_FOLLOWUP_INITIAL`，异常反向把报告标记为 `AI_FAILED`。
- `WorkflowApplicationService.publishAutomatically` 现在把首次随访视为报告发布后的非阻断步骤：随访权益不足或随访专属持久化失败只记录类型化告警，健康报告和 PDF 保持已发布。
- 新增迁移 `V38__add_initial_followup_to_yearly_membership.sql`，为 `AI_HEALTH_YEARLY` 补充一次 `AI_FOLLOWUP_INITIAL` 会员权益；不修改已执行迁移。
- 当前 15 图任务的健康评估、健康报告和 PDF 已存在，服务重建后会修复报告状态和图片分析快照，不重新调用 Qwen、不重复扣除会员 AI 报告权益。

## 2026-08-16 图片事实进入评估与报告链路修复

- 复核截图对应的 15 图任务后确认：Qwen 已完成 15 页图片直读并保存约 200 条图片事实，但图片报告跳过 OCR 后，规则评分仍只读取空的 `indicators`，导致健康总览显示 `0/12`，报告生成“数据不足”。
- 新增 `rayk-ai/app/interpretation/image_facts.py`：保留全部图片页的数值、文字所见和检查小结；对可安全标准化的常见指标投影到既有健康维度规则，未识别项目仍保留在综合评估上下文，不丢失原始事实。
- 评估入口现在先完成一次 Qwen 图片直读，再把同一份图片事实同时交给规则评分、RAG 综合解读和报告生成，避免评分与大模型读取两套数据。
- PDF 生成请求现在携带 `imageAnalysis`，图片报告不再因 Java 的 `indicator_value` 为空而显示 0 项覆盖；PDF 生成仍与 PDF 原生 OCR 路径独立。
- 当前截图对应报告已用保存的图片分析快照完成一次无重复扣费的数据修复：健康维度为 5 个有效、4 个需关注，报告 PDF 已生成第 2 版。该历史记录的最终模型汇总曾因超时/校验失败走规则降级；后续新上传会保留图片事实并由 DeepSeek 负责最终综合解读。

## 2026-08-16 Qwen 图片直读与 DeepSeek 综合报告改造

- 图片报告继续由 `qwen3.7-flash` 分批直接读取原始图片，只输出逐页结构化事实；Qwen 不再负责最终健康报告文本生成。
- DeepSeek 现在统一负责最终 `ComprehensiveInterpretation`：图片模式接收 Qwen 的 `imageAnalysis`、规则结果、健康档案、健康拍和 RAG 证据；PDF/文字模式仍直接接收结构化文字事实。
- PDF 不是由大模型直接生成，而是由 Python ReportLab 按最终结构化解读稳定排版；图片与 PDF 原生解析路径保持独立。
- 图片模式的最终成功标识应为 `source=DEEPSEEK`、`model` 为 `DEEPSEEK_MODEL` 配置值且 `fallbackReason` 为空；若 DeepSeek 超时或输出校验失败，才允许进入基于图片事实的 `RULE_FALLBACK`。

## 2026-08-16 综合报告质量与校验降级修复

- 排查最新 15 图报告确认：Qwen3.7-flash 的 4 个图片批次均完成，失败点在 DeepSeek 最终 JSON 的本地证据校验；一个证据不足的可选疾病候选触发了整份综合解读降级，导致页面只显示泛化规则文案。
- `rayk-ai/app/interpretation/service.py` 现在先校验整份输出的确诊、剂量、自行调药等硬边界，再逐个处理 `diagnosticReferences`。弱候选会单独剔除，DeepSeek 已生成的摘要、重点发现、异常解释、交叉发现和建议继续生成 PDF，不再因单段失败整体丢失。
- 已适度放宽疾病候选门槛：`RISK_SIGNAL` 可由一项已核对异常或图片页事实支持；`POSSIBLE` 有原报告方向性小结或图片页事实时不再强制两项化验；药物方案不是必填，只有 RAG 有依据时才输出。确诊措辞、剂量和自行停换药仍是硬拒绝边界。
- DeepSeek 不可用时的规则降级改为按白蛋白、胆红素、甘油三酯、血糖、血钾、肝酶、尿酸、血红蛋白等指标生成不同的解释、可能影响和复查动作，避免所有异常都显示同一套模板。
- 验证：相关 Python 回归测试 35 项通过，`compileall` 通过；本机完整 pytest 仍因环境缺少 `reportlab` 在收集 `tests/test_api.py` 时中止，未将其宣称为全量通过。AI Docker 镜像已重建，`rayk-ai` 健康检查通过。修复前已生成的旧报告不会自动重写，需要重新点击“重新生成 AI 解读”或重新提交图片任务验证新结果。

## 2026-08-16 移除健康报告详情页无效重生成按钮

- 客户健康报告详情页移除了“重新生成 AI 解读”按钮、确认弹窗和页面内重复评估逻辑；保留降级提示、报告查看和 PDF 下载。
- 检验报告详情页原有的 AI 评估入口未修改，后端评估接口和平台侧恢复能力也未删除；用户如需重新发起评估，应从检验报告详情页进入。
- 已完成 `type-check`、`lint`、H5、微信开发包和微信生产局域网包构建，并确认源码及三个构建输出均无该按钮文本、样式或 handler。产物目录为 `rayk-miniapp/dist/build/h5`、`rayk-miniapp/dist/release/mp-weixin-dev` 和 `rayk-miniapp/dist/release/mp-weixin-prod-lan`。

## 2026-08-17 健康报告 A4 打印排版调整

- `rayk-ai/app/report/service.py` 的 ReportLab PDF 生成器已按 A4 打印参数调整：用户报告标题 20pt、日期/基本资料 11pt、一级标题 15pt、二级标题 13pt、小标题 12pt、正文 12pt、重点结论 12.5pt、免责声明 10pt、页脚 9pt；同步调整行距、段前段后距和页边距。
- 报告页首移除单独的“三羊健康评估报告”，只保留用户报告标题；异常结果解释按完整内容块分页，避免标题和结果留在上一页而解释拆到下一页。疾病推断中的“中西医结合治疗建议”单独成标题，西医治疗思路、西医药物治疗参考、中医治疗思路和中医药物/治法参考各自独立成段。
- 验证：重启开发环境 `rayk-ai` 后通过 `/api/v1/reports/generate` 生成带疾病推断参考的测试 PDF；Poppler 核对为 A4（595.276×841.89pt），渲染检查 2 页无裁切、重叠，首页标题和四段治疗建议均符合要求。未修改 PDF 原生解析、图片识别或前端构建产物。

## 2026-08-17 健康报告跳转原检验报告详情

- 客户健康报告详情页的“查看原检验报告”不再直接下载或预览文件，改为使用当前评估关联的 `assessment.reportId` 跳转到 `/pages-customer/lab-report/detail?id=...`，进入对应检验报告详情页查看完整分类结果和原始文件入口。
- 已移除该入口专用的文件列表请求、下载/预览分支和加载状态；健康报告 PDF 下载逻辑保持不变。
- 验证：`type-check`、`lint`、H5、微信开发包、微信生产局域网包均通过；三个产物已同步到 `rayk-miniapp/dist/build/h5`、`rayk-miniapp/dist/release/mp-weixin-dev` 和 `rayk-miniapp/dist/release/mp-weixin-prod-lan`。

## 2026-08-17 健康报告 PDF 下载兼容与文件名

- 客户健康报告详情页的 H5 下载改为使用当前 `patientName` 生成 `XXX健康报告.pdf`；微信端下载改为“下载 -> 按用户名保存 -> 打开 PDF”，补充下载/打开失败提示和 loading 生命周期，兼容微信开发者工具；基础库不支持自定义文件路径时自动回退到临时文件打开。
- Java 健康报告内容接口的 `Content-Disposition` 文件名改为从权限范围内的患者姓名生成 `XXX健康报告.pdf`，无姓名时使用 `健康报告.pdf`。
- 验证：前端 `type-check`、`lint`、H5、微信开发包和微信生产局域网包均通过；Java 跳过测试构建镜像并启动后 `rayk-server` 已 healthy，数据库迁移已验证为最新版本。Docker 全量 Maven 测试仍被既有 `VoiceReminderTextFactoryTest` 睡眠文案断言失败阻断，未将其宣称为全量通过。

## 2026-08-17 小程序虚拟支付会员接入

- 复核原会员支付实现：原代码是标准小程序 JSAPI 支付（微信支付 API v3 下单、`prepay_id`、`uni.requestPayment`），不适用于用户已开通的小程序虚拟支付权限。
- 年度会员改为虚拟支付道具直购模式：Java 服务端生成 `signData`、`paySig`、`signature`，小程序端调用 `wx.requestVirtualPayment`；订单金额、商品 ID、环境、用户 OpenID 和交易号均在发货通知中复核后才开通会员，重复通知使用订单行锁保证幂等。
- 前端在微信支付回调成功但服务端发货通知尚未落库时显示“支付结果确认中”并锁定重复支付按钮，避免复用同一个 `outTradeNo` 重复发起支付。
- 新增虚拟支付发货通知接口：`POST /api/payments/wechat/virtual/notify`。微信虚拟支付后台需订阅 JSON 推送到公网 HTTPS 地址；原标准支付 `/api/payments/wechat/notify` 代码保留为兼容路径，但年度会员不再走该路径。
- 微信登录成功后将当前 `session_key` 仅保存到 Redis 私有键空间，供服务端生成用户态签名；不写入日志、JWT、前端或 Git。开发 Compose 默认 `MEMBERSHIP_PAYMENT_ENABLED=false`，但允许通过未提交 `.env` 显式设置为 `true` 进行真实支付验收；开启前必须配置 AppKey、ProductID 和公网 HTTPS 回调。
- 新增配置：`WECHAT_VIRTUAL_APP_ID`、`WECHAT_VIRTUAL_MERCHANT_ID`、`WECHAT_VIRTUAL_OFFER_ID`、`WECHAT_VIRTUAL_APP_KEY`、`WECHAT_VIRTUAL_SANDBOX_APP_KEY`、`WECHAT_VIRTUAL_ENV`、`WECHAT_VIRTUAL_MODE`、`WECHAT_VIRTUAL_PRODUCT_ID`、`WECHAT_VIRTUAL_NOTIFY_URL`。用户已提供商户号 `1116509042`、OfferID `1450618433`，并指定回调域名 `xingxuyuan.com`；正式支付还必须配置 AppKey、ProductID，并确保该域名 DNS、HTTPS 证书和公网 443 已转发到 Nginx。
- 验证：Java 跳过测试构建成功并启动为 healthy，Compose 配置校验通过；前端 `type-check`、`lint`、H5、微信开发包和微信生产局域网包均已通过并同步。完整 Maven 测试仍受既有 `VoiceReminderTextFactoryTest` 睡眠文案断言失败影响，未将其宣称为全量通过。

## 2026-08-17 虚拟支付开发环境真实验收准备

- 虚拟支付回调地址已配置为 `https://xingxuyuan.com/api/payments/wechat/virtual/notify`；开发 Compose 改为默认关闭、通过未提交 `.env` 的 `MEMBERSHIP_PAYMENT_ENABLED=true` 显式开启。
- 当前 OfferID 为 `1450618433`，商户号为 `1116509042`。开发容器已重新构建并恢复 healthy，实际 Compose 配置仍为 `MEMBERSHIP_PAYMENT_ENABLED=false`，不会误触发扣款。
- 2026-08-17 从当前环境访问 `https://xingxuyuan.com/health` 的 TLS 握手失败，暂不能证明该域名的公网 HTTPS、证书和 443 到 Nginx 的转发已就绪；在微信后台配置回调前必须先完成这部分外部部署。
- 真机验收前还需在服务器未提交的 `.env` 中配置正式 AppKey、已发布且金额一致的 ProductID，并显式开启支付；AppKey 不进入 Git 或聊天记录。
- 新增 `compose.real-payment-dev.yml` 作为显式真实支付验收配置：强制开启 `MEMBERSHIP_PAYMENT_ENABLED=true`，使用 `xingxuyuan.com` 的 80/443 和证书目录，并要求 AppKey/ProductID 非空；普通 `compose.dev.yml` 仍默认关闭真实支付。

## 2026-08-17 电子版 PDF 自动评估失败排查

- 最新电子版 PDF 报告的 `LAB_REPORT_OCR` 任务已成功完成，文件为 `application/pdf`，失败点不在 PDF 原生解析或 OCR。
- 自动评估没有创建 `HEALTH_ASSESSMENT` 任务，原因是当前测试用户实际处于 `FREE_CUSTOMER`，免费 `AI_HEALTH_REPORT` 额度已被历史成功评估消耗；历史年度会员记录已经过期。因此后端按会员权益规则拒绝本次评估，报告被标记为 `AI_FAILED`。
- `LabReportVo` 现在返回安全的 `failureReason`，检验报告详情页会展示具体阻断原因；自动评估日志记录类型化错误码，不记录健康原文、手机号或密钥。会员校验仍保留，不通过前端或后端逻辑绕过额度。
- 恢复方式：为该用户完成真实会员支付并确认发货通知激活年度会员，或仅在开发环境使用会员开发切换，然后从检验报告详情页点击“重新生成评估与健康报告”。本次 OCR 结果会复用，不需要重新上传 PDF。
- 验证：Java Docker Maven 测试 65 项通过；前端 `type-check`、`lint`、H5、微信开发包、微信生产局域网包均通过；开发 API `http://192.168.0.100:8088/health` 返回 200，三个前端产物已重新同步。

## 2026-08-17 DeepSeek Pro 运行时切换修复

- 平台管理员已将 `ai_model_runtime_config` 中的唯一选中模型切换为 `deepseek-v4-pro`，数据库核对结果为 Pro `selected=1`、Flash `selected=0`。
- 修复 `rayk-ai/app/interpretation/service.py`：图片报告的 Qwen 直读仍只负责图片事实，DeepSeek 最终综合报告现在同样使用 Java 每次评估传入的后台实时模型选择；此前图片模式会错误回退到 `DEEPSEEK_MODEL` 环境默认值，可能绕过平台管理员切换。
- Java 和 AI 服务增加安全模型路由日志，只记录模型代码和图片数量/模式，不记录密钥或健康内容。下一次评估应在两端日志中显示 `deepseek-v4-pro`；若 AI 不可用才会按既有规则降级。
- 验证：定向 Python 健康评估回归测试 29 项通过；Java Docker 构建编译成功，运行镜像已重建并启动 healthy；全量 Java 测试仍有既有 `VoiceReminderTextFactoryTest` 睡眠文案断言失败（65 项中 1 项），与本次模型路由改动无关。下一次评估时 Java 与 AI 日志会分别记录实际模型代码。

## 2026-08-18 会员页面视觉与资源改造

- 客户会员中心已按普通客户、年度会员和开通会员三种状态改造：普通客户展示基础权益和方案入口，年度会员展示有效期、权益覆盖、会员专属服务和续费入口，开通页展示年度方案权益对比、开通后可获得内容和原有订单支付入口。
- 页面使用 `/api/client/membership/summary` 和 `/api/client/membership/plans` 的真实数据渲染权益状态、剩余次数、有效期和价格；未从健康数据接口取得的健康分不再伪造，显示为“待评估”。开发调试切换仍只在开发构建显示，支付和其他会员页面未改动。
- 用户提供的 27 个 SVG 资源按 `common`、`free-member`、`yearly-member`、`open-member` 分类归档到 `rayk-miniapp/src/pages-customer/static/member/`；此前放在 `src/assets` 会让微信生产压缩器处理约 27MB 的内嵌 PNG，触发 `terser EINVAL`。当前资源作为 `pages-customer` 分包静态文件发布，避免进入微信主包。
- 验证：`type-check`、`lint`、`build:h5`、`build:mp-weixin:dev`、`build:mp-weixin` 和 `sync:mp-weixin:prod` 均通过；构建产物需核对会员资源位于 `pages-customer/static/member` 分包目录，不再位于主包 `static/member`。

## 2026-08-18 关闭会员权益使用记录入口

- 按产品要求移除年度会员中心“续费与权益使用记录”入口，并去掉开通会员页“查看权益使用记录”的提示；订单、支付、历史记录页面和后端接口保留，不删除已有数据。
- 本次仅涉及客户会员页面展示，未改变会员权益核销、订单状态或微信虚拟支付流程。
- 验证：串行执行 `vue-tsc`、ESLint、H5 构建、微信开发包构建、微信生产包构建和两个 release 包同步均通过；四个输出目录的会员静态资源均为 27 个，构建产物中已无上述入口文案。构建时使用低并发以适配当前 Windows 页面文件限制。

## 2026-08-18 开通会员页结构优化

- 删除开通页“开通后可获得”卡片，将脱敏示例的“开通动态播报”恢复到年度会员权益对比上方；不读取其他客户的真实订单或会员记录。
- 优化权益对比标题、年度会员和免费客户左侧标识，改用独立视觉资源，保留方案价格、权益图标、订单创建和支付流程。
- 验证：`vue-tsc`、ESLint、H5、微信开发包、微信生产包及 release 同步均通过。

## 2026-08-18 开通会员页小组件 SVG 替换

- 使用用户提供的 `member_svg_replacements.zip` 中的 `membership-broadcast.svg`、`membership-compare.svg`、`membership-annual-badge.svg` 和 `membership-free-user.svg`，替换开通页动态播报、权益对比标题、年度会员标识和免费客户标识四个 CSS 小组件；额外的 `membership-broadcast-mini.svg` 一并归档备用。
- 新资源位于 `rayk-miniapp/src/pages-customer/static/member/replacements/`，仅替换视觉资源和页面引用，不改变会员方案、订单创建、支付或权益逻辑。
- 验证：此前已完成前端类型检查、Lint、H5、微信开发包和微信生产局域网包构建；本次修复重新完成微信开发包构建和 release 同步，并核对开发包请求已改用新的局域网地址。

## 2026-08-18 微信开发包无法进入修复

- 排查确认 Docker、Nginx 和 Java 服务均为 healthy，`localhost:8088/health` 正常；微信开发包失败原因是 `.env.development` 仍使用旧局域网地址 `192.168.0.100`，当前电脑 WLAN 地址已变为 `192.168.0.101`，导致 `mock-login` 和 `settings` 请求在到达 Nginx 前被拒绝。
- 已将未提交的 `rayk-miniapp/.env.development` 更新为 `http://192.168.0.101:8088`，并重新构建开发微信包；真实手机联调时需让手机与电脑处于同一 Wi-Fi，若路由器再次分配新地址，需要同步更新该文件后重建。
- 本次验证：`http://192.168.0.101:8088/health` 返回 200，开发 `mock-login` 返回 200；`build:mp-weixin:dev` 和 `sync:mp-weixin:dev` 均通过，`dist/release/mp-weixin-dev` 已更新。

## 2026-08-18 微信主包超限修复

- 微信开发者工具检测到主包约 27.9MB，超过 4MB 上限；体积来源是会员 SVG 内嵌 PNG，原先位于根 `src/static/member/`，会随主包复制。
- 已将会员资源移动到 `src/pages-customer/static/member/`，并把会员页面引用改为 `/pages-customer/static/member/...`。会员页面本来就在 `pages-customer` 分包中，因此视觉资源现在与页面一起按分包加载，不改变会员路由、订单、支付和权益逻辑。
- 复杂插画仍以 `.svg` 文件提供前端接口，但内嵌 PNG 已按实际显示尺寸缩放压缩；完全相同的会员图标统一引用 `common` 目录并删除重复副本，会员资源源码总量由约 26.4MB 降至约 0.80MB。
- 已验证三份微信产物均不含根 `static/member`：主包约 0.73MB，`pages-customer` 分包约 0.97MB，递归总包约 1.79MB；会员资源均位于 `pages-customer/static/member`。H5 资源路径也已同步验证。
- 已更新同步脚本：微信开发者工具占用 release 目录时，会递归镜像并清理旧子目录资源，不再残留历史会员大图。`type-check`、`lint`、`build:h5`、`build:mp-weixin`、`build:mp-weixin:dev`、`sync:mp-weixin:prod` 和 `sync:mp-weixin:dev` 均通过。微信开发者工具需重新导入当次生成的 `dist/release/mp-weixin-dev`。

## 2026-08-18 开通会员页播报与底部提示优化

- 播报卡片移除“示例动态”标签，改为脱敏的会员开通动态，例如“王** 刚刚开通年度健康会员”“李** 已解锁会员专属服务”；固定文案和时间区域宽度，避免气泡、文字和时间互相遮挡，轮播周期由 16 秒调整为 8 秒。
- 删除开通页底部的会员协议、自动续费规则和“支持微信支付”提示展示；支付按钮和订单创建逻辑保留，不影响实际支付流程。

## 2026-08-18 会员权益概览卡片优化

- 会员中心概览卡片新增状态说明，左侧保留真实权益数量并补充基础权益总数；底部统计改为“已解锁权益 / 待解锁权益 / 权益覆盖率”，减少“有效权益”等含义不清的表述。
- 健康分当前没有真实分值时改为灰色空环，仅显示“待评估”，不再用蓝色进度环制造已有分数的视觉暗示；会员中心残留的“统一健康报告”名称同步改为“AI 健康报告”。
- 验证：`vue-tsc`、ESLint、`build:h5`、`build:mp-weixin`、`build:mp-weixin:dev`、`sync:mp-weixin:prod` 和 `sync:mp-weixin:dev` 均通过。

## 2026-08-18 取消 AI 报告重新解读

- 客户已发布检验报告不再允许再次提交 AI 评估；后端移除 `PUBLISHED → AI_REPORT_REGENERATE` 分支，已发布报告的原有 AI 结果、健康报告和 PDF 不受影响。
- 保留首次 AI 健康报告生成，以及 `AI_FAILED`、OCR 失败和图片直读失败后的本次任务重试；会员年度额度继续使用 `AI_HEALTH_REPORT`，不再消耗“报告重新解读”额度。
- 新增 `database/migrations/V39__remove_ai_report_regeneration.sql`，软停用 `AI_REPORT_REGENERATE` 权益和年度方案映射，保留历史使用流水；会员中心“AI 健康报告”卡片改为展示正常 AI 报告权益。
- 已发布报告禁止继续追加文件并重新发起处理，避免通过上传页绕过取消的重解读流程。Java 服务重建并执行 Flyway V39 后，数据库配置才会在运行环境生效。

## 2026-08-19 微信开发包再次无法进入修复

- 排查确认 Docker、Nginx、Java 和 AI 服务均为 healthy，`http://127.0.0.1:8088/health` 与 `http://192.168.0.100:8088/health` 均返回 200。
- 本机 WLAN 地址从此前的 `192.168.0.101` 重新变为 `192.168.0.100`，但 `rayk-miniapp/.env.development` 仍指向旧地址，导致微信开发包请求失败。
- 已将开发 API 地址改为 `http://192.168.0.100:8088`，重新执行 `build:mp-weixin:dev` 并原地同步 `dist/release/mp-weixin-dev`；开发者工具需重新编译/刷新当前项目。
- `scripts/build-mp-weixin-dev.mjs` 已增加局域网 IPv4 自动探测；后续 DHCP 地址变化后重新构建开发包会自动选择当前 RFC1918 地址，也可通过 `VITE_DEV_API_BASE_URL` 显式覆盖。

## 2026-08-19 修复开发包模拟会员报错

- 原因不是前端按钮或接口地址，而是服务此前只用基础 `compose.yml` 启动，导致 `MEMBERSHIP_DEVELOPMENT_MODE=false`；开发包仍显示调试按钮，后端则按生产规则返回 `AUTH_FORBIDDEN`。
- 已使用 `docker compose -f compose.yml -f compose.dev.yml up -d rayk-server` 重新启动开发服务，开启开发会员切换；未删除任何数据卷。
- 已通过真实接口回归：客户调用 `/api/client/membership/dev/switch` 传入 `YEARLY` 返回 `ACTIVE / AI_HEALTH_YEARLY / 364 天`，`/health` 返回 200。
- 本地开发必须叠加 `compose.dev.yml`；只执行基础 `docker compose up` 会关闭模拟会员、开发登录等开发专用能力。

## 2026-08-19 会员中心健康分动态显示

- 会员中心不再固定显示“待评估”：进入页面时读取当前客户 `/api/v1/me/assessments`，取最近一次 `SUCCESS` 评估，并按其中状态为有效且分数在 0–100 的健康维度计算平均分，四舍五入后显示在健康分仪表盘。
- 没有成功评估、评估没有有效维度或接口暂时不可用时，继续显示灰色空环和“待评估”，不会用默认分数填充，也不会影响会员权益展示。
- 已更新 README 使用说明；`type-check`、ESLint、H5、微信开发包、微信生产包及两个 release 包同步均已完成，三个前端输出目录已核对。

## 2026-08-19 修复 Docker Desktop 网关连接反复失败

- 本次不是局域网地址变化：电脑 WLAN 仍为 `192.168.0.100`，开发包仍指向 `http://192.168.0.100:8088`；Java、AI、Nginx 容器内部均 healthy，但宿主机访问 8088 建立 TCP 连接后被 Docker Desktop 端口转发直接断开，且请求未进入 Nginx 日志。
- 已安全重建 Nginx 容器，未停止或删除业务数据卷；验证 `http://127.0.0.1:8088/health` 和 `http://192.168.0.100:8088/health` 均返回 200，Nginx healthy。
- `scripts/start-dev.ps1` 现在会在业务服务启动后自动执行 `docker compose -f compose.yml -f compose.dev.yml up -d --force-recreate nginx`，并通过宿主机端口执行 `/health` 检查；只刷新网关端口转发，降低 Docker Desktop 休眠、WLAN/WSL 网络切换或后端容器重建后的复发概率。

## 2026-08-19 会员中心仪表盘改版

- 将会员权益和健康分的两个圆环改为双指标卡片：卡片分别展示核心数值、指标标签、横向进度条和状态说明，底部三项权益统计保留不变。
- 健康分仍使用最近一次成功评估的有效维度平均分；没有有效评估时展示“待评估”和空进度条，不改变接口、权益计算或评估数据。
- 验证：`vue-tsc`、ESLint、`build:h5`、`build:mp-weixin:dev`、`build:mp-weixin` 和 `sync:mp-weixin:prod` 均通过；H5、微信开发包和微信生产局域网包已重新生成。

## 2026-08-19 会员权益按产品表统一

- 新增 `database/migrations/V40__align_membership_rights_with_product_table.sql`，不修改已执行迁移：免费客户 AI 健康评估、健康拍、吃饭语音提醒、睡眠语音提醒统一为各 3 次；年度会员 AI 健康评估改为会员期内不限次数，首次健康随访保留 1 次，健康拍继续为每日 1 次，持续随访和两类语音提醒继续按会员期使用。
- 同步 `application.yml`、`compose.yml` 和 `.env.example` 的免费额度默认值，避免部署环境变量仍把新权益覆盖为 1。当前 Docker 数据库已成功执行 Flyway V40，`membership_plan_benefit` 实际核对结果与产品表一致。
- 健康拍历史和指标趋势增加后端范围校验：免费客户只能查看近 3 天基础数据，年度会员可以查看完整历史；健康档案和体检报告上传、保存、查看仍对两类用户保留，不因历史查看范围调整而隐藏原报告。
- 开通会员页增加截图对应的 13 项权益对比表，显示价格、AI 评估/报告次数、随访、健康拍、历史记录、趋势和语音提醒；会员有效期仍由会员中心显示，不在对比表中单独列行；保留原有订单创建与支付流程。
- 验证：Java Docker Maven 构建成功，65 项测试通过；Docker 服务 healthy，Flyway 已到 v40；前端 `type-check`、ESLint、H5、微信开发包、微信生产包及两个 release 包同步均通过。生产环境仍需部署新 Java 镜像并执行 Flyway，不能只替换前端包。

## 2026-08-19 会员开通页免费用户说明文案

- 开通会员页免费用户卡片的说明已从“健康档案和体检报告永久保存、随时查看”调整为“体验基础健康管理服务”，不改变实际权益和权限逻辑。
- 已重新通过前端类型检查、ESLint、H5、微信开发包、微信生产局域网包构建，并确认三端产物包含新文案。

## 2026-08-19 拆分 AI 健康评估与 AI 健康报告权益

- 新增 `V41__split_ai_assessment_and_report_entitlements.sql`，新增 `AI_HEALTH_ASSESSMENT` 权益；免费客户 AI 健康评估与 AI 健康报告各 3 次，年度会员两项分别不限次数。
- AI 评估提交流程现在同时预占两项权益，AI 流程成功后分别确认，失败时分别释放；历史综合评估产生的旧 `AI_HEALTH_REPORT` 流水会在评估额度统计中兼容计入，避免历史使用被遗漏。
- 年度会员中心的“AI 健康评估”和“AI 健康报告”卡片状态统一显示“已解锁”，免费客户评估卡片改为读取独立的 `AI_HEALTH_ASSESSMENT` 权益。
- 已同步新增免费评估额度配置 `MEMBERSHIP_FREE_AI_ASSESSMENT_TRIAL`，默认值为 3；生产部署需执行 V41 后再重建 Java 服务。

## 2026-08-19 修复模拟年度会员无权限

- 原因是微信开发包显示了开发调试卡片，但 Java 容器此前只使用基础 `compose.yml` 启动，实际 `MEMBERSHIP_DEVELOPMENT_MODE=false`，后端按安全规则返回 403。
- 已使用 `docker compose -f compose.yml -f compose.dev.yml up -d` 重新创建开发服务，确认 `MEMBERSHIP_DEVELOPMENT_MODE=true`；开发客户调用 `/api/client/membership/dev/switch` 切换 `YEARLY` 已返回 HTTP 200、`ACTIVE` 和 `AI_HEALTH_YEARLY`。
- 后续本地开发必须使用 `scripts/start-dev.ps1` 或叠加 `compose.dev.yml`，不能只执行基础 `docker compose up -d`；生产 Compose 仍保持开发模拟接口关闭。

## 2026-08-20 修复新域名正式包配置

- 已将未提交的 `rayk-miniapp/.env.production` 从旧域名 `xingxuyuantech.com` 切换为 `https://xingxuyuan.com`，并在 `rayk-miniapp/.env.example` 增加正式域名配置说明；不把环境密钥或本地环境文件提交到 Git。
- 已重新完成前端类型检查、ESLint、H5、微信开发包和微信生产包构建，并同步 `dist/release/mp-weixin-dev` 与 `dist/release/mp-weixin-prod-lan`；产物请求地址已核对为新域名。
- 本地证书 `crets/xingxuyuan.com_nginx` 的 SAN 包含 `xingxuyuan.com` 和 `www.xingxuyuan.com`，有效期至 2026-11-15；已备份远程旧部署，并将新证书、Nginx 配置和当前 H5 产物部署到 `/opt/zhiyu-health`。
- 已在腾讯云 DNSPod 增加根域名和 `www` A 记录，均指向 `62.234.44.238`。此前“怎么还不行”的直接原因是根域名没有 A 记录，旧的 `_dnsauth` TXT 记录不能提供网站解析；DNS、HTTPS 和站点已实测恢复：`https://xingxuyuan.com/health` 与 `https://www.xingxuyuan.com/health` 均返回 200，首页返回 200，证书主题为 `xingxuyuan.com`。
- 已用当前 Java 源码重新构建 `rayk-server` 镜像并执行 Flyway，远程 MySQL、Redis、MinIO、AI、Java、Nginx 容器当前均为 healthy。AI 镜像重建曾因远程 Python 包镜像下载长时间无响应未完成，当前运行的是已缓存且健康的 AI 镜像，需另行安排 AI 镜像升级验证。
- 远程已保留真实支付配置的非敏感参数，但 `MEMBERSHIP_PAYMENT_ENABLED=false`；之前在对话中暴露过的旧 AppKey 不得继续使用，必须在腾讯云轮换后再通过服务器密钥配置启用真实支付。

## 2026-08-20 开发环境启用真实微信虚拟支付测试

- 按用户要求将之前提供的微信虚拟支付 AppKey 写入远程服务器 `/opt/zhiyu-health/.env`，未写入代码、日志或 Git，也未在输出中回显。
- 使用 `compose.real-payment-dev.yml` 强制重建 `rayk-server` 和 Nginx；容器内已核对 AppKey 存在、`MEMBERSHIP_PAYMENT_ENABLED=true`、商品 ID 为 `vip_year_399`，服务健康检查返回 200。
- 当前仅用于开发环境真实支付联调，测试结束后必须轮换该 AppKey；不得将当前密钥状态直接作为正式生产安全状态。

## 2026-08-20 修复生产包微信登录凭证失败

- 根因已确认：生产包 `rayk-miniapp/src/manifest.json` 使用企业 AppID `wxf6f4549c8c962948`，但远程 `/opt/zhiyu-health/.env` 仍保留旧 AppID `wx47c756d2ffebe0e9`；微信 `code2session` 因登录码与 AppID 不匹配返回 `40029 invalid code`。
- 已将远程微信登录配置切换到生产包对应 AppID，并同步本机部署环境中对应的 AppSecret；通过微信 `client_credential` 接口验证 AppID/AppSecret 匹配，未输出密钥。
- 已强制重建远程 Java 容器；容器内核对 AppID、AppSecret 和虚拟支付 AppKey 均已加载，`MEMBERSHIP_PAYMENT_ENABLED=true`，HTTPS `/health` 返回 200。
- 生产微信包自身请求地址已核对为 `https://xingxuyuan.com`，AppID 已核对为 `wxf6f4549c8c962948`。重新导入 `dist/release/mp-weixin-prod-lan` 后，必须重新点击一次登录；微信登录码为一次性凭证，修复前生成的旧码不能重复使用。

## 2026-08-20 修复生产会员资源和虚拟支付关闭订单重试

- 已确认会员 SVG 源文件和 H5 产物均存在，但 UniApp 的微信小程序构建目录没有自动复制 `src/pages-customer/static/member/`，导致生产小程序会员权益卡片图标空白。同步脚本现在会在镜像 release 包前显式复制该目录，资源仍位于 `pages-customer` 分包，不会回到主包；本次 H5、微信开发包和微信生产局域网包均核对为 15 个 SVG 资源。
- `requestVirtualPayment:fail ORDER_CLOSED` 的原因是微信虚拟支付的 `outTradeNo` 只能使用一次，原前端在失败后重复使用同一业务订单号。支付失败且错误包含 `ORDER_CLOSED` 时，前端现在自动创建一个同方案的新业务订单并只重试一次；支付成功后的发货通知确认和会员开通逻辑不变。
- `npm run build:mp-weixin` 现在会自动同步 `mp-weixin-prod-lan`，避免构建目录与实际验收包不一致。已通过 `vue-tsc`、ESLint、H5 构建、微信开发包构建、微信生产包构建和 `git diff --check`。新的微信包仍需重新导入开发者工具并上传/体验，服务器 H5 资源已实测 200；未在真实微信支付界面完成扣款验收。

## 2026-08-20 修复虚拟支付 PAY_SIG_INVALID

- 真机回传的最新错误为 `requestVirtualPayment:fail PAY_SIG_INVALID`。代码复核发现 Java 端把支付签名原串写成了 `"/requestVirtualPayment&" + signData`，而微信要求使用不带前导斜杠的 `"requestVirtualPayment&" + signData`；AppKey、OfferID、商品 ID 和环境配置仍从服务器密钥/环境变量读取，未写入源码。
- 已修正支付签名 API 名称，并新增 Java 单元测试固定校验 `paySig` 与用户态 `signature` 的 HMAC 原串，防止再次引入前导斜杠。
- 本次修复需要重新构建并部署 Java 镜像，随后重新生成微信包；服务端与客户端必须同时更新后再用新订单测试。之前的失败订单不再复用。真实扣款和发货通知仍需用户在微信真机完成最后验收。

## 2026-08-20 会员真实支付确认页视觉优化

- 会员订单页导航标题由“订单结果”改为“确认支付”，页面改为与会员中心一致的浅绿渐变风格：增加品牌与安全支付标识、年度会员价格卡、支付后可享服务概览、支付说明和更清晰的主次按钮层级。
- 文案按支付状态动态显示“准备开启年度会员”“正在打开微信支付”“权益确认中”和“会员已开通”，真实支付按钮改为“立即支付 ¥399/年”，不再展示“微信虚拟支付并开通”等技术化表述；支付签名、订单创建、关闭订单重试、发货通知确认和会员激活逻辑均未修改。
- 微信底部原生支付面板由微信客户端控制，页面无法修改其字体、布局或商品展示名称；本次仅优化弹出面板下方的小程序页面。已重新通过 `vue-tsc`、ESLint、H5、微信开发包和微信生产局域网包构建，三个输出目录均已同步。

## 2026-08-21 PDF Qwen OCR 切换为 qwen3.7-flash

- 按当前测试要求，PDF OCR 的 Qwen 云模型已从 `qwen3.5-ocr` 切换为 `qwen3.7-flash`；Python 默认值、根目录 `.env.example`、AI 服务 `.env.example`、Compose 默认值、本地 `.env` 和定向测试已同步。
- PDF 处理结构未合并或降级：电子 PDF 仍先走原生文本/表格/检查小结解析，Qwen 只读取 PDF 渲染页并作为视觉补充；本地质量校验和 Paddle/PDF 回退保持不变。图片直读仍是独立的 Qwen Vision 路径。
- Qwen OCR 错误信息和 `OcrRecognizeData.engine` 现在使用实际配置的模型名，便于确认下一次 PDF 识别是否真正调用 `qwen3.7-flash`；代码切换不等于线上容器已重建或真实样本验收。

## 2026-08-21 PDF OCR 增加按页备用模型级联

- PDF 页面现在先调用 `qwen3.7-flash`；只有该页请求失败、响应无效或没有提取出可用检验内容时，才对同一页调用 `qwen3.5-ocr`。图片直读路径仍只调用 Qwen Vision，不使用该 PDF 备用模型。
- 两个云模型都无法完成某一页时，PDF 任务整体进入既有本地 PDF 原生解析/PaddleOCR 降级，避免把部分成功结果与缺页结果直接交给健康评估；电子 PDF 的原生文本、表格和检查小结仍保留为校验基线。
- 新增 `QWEN_OCR_FALLBACK_MODEL=qwen3.5-ocr` 配置。引擎标识会记录 `qwen3.7-flash>qwen3.5-ocr+PDF-native-validation`，便于确认是否发生备用模型调用；定向 OCR/视觉回归共 19 项通过，尚未用真实 PDF 和线上容器完成生产验收。

## 2026-08-21 PDF OCR 并发压力测试配置

- 按测试要求将 `QwenOcrSettings` 的本地并发配置上限从 6 提高到 50；当前开发环境 `.env` 设置 `QWEN_OCR_CONCURRENCY=25`。这只是本地请求工作线程数，不等同于百炼官方 RPM/TPM 限额，正式环境仍应按实际额度和 429 情况评估。
- 已通过定向 OCR/视觉回归 19 项，重建 `rayk-ai` 镜像并重启容器；容器内实际核对为 `qwen3.7-flash`、备用 `qwen3.5-ocr`、并发 25，AI 健康检查返回 200。
- 已重新生成 H5、微信开发包和微信生产局域网包；尚未用同一份真实 PDF 对比并发 3 与 25 的实际耗时、内存和限流情况。

## 2026-08-21 Qwen 图片与 PDF 模型 ID 更新

- 按当前测试要求，图片直读和 PDF 页级 OCR 的主模型统一改为 `qwen3.7-flash-2026-07-15`；`QWEN_OCR_MODEL` 与 `QWEN_VISION_MODEL` 的本地默认值、Compose 默认值、Python 默认值和测试断言已同步。
- `qwen3.5-ocr` 仍只作为 PDF 单页主模型失败后的备用模型，图片直读不会调用该备用模型；PDF 原生解析、PaddleOCR 和健康报告生成链路未改变。
- 已重建并重启 `rayk-ai`；容器内实际核对为 `QWEN_OCR_MODEL=qwen3.7-flash-2026-07-15`、`QWEN_VISION_MODEL=qwen3.7-flash-2026-07-15`、备用 `qwen3.5-ocr`，健康检查返回 200。后续可从 AI 请求日志和 `ai_task.engine_version` 核对实际调用的精确模型 ID。

## 2026-08-21 修复评估误失败并增加识别进度条

- 修复评估链路的状态污染：健康评估和健康报告已经成功落库后，如果 PDF 版本或对象存储产物保存阶段异常，不再把检验报告反向标记为 `AI_FAILED`；会记录带报告 ID 和异常类型的服务端日志，并尝试恢复缺失的已发布 PDF 产物。
- 增加历史状态自修复：详情接口发现旧记录为 `AI_FAILED`、但对应的健康评估为 `SUCCESS` 且健康报告为 `PUBLISHED` 时，会自动恢复为 `PUBLISHED`，并补齐进度为 100%，不会重新调用模型或重复扣除权益。
- `lab_report` 新增 `processing_progress` 和 `processing_message` 字段（迁移 `V42__lab_report_processing_progress.sql`）。图片和 PDF 都会在上传、识别、整理、AI 评估和报告生成阶段更新进度；客户检验报告详情页轮询该字段，动态展示百分比、进度条和阶段提示。前端进度是阶段性可观测估计，服务端未提供模型 token/page 级流式进度时不会伪装成精确模型进度。
- PDF 与图片仍保持独立路径：PDF 继续按 `qwen3.7-flash → qwen3.5-ocr → PDF 原生解析/PaddleOCR` 处理，图片继续走 Qwen Vision 直读；本次只增加两条路径共同使用的报告状态进度字段，没有合并 OCR 实现。
- 验证：Docker Java 构建阶段 Maven 测试通过，Java 容器健康并已成功执行 Flyway V42；Python 定向 OCR/视觉回归 19 项、`compileall`、前端 `type-check` 和 ESLint 通过；H5、微信开发包、微信生产局域网包均重新构建并同步，`git diff --check` 无空白错误。真实图片/PDF 新上传仍需在微信端或 H5 做一次端到端验收。

## 2026-08-21 开发三羊健康助手首版

- 新增数据库迁移 `V43__medical_assistant.sql`，建立客户隔离的对话与消息表，并新增 `AI_MEDICAL_ASSISTANT` 权益：免费客户 3 次，年度会员期内不限次数。已执行迁移的环境必须先部署 Java 镜像，再由 Flyway 自动执行 V43；不要修改已执行迁移。
- Java 新增 `/api/client/medical-assistant` 客户接口：创建/列出/读取本人对话、发送消息。后端按当前登录客户数据范围加载健康档案、最近一次评估、健康拍体征和近期健康报告；前端 patientId 不参与授权。AI 调用前预占额度，回答与消息落库后确认，调用失败释放。
- Python 新增 `/api/v1/medical-assistant/answer`，暂用现有 DeepSeek 配置中的 `deepseek-v4-flash`。服务包含急症关键词拦截、结构化 JSON 校验、上下文来源标记、超时/上游错误向 Java 传递和不诊断免责声明；不读取图片/PDF，也没有改动 PDF 与图片 OCR 路径。
- 小程序新增 `pages-customer/medical-assistant/index`，页面名为“三羊健康助手”，提供资料范围提示、快捷问题、对话气泡、依据标签、急症提示和免责声明；已从客户菜单与“我的”进入。首版非流式文字对话，未接入图片/文件和语音。
- 已通过 Python 助手定向测试 3 项、`compileall`、前端 `vue-tsc`、ESLint；Docker Java + AI 构建成功，Java Maven 全量测试 66 项通过。尚未执行三端前端正式构建、Flyway 在真实开发数据库上的 V43 验收和真实 DeepSeek 端到端对话验收，生产上线前需分别完成这些验证。

## 2026-08-21 三羊健康助手改为 Qwen 流式对话

- 三羊健康助手固定使用 `qwen3.7-flash-2026-07-15`；模型名称不展示在客户端，也不跟随平台管理员的健康评估/报告模型切换。
- Python 新增 `/api/v1/medical-assistant/answer/stream`，通过上游流式响应提取 `reply` 字段并发送 SSE 增量事件；急症安全拦截和服务未配置时也会以相同事件格式降级输出。
- Java 新增 `/api/client/medical-assistant/conversations/{conversationId}/messages/stream`，负责客户权限校验、额度预占、SSE 转发、完整结构化答案落库和额度确认；流式失败会释放额度，原一次性发送接口仍保留兼容。
- Nginx 对助手路径关闭代理缓冲和 gzip，避免 SSE 事件被网关攒到最后一次性返回。小程序端在微信使用 `wx.request` 分块回调，H5 使用 Fetch ReadableStream，并在不支持流式能力时回退一次性接口。
- 小程序页面已去除模型标签和输入区说明小字，发送按钮改为带上行箭头的主操作按钮；正在生成时直接在助手气泡内显示动态等待点。
- 本次改动未触碰 PDF、图片 OCR 或报告生成路径。Python 测试、前端类型检查和 ESLint 已通过；Java Docker 构建曾因 SSE 泛型推导报错，已修复后需重新完成 Docker 构建、容器重启及 H5/微信开发/微信局域网包构建。真实 Qwen 线上流式对话仍需登录后验收。
- 本次补充了助手专用 `QWEN_ASSISTANT_*` 配置，默认模型为 `qwen3.7-flash-2026-07-15`。助手不再复用 `QWEN_VISION_*`、`QWEN_OCR_*` 的工作空间地址；未填写助手地址时使用 DashScope 兼容端点，仅为兼容现有开发环境按顺序复用已有 Qwen API Key。报告综合解读仍独立使用 `DEEPSEEK_*`，不会因助手切换而改变。

## 2026-08-21 健康助手模型与流式错误处理收尾

- 健康助手的专用模型已固定为 `qwen3.7-flash-2026-07-15`；容器实际加载配置已核对为该模型，客户端不展示模型名称。报告综合解读仍使用平台管理员选择的 DeepSeek，不受本次切换影响。
- 修复 Qwen 流式请求遇到 HTTP 异常时的错误处理：流式响应未读取时不再调用 `response.json()`，避免触发 `ResponseNotRead` 并留下空的 HTTP 200；现在会记录模型、异常类型和状态，并向 Java/小程序发送可结束的安全 `error` SSE 事件，额度释放逻辑保持不变。
- Python 助手定向测试 7 项通过，`compileall` 通过；最新 `rayk-ai` 容器健康检查通过。现场合成请求曾收到 Qwen `ConnectTimeout`，已确认客户端能收到终止事件且容器日志无 ASGI traceback；这属于上游网络/供应商响应，真实账号仍需在当前网络和额度条件下验收成功回答。
- 本次只修改健康助手对话模型与流式链路，没有修改 PDF、图片 OCR、健康评估或健康报告生成路径。前端三端构建产物沿用本次助手页面改动后的已验证版本：`dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-prod-lan`。

## 2026-08-21 修复健康助手流式回答卡住

- 根因已确认：Qwen 在本次请求中很快返回了 HTTP 400 错误事件，但 Java 使用 `publishOn(boundedElastic)` 处理流时没有恢复租户上下文；释放 `AI_MEDICAL_ASSISTANT` 预占额度时数据库查询抛出“租户上下文缺失”，异常被 Reactor 作为 `onErrorDropped` 丢弃，SSE 没有发出结束事件，小程序因此一直显示三个等待点。
- Java 流式回调现在显式恢复并保留当前租户上下文；权益释放/确认也使用同一租户上下文，释放失败仍会记录日志并保证向客户端发送结束错误事件。小程序 H5/微信端在连接异常结束但未收到 `done/error` 时也会主动解除发送状态并提示重试，不会永久卡住。
- 已重新构建并重启 Java 容器，Java 编译构建成功，服务健康；Nginx 配置检查通过。前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均已重新完成。
- 最新验证中 Qwen 上游仍可能返回 HTTP 400/连接异常，但现在会在短时间内显示“健康助手暂时未完成回答，请稍后重试”，并释放预占权益；这属于当前 Qwen 上游请求/配置状态，不再表现为页面无限等待。
## 2026-08-21 修复健康助手 Qwen 端点与流式错误

- 用户实测健康助手仍提示“暂时未完成回答”。日志确认真正原因不是前端等待：健康助手路由错误复用了报告解读服务的 DeepSeek 配置，把 `qwen3.7-flash-2026-07-15` 发送到了只支持 DeepSeek V4 的工作区，Qwen 端返回 `400 invalid_request_error`。
- 已将 `rayk-ai/app/api/router.py` 改为独立装配 `MedicalAssistantSettings`；健康助手不再继承 DeepSeek 或 OCR/视觉服务的工作区地址。无专用工作区时使用 DashScope 兼容端点，API Key 仍兼容复用已有 Qwen Key。
- 同时为助手上下文增加独立预算：评估快照、报告摘要和对话历史分别裁剪，输出上限调整为 4000，避免完整健康资料触发模型上下文 400。服务会记录脱敏后的上游错误码，不记录密钥或健康正文。
- Java SSE 异步续接允许已完成认证请求的 ASYNC dispatch，避免流结束时被 Spring Security 二次拦截；此前的租户上下文恢复和前端 EOF 收口修复继续保留。
- 验证：本地 Qwen 端点使用 `qwen3.7-flash-2026-07-15` 返回 `done`；Java 测试 66 个通过，健康助手 Python 测试 7 个通过。生产正式验收仍需在当前微信登录会话下实测。

## 2026-08-22 修复开发包登录地址过期

- 用户反馈微信开发者工具无法登录。排查发现服务和当前开发配置使用 `http://192.168.0.100:8088`，但现有 `mp-weixin-dev` 包内仍编译着旧地址 `http://192.168.0.101:8088`；旧地址已不可达，因此请求未进入 Nginx/Java。
- 未修改登录业务逻辑。重新执行前端类型检查、ESLint、H5、微信开发包和微信生产局域网包构建；开发包内 API 地址已核对为 `192.168.0.100:8088`。
- 验证：`192.168.0.100:8088/health` 返回 200，模拟登录接口通过该地址返回 200；`192.168.0.101:8088` 不可达。微信开发者工具需重新编译/刷新已导入的 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev`。

## 2026-08-21 修复健康助手 Java SSE 解码丢消息

- 根因已进一步确认：Python/Qwen 实际已经返回流式结果，但 Java `WebClient` 使用 `bodyToFlux(String.class)` 后又按原始 `data:` 文本和空行分隔符解析；Spring 已经把 SSE 解码成事件对象，导致 Java 没有消费到任何 `delta/done`，最终只触发 `onComplete` 并向客户端发送“暂时未完成回答”，助手消息也没有落库。
- `AiServiceClient` 现在直接消费 `ServerSentEvent<String>` 并提取事件 data，再交给原有结构化事件处理、额度确认和消息落库逻辑；前端、PDF、图片 OCR、健康评估和报告生成路径未修改。
- 已使用已认证的开发客户完整回归：接口 HTTP 200，收到 27 个 `delta`，收到 `done` 且无 `error`；MySQL 最新消息角色为 `ASSISTANT`，长度 168；Java 日志记录正常 `signal=onComplete`。Java Docker 构建阶段 Maven 66 项测试全部通过，容器健康。

## 2026-08-22 三羊健康助手对话历史与交互优化

- 小程序助手页面去掉用户可见的英文上下文字段、重复的助手标题和聊天气泡下方的“本次参考”展示；后台仍保留结构化依据用于追溯，不在聊天界面展开。
- 快捷问题和追问提示改为只填入输入框，不再点击即自动调用大模型；用户确认后点击发送才会提交。
- 新增可收起的“对话记录”侧栏，支持新建独立对话、切换历史对话和显示最近更新时间。每个会话仍由后端独立维护上下文，切换会话不会把其他会话的问题带入当前请求。
- 新增删除对话能力：前端二次确认，后端新增 `DELETE /api/client/medical-assistant/conversations/{conversationId}`，通过当前客户、租户和患者归属校验后对会话及消息执行 MyBatis 逻辑删除；删除当前会话后自动切换到其他会话，没有会话时自动创建新对话。不会影响健康档案、体检报告或评估数据。
- 对话记录入口已从自定义导航栏右侧移到首屏内容区的独立操作卡片，避开微信右上角原生“…”胶囊区域，降低误触；助手页继续使用自定义导航栏仅承载返回和标题。
- 助手头像替换为压缩后的羊头像 `rayk-miniapp/src/pages-customer/static/assistant/sheep-avatar.jpg`（384×384，约 30 KB），用于首屏标识和助手消息头像；发送按钮调整为 `128rpx × 72rpx`，与输入框同高，降低占用并保留明确主操作样式。
- 发送区统一为与输入框同高的紧凑主操作按钮，重置微信原生按钮的默认最小尺寸，避免在小程序端被撑大。
- 本次未修改 PDF、图片 OCR、健康评估和健康报告生成路径。已通过前端 `vue-tsc`、ESLint、H5/微信开发/微信生产局域网包构建；Java Docker 构建成功，Maven 66 项测试通过，开发客户创建/删除/列表回归通过，Java 容器健康。

## 2026-08-22 健康助手名称与适老化排版优化

- 客户端所有用户可见入口、导航标题、我的页面入口和健康助手运行时提示统一使用“健康助手”；内部路由、API、Java/Python 包名和 `AI_MEDICAL_ASSISTANT` 权益代码保持不变。
- 新增 `V44__rename_medical_assistant_benefit.sql`，将已存在的会员权益名称从“三羊健康助手”改为“健康助手”，避免会员权益页继续显示旧名称；不修改已执行的 V43 迁移。
- 助手页面按中老年可读性调整字号和行距：正文、快捷问题、输入框、历史记录和操作按钮均提高字号，并保留适合触控的按钮高度。
- 绿色首屏卡片改为紧凑布局：羊头像定位到右上作为视觉锚点，左侧文本保留可读宽度，减少无效留白；未改动 SSE、历史会话、删除会话、会员额度或 OCR/报告链路。
- 待验证：完成前端三端构建后，在 375px 小屏、微信开发者工具和真机上核对动态字号、换行与卡片视觉效果；Vue 专项 UI Pro Max 数据库没有匹配，采用通用移动端可读性与触控规范。

## 2026-08-22 健康助手首屏文案与操作区细化

- “健康管理陪伴”字号进一步放大；首屏说明改为“结合健康档案、评估和健康拍，帮你理清重点，找到下一步”，减少长句换行和首屏纵向占用。
- 输入框移除占位提示文字，底部健康管理说明小字移除，避免对中老年用户造成视觉噪音；医疗安全边界仍由回答内容、后端提示和急症拦截保留。
- 历史对话侧栏的“删除”改为带边框、背景和最小触控尺寸的按钮，解决手机端文字过小、难以点击的问题；未修改删除确认、权限校验和会话逻辑。
- 待验证：完成三端前端构建后，在微信开发者工具和真机上核对首屏换行、输入区高度及历史侧栏按钮触控范围。

## 2026-08-22 健康助手报告标签、头像与标题格式优化

- 首屏上下文标签及说明中的“健康评估”改为“健康报告”，与用户实际查看的报告内容保持一致；内部接口、权限和数据来源不变。
- 放大首屏羊头像和助手消息头像，同时为首屏文字保留避让宽度，避免小屏重叠；未修改头像资源和会话逻辑。
- 聊天消息渲染 `**标题**` 时转换为实际加粗文本并清理星号，普通正文、换行和流式增量显示保持不变。
- 待验证：完成前端三端构建后，在微信开发者工具和真机上核对头像比例、长消息换行、加粗标题和流式增量显示。

## 2026-08-22 修复助手实时日期时间与输入法确认栏

- 健康助手此前没有联网、天气、定位或搜索工具；时间问题直接交给 Qwen 后会出现日期错误（例如把当前日期回答成旧日期），天气问题也可能被模型凭记忆猜测。现已在 Python 助手服务增加明确能力边界：纯天气问题直接提示当前未接入实时天气，避免编造；天气联网能力尚未接入，仍需后续选择供应商、城市来源和用户授权方案。
- “现在几点”“当前时间”等纯时间问题不再调用 Qwen，由 AI 服务使用 `Asia/Shanghai` 固定时区的服务端时钟直接返回北京时间；普通健康问题的运行时上下文也会带上服务端当前时间和 `liveInternetAccess=false`，模型不能声称自己已联网。
- 小程序输入框增加 `show-confirm-bar=false` 并将键盘确认动作设为 `send`，用于去掉微信小程序输入控件的原生确认栏。截图中如果仍有系统输入法自己的“完成”工具条，那是 iOS/微信系统层控件，页面 CSS 无法控制；应用侧已无助手页面自定义“完成”文案。
- 验证：Python 助手定向测试 10 项通过；前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均构建成功并同步到 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-prod-lan`。尚未接入真实天气 API，也未在真机上确认系统键盘工具条是否由微信版本保留。

## 2026-08-22 调整健康助手首屏头像位置

- 首屏绿色介绍卡片的羊头像由 `140rpx` 调整为 `112rpx`，位置改为 `top: 36rpx; right: 48rpx`，并同步收窄圆角和阴影，使头像完整落在第一圈半圆环的视觉范围内，不再贴近或超出卡片边缘。
- 仅修改 `rayk-miniapp/src/pages-customer/medical-assistant/index.vue` 的首屏视觉样式；对话、历史会话、输入区、AI 调用、会员权益和报告/OCR 链路不变。
- 待验证：重新构建后在微信开发者工具和 375px 真机上核对头像与半圆环的相对位置。

## 2026-08-22 修复健康助手中文乱码与头像环形定位

- 健康助手 Java SSE、Python SSE 和 Java 全局 Servlet 响应现在显式使用 UTF-8，避免微信端在 `text/event-stream` 未声明字符集时把中文动态内容按 Latin-1/Windows-1252 解码。
- 小程序端对会话列表、会话详情、一次性降级响应以及流式 `delta/done` 增加兼容性文本修复；已经落库的旧乱码只在展示层恢复，不改写原始会话数据。
- 首屏第一圈、第二圈装饰环已整体向左下调整，第一圈圆心与右上头像中心对齐；头像仍保持在首屏卡片内部，未改动助手接口、对话历史或会员权益逻辑。
- 验证已完成：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均通过；Java Docker 构建及 66 项测试通过，Python 助手定向测试 10 项通过。`rayk-ai`、`rayk-server` 已重建并重启，开发网关 `/health` 返回 200。微信开发者工具需重新导入或编译 `dist/release/mp-weixin-dev`，真机仍需实际发送一条新消息确认供应商返回内容。

## 2026-08-22 区分健康助手新对话提示与回答追问

- 新对话欢迎卡片中的提示词支持点击直接发送给大模型；发送过程中不填入输入框，失败时也不会把提示词回填到输入框，用户可重新点击或自行输入。
- 助手回答后的追问提示仍为只读参考，不会自动发送，也不会进入输入框；输入框和底部发送按钮仍由用户主动确认后提交。
- 首屏两圈装饰环缩小为 `240rpx` 和 `360rpx`，保持与头像同一视觉中心并避开左侧标题、说明文字；对话接口、流式输出、历史会话和会员额度未修改。
- 验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均已完成；仍需在微信开发者工具和真机分别点击新对话提示、回答追问，确认前者新增用户消息并触发回答，后者无输入和消息变化。

## 2026-08-22 健康助手额度提示与会员权益表适老化优化

- `AI_MEDICAL_ASSISTANT` 的后端权益保持免费客户 3 次对话、年度会员期内不限次数；流式接口现在保留后端 `60401`（会员权益不足）错误码，避免额度用尽被前端误显示为普通网络失败。
- 免费客户额度用尽时，健康助手会弹出“健康助手次数已用完”提示，说明 3 次规则，并提供“开通会员”按钮跳转年度会员开通页；前端没有绕过 Java 权限校验或自行增加额度。
- 健康助手回答后的追问提示气泡改为更适合中老年阅读的居中布局，统一调整内边距、最小高度、字号和箭头定位，避免文字贴边或视觉偏移。
- 会员开通页权益表新增“健康助手”一行（免费客户 3 次对话、年度会员期内不限次数），表头和表格正文整体放大并提高行高与单元格留白，保留现有会员支付和其他权益逻辑。
- 已验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均构建成功，三个输出目录均已同步本次代码。仍需在微信开发者工具和真机分别验证回答追问气泡换行、免费客户第 4 次对话弹窗、弹窗跳转开通页以及小屏会员表格可读性。

## 2026-08-22 精简支付确认页与调整健康助手追问布局

- 支付确认页移除“支付后立即享受”三项权益卡片和底部免责声明，仅保留订单状态、支付通知和操作按钮；真实微信支付、模拟开通和订单状态轮询逻辑未修改。
- 健康助手回答后的追问提示气泡保留适老化字号和高度，文字改为左对齐，并让文字节点占满气泡内容区，避免居中布局造成阅读不自然。
- 已验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均构建成功并同步到三个输出目录；仍需在微信开发者工具和真机核对支付确认页内容、支付按钮以及追问文字换行。

## 2026-08-22 账户、会员权益表和消息时间文案调整

- “我的”页面移除用户可见的 `Version 0.1.0` 版本号，不影响构建版本或接口兼容。
- 开通会员页权益表中的“健康助手”改为“AI健康助手”，免费 3 次对话和年度会员不限次数规则不变。
- 消息中心的报告发布时间仅将 ISO 日期时间中的 `T` 替换为空格，显示为更易读的日期与时间格式，随访截止日期和后端时间数据不变。
- 已验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均构建成功并同步；仍需在微信开发者工具和真机核对“我的”页面底部、会员表格和消息中心时间显示。

## 2026-08-22 平台管理员按手机号管理客户会员

- 旧的内测包已移出当前 release 目录，归档位置为 `E:\health-archive\mp-weixin-internal-20260822`；当前上传新内测版本使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev`。
- 新增接口 `GET/PUT /api/v1/platform/customer-membership`，服务端校验平台管理员权限、客户角色和目标租户；管理员可按手机号开通 365 天年度会员或取消有效付费会员。
- 新增页面 `/pages-platform/membership/index` 和平台工作台“会员管理”入口。Java Docker 构建、66 项 Maven 测试、前端类型检查、Lint、H5、微信开发包和微信生产局域网包均已通过；真实管理员账号和真实数据库端到端验收仍待执行。

## 2026-08-22 健康助手生产包初始化失败排查

- 用户将包含健康助手页面的微信生产包上传到开发版本后，页面资源和首屏卡片可以显示，但初始化区域提示“暂时无法打开助手 / 系统内部错误”。该错误发生在 `GET/POST /api/client/medical-assistant/conversations`，不是头像、前端包资源或 Qwen 首次回答调用失败。
- 当前工作区已用已认证开发客户完成同样流程：列表和创建会话均返回 HTTP 200，Java 容器日志显示 Flyway 已执行到 V44，数据库中已存在 `medical_assistant_conversation` 和 `medical_assistant_message`。因此代码和本地数据库链路正常。
- 生产域名 HTTPS、Nginx 路由和未登录接口鉴权可达；但当前环境没有可用的生产 SSH 密钥，未执行远程修改。结合生产包请求地址为 `https://xingxuyuan.com`，最可能原因是只上传了小程序前端包，生产 `rayk-server` 尚未包含健康助手 Java 模块，或生产数据库尚未执行 V43/V44，导致后端未处理该初始化请求时返回 `99999 SYSTEM_ERROR`。
- 正确修复是先在生产 `/opt/zhiyu-health` 使用当前源码重建 `rayk-server`、`rayk-ai` 和 Nginx，并确认 Flyway 到最新版本，再重新打开已上传的小程序开发版本；不需要再次重复上传前端包。生产部署命令已同步写入 README。生产 `.env` 中的 `QWEN_ASSISTANT_API_KEY` 只影响发送健康问题，不影响首屏会话初始化。

## 2026-08-22 健康助手生产部署完成

- 已将健康助手 Java 控制器、服务、DTO、AI 客户端改动，Python 助手服务，V43/V44 Flyway 迁移，以及生产 Compose/Nginx 配置安装到 `/opt/zhiyu-health`；原有文件已备份到 `/opt/zhiyu-health-backups/assistant-20260822`。
- 远程 `rayk-server` Maven 构建成功，65 项服务端测试通过；`rayk-ai` 和 `rayk-server` 镜像均已重建，MySQL、Redis、MinIO 未删除或重建数据卷。Flyway 最新版本已确认是 44，`rayk-server`、`rayk-ai`、Nginx 均为 healthy。
- Nginx 配置检查和热加载成功；`https://xingxuyuan.com/health` 返回 200，未登录访问助手会话接口返回 401，说明公网路由和鉴权链路正常。已删除本次临时 SSH 公钥、远程上传暂存目录和本地临时私钥；远程备份保留用于回滚。
- 尚未代替用户在微信客户端完成已登录会话的真实页面回归；现在可关闭并重新打开小程序开发版本，验证助手首屏、创建会话及发送消息。若首屏正常但发送失败，应继续查看生产 `rayk-ai` 日志和 `.env` 中的 Qwen 助手配置，而不是重复上传前端包。

## 2026-08-23 增加 Git 到线上版本一致性链路

- 新增 `scripts/release/build-release.mjs`：正式构建要求工作区干净，使用当前 Git 提交生成 `releaseId`，依次构建 H5、微信开发包和微信生产局域网包，并在 `build/release` 输出 `release-manifest.json` 与 Compose 使用的 `release.env`。本地临时验证可显式使用 `--allow-dirty`，但该产物不能作为正式发布版本。
- Java 新增公开 `GET /api/system/version`，Python AI 服务新增 `GET /version`（经 Nginx 为 `/ai/version`）；两者返回 releaseId、Git SHA、构建时间、数据库迁移版本和三个前端包指纹。小程序通过 `X-Client-Release-Id` 与 `X-Client-Git-Commit` 标识自身来源，不在界面显示版本号。
- Compose 的 Java/Python 镜像标签和运行环境改为读取同一份 `release.env`，默认开发行为仍使用 `dev` 标签；未传入发布环境文件时，版本接口会显示 `dev-local/unknown`，不能据此宣称线上已对齐。
- 新增 `scripts/release/verify-release.mjs`，部署同一份清单和镜像后运行 `node scripts/release/verify-release.mjs https://xingxuyuan.com`，会严格比较线上接口与本地清单。此次仅完成代码和本地发布链路，尚未把新增版本接口部署到线上，也未进行远程 Docker 操作；下一次生产发布必须用同一份 `release.env`、镜像和小程序包。

## 2026-08-24 线上平台管理员手机号更新

- 已核对线上仅有一个有效平台管理员账号 `admin`（ID 10001），并确认新手机号未被其他账号占用。
- 已将管理员登录手机号更新为 `150****3671`；数据库仅保存脱敏值和不可逆哈希，不保存完整手机号。
- 后续管理员需使用该手机号完成微信授权登录；本次未修改客户数据、会员权益或其他账号。

## 2026-08-24 修复线上平台会员管理接口

- 根因：线上 `rayk-server` 仍运行未包含 `PlatformController` 的旧镜像，所以会员管理请求被 Spring 当作静态资源处理；同步当前 Java 源码后，又发现生产库已执行 V43/V44 但缺少 V42，Flyway 校验因此阻止服务启动并造成公网 502。
- 已同步本地当前 Java 后端、生产 Compose 配置和迁移资源到线上 `/opt/zhiyu-health`，保留旧 Java 镜像 `rayk-a1-server:backup-platform-membership-20260824` 作为回滚点；未删除 MySQL、Redis、MinIO 数据卷。
- `V42__lab_report_processing_progress.sql` 已使用一次性生产迁移窗口补执行，`lab_report.processing_progress`、`processing_message` 已存在，Flyway 记录已补齐 V42；迁移完成后已关闭 out-of-order 开关，线上线下默认都恢复严格顺序校验。
- 线上 `rayk-server` Maven 测试 66 项全部通过并已恢复 healthy；`https://xingxuyuan.com/health` 和 `/api/system/version` 返回 200，未登录访问 `/api/v1/platform/customer-membership` 返回 401，说明会员管理接口已经加载并由权限层拦截，不再是系统内部错误。
- 后续线上、线下必须使用同一份当前后端源码和对应 Compose 配置一起构建部署；生产发布后至少核对 Java healthy、Flyway 版本、`/health`、`/api/system/version` 及会员管理接口鉴权状态。

## 2026-08-24 修复生产会员支付被降级为模拟开通

- 根因：生产 `rayk-server` 容器实际生效的 `MEMBERSHIP_PAYMENT_ENABLED=false`，导致会员订单接口向小程序返回开发/模拟支付状态；虚拟支付 AppKey、ProductID、商户号、OfferID、环境和回调地址均已存在，不是支付密钥丢失。
- 已在 `compose.prod.yml` 显式设置 `MEMBERSHIP_PAYMENT_ENABLED=true`；开发环境仍保持默认关闭，真实支付测试继续使用单独的 `compose.real-payment-dev.yml` 和未提交密钥配置。
- 已同步生产配置并重启 `rayk-server`；后续生产会员开通页应显示真实支付按钮并调用 `wx.requestVirtualPayment`，模拟开通入口不应再作为生产支付路径出现。
- 待用户在微信客户端重新进入开通页做一次真实支付调起验证；若仍显示模拟开通，先关闭旧小程序页面并重新打开，避免继续使用旧页面缓存。

## 2026-08-24 调整线上登录会话有效期

- 生产 Compose 显式将 `JWT_EXPIRE_SECONDS` 设置为 `604800` 秒（7 天），覆盖服务器 `.env` 中原来的 7200 秒配置；Redis 会话 TTL 与 JWT 有效期保持一致。
- 已同步线上配置并重建 `rayk-server`，容器内实际生效值已核对为 `604800`，服务状态为 healthy，公网 `/health` 返回 200；会员有效期和登录会话有效期仍是两套独立规则。
