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
小程序默认进入公开首页，未登录也可以浏览健康服务介绍、服务工作台和常见问题；用户主动选择使用个人数据或提交反馈时，再自行进入登录。企业主体通过微信授权手机号匹配客户、预录入医生和平台管理员；本地开发包的微信手机号登录也走真实微信校验，H5 三角色调试请使用开发调试入口，不要把固定 Mock 手机号当作真实身份测试。登录页的《用户服务协议》和《隐私政策》支持点击进入小程序内阅读页；登录前必须由用户主动勾选同意，未勾选时会先弹出协议确认框；正式发布前仍需由运营方核对主体、联系方式、第三方服务清单和法务文本。
  → 完善健康档案与问卷
  → 上传 PDF 或图片体检报告
  → OCR 保留原分类、原顺序和原内容
  → 指标标准化与 12 维健康评估
→ RAG 医学知识检索 + Qwen3.8-Flash 图片直读/后台选定的 DeepSeek 模型综合解读
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
- 图片报告即使 OCR 质量不足或 OCR 服务异常，只要原始图片已保存，详情页也会提供“直接用图片生成健康报告”；该操作由 `qwen3.8-flash` 重新阅读全部图片，再由平台管理员当前选定的 DeepSeek 模型结合图片事实、健康档案、健康拍和 RAG 生成报告。

### 智能健康评估

- 结合体检结果、健康档案、问卷、既往史、家族史、生活方式综合描述、过敏史、当前用药和最近一次成功的面部健康检测体征。
- 体检报告按原分类向评估提供结构化指标、检查所见和检查小结，不直接使用未经清洗的整份原始 OCR 文本。
- 图片体检报告启用 Qwen Vision 后，Java 为已存储图片生成短时签名地址，Python 先按批次将原始图片交给 `qwen3.8-flash` 直读为逐页结构化事实，再把图片分析结果、健康档案、健康拍结果和 RAG 证据交给平台管理员当前选定的 DeepSeek 模型生成现有综合报告格式；图片内容优先，OCR 结构化结果只作低可信线索。PDF 仍保持独立解析路径，不进入图片直读链路。
- PDF 启用云 OCR 时按页级联：先用 `qwen3.8-flash`，只有页面请求失败或未提取出可用检验内容时才用 `qwen3.5-ocr` 重试该页；两者仍失败才整体降级到 PDF 原生解析与 PaddleOCR。电子 PDF 的原生文本、表格和检查小结继续作为校验基线，不因模型切换被覆盖。
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
- 当前使用 `qwen3.8-flash` 进行文字问答；模型名称不展示在客户端，也不影响平台管理员对健康评估/报告模型的切换。
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

### 健康树洞

- 客户从首页“健康树洞”卡片进入，使用独立的树洞会话类型；每天可以记录身体感受、心情、压力、睡眠等状态，AI 会即时回复并通过 SSE 逐步展示。
- 树洞回答复用已配置的 `qwen3.8-flash`，但使用专门的陪伴式提示词和同一套急症识别、健康管理免责声明；用户消息只按本人客户数据范围读取。
- 树洞记录保存在现有助手消息表中，七天阶段反馈单独保存到 `health_tree_hole_feedback`。从本周期第一条记录起算满 7 个自然日后，后台定时任务会自动尝试生成阶段反馈，同一周期只生成一份；健康树洞页面在树洞记录入口下方展示“七天反馈总结”卡片。
- 后台任务默认每天北京时间 02:15 扫描已有树洞记录且周期到期的客户，按客户本人数据范围组装记录并调用反馈模型；页面进入或返回时刷新卡片状态，任务暂时未生成时提供“立即整理”兜底按钮，失败时提供重试。当前未接入微信订阅消息或主动推送，用户仍需持续主动记录，反馈才有可参考内容。
- 树洞使用独立的 `AI_HEALTH_TREE_HOLE` 会员权益：普通客户从首次有效记录起免费体验 7 天，年度健康会员期内不限使用；不再与普通健康助手的 3 次额度共用。七天体验结束后，普通客户需要开通年度健康会员才能继续记录和生成后续反馈。
- 后端迁移为 `V57__health_tree_hole.sql` 与 `V58__health_tree_hole_entitlement.sql`，已部署到服务器隔离开发环境 `/test-api` 并完成容器健康检查；本次未修改生产服务，正式上线前仍需迁移、部署和真实微信端验收。

### 首页视频播放栏

- 首页视频播放栏代码已保留并支持微信小程序和 H5 的原生播放控件（播放、暂停、进度拖动和全屏）；当前生产隐藏版使用 `VITE_HOME_VIDEO_ENABLED=false`，恢复原“了解自己，是照顾健康的第一步”文案卡。
- 视频地址通过前端构建环境变量 `VITE_HOME_VIDEO_URL` 配置，封面图可选用 `VITE_HOME_VIDEO_POSTER`；两者都应使用 HTTPS，且视频域名必须加入微信小程序业务域名白名单。
- 后续素材和域名准备好后，将 `VITE_HOME_VIDEO_ENABLED` 设为 `true` 并重新构建发布；未配置地址或视频加载失败时显示明确占位状态。项目当前没有随代码提交视频文件。

### 实物健康商城

- 商城和首页视频播放栏均由前端构建开关控制；当前生产隐藏版底部导航恢复为“首页、工作台、消息、我的”，商城路由和代码仍保留，准备好商品/履约/支付材料后再重新打开入口。
- 商城只销售平台管理员发布的实物商品，不预置虚构商品，也不把会员虚拟权益当作商城商品。
- 客户流程为：查看在售商品 → 维护收货地址 → 创建单商品订单 → 普通微信支付 → 查看订单状态。
- 下单时原子扣减库存；支付取消或订单超时会释放库存；订单保存商品信息和收货地址快照，避免商品或地址后续修改影响历史订单。
- 平台管理员从平台工作台的“商城商品”维护商品名称、图片、描述、价格、库存和上下架状态；商品接口由 Java 后端按角色和数据范围校验。
- 开启商城开关后，平台管理员可通过底部“商城”只读浏览当前在售商品；收货地址、订单和支付接口仍仅对客户工作台开放。
- 商品支付独立于会员虚拟支付，使用普通微信支付 API v3 JSAPI；商城支付默认关闭 `MALL_PAYMENT_ENABLED=false`。真实付款只以微信支付回调将订单更新为 `PAID` 为准，小程序支付面板返回成功后会等待订单确认，避免把尚未收到回调的交易显示为已完成。完成商户 API v3 证书、支付回调、发货、退款和售后流程验收后，才可在未提交的服务器 `.env` 中将 `MALL_PAYMENT_ENABLED=true` 显式开启；生产 Compose 会读取该显式开关。不要把会员虚拟支付 AppKey、OfferID 或 ProductID 用于实物订单。
- 当前首版不包含购物车、物流发货、售后退款后台和多商品合单；正式上线前需要补齐履约、退款、库存对账和订单后台流程。

### 金豆会员

- 金豆余额、账本流水、平台购豆、集市挂单/成交、区域返利和机器人兑换统一使用 `DECIMAL(24,6)`，支持最小 `0.000001` 金豆；历史整数数据迁移后自动保留为六位小数。
- 普通会员购买金豆后按双账本分配，传奇人物集市购买的数字银行金豆全部进入数字银行；小数数量的支付金额按人民币分向上取整到分，用户虚拟支付加价规则和卖家/推荐人结算金额不变。
- 该能力受 `GOLD_BEAN_ENABLED` 与支付开关控制；当前线上生产已执行至 Flyway V66，并按线上版本商品配置开启金豆会员及平台注册/推荐注册虚拟支付，仍需用真实小额订单完成业务回归后再扩大使用范围。

### 管理员桌面网站

- 平台管理员可以直接使用同一套 UniApp H5 访问桌面管理网站，不需要另建一套 Vue 管理后台；登录、角色权限、Java 接口和微信小程序保持一致。
- 浏览器窗口宽度达到 1024px 时，管理员页面自动切换为桌面布局：左侧固定导航、顶部当前模块和管理员信息、宽屏内容区；医院、随访、会员、金豆、商城、反馈和 AI 模型管理均可从侧栏进入。
- 小屏浏览器和微信小程序继续使用原有单列布局。桌面 H5 构建输出仍为 `rayk-miniapp/dist/build/h5`，部署到正式域名或内网地址前需要单独配置对应静态站点和 API 地址。

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
├─ Qwen3.8-flash → Qwen3.5-OCR PDF 页级联 / 图片直读
        ├─ DeepSeek + RAG
        ├─ 健康助手（Qwen3.8 Flash）
        └─ 报告与随访智能处理
```

| 层级 | 技术 |
| --- | --- |
| 客户端 | UniApp、Vue 3、TypeScript |
| 业务后端 | Java、Spring Boot、MyBatis、Flyway |
| AI 服务 | Python、FastAPI、PaddleOCR、PDF 结构解析 |
| 外部 AI | Qwen3.8-flash、Qwen3.5-OCR、DeepSeek |
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

本机 Docker 内存不足时，可执行 `npm run build:mp-weixin:dev:remote` 生成 `dist\release\mp-weixin-dev-remote` 远程开发包，让微信开发者工具直接访问线上 `https://xingxuyuan.com`；该命令不会覆盖保留开发登录的 `mp-weixin-dev`，不会修改线上容器，且远程包关闭本地开发身份入口，必须使用线上真实授权手机号登录。恢复局域网联调时重新执行 `npm run build:mp-weixin:dev`。

服务器隔离测试环境部署后，可执行 `npm run build:mp-weixin:dev:remote-test` 生成 `dist\release\mp-weixin-dev-remote-test`。它访问 `https://xingxuyuan.com/test-api`，只连接服务器上的独立测试 Docker、数据库和对象存储，并开启开发身份选择器。服务器启动隔离项目时必须显式带上 `--env-file .env.remote-dev`，并使用 `compose.yml -f compose.remote-dev.yml`，这样 MySQL、Redis、MinIO 才会使用 `rayk_remote_dev_*` 独立数据卷；测试环境已配置与该 AppID 匹配的小程序 AppSecret，使用真实授权手机号登录时会走微信校验；开发身份选择器仍只用于测试账号。当前测试环境按生产实际开关启用会员微信支付（共用生产商户配置但使用独立测试回调），商城及商城支付仍关闭；测试会员支付会产生真实交易，必须使用小额订单并核对回调/退款，不能当作无扣款沙盒。

每次前端修改后必须同步维护三个输出：

| 用途 | 目录 |
| --- | --- |
| H5 调试界面 | `E:\health\rayk-miniapp\dist\build\h5` |
| 微信开发包 | `E:\health\rayk-miniapp\dist\release\mp-weixin-dev` |
| 微信远程开发包 | `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote` |
| 微信生产局域网验收包 | `E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan` |

微信开发者工具按用途导入 release 目录：`mp-weixin-dev` 是保留开发登录的局域网包，`mp-weixin-dev-remote` 和 `mp-weixin-prod-lan` 访问线上 HTTPS 服务；三者都不是可直接提交审核的正式互联网生产包。

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
- Qwen3.8-flash：图片体检报告分批直读；DeepSeek负责最终报告汇总。Qwen 由 `QWEN_VISION_ENABLED` 和服务端密钥显式启用，失败时保留规则降级边界。图片阶段按 `QWEN_VISION_BATCH_SIZE` 分批，并按 `QWEN_VISION_MAX_IMAGE_BYTES`、`QWEN_VISION_MAX_TOTAL_IMAGE_BYTES` 和 `QWEN_VISION_MAX_IMAGE_PIXELS` 做安全压缩与大小控制；Qwen HTTP 状态、超时和响应格式错误会区分记录，不记录带签名的 MinIO 地址。Java 到 AI 的本地开发读取超时为 600 秒，覆盖分批分析和 DeepSeek 综合解读的完整链路。
- Qwen3.8-flash → Qwen3.5-OCR：PDF 渲染页按页级联识别；电子 PDF 原生文本/表格解析作为校验基线，两种云模型失败后由本地 PaddleOCR 兜底。图片报告直读仍单独使用 Qwen Vision。
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

开发环境真实支付验收使用 `compose.real-payment-dev.yml`：一级代理注册、推荐代理注册、平台购豆和会员间金豆交易均使用各自配置的微信虚拟商品；买家完成 `gold_bean` 虚拟道具支付后，平台再通过“商家转账到零钱”向卖家结算，只有卖家转账明确成功后才交割金豆。推荐注册验收必须配置 `GOLD_BEAN_REFERRAL_REGISTRATION_PRODUCT_ID`、`GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_ENABLED=true`、`GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_SCENE_ID`、普通微信支付商家转账能力和推荐人收款绑定；金豆集市验收必须配置 `GOLD_BEAN_PRODUCT_ID=gold_bean`、`GOLD_BEAN_TRADE_PAYMENT_ENABLED=true`、`GOLD_BEAN_TRADE_TRANSFER_SCENE_ID`、虚拟支付回调和卖家微信收款绑定，缺少商家转账权限时不得开启。确认域名已指向当前服务器后执行：`docker compose -f compose.yml -f compose.real-payment-dev.yml up -d --build`。该配置会占用 80/443 端口，只能在具备公网 DNS、证书和防火墙权限的验收服务器上使用；普通本地开发继续使用 `compose.dev.yml`。

## 开发环境金豆会员需求 V1

金豆会员页提供最近 7 天累计直推人数折线图，以及默认折叠的两级推荐关系图；关系图展开后显示本人、直推成员和第二层团队成员，多分支可横向滑动查看，超出移动端展示上限时显示剩余人数汇总。关系昵称和等级由服务端按当前登录用户的真实推荐记录动态返回，不展示手机号。

区域返利当前口径：仅当钻石会员开辟区域所在城市的用户通过推荐注册，且区域开辟人 A 自己因此获得 `REFERRAL_DIRECT` / `REFERRAL_DOWNLINE` 奖励金豆时触发。服务端以注册用户 `gold_member_account.city` 与区域 `gold_region.city` 的同城匹配作为前置条件；跨城市或缺少城市信息的注册不产生区域盈利。以 A 自己实际获得的注册推荐奖励为基数，仅给 A 的直接推荐人额外返利 20%，更上级不再获得区域返利。同一次注册中其他账户收到的推荐奖励不会再作为区域返利基数，A 也不会重复获得 100% 区域奖励；每日奖励、等级解锁奖励、初始奖励和金豆集市交易不触发这套区域返利。

《需求梳理.docx》中的金豆、普通会员直推等级、每日奖励、活跃保护期和区域演示能力已作为独立能力接入，不改变现有普通客户会员权益。开发包和当前生产局域网验收包使用 `VITE_GOLD_BEAN_ENABLED=true`；生产 Java 服务使用 `GOLD_BEAN_ENABLED=true`、`GOLD_BEAN_DEVELOPMENT_MODE=false` 和支付开关显式配置，底部第三个入口显示为“俱乐部”，平台管理员显示为“金豆运营”并直接进入金豆会员运营控制台。生产仍通过服务端权限与支付校验保护，不以隐藏前端入口作为授权机制。

开发包客户可从底部“俱乐部”或“我的”中的“俱乐部”进入金豆会员页面，查看等级、历史最高等级、直推进度、数字银行/交易双账本、每日奖励和 7 天保护期，并演示注册、推荐关系和钻石区域。普通会员注册成功后每日增加 60 金豆，连续奖励 20 天；第 15-19 天仍没有直推时连续提醒 5 天，20 天内已经直推则跳过这组提醒。第 20 天奖励完成后进入 7 天活跃保护期，20 天内解锁铜牌、银牌、金牌或钻石不会提前开启/刷新保护期；保护期结束后开始按自然日掉级，掉级期间保持 100% 交易额度，掉到普通会员后继续 1 个自然日未推荐才按 50% 执行，直推成功后恢复 1 个等级（不超过历史最高等级）并重新进入 7 天保护期。达到历史最高等级后继续直推只刷新 7 天保护期，不再增加等级；已有数字银行金豆和可交易金豆均保留会员间交易权。金豆集市已从俱乐部概览拆为独立页面 `/pages-customer/gold-bean-market/index`，普通会员支持“市场挂单 / 我的挂单”切换、发布、买入、下架和服务端分页（每页 12 条），传奇人物只显示市场挂单和买入入口，不显示发布或我的挂单入口；普通会员与传奇人物均从各自俱乐部入口进入，后端仍按买家资格隔离可见来源。传奇人物俱乐部同时提供按参考视觉重做的展示页，使用独立 SVG 呈现英雄卡、集市入口和七项专属医疗服务；七项服务仅作展示，不提供相应业务页面。传奇人物资格使用独立俱乐部页面，仅允许被平台管理员预先录入手机号的会员进入集市购买全国数字银行金豆挂单，不能向平台直接购买或发布挂单。原“消息”功能保留为工作台“消息中心”卡片，同时在“我的”增加“我的消息”入口；消息页面继续按角色展示对应范围。注册演示中的所在地区使用微信原生两级选择器（省 → 市），固定为 `level=city`，不再允许自由输入或选择区/县；提交时按“省 / 市”规范化写入现有 `city` 字段，兼容 `V46__gold_bean_membership.sql`，无需改动生产数据结构。后端通过 `gold_member_account`、`gold_member_referral`、`gold_member_ledger`、`gold_region` 等表记录状态；未完成注册不会提前发放每日金豆，注册成功当天计为第 1 天且只发放当天 60 金豆；个人推荐码仅在注册成功后生成，推荐关系按直推绑定，奖励按下线链路计算：直接推荐人获得 11 金豆，其上各级上级代理在下游会员注册时各获得 5 金豆，不限制为两级；已注册会员可一键复制推荐码。区域开辟仅允许钻石会员申请，一个账号只能开辟一个区域，推荐链最多三层；仅与开辟区域同城的用户注册奖励会触发服务端区域返利：开辟人本人已获得该笔注册推荐奖励，直接推荐人额外获得 20%，更上级不再获得区域返利，跨城市或缺少城市信息不产生区域盈利，且不需要平台手工结算。

开启 `GOLD_BEAN_PAYMENT_ENABLED=true` 且分别配置平台注册、推荐注册和金豆交易虚拟商品 ID 后，所有金豆虚拟支付链路统一按 `GOLD_BEAN_VIRTUAL_PAYMENT_SURCHARGE_PERCENT`（默认 12%）向买家收取费用：平台注册业务费基准 1000 元，实际支付 1120 元；推荐注册业务费基准 1000 元，实际支付 1120 元；金豆集市卖家结算单价基准 1 元/豆，买家支付 1.12 元/豆。`amountCent`/`totalAmount` 始终保存平台业务或收款人应收金额，新增支付金额快照只用于虚拟支付下单和回调校验，因此推荐人和卖家商户转账金额不增加 12%。当前生产线上版本商品对应为 `test_member=1120`（推荐注册费）、`normal_member_998=1120`（普通会员）、`gold_bean=1.12`（金豆）和 `vip_year_399=399`（年度会员）；后台商品价格必须与服务端商品/金额校验一致。一级代理必须填写平台一次性注册码，推荐代理必须填写已注册推荐人的推荐码，虚拟支付回调确认后由平台通过商家转账到零钱向推荐人或卖家结算。两种编码互斥，后端会校验注册归属、商品/金额、付款 OpenID、虚拟支付回调和收款绑定。生产已开启平台注册/推荐注册支付和推荐人转账场景，金豆集市卖家转账仍由 `GOLD_BEAN_TRADE_PAYMENT_ENABLED=false` 保持关闭，待商家转账能力完成单独验收后再开启。关闭对应支付开关时不会创建支付参数或伪造扣款；所有回调仍须完成订单、商品/金额、OpenID、AppID、交易号等校验后才开通会员并发放注册成功当天的第 1 天每日金豆（60 金豆）。

平台首会员注册受一次性授权码控制：平台管理员在“金豆会员运营”页生成授权码，可选绑定已验证手机号并设置有效期；完整码只在生成成功时显示一次，列表只显示掩码。首个无推荐人的客户必须同时填写平台授权码，留空会被后端拒绝；创建支付订单时授权码进入 `RESERVED`，取消或超时释放占位，支付回调成功后才变为 `CONSUMED`。数据库以 `PLATFORM_ROOT` 唯一占位键防止并发产生多个平台首会员，平台首会员完成后不再允许继续生成或使用该入口。

平台管理员可从平台工作台进入“金豆会员运营”，查看会员账户、推荐关系、注册费用归属、金豆流水和支付订单，并按用户昵称或手机号、注册状态、流水类型、订单状态或订单类型筛选；机构、订单号、推荐码、流水描述和交易号仅作记录展示，不参与关键词搜索。同时可生成、查看掩码和撤销平台首会员一次性授权码，录入、查看掩码和撤销传奇人物手机号白名单，并在传奇名单中查看已匹配用户的数字银行余额（未匹配用户显示“未建档”）。页面路由为 `/pages-platform/gold-bean/index`，服务端接口为 `/api/v1/platform/gold-bean/*`（支付订单为 `/orders`，授权码为 `/platform-invites`，传奇名单为 `/legendary`，区域列表为 `/regions`，区域自动返利记录为 `/region-profits`，后两者仅用于只读审计），由现有 `PLATFORM_ADMIN` 权限和金豆服务端开关共同保护；推荐注册订单会显示推荐人和商家转账结算状态，管理员不能改账或伪造结算成功。区域代理奖励由会员奖励流水自动触发，平台不录入、不手工结算区域盈利。

真实金豆支付和会员间交易按环境显式配置：本地使用 `compose.real-payment-dev.yml`，线上隔离开发容器使用对应的 `REMOTE_DEV_GOLD_BEAN_*` 开关与商户配置，生产使用服务器 `.env` 与 `compose.prod.yml` 的生产配置。当前线上隔离容器和生产容器均已执行至 V66；生产已开启平台注册/推荐注册虚拟支付和推荐注册商家转账场景 `1005`，金豆集市交易支付仍关闭。推荐转账的 `WAIT_USER_CONFIRM` 确认页、首次授权自动收款、状态查单和定时补偿已接入，只有微信最终 `SUCCESS` 才算推荐人到账；区域代理返利由奖励流水自动触发，记录的是金豆数量而不是人民币，机器人权益兑换也只扣数字银行金豆，不触发真实现金支付。生产局域网包不能替代正式微信审核包，使用前需重新导入对应 release 目录。

金豆集市区域交易规则：每个新挂单保存卖家注册区域城市快照；普通客户的市场查询和买入接口只允许本注册区域，缺少注册区域城市的普通客户不能发布或购买；传奇资格客户不受区域限制，可以浏览和购买全国各地的挂单（继续遵守传奇仅购买数字银行挂单的既有规则）。V62 会为历史挂单按卖家账户城市回填区域，无法回填的历史挂单仅对传奇客户保留可见，普通客户不能通过接口绕过区域限制。

金豆会员等级规则：直推达到铜牌/银牌/金牌/钻石分别解锁 100/300/600/1000 金豆；普通会员先完成每日 60 金豆、连续 20 天的奖励，奖励期内直推不提前开启保护期，第 15-19 天无直推时连续提醒，奖励完成后进入 7 天活跃保护期。保护期第 4-6 天连续提醒推荐新人；保护期结束后按自然日每天降低一级，掉级期间交易额度保持 100%，掉到普通会员后继续 1 个自然日未推荐才按 50% 执行。掉级过程中推荐成功后恢复 1 级（不超过历史最高等级）并重新开始 7 天保护期；达到历史最高等级后继续直推只刷新保护期。

机器人权益兑换：已注册且状态有效的客户仅可兑换一次，固定消耗 10000 枚数字银行金豆。未兑换客户的入口保持可点击；卡片不内嵌显示“还需多少豆”等余额计算文案，余额不足时点击兑换弹窗提示“数字银行金豆余额不足”，服务端事务会在扣豆前再次校验余额、一次性兑换条件和账户状态，任何失败都不会扣豆。余额满足后，服务端锁定账户、扣减数字银行并写入幂等金豆流水和兑换记录；兑换成功后小程序立即弹出企业微信群二维码，由专人对接。群名称可由 `GOLD_BEAN_ROBOT_GROUP_NAME` 外部配置，二维码地址优先使用 `GOLD_BEAN_ROBOT_GROUP_QR_IMAGE_URL`；未配置时使用随小程序内置的机器人权益对接群二维码，兑换仍会正常扣除 10000 枚数字银行金豆。弹窗支持微信长按识别、保存到相册和调用“扫一扫”；兑换完成后按钮显示“已兑换”，同一用户不能再次兑换。建议使用企业微信群活码，便于群满或运营调整时轮换；普通二维码、群满、群解散、管理员停用或规则变化仍可能导致入口失效，不能把“永不过期”作为绝对承诺。当前生产已随本次生产局域网包启用该能力，正式验收前仍需用测试账号确认二维码长按保存与扫码流程。

## 普通客户会员体系（V1）

会员能力只面向 `CUSTOMER`，平台管理员和医生不显示会员入口。免费客户和年度会员都可以永久保存、随时查看健康档案，并支持体检报告上传、保存和查看。当前产品权益为：免费客户 AI 健康评估 3 次、AI 健康报告 3 次（两项分别计数）、首次健康随访 1 次、健康拍 3 次、吃饭/睡眠语音提醒各 3 次；年度健康会员有效期 365 天，AI 健康评估和 AI 健康报告分别在会员期内不限次数，支持持续随访、每天 1 次健康拍以及会员期内持续使用两类语音提醒。免费客户的健康拍历史和检验趋势按近 3 天基础范围查看，年度会员可查看完整历史趋势。年度会员方案和权益由数据库配置，开发环境默认关闭真实支付并支持模拟订单开通；健康报告发布不因随访权益配置或生成失败而回滚。

AI 健康评估和 AI 健康报告在一次完整评估流程中分别预占、确认或释放，确保两项次数独立统计且外部 AI 失败不会扣除额度。历史综合评估产生的旧 `AI_HEALTH_REPORT` 流水会兼容计入评估额度，避免拆分权益后遗漏既有使用记录。

普通客户触发任一可消耗权益的 `60401` 限制时，小程序统一弹出“权益次数已用完”提示，并可跳转开通年度会员；异步生成的初始随访若因额度不足跳过，也会在随访列表中提示。后端额度校验仍是唯一准入依据，前端只负责展示引导。

后端接口位于 `/api/client/membership`：`summary`、`plans`、`benefits`、`usage`、`orders` 和订单支付接口。所有消耗通过 `membership_usage` 的 `RESERVED` → `CONFIRMED`/`RELEASED` 两阶段流水记录，外部 AI、健康拍或 TTS 调用失败不会扣除权益。健康拍会员权益当前仅保留每日额度，不再配置滚动 30 天额度；“报告重新解读”不再作为会员权益提供。健康拍历史和指标趋势的查看范围也由 Java 后端按会员状态校验，不能只靠前端隐藏。数据库迁移为 `V32__customer_membership.sql` 和后续会员权益调整迁移（含 `V39__remove_ai_report_regeneration.sql`、`V40__align_membership_rights_with_product_table.sql`、`V41__split_ai_assessment_and_report_entitlements.sql`）。支付确认页仅保留订单状态、支付通知和操作按钮，不再展示支付后权益卡片或底部免责声明。

开发包会员调试：使用 `compose.yml` + `compose.dev.yml` 启动后端，并导入 `dist\release\mp-weixin-dev`。客户进入“健康会员”页面会看到“开发调试”卡片，可切换“恢复免费客户”或“模拟年度会员”来验证两套权益。该接口由后端 `MEMBERSHIP_DEVELOPMENT_MODE` 保护，`compose.dev.yml` 才会开启，生产环境默认关闭；切勿将开发包用于正式发布。

会员前端包含普通客户中心、年度会员中心和开通会员三种状态页面，统一使用真实会员 `summary/plans` 接口渲染权益、剩余次数、有效期和支付入口；开通页新增与产品表一致的 14 项权益对比表（包含“AI健康助手”，不单独展示会员有效期行），表格字号和换行按中老年用户阅读场景放大。会员中心的健康分动态读取当前客户最近一次成功 AI 评估，并按有有效分数的健康维度计算平均值，没有有效评估时显示“待评估”。会员权益和健康分仪表盘采用双指标卡片与横向进度条展示，避免圆环同时承载多个含义造成阅读负担。会员 SVG 资源位于 `rayk-miniapp/src/pages-customer/static/member/`，随 `pages-customer` 分包发布；复杂插画保留 SVG 封装但已压缩内嵌位图，通用图标统一复用，避免约 27MB 的重复资源进入微信主包。

本地环境可通过以下变量控制：`MEMBERSHIP_ENABLED`、`MEMBERSHIP_PAYMENT_ENABLED` 以及六项 `MEMBERSHIP_FREE_*_TRIAL`（AI 评估、AI 报告、首次随访、健康拍、吃饭提醒、睡眠提醒）。本地和开发包默认保持 `MEMBERSHIP_PAYMENT_ENABLED=false`，如需在开发环境进行真实支付测试，可在未提交的 `.env` 中显式设置为 `true`，同时配置虚拟支付 AppKey、ProductID 和公网 HTTPS 回调。当前年度会员真实支付按小程序虚拟支付“道具直购”模式接入：需配置 `WECHAT_VIRTUAL_APP_ID`、虚拟支付商户号 `WECHAT_VIRTUAL_MERCHANT_ID`、`WECHAT_VIRTUAL_OFFER_ID`、现网 `WECHAT_VIRTUAL_APP_KEY`、`WECHAT_VIRTUAL_PRODUCT_ID`，并将 `WECHAT_VIRTUAL_ENV=0`、`WECHAT_VIRTUAL_MODE=short_series_goods`。AppKey 只能通过服务器密钥管理或未纳入 Git 的 `.env` 提供，不能写入源码或前端；商品 ID 和价格必须与微信虚拟支付后台已发布的道具一致。当前回调地址为 `https://xingxuyuan.com/api/payments/wechat/virtual/notify`，需在小程序虚拟支付后台订阅 JSON 推送、完成 DNS/HTTPS/公网 443 转发并进行真实小额验收。服务端使用 `wx.requestVirtualPayment` 所需的 `signData`、`paySig` 和 `signature`，支付成功以后端发货通知校验并开通会员为准。
