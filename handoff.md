# 三羊健康项目交接说明

## 2026-09-11 对齐直推趋势图日期坐标

- 仅调整趋势图下方日期标签：日期标签改为使用与数据点相同的横坐标，并以标签中心对齐对应点位；未修改折线、点位、网格线或趋势数据计算。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；H5、微信开发包、远程隔离测试包和生产局域网包均重新生成。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.C53hxWWw.js`，SHA-256 为 `69a093b75c9016da69268a8f593ee4d210b2ea2dfa717b3ae7d70789cf758b5e`，与本地一致，远程目录包含 `trend-date-label` 且不再包含 `trend-canvas`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-date-alignment-before-20260911-152333/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产环境未更新，生产 H5、Java、数据库、容器和配置均未修改。

## 2026-09-11 恢复跨端 CSS 趋势折线并修复测量时序

- 微信端 Canvas 未可靠绘制，已移除 Canvas 路径，恢复为按实际图表像素坐标生成的 CSS 折线段；修正同值水平段合并边界，保留 `09/05→09/06→09/11` 的真实路径和数据点，不显示点位数字。
- 趋势图节点渲染后使用组件作用域 selector query 测量实际宽高，并在节点尚未出现时重试；折线仅在实际尺寸有效后生成，避免初始近似比例导致的端点错位和尖角。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；H5、微信开发包、远程隔离测试包和生产局域网包均重新生成。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.8nMsl_Hx.js`，SHA-256 为 `3c4714ef2769805f41e719b2fdf3726eabab9097db1048afea0ce5ef1e4b38ac`，与本地一致，远程目录包含 `trend-segment` 且不再包含 `trend-canvas`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-css-centerline-before-20260911-150401/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产环境未更新，生产 H5、Java、数据库、容器和配置均未修改。

## 2026-09-11 修复直推趋势图拐点端头外露

- 针对真机截图中 `09/06` 拐点仍露出尖角的问题，改为完整不透明的拐点圆点覆盖线段端头，并将所有线段末端收进对应点标记范围；保留 `09/05→09/06` 上升、`09/06→09/11` 水平的数据路径。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；四份前端产物均重新生成，数字标注仍未显示。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.CKKuVGyS.js`，SHA-256 为 `3c84ca23aee3d8185c01c9be93558208dc73f04ec2afaa5221d77bbe2eefcbee`，与本地一致，远程目录未发现 `trend-point-value`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-opaque-marker-before-20260911-143807/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产环境未更新，生产 H5、Java、数据库、容器和配置均未修改。

## 2026-09-11 消除直推趋势图拐点尖角

- 修复折线在首个上升拐点处因独立旋转线段端帽露出的尖角：线段末端收进数据点标记覆盖范围，并移除线段端点圆角；数据路径仍保持 `09/05→09/06` 上升、`09/06→09/11` 水平，未改变趋势数据。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；四份前端产物均重新生成，数据点数字标注仍未显示。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.DwCo45VE.js`，SHA-256 为 `a5691dddf942b62e358b6260071efd8ed0582996374d1feb58fd5ad0b5a23abd`，与本地一致，上一版组件资源已移除。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-join-cap-before-20260911-141814/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产环境未更新，生产 H5、Java、数据库、容器和配置均未修改。

## 2026-09-11 修正直推趋势图连续段边界

- 修复同值水平段合并的边界错误：趋势数据为 `0, 2, 2, 2, 2, 2, 2` 时，第一条折线严格只连接 `09/05→09/06`，后续 `09/06→09/11` 才合并为水平线，不再出现从 `09/05` 一直斜连到 `09/11` 的错误路径。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；H5、微信开发包、微信远程隔离测试包和微信生产局域网包均重新生成。数据点数字标注仍未显示。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.d71PNZpH.js`，SHA-256 为 `e5c6efbd9d0c6102b223333f512104c60c9e7be9955a053c50ff616b6d9bc71a`，与本地一致，远程目录未发现 `trend-point-value`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-boundary-before-20260911-114630/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产环境未更新，生产 H5、Java、数据库、容器和配置均未修改。

## 2026-09-11 合并直推趋势图连续水平基线

- 针对真机截图中 09/06–09/11 同值水平线仍有细小段差的问题，趋势图现在按相同直推人数合并水平段，不再为每个相邻日期单独绘制短线；网格线也改为与 2px 数据线共享中心基线。
- 保留实际图表尺寸的像素坐标计算、折线中心线定位、数据点圆点和无障碍趋势摘要；数据点上方数字标注继续不显示。该处理符合折线图连续路径和可读性要求。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；四份前端产物均重新生成。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.0h_g3ixz.js`，SHA-256 为 `43f3db975e34aaeef3f0bdfbf4db5b5cff8a022ca7bc5cc0e57d2bca716ded41`，远程目录未发现 `trend-point-value`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-continuous-path-before-20260911-113757/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产 Java、数据库、容器和配置未更新；生产 H5 活跃入口保持原 `index-ccDubFix.js`。

## 2026-09-11 精确修正直推趋势图首个跃升拐点

- 上一版仍存在首个从 0 人跃升到 2 人的拐点细小凸出，根因是折线段按百分比和固定宽高比近似定位，线条中心未完全落到数据点上。
- `rayk-miniapp/src/components/GoldBeanPanel.vue` 现在在渲染后读取实际图表宽高，按像素计算每个点的坐标、线段长度和旋转角度；2px 线条使用 `bottom = 点坐标 + 1px` 对齐中心线，点、折线、网格线和日期刻度共用同一绘图区。数据点数字标注继续保持移除。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev:remote-test`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；本地 H5、微信远程隔离测试包、微信开发包和微信生产局域网包均已重新生成。
- 远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`；最新组件资源为 `GoldBeanPanel.PNqUf4QV.js`，SHA-256 为 `5ed000a6acebb32da276ed2d904a40d22750665c8d3714296d25b7e5b04649d6`，远程目录未发现 `trend-point-value`。同步前回退副本位于 `/opt/zhiyu-health/backups/trend-centerline-before-20260911-112341/h5-remote-dev`。
- 远程六个容器均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。未重启生产容器、未修改生产数据库或环境配置；为撤销一次归档目录层级误落风险，生产 H5 活跃入口已恢复为原 `index-ccDubFix.js`，临时未引用资源已清理，清理前副本位于 `/opt/zhiyu-health/backups/production-static-before-cleanup-20260911-112341/h5`。

## 2026-09-11 修正直推趋势图首个跃升点对齐

- 修复折线段按元素底边定位造成的端点偏移：折线现在按线条中心线与数据点对齐，首个从 0 人跃升到 2 人的节点不再出现错位折角。
- 已通过前端类型检查、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包构建；远程开发测试 H5 已更新，旧版本回退副本位于 `/opt/zhiyu-health/backups/trend-endpoint-before-20260911-104512/h5-remote-dev`。
- 远程测试静态资源已核对为最新哈希，未残留点位数字标注；远程六个容器 healthy，`/test-api/health` 正常。生产环境未修改。

## 2026-09-11 修正直推趋势图基线与点位标注

- 直推成长趋势折线图已移除每个数据点上方的数值标注，避免图面拥挤；纵轴刻度和无障碍趋势摘要仍保留。
- 统一折线点、水平网格线和日期刻度的绘图区边界，修复数据点与基线视觉错位；仅修改 `rayk-miniapp/src/components/GoldBeanPanel.vue` 的展示层逻辑与样式。
- 已通过前端类型检查、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包构建；远程开发测试 H5 已同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5-remote-dev`，同步前回退副本位于 `/opt/zhiyu-health/backups/trend-baseline-before-20260911-1033/h5-remote-dev`。远程测试容器全部 healthy，`/test-api/health` 返回 200；生产容器和生产静态资源未修改。

## 2026-09-11 隔离开发环境新增推荐关系图与直推趋势图

- 金豆会员页新增默认折叠的“推荐关系图”：展开后显示本人、直推成员（第 1 层）和团队成员（第 2 层），多分支支持横向滑动；为控制移动端体积，最多展开 12 个直推分支、每个分支 6 位下级，超出数量会显示汇总提示。节点昵称和等级每次从当前用户/金豆账户动态读取，不返回手机号。
- 原“直推成长”里程碑卡改为最近 7 天累计直推人数折线图，同时保留当前等级、下一等级差额和等级门槛说明。服务端新增本人专属 `GET /api/client/gold-bean/referral-network`，按当前登录人和租户查询真实推荐关系与注册时间；前端接口失败时不会影响金豆账户主页面，并提供关系图重试入口。
- 远程 Java 21 Docker 构建完整 160 项测试全部通过，新增两级关系与 7 日趋势单测通过；隔离开发客户登录后实测摘要和新接口均返回 200，趋势固定返回 7 个日期点。前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包构建均通过，验收包为 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。
- 已仅重建 `rayk-remote-dev-rayk-server-1` 并同步隔离测试 H5，测试服务 healthy，`https://xingxuyuan.com/test-api/health` 返回 200；生产容器、生产数据库和生产 H5 未重启、未发布。部署前备份位于 `/opt/zhiyu-health/backups/referral-network-dev-before-20260911-1003`。

## 2026-09-10 生产人工开户平台根代理

- 按用户明确确认，未执行真实平台注册支付，在生产环境为已存在的系统用户 `177****6158` 创建 `ACTIVE/PAID` 金豆会员账户，作为平台根代理（无上级推荐人），注册地区为“河南省驻马店市”，并生成唯一推荐码。
- 同一事务创建平台注册根节点人工开户记录（`REGISTRATION_FEE=PAID`、`PLATFORM_ROOT`、`DEVELOPMENT_RECORD` 标记），并写入两条 `ADMIN_MANUAL_CREDIT` 账本流水：`TRADING` 与 `DIGITAL_BANK` 各 `5000.000000` 金豆。当前未修改第二位用户，也未发起真实支付、商户转账或推荐结算。
- 生产复核确认账户、平台注册根节点、地区、余额和两条账本流水一致；生产 Java/MySQL 容器 healthy，`https://xingxuyuan.com/health` 返回 `UP`。操作前相关行备份位于服务器 `/opt/zhiyu-health/backups/manual-platform-root-before-20260910-112909/`。

## 2026-09-10 生产人工推荐注册代理

- 按用户明确确认，为已存在的系统用户 `199****9319` 完成推荐注册人工开户：推荐人是平台根代理 `177****6158`，推荐码为其已生成的推荐码，注册地区为“河南省驻马店市”。B 账户已设为 `ACTIVE/PAID`，并生成个人推荐码。
- 同一事务创建人工推荐注册订单和 `gold_member_referral` 的 A→B 关系（`REGISTRATION_FEE=PAID`、`REFERRER`、`DEVELOPMENT_RECORD` 标记），未发起真实支付，也未向 A 发放注册推荐费；A 的直推人数已按关系校正为 1。B 的 `TRADING` 与 `DIGITAL_BANK` 各写入 `5000.000000` 金豆及对应 `ADMIN_MANUAL_CREDIT` 流水。
- A、B 均有有效微信绑定，后续真实推荐支付可按正常 `REFERRER` 链路校验；生产 Java/MySQL 容器 healthy，`https://xingxuyuan.com/health` 返回 `UP`。操作前备份位于服务器 `/opt/zhiyu-health/backups/manual-referral-before-20260910-160714/`，关系修正前备份位于 `/opt/zhiyu-health/backups/manual-referral-correction-before-20260910-161200/`。

## 2026-09-10 管理员端扩展为桌面 H5 管理网站

- 新增 `rayk-miniapp/src/components/PlatformAdminShell.vue`，将现有 UniApp 管理端扩展为宽度达到 1024px 时的桌面布局：左侧分组导航、顶部当前模块/管理员信息、服务状态和退出登录入口；保持现有品牌绿色体系，并补充键盘焦点、悬停和按压反馈。
- 已接入平台运营总览、合作医院、预录入医生、健康随访及详情、年度会员、金豆会员运营、商城商品、反馈中心和 AI 模型管理页面。医院详情、新建医院等深层页面会自动高亮“医院管理”。桌面端隐藏 UniApp 底部 TabBar，小屏和微信端不改变原单列页面。
- 未新增独立 Vue 后台，未改变 Java 权限校验、角色范围或 API；桌面 H5 与小程序共用登录会话和服务端权限。退出管理台会二次确认并清理本地会话。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`。本次产物已更新到 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`，并确认各包包含 `PlatformAdminShell`。
- 本次只更新代码和本机前端产物，未同步线上测试/生产服务器，未上传微信开发者工具、提交审核或发布生产；正式使用前仍需在浏览器打开 H5 静态站点并用平台管理员账号登录。

## 2026-09-09 同步生产包并发布线上生产环境

- 已按发布清单 `release-20260909-092629-prod-login-layout` 重新生成并更新 H5、微信开发包、微信生产局域网包；生产包目录为 `rayk-miniapp/dist/release/mp-weixin-prod-lan`。当前发布清单基于工作区未提交状态，`gitDirty=true`，后续正式审核包建议从干净提交重新构建。
- 已将当前源码、V66 数据库迁移、Compose 配置和生产 H5 同步到服务器 `/opt/zhiyu-health`；服务器原 `.env`、证书、支付密钥和数据卷未被覆盖。发布前备份位于 `/opt/zhiyu-health/backups/release-20260909-092629-prod-login-layout-before/`，包含源码/配置、H5 和 MySQL 备份。
- 已在服务器使用生产 `.env` 重建并强制重启 `rayk-server`、`rayk-ai`、`nginx`；MySQL、Redis、MinIO 未重建或删除。六个生产容器均为 healthy，公网 `/health`、`/ai/health` 均正常。
- 本地发布清单与线上 `GET https://xingxuyuan.com/api/system/version` 已严格校验一致，线上数据库迁移版本为 V66；本次未执行微信开发者工具上传、审核或正式发布操作，需由具备小程序权限的账号导入 `mp-weixin-prod-lan` 后提交审核/发布。

## 2026-09-09 调整登录页纵向布局

- 登录页改为自适应纵向布局：收紧继承的底部内边距，并将“数据安全保障中”固定在页面安全区上方，减少小屏设备底部无效留白；开发调试身份展开区域不参与该底部吸附布局。
- 已通过 `rayk-miniapp` 的 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；四份前端输出已同步更新。本次未发布线上生产环境。

## 2026-09-09 针对微信审核失败原因补充整改

- 登录页已移除可见的“微信”登录文案和微信官方图标引用，保留原有小程序登录能力；可见按钮统一改为“手机号快捷登录/快捷登录”，授权失败提示也改为通用登录文案。修改位于 `rayk-miniapp/src/pages/login/index.vue`。
- 新增 `rayk-miniapp/src/components/AiGeneratedNotice.vue`，在健康助手/健康树洞、客户健康评估、业务端健康评估和健康报告列表/详情页增加醒目的“人工智能生成内容”提示，并明确仅供健康管理参考、不作为临床诊断依据；规则回退结果不会标记为 AI 生成。
- 已通过 `rayk-miniapp` 的 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；并额外同步 `npm run build:mp-weixin:dev:remote-test`。最新输出为 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`，`git diff --check` 通过。
- 本次未推送生产环境、未修改服务器或数据库。微信公众平台的“深度合成-AI问答”服务类目仍需运营方在平台后台按审核意见选择，代码包无法替代该项平台配置。

## 2026-09-08 按线上版本商品配置启用生产金豆会员

- 用户已将微信公众平台“线上版本”商品调整为：`test_member` 1120 元（推荐注册费）、`normal_member_998` 1120 元（普通会员）、`gold_bean` 1.12 元（金豆）和 `vip_year_399` 399 元（年度会员）。生产服务器已按服务端业务基准 1000 元 + 12% 虚拟支付服务费核对，平台注册/推荐注册实际支付均为 1120 元，推荐人结算基数仍为 1000 元。
- 生产 `/opt/zhiyu-health/.env` 已启用 `GOLD_BEAN_ENABLED=true`、`GOLD_BEAN_DEVELOPMENT_MODE=false`、`GOLD_BEAN_PAYMENT_ENABLED=true`，并注入平台注册、推荐注册、金豆商品 ID、单价、金额及生产虚拟支付回调地址；推荐注册商家转账场景使用已配置的 `1005`。金豆集市卖家转账保持 `GOLD_BEAN_TRADE_PAYMENT_ENABLED=false`，未在本次部署中发起真实订单、支付或转账。
- 后端金豆服务不再把 `GOLD_BEAN_DEVELOPMENT_MODE` 作为生产可用性的唯一条件；生产启用支付时，记录式免费注册路径会被拒绝，必须走已配置的虚拟支付和回调校验。生产机器人权益名称回退为中文默认值“机器人权益服务群”。
- 本次发布标识为 `release-20260908-2247-prod-goldbean`，生产 `/api/system/version` 已返回该标识、V66、H5 指纹和三个微信包指纹；生产 `rayk-server`、`rayk-ai`、Nginx、MySQL、Redis、MinIO 均 healthy，`/health` 与 `/ai/health` 返回 UP。
- 生产 H5、Java、AI 和 Nginx 已同步，生产局域网验收包已生成到 `rayk-miniapp/dist/release/mp-weixin-prod-lan`；微信开发者工具仍需重新导入该目录，服务器部署不会自动替换当前打开的小程序工程。生产局域网包不是可直接提交审核的正式微信商店包。
- 发布前备份位于 `/opt/zhiyu-health/backups/production-before-release-20260908-2247-prod-goldbean/`，未删除或重建 MySQL、Redis、MinIO 数据卷；远程 Java 构建的 159 项测试全部通过，本机前端类型检查、Lint、H5、微信开发包和生产局域网包构建均通过。

## 2026-09-08 区域返利改为仅直接推荐人 20%

- 已按最新方案调整区域盈利：仅统计钻石会员开辟人 A 在该开辟城市内，因代理推荐注册实际获得的 `REFERRAL_DIRECT` / `REFERRAL_DOWNLINE` 金豆作为返利基数；A 的直接推荐人额外获得该基数的 20%，再上级不再产生新的区域返利。`A→B→C` 中若 C 开辟区域，只给 B 20%，A 不再获得 5%或15%。跨城市、缺少城市信息、每日奖励、等级奖励、初始奖励及金豆交易均不触发这套区域返利。
- 服务端修改位于 `rayk-server/src/main/java/com/rayk/health/goldbean/application/GoldRegionProfitService.java`，并更新 `GoldRegionProfitServiceTest` 回归用例；历史 `GRAND_UPLINE` 流水保留读取兼容，但新分配不再写入该类型，未编辑已执行的 Flyway 迁移，数据库仍为 V66。
- 本地前端已通过 `npm run type-check`、`npm run lint`；已重新生成 H5、微信开发包、微信生产局域网包及线上隔离测试微信包。发布清单为 `release-20260908-220110-region20`，代码提交基线 `e52760132e15a5170ebf73310257a9738c8cd159`，工作区按实际情况标记为 dirty。
- 线上隔离测试环境已更新 `rayk-remote-dev-rayk-server-1` 和测试 Nginx，`/test-api/health` 返回 200，容器全部 healthy；测试 H5 使用新静态目录，旧目录保留为回退副本。生产环境已更新 Java、AI、Nginx，生产 `/health`、`/api/system/version`、`/ai/health` 均返回 200，生产容器及 MySQL、Redis、MinIO 均 healthy。
- 本次远程发布前已完成数据库及当前镜像备份：测试 `/opt/zhiyu-health/backups/remote-dev-before-20260908-220110-region20/`，生产 `/opt/zhiyu-health/backups/production-before-20260908-220110-region20/`。未使用本机 Docker，未删除数据卷；生产支付回调配置仍为正式地址，合法签名 GET 返回 200，非法/无签名请求返回 403。
- 微信小程序包已更新到 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`、`rayk-miniapp/dist/release/mp-weixin-dev` 和 `rayk-miniapp/dist/release/mp-weixin-prod-lan`；仍需在微信开发者工具重新导入/上传对应包，服务器发布不会自动替换开发者工具当前打开的工程。

## 2026-09-08 同步生产微信虚拟支付消息推送配置

- 已按微信公众平台当前“明文模式 + JSON”配置，将消息推送 Token 同步到生产 `.env` 及生产 Compose 合并环境文件；生产虚拟支付回调地址保持为 `https://xingxuyuan.com/api/payments/wechat/virtual/notify`，环境为 `0`、模式为 `short_series_goods`。
- 已确认生产虚拟支付所需 AppID、商户号、OfferID、AppKey、商品 ID、回调地址和推送 Token 均已配置；EncodingAESKey 在明文模式下不参与当前服务验签，服务端没有把它当作支付 AppKey 使用。
- 仅重建 `rayk-server` 应用容器，MySQL、Redis、MinIO、AI 和 Nginx 未重建；服务恢复 healthy。使用线上回调实测，正确 Token 签名 GET 返回 200 并回显 `echostr`，错误签名返回 403。
- 原生产配置已备份至 `/opt/zhiyu-health/backups/message-push-config-before-20260908-213945/.env`。截图中包含 Token 和 EncodingAESKey，正式上线前应在微信平台轮换已暴露的密钥，并同步更新服务器配置。

## 2026-09-08 同步当前功能到生产环境

- 已将当前工作区构建并部署到线上生产服务器 `/opt/zhiyu-health`，发布标识为 `release-20260908-204638-workspace`；因工作区原本存在未提交修改，本次发布标记为 `gitDirty=true`，未伪装成干净版本。
- 已更新生产 H5 静态资源、Java 服务、AI 服务和生产 Compose 配置；MySQL、Redis、MinIO 数据容器未删除或重建。生产容器 `rayk-server`、`rayk-ai`、`nginx` 及其依赖当前均为 healthy。
- 生产数据库已由 Flyway 从 V46 增量迁移至 V66，66 条迁移均校验成功；发布前备份位于 `/opt/zhiyu-health/backups/production-release-before-20260908-204947/`，包含项目文件、生产 `.env` 和 MySQL 压缩备份。
- 已通过公网 `/health`、`/api/system/version`、`/ai/health` 和微信虚拟支付回调 GET 验证；版本接口回报数据库 V66，回调地址使用生产路径 `/api/payments/wechat/virtual/notify`。
- Java 生产构建的 159 项测试全部通过；AI 镜像构建成功，但当前 AI 测试基线仍有 3 项既有断言不一致，未在本次发布中绕过或修改。生产上线后的业务验收仍需重点覆盖 AI 报告、七天反馈、金豆/会员支付和机器人权益兑换流程。
- 微信小程序生产局域网包已在本机重新生成到 `rayk-miniapp/dist/release/mp-weixin-prod-lan`；小程序包仍需在微信开发者工具中导入并按发布流程上传，服务器部署不会自动提交微信审核。

## 2026-09-08 删除机器人权益弹窗多余提示

- 已删除兑换成功弹窗中圈选的“也可以长按二维码直接识别。”，保留二维码长按保存提示、扫码加入按钮及其原有交互；本次按现有弹窗层级做最小 UI 调整。
- 已通过 `npm run type-check` 和 `npm run lint`，并重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；各输出均核对不再包含该文案，远程包仍保留“长按图片保存二维码”和“扫一扫加入”相关内容。

## 2026-09-08 重新同步远程隔离测试包并重置机器人兑换测试账号

- 已重新执行 `npm run build:mp-weixin:dev:remote-test`，并同步到 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`；包内已核对包含“长按图片保存二维码”和“请先长按图片保存二维码保存到手机，再点击扫一扫加入对接群。”，且未包含旧的 `saveImageToPhotosAlbum` 调用。
- 微信开发者工具若仍显示旧页面，应关闭当前旧工程后重新导入上述目录并清理编译缓存；本次包目录已在 2026-09-08 18:09:57 更新。
- 按测试要求，在远程开发环境 `rayk_health_remote_dev` 中为手机号 `150****3671` 备份并清理本次新产生的机器人兑换记录及对应 `ROBOT_REDEEM` 扣豆流水，数字银行余额恢复为 `12000` 豆，可交易余额保持 `90` 豆；备份位于 `/opt/zhiyu-health/backups/robot-redeem-reset-before-20260908-1811-fourth/`。
- 已通过线上隔离测试 API 回查总余额 `12090` 豆；生产容器、生产数据库和本机 Docker 均未修改。

## 2026-09-08 第三次重置隔离测试账号机器人兑换

- 按测试要求，在远程开发环境 `rayk_health_remote_dev` 中为手机号 `150****3671` 备份并清理本次新产生的机器人兑换记录及对应 `ROBOT_REDEEM` 扣豆流水，数字银行余额恢复为 `12000` 豆，可交易余额保持 `90` 豆；会员等级和其他账户数据未修改。
- 本次备份位于 `/opt/zhiyu-health/backups/robot-redeem-reset-before-20260908-1800-third/`，线上生产容器、生产数据库和本机 Docker 均未修改。
- 已通过线上隔离测试 API 回查，当前总余额为 `12090` 豆，数字银行余额为 `12000` 豆，可交易余额为 `90` 豆；该账号可再次测试机器人权益兑换。

## 2026-09-08 再次重置隔离测试账号机器人兑换

- 按测试要求，在远程开发环境 `rayk_health_remote_dev` 中为手机号 `150****3671` 备份并清理本次新产生的机器人兑换记录及对应 `ROBOT_REDEEM` 扣豆流水，数字银行余额恢复为 `12000` 豆，可交易余额保持 `90` 豆；会员等级和其他账户数据未修改。
- 本次备份位于 `/opt/zhiyu-health/backups/robot-redeem-reset-before-20260908-1740-second/`，线上生产容器、生产数据库和本机 Docker 均未修改。
- 已通过线上隔离测试 API 回查，当前总余额为 `12090` 豆，数字银行余额为 `12000` 豆，可交易余额为 `90` 豆；该账号可再次测试机器人权益兑换。

## 2026-09-08 将二维码保存按钮改为纯提示

- “长按图片保存二维码”现在是非交互提示元素，不再绑定 `saveImageToPhotosAlbum`、相册授权或保存点击事件；用户通过二维码图片自身的微信 `show-menu-by-longpress` 菜单保存图片。
- “扫一扫加入”按钮继续保留原有扫码功能，操作提示仍明确要求先长按图片保存二维码，再扫码加入对接群。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、线上隔离测试包和生产局域网包；本次未修改后端、测试账号余额或兑换记录。

## 2026-09-08 调整机器人权益二维码操作提示文案

- 兑换成功弹窗的保存按钮已改为“长按图片保存二维码”；黄色提示已改为“请先长按图片保存二维码保存到手机，再点击扫一扫加入对接群。”，原有保存、扫码和相册授权逻辑保持不变。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、线上隔离测试包和生产局域网包；本次未修改后端、测试账号余额或兑换记录。

## 2026-09-08 重置隔离测试账号机器人兑换并补充操作顺序提示

- 已在 `rayk_health_remote_dev` 中为测试账号 `150****3671` 备份并清理上一条机器人兑换记录及对应 `ROBOT_REDEEM` 扣豆流水，数字银行余额恢复为 `12000` 豆，可交易余额保持 `90` 豆；当前可重新走一次性兑换流程。备份位于 `/opt/zhiyu-health/backups/robot-redeem-reset-before-20260908-1732/`，生产数据未修改。
- 机器人兑换弹窗新增明确提示：“请先点击‘保存二维码’保存到手机，再打开微信‘扫一扫’加入对接群”；原有长按识别说明保留。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、线上隔离测试包和生产局域网包；本次未执行第二次实际兑换，未再次扣除金豆。

## 2026-09-08 修复机器人权益二维码保存到相册

- 修复微信端点击“保存二维码”时报 `saveImageToPhotosAlbum:fail api scope is not declared` 的问题：在 `src/manifest.json` 的微信配置中声明 `scope.writePhotosAlbum`，并在保存前显式调用 `uni.authorize`；已授权直接保存，曾拒绝时引导用户打开设置。
- 已通过前端 `type-check` 和 ESLint；H5、微信开发包、微信远程隔离测试包和生产局域网包均已重新生成。三个微信包的最终 `app.json` 均已核对包含相册权限声明，远程隔离包同时包含机器人二维码资源和 `asset://robot-group-qr` 映射。
- 本次仅修改小程序权限声明与保存流程，未修改兑换扣豆、二维码内容、后端接口或生产环境；线上隔离服务仍保持健康。

## 2026-09-08 修复线上隔离环境机器人权益二维码未配置

- 已定位根因：线上隔离 `.env.remote-dev` 中 `REMOTE_DEV_GOLD_BEAN_ROBOT_GROUP_QR_IMAGE_URL` 实际为空，旧版隔离服务端因此返回“二维码尚未配置”；当前微信远程测试包内的 `robot-group-qr` 图片资源本身存在。
- 已备份远程配置至 `/opt/zhiyu-health/backups/robot-qr-config-before-20260908-1714/.env.remote-dev`，并在隔离配置中设置 `asset://robot-group-qr`；仅重建 `rayk-remote-dev-rayk-server-1`，容器环境已核对该值且状态为 `running|healthy`。
- 已将 `compose.remote-dev.yml` 的默认值同步为 `asset://robot-group-qr`，避免后续隔离部署再次因空配置失效。小程序会把该标记解析为包内二维码；若以后使用企业微信活码，可继续通过远程环境变量替换为外部 HTTPS 地址。
- 本次未再次执行机器人兑换，因此没有额外扣除金豆；生产容器、生产数据库、生产静态资源和本机 Docker 均未修改。

## 2026-09-08 隔离测试账号补充机器人兑换余额

- 已按测试要求将线上隔离环境 `rayk_health_remote_dev` 中手机号 `150****3671` 账号的数字银行余额设为 `12000` 豆；可交易余额保持 `90` 豆，会员等级、注册状态和推荐关系未修改。
- 本次仅更新 `rayk-remote-dev` Compose 项目对应的 MySQL 测试容器，用于验证机器人权益一次性兑换；同服务器上的生产容器、生产数据库、支付配置和本机 Docker 均未修改。
- 已通过线上隔离测试 API 回查，当前总余额为 `12090` 豆，数字银行余额为 `12000` 豆，可交易余额为 `90` 豆。机器人真实微信扫码/入群链路仍需在最新隔离包中手动验收。

## 2026-09-08 运营数据搜索改为用户昵称和手机号

- 平台管理员金豆运营数据的会员账户、支付订单、推荐关系和金豆流水四个页签，公共搜索框已改为按用户昵称/显示名和手机号匹配；完整手机号通过已有手机号哈希校验，兼容掩码手机号搜索，不输出明文手机号。
- 机构、订单号、推荐码、流水描述、交易号等非用户字段不再参与关键词搜索，但记录卡片中的机构和订单号展示、状态筛选仍保留。
- 前端已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；后端 Java 21 Docker 构建及全量 Maven 测试通过，H5 和后端已同步至线上隔离开发环境，健康检查通过，正式生产环境未修改。

## 2026-09-08 区域盈利按注册城市隔离

- 按最新业务口径，钻石会员 A 新开辟城市区域后，区域盈利只统计该城市用户通过推荐注册、且 A 自己实际收到的 `REFERRAL_DIRECT` / `REFERRAL_DOWNLINE` 金豆奖励；服务端同时校验注册用户账户的 `gold_member_account.city` 与区域的 `gold_region.city`，跨城市或缺少城市信息的注册不会产生区域盈利或上级返利。
- 原有返利比例和基数不变：A 由 B 推荐时 B 额外获得 A 该笔奖励的 20%；B→C→A 时 C 获得 5%、B 获得 15%。每日奖励、等级奖励、初始奖励和金豆集市交易仍不触发区域返利。
- 已在 `GoldRegionProfitService` 增加同城前置校验，并补充跨城市注册不返利回归测试；`GoldRegionProfitServiceTest` 7 项、`GoldBeanRegistrationRewardTest` 6 项合计 13 项通过。未新增数据库结构、未修改小程序前端和支付配置；本次未部署线上环境。

## 2026-09-08 前端金豆余额最多显示两位小数

- 数字银行、可交易金豆及总余额在客户俱乐部、金豆集市、传奇俱乐部和平台运营界面统一最多显示两位小数；仅改变展示格式，实际余额和计算精度不变。
- 金豆流水金额、集市挂单数量、奖励数量和其他交易明细继续使用原有精度显示，避免把非余额数据误做两位截断；用户主动输入仍按整数规则校验。
- 已重新生成 H5、微信开发包、远程隔离测试包和生产局域网包。

## 2026-09-08 金豆用户输入改为整数

- 按最新业务口径，平台购豆、集市发布和集市买入的用户输入统一改为整数数字键盘（`type=number`、`step=1`、`inputmode=numeric`），提交时增加整数校验并分别提示购买、发布或买入数量必须为整数。
- 账户余额、后台奖励、区域返利和历史账本仍保留既有小数精度；本次只限制用户主动输入的交易数量，不修改道具价格、支付金额、后端接口或数据库。
- 已重新生成 H5、微信开发包、远程隔离测试包和生产局域网包。

## 2026-09-08 修复金豆小数点输入被清除

- 金豆平台购买、集市发布和集市买入输入改为保留输入中的原始字符串，用户输入 `.` 或 `10.` 等中间状态时不会被 `v-model.number` 即时转换并清除；提交时才转换为数字并沿用原有六位精度校验。
- 道具价格配置只参与支付金额计算，不会再影响数量输入；已重新生成 H5、微信开发包、远程隔离测试包和生产局域网包，未修改后端或支付配置。

## 2026-09-08 修复金豆数量输入仅支持整数

- 微信端金豆购买、集市发布和买入输入改用支持小数点的 `digit` 数字键盘；现有前端数量校验、六位精度和后端 `DECIMAL(24,6)` 逻辑保持不变。
- 仅调整输入控件类型，不改变价格、账户分账、订单或交易接口；前端需重新生成 H5、微信开发包、远程隔离测试包和生产局域网包。

## 2026-09-08 隐藏金豆小数规则提示文案

- 金豆购买和金豆集市的数量输入框不再展示最小值、最大位数或“支持小数”的说明；无效输入统一提示“请输入有效的购买/发布/买入数量”。
- 仅调整界面文案，输入精度、前端校验、后端 `DECIMAL(24,6)` 数据和接口行为均保持不变。
- 前端需重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；未修改后端、数据库或生产环境。

## 2026-09-08 机器人权益兑换对接群二维码与扣豆完善

- 机器人权益兑换仍限定已注册、状态有效且每个用户仅一次；服务端在账户行锁事务内再次校验兑换状态和数字银行余额，成功后固定扣除 10000 枚数字银行金豆，写入幂等金豆流水和兑换记录，失败不会扣豆。
- 兑换记录的群二维码地址支持 `GOLD_BEAN_ROBOT_GROUP_QR_IMAGE_URL` 外部配置；外部地址为空时返回内置二维码标记，由小程序解析为 `src/assets/ui/gold-bean/robot-group-qr.jpg`，因此开发/隔离测试环境未配置外部活码时也能完成兑换验收。外部配置仍可用于后续轮换群活码。
- 兑换成功弹窗新增二维码加载失败提示、微信长按识别说明、保存到相册和“扫一扫加入”按钮；微信端保存前会下载/解析二维码并处理相册权限，H5 端改为预览并提示长按保存。扫一扫调用微信扫码能力，识别后由微信页面继续处理入群。
- 已通过机器人权益 Java 定向测试 `9` 项、前端 `type-check` 和 ESLint；H5、微信开发包、远程隔离测试包和生产局域网包需在本次改动后重新构建。未部署后端到线上隔离或生产环境，远程真机端到端兑换仍需先重启使用本次 Java 代码的隔离服务。

## 2026-09-07 掉级期间保持完整交易额度（线上隔离开发环境）

- 按最新口径调整保护期后的状态机：第 8 天保护期结束后继续按自然日掉级，但掉级期间保持 100% 交易额度；掉到普通会员当天仍保持 100%，普通会员继续 1 个自然日未推荐后才进入配置的 50% 交易限制。当前等级枚举最低为普通会员，因此不新增更低等级。
- 直推恢复逻辑不变：掉级期间推荐成功最多恢复 1 个等级且不超过历史最高等级，同时恢复 100% 交易额度并刷新 7 天保护期；数字银行和可交易金豆余额不清空。
- 同步更新后端提醒、会员页说明和 README 口径；金豆相关定向 Java 测试 37 项全部通过，全量 Java 21 Docker 测试 148 项全部通过，后端隔离镜像已构建并重建服务，容器健康检查通过。
- 前端已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；H5 已同步至隔离开发目录，正式生产环境未修改。

## 2026-09-07 区域返利收窄为注册推荐奖励

- 按最新业务口径，区域返利仅针对钻石会员开辟区域内代理推荐注册产生的 `REFERRAL_DIRECT` / `REFERRAL_DOWNLINE` 金豆奖励；每日奖励、等级解锁奖励、初始奖励和金豆集市交易均不触发区域返利。
- 以钻石区域开辟人 A 为中心向上读取最多两级推荐关系：A 由 B 直接推荐时，A 自己收到的注册推荐奖励作为基数，B 额外获得 20%；B→C→A 时，C 额外获得 5%，B 额外获得 15%。同一次注册中 D、C、B 等其他账户收到的推荐奖励不会再次作为 A 区域返利基数，A 也不会重复获得 100% 区域奖励；返利仍使用双账本、整枚金豆和幂等流水。
- 已将 `GoldRegionProfitService` 的触发主体收窄为区域开辟人 A 自己的注册推荐奖励，并补充非注册奖励及同一区域其他账户奖励不返利的回归测试；小程序类型检查、Lint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新构建。本次未部署生产环境。由于当前工作机未安装 Maven，定向 Java 测试需在已有 Java 21 Docker 构建环境中执行。

## 2026-09-07 钻石开辟区域改用省市选择器

- 钻石会员“区域权限”卡片已改为与注册流程一致的原生省市选择器，仅选择到市级；卡片会显示已选的“省 / 市”，并保留申请中的加载、成功和失败反馈。
- 区域申请提交时继续调用原 `openGoldRegion` 接口，发送注册流程同样的 `省 / 市` 文本；后端区域唯一性、推荐链层级、钻石等级鉴权和不可更改规则未改动。
- 小程序已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；H5、微信开发包、远程隔离测试包和生产局域网包均来自本次构建。未部署生产环境。

## 2026-09-07 隔离测试账号切换钻石会员验收

- 已将隔离测试环境中手机号为 `150****3671` 的已注册账号会员等级和历史最高等级设为 `DIAMOND`（钻石会员），直推人数仍为 `0`，未伪造推荐关系或补发等级解锁奖励。
- 该账号注册状态仍为 `PAID`，数字银行 62、可交易金 60 的账本余额未修改；平台级传奇白名单原为 `ACTIVE`，为避免客户端优先展示传奇俱乐部而遮挡钻石会员页，已在隔离库软撤销为 `REVOKED`，历史记录保留且可恢复。
- 本次仅修改隔离数据库 `rayk_health_remote_dev`，并核对目标 MySQL 容器属于 `rayk-remote-dev` Compose 项目；未修改生产数据库、生产容器、生产静态资源或支付配置。验收时重新登录并进入“俱乐部”即可看到钻石会员页面。

## 2026-09-07 传奇人物俱乐部视觉展示改造

- 按用户提供的参考图重做传奇人物俱乐部展示层：新增深绿英雄卡、数字银行余额展示、金豆集市入口、专属服务区和底部品牌愿景横幅，采用主色 `#0F8A6D`、深绿 `#0B5D4E`、浅薄荷 `#DFF7F0` 与金色 `#D7B46A`。
- 已复用用户提供的 `传奇人物俱乐部_SVG资源包`，复制到 `rayk-miniapp/src/assets/ui/legendary-club/`，用于徽章、集市、礼盒、金豆、七项服务图标、未来服务标签和波纹装饰；没有新增图片生成或外部字体依赖。
- 七项专属服务均为不可点击的展示卡片，没有新增路由、接口或业务实现；原有传奇资格摘要加载、数字银行余额显示和“金豆集市”入口跳转保持不变，普通会员、管理员和其他页面未改动。
- 本次前端已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；H5、微信开发包、微信远程隔离测试包和生产局域网包均来自本次源码构建。未部署生产环境，真机视觉验收仍需重新导入远程隔离测试包并清理旧编译缓存。

## 2026-09-07 传奇俱乐部英雄徽章与集市入口细节调整

- 将英雄徽章与背景圆环统一到同一定位和中心，窄屏下同步调整圆环尺寸，避免徽章向右下偏移。
- 删除金豆集市入口右侧的礼盒/健康好物宣传块，入口恢复为图标、标题、说明和独立箭头；集市跳转行为不变。
- 已重新通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；构建包未残留宣传块文案，未修改后端或其他页面。

## 2026-09-07 传奇俱乐部英雄卡细节微调

- 删除英雄卡底部 `HEALTHIER LIFE / BRIGHTER TOMORROW` 英文小字。
- 将徽章与圆环整体向左调整 30rpx，窄屏断点保持同向偏移，避免装饰图形贴近右侧边缘。
- 已重新生成 H5、微信开发包、微信远程隔离测试包和生产局域网包；未修改接口、跳转和其他角色功能。

## 2026-09-07 金豆相关虚拟支付统一加价 12%（线上隔离开发环境）

- 金豆相关虚拟支付链路已统一按 `GOLD_BEAN_VIRTUAL_PAYMENT_SURCHARGE_PERCENT` 加价，默认 12%，范围包括平台注册、推荐注册和金豆集市买入；买家支付金额与平台业务/收款人结算金额已分开持久化。
- 平台平台注册业务基准为 998 元，买家实际支付 1117.76 元；推荐注册业务基准为 998 元，买家实际支付 1117.76 元，推荐人商家转账为 998 元；集市卖家结算基准为 1 元/豆，买家实际支付 1.12 元/豆。推荐人和卖家商家转账仍按基准金额，不会把 12% 加价转给收款人。
- 新增 V64 迁移，为注册订单和集市交易保存支付金额快照；历史数据回填为原金额，旧订单保持兼容。虚拟支付下单、标准回调和虚拟支付回调均按支付快照校验，转账和金豆交割继续按基础结算金额执行。
- 已在隔离服务器 Java 21 Docker 环境构建并部署，V64 已由 Flyway 执行，`https://xingxuyuan.com/test-api/health` 返回 200；容器实际生效的平台注册费和推荐注册费均为 `99800` 分，加价配置为 `12%`。本次远程 Java 21 Docker 全量 `141` 项测试全部通过。
- 前端已通过 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和远程隔离测试包构建；最新验收包为 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。本次只更新隔离测试环境，未修改生产容器、生产数据库或生产静态资源。
- 微信虚拟支付后台商品价格已按实际支付价配置并重新发布：平台注册商品和推荐注册商品均为 1117.76 元，`gold_bean` 为 1.12 元/豆；微信侧真实支付、回调和收款到账尚未由本次构建自动验收。商品价格若与 `goodsPrice` 不一致，微信会拒绝支付请求。

## 2026-09-07 传奇俱乐部移除最近集市买入记录

- 传奇人物俱乐部页面移除“最近集市买入记录”卡片，同时删除对应流水查询、刷新状态和无用样式；保留数字银行余额与金豆集市入口。
- 前端需重新通过 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和微信远程隔离测试包构建；本次只修改展示层，后端账本和集市交易数据不受影响。

## 2026-09-07 修复传奇俱乐部首次进入不加载

- 俱乐部页首次进入时，普通会员面板与传奇面板现在同步挂载，仅通过 `v-show` 切换展示，避免异步判断传奇资格后组件引用尚未建立，导致首次进入只显示空白/加载态、第二次点击才触发刷新。
- 传奇资格查询成功后会在首次进入直接调用已挂载的传奇面板刷新；未命中传奇资格时普通会员面板行为不变，管理员、游客和消息入口不受影响。
- 已重新构建并核对 H5、微信开发包、微信生产局域网包和微信远程隔离测试包；本次只修改前端生命周期与渲染时序，生产环境未修改。

## 2026-09-07 平台一级代理平台购豆入口恢复（线上隔离开发环境）

- 已定位平台购豆消失的原因：传奇人物改为“只能购买集市数字银行挂单”时，平台直购创建接口被一并全局关闭，会员摘要也固定返回 `goldBeanPurchaseEnabled=false`，导致一级代理页面和接口同时不可用。
- 已恢复平台直购订单：已注册的平台一级代理（以及服务端定义的其他平台购豆资格会员）可在普通金豆会员页创建 `GOLD_BEAN_PURCHASE` 虚拟支付订单，支付金额按配置加价 12%，回调成功后按双账本规则入账；传奇资格用户仍由服务端拒绝新建平台直购订单，只能走集市数字银行挂单。
- 新增回归测试覆盖平台购豆订单金额/数量、平台一级代理摘要入口和传奇用户服务端拦截。远程 Java 21 Docker 构建通过 Maven 全量 `146` 项测试，失败 0、错误 0；隔离服务已重建并 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。
- 前端已重新通过 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和微信远程隔离测试包构建；最新验收包为 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。真实微信支付回调仍需用新的平台购豆订单完成真机验收，生产容器、生产数据库和生产静态资源未修改。

## 2026-09-07 管理员查看传奇人物数字银行余额

- 平台管理员“金豆会员运营”页的传奇人物资格列表现在展示已匹配会员的数字银行余额；未匹配或未建档显示“未建档”，手机号继续脱敏。余额由服务端只读读取 `gold_member_account.digital_bank_balance`，名单接口仍由 `PLATFORM_ADMIN` 权限和开发环境开关共同保护。
- 已新增余额字段的后端映射测试，远程隔离 Java 21 Docker 构建通过 Maven 全量 `139` 项测试，失败 0、错误 0；隔离服务已重建并健康检查 HTTP 200。
- 前端 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和现有微信远程隔离测试包均已重新构建；未修改生产容器、生产数据库或生产静态资源。

## 2026-09-07 传奇页面与集市展示精简

- 传奇俱乐部移除无用的交易规则提示卡；传奇用户在金豆集市不展示“可交易金豆”余额、发布挂单卡片和“我的挂单”入口，只保留全国数字银行挂单浏览/买入及数字银行余额展示。普通会员的发布、区域交易和余额展示不变。
- 传奇买入仍由服务端强制只接受 `DIGITAL_BANK` 挂单，成交后全部入买方数字银行；前端仅做对应展示和旧客户端异常提示，不能替代后端鉴权。
- 前端已通过 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和微信远程隔离测试包构建；最新验收包为 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。远程隔离环境健康检查保持 200，生产环境未修改。

## 2026-09-07 传奇人物仅集市数字银行挂单交易规则

- 传奇人物交易规则已收紧为：只能在金豆集市购买全国范围的 `DIGITAL_BANK` 数字银行金豆挂单，买入后全部进入买方数字银行；不能向平台直接购买，也不能新建金豆挂单。
- Java 后端的集市查询和买入鉴权继续作为唯一准入依据；新增传奇卖家创建挂单拒绝和平台直购接口关闭校验，旧的历史直购订单回调路径保留，避免已创建订单无法正常完成交割。普通会员的本区域 `TRADING` 挂单交易不变。
- 传奇俱乐部已移除平台直购数量/支付卡片，改为集市入口和规则提示；金豆集市对传奇用户隐藏“我的挂单”和“发布挂单”，并保留对服务端异常或旧客户端请求的前端提示。传奇俱乐部最近流水同时纳入集市买入记录。
- 前端已通过 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和远程隔离测试包构建；最新隔离验收包为 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。远程隔离 Java 21 Docker 构建通过 Maven 全量 `138` 项测试，容器已重建并 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。
- 本次只部署远程隔离测试环境，备份位于 `/opt/zhiyu-health/backups/legendary-market-only-before-20260907-084633`；未修改生产容器、生产数据库或生产静态资源。真实微信虚拟支付及新交易仍需在最新隔离包中用新挂单订单验收。

## 2026-09-07 隔离测试账号加入传奇资格

- 已将隔离测试环境中手机号为 `150****3671` 的已注册账号加入平台级传奇资格白名单，资格状态为 `ACTIVE`；账号原有会员等级、手机号绑定、登录信息和金豆账本未修改。
- 该账号当前仍是 `ORDINARY` 会员且注册状态为 `PAID`，已核对传奇白名单与账号现有手机号哈希一致，登录后可用于传奇购豆/金豆集市跨区域等功能验收。
- 本次仅修改隔离测试库 `rayk_health_remote_dev`，未修改生产数据库、生产容器或生产静态资源。

## 2026-09-06 平台管理员金豆运营页移除规则快照卡片

- 删除平台管理员金豆运营页的“当前规则快照”卡片及其无用样式，保留会员统计、传奇人物资格、授权码和运营数据筛选功能；删除后统计区直接衔接“运营数据”，没有额外空卡片。
- 已通过前端 `type-check` 和 ESLint；H5、微信开发包、微信生产局域网包和微信远程隔离测试包均已重新构建，构建产物不再包含该卡片文案。
- 之前开发者工具打开的是旧的 `MP-WEIXIN-DEV-REMOTE-TEST` 包，已重新生成 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`；本次仅修改前端页面与构建产物，未修改后端、数据库或生产容器，验收时需重新导入该目录。

## 2026-09-06 传奇购豆虚拟支付回调两处风险修复（线上隔离开发环境）

- 虚拟支付回调现在强制要求微信回传非空交易号；缺少交易号、商品/金额等校验不通过时不会执行入账，也不会把订单标记为 `PAID`。
- 传奇购豆回调在数字银行入账前再次校验当前传奇资格；资格被撤销的订单会回调失败并回滚，不会继续发放金豆。
- 新增 `GoldBeanPaymentServiceTest` 覆盖“无交易号拒绝且不入账”和“先校验传奇资格再入账”。隔离服务器 Java 21 Docker 构建通过，Maven 全量 `137` 项测试失败 0、错误 0；隔离服务已重建并 healthy，`/test-api/health` 返回 200。
- 本次未修改生产容器、生产数据库或生产静态资源。真实微信订单仍需在隔离包上完成一次支付回调验收；代码、测试和健康检查通过不等同于微信侧真实到账验收。

## 2026-09-06 金豆集市区域乱码修复（线上隔离开发环境）

- 已确认截图中的乱码不是微信字体或前端布局问题：测试库 `gold_member_account.city` 与 `gold_member_trade_listing.region_city` 被保存成了 UTF-8 二次编码，API 原样返回后只有区域文案出现乱码。
- 新增 V63 数据修复迁移，已在隔离测试库执行成功；账号城市、注册订单城市和挂单区域均恢复为标准 UTF-8。后端新增安全的 `TextEncodingUtils`，在注册、区域开辟、挂单区域读取和页面摘要返回处纠正历史二次编码；有效中文、英文和正常拉丁文本保持不变。
- 新增编码回归测试；远端 Java 21 Docker 构建通过，Maven 全量 `135` 项测试通过（失败 0、错误 0）。隔离 `rayk-server` 已重启并 healthy，生产容器、生产数据库和生产静态资源未修改。
- 当前截图中的最新挂单区域数据已核对为标准字节序列，重新进入金豆集市即可显示正常中文。现有微信包无需改前端代码即可读取修复后的 API 数据；若使用旧包仍看到旧缓存，退出小程序后重新编译/进入即可。

## 2026-09-06 自动收款授权状态恢复修复（线上隔离开发环境）

- 已定位“点击自动收款后只提示授权状态已更新、后续推荐奖励仍需手动确认”的根因：微信授权状态查询在 `WAIT_USER_CONFIRM` 时可能只返回状态、不重复返回 `package_info`；旧代码却把数据库中首次申请保存的授权包覆盖为空。前端因此拿不到可再次打开微信授权页的参数，实际上没有完成首次授权，后续订单自然继续走普通商家转账并要求逐笔确认。
- 后端现已在授权状态查询缺少字段时保留已有 `package_info`，已生效状态缺少 `authorization_id` 时保留已签回的授权编号；对历史上已经丢失授权包且没有可用授权的记录，下一次点击会安全创建新的授权申请。前端不再把未生效状态提示为“授权状态已更新”，改为明确提示尚未完成授权。
- 隔离环境 `rayk-server` 已完成含 Maven 测试的 Docker 构建并重启，容器 healthy；`https://xingxuyuan.com/test-api/health` 与生产 `https://xingxuyuan.com/health` 均返回 200。生产容器、生产数据库和生产静态资源未修改。
- 前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新构建；最新验收包为 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。旧订单不会被重新发起或重复转账，必须用新订单验证授权生效后的免确认转账。
- 真机验收：推荐人导入最新隔离包并进入“俱乐部”，点击“自动收款”，在微信授权页完成同意/确认；返回后应显示自动收款已开启或不再显示入口。再用新推荐注册订单验证平台转账，订单应直接进入成功，不再出现“确认收款”。代码、容器和健康检查已验证，真实微信“首次授权→新订单→自动转账→到账”仍需完成这次真机验收。

## 2026-09-06 机器人权益条件提示文案移除

- 已删除金豆会员页机器人权益卡片中的“已满足兑换条件”文案，保留兑换资格判断、按钮禁用逻辑和兑换流程；同时移除对应的无用样式。
- 前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新构建，构建产物中已确认不存在该文案。

## 2026-09-06 俱乐部暂无数据与隔离测试环境恢复

- 已定位截图中的“暂无数据”：隔离测试 API 曾因 `rayk-remote-dev` 启动时未正确加载 `.env.remote-dev`，错误挂载生产 `rayk_mysql_data`，导致测试 MySQL 与生产 MySQL 争抢 `ibdata1` 锁；Java 后端未启动，`/test-api` 返回 502。该状态会被旧前端请求层误显示为“暂无数据”，因为 502 HTML 没有标准 `code/message`。
- 已使用 `--env-file .env.remote-dev -f compose.yml -f compose.remote-dev.yml` 重新拉起隔离项目；现在 MySQL、Redis、MinIO、AI、Java、Nginx 六个服务均 healthy，隔离 MySQL 使用 `rayk_remote_dev_mysql_data`，`https://xingxuyuan.com/test-api/health` 返回 200，生产 `https://xingxuyuan.com/health` 仍返回 200。未删除数据卷、未重建生产服务、未修改生产数据库。
- 前端 `request.ts` 已补充 502/503/5xx 和非 JSON 响应的明确中文错误，金豆会员页也会拒绝静默接受空 summary，避免服务故障再次伪装成空数据。已重新通过 `type-check`、ESLint、H5、微信开发包、线上隔离测试包和生产局域网包构建；最新隔离验收包为 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。
- 真机/开发者工具需重新导入或重新编译上述最新隔离包，并重新进入“俱乐部”。接口健康已验证，具体账号的会员数据仍需在登录态下验收；若账号在隔离库尚未注册，首次进入会由后端创建普通会员账户。

## 2026-09-06 机器人权益卡片未配置提示移除（线上隔离开发环境）

- 删除金豆会员页“机器人权益”卡片中截图红框的“服务群二维码尚未配置，兑换完成前请联系管理员。”提示，保留卡片说明文案、标题、兑换资格状态和“兑换机器人权益”按钮；兑换确认弹窗文案未改动。
- 前端 `type-check`、ESLint、H5、微信开发包、`mp-weixin-dev-remote-test` 和生产局域网包均已重新构建；生产 H5 目录、生产容器和生产数据库未修改。
- `rayk-remote-dev` 六个服务均 healthy，`https://xingxuyuan.com/test-api/health` 和生产 `https://xingxuyuan.com/health` 均返回 200。

## 2026-09-06 普通会员 20 天奖励与活跃保护顺序修复（线上隔离开发环境）

- 已修复普通会员状态机：注册成功后每日发放 60 金豆，最多连续 20 天；第 15-19 天没有直推时连续提示 5 天，20 天内已有直推则不显示这组提示。
- 奖励期内的直推只记录推荐关系、等级和推荐奖励，不提前开启 7 天活跃保护期；第 20 天奖励完成后首次结算自动开启 7 天保护期。保护期结束后交易额度限制为 50%，数字银行和可交易两个账本仍保留会员间交易权；直推成功恢复 100% 额度并刷新 7 天保护期。
- 后端回归新增“第 20 天完成后开启保护期”“第 15-19 天无直推提醒”“奖励期内直推抑制提醒但不提前保护”测试；本地金豆定向测试 22 项、Java 全量测试 132 项通过。远端 Java 21 Docker 构建通过全量 125 项测试（失败 0、错误 0）。
- 前端 `type-check`、ESLint、H5、微信开发包和生产局域网包均已重新构建；隔离测试 H5 使用独立挂载目录 `rayk-miniapp/dist/build/h5-remote-dev`，`mp-weixin-dev`、`mp-weixin-prod-lan` 输出均来自本次构建，标准本地 H5 输出仍为 `rayk-miniapp/dist/build/h5`。
- 远端部署前备份为 `/opt/zhiyu-health/backups/gold-daily-protection-before-20260906-113700/source-config.tgz`，本次生成的隔离 H5 另保存在同目录的 `h5-shared-before-restore`；为避免隔离测试覆盖生产静态目录，已将生产 Nginx 当前静态内容恢复到共享 `rayk-miniapp/dist/build/h5`，隔离 Nginx 改挂 `h5-remote-dev`。已重建 `rayk-remote-dev-rayk-server-1` 和隔离 Nginx，六个隔离服务 healthy，Flyway 仍为 V60，`https://xingxuyuan.com/test-api/health` 返回 200，生产 `/health` 返回 200；生产容器和生产数据库未重建/修改，生产共享 H5 最终保持为生产 Nginx 当前内容。真实微信支付未在本次验证中执行。

## 2026-09-06 奖励期内多次解锁等级的保护期边界修复

- 修正判断依据：是否立即启动/刷新 7 天保护期只看初始 20 天每日奖励是否完成，不再看当前是否仍为普通会员。这样用户在第 20 天前已经解锁铜牌后，继续解锁银牌、金牌或钻石时，也不会提前开启或刷新保护期。
- 初始 20 天奖励期间若存在旧的保护期、掉级锚点或额度限制状态，结算时会清理为奖励期状态；第 20 天奖励完成后的首次结算才统一启动 7 天保护期。20 天之后的推荐仍按既有规则启动/刷新保护期。
- 新增回归测试覆盖“铜牌状态下第 20 天前解锁银牌仍无保护期，完成第 20 天后才启动保护期”。远端 Java 21 Docker 构建完整 Maven 测试 `131` 项通过（失败 0、错误 0）。
- 本次尝试切换线上隔离新镜像时发现隔离 Compose 与生产 Compose 共用 `rayk_mysql_data`、Redis 和 MinIO 数据卷；强制重建会让两套 MySQL 争抢 `ibdata1`。已停止本次新建的冲突隔离容器，保留全部数据卷，生产 `/health` 仍返回 `200`；新逻辑已完成源码和远端构建验证，但尚未切换到 `https://xingxuyuan.com/test-api`，需先完成隔离卷/Compose 拆分后再部署。

## 2026-09-06 推荐奖励收款操作区优化（线上隔离开发环境）

- 推荐奖励列表中的“确认收款/刷新状态”和“自动收款”现在使用同一操作区并排显示，统一触控尺寸；自动收款说明卡在没有待处理奖励时仍保留独立入口，避免入口消失。
- 删除推荐奖励列表中额外的失败原因提示文案，保留状态信息和按钮反馈；微信 `requestMerchantTransfer:fail:internal`、取消或关闭收款确认弹窗时，前端统一提示“尚未确认收款”（自动收款授权场景提示“尚未完成自动收款授权”）。
- 前端 `type-check`、ESLint、H5、微信开发包、线上隔离测试包和生产局域网包均已重新构建；最新线上隔离测试包为 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。

## 2026-09-06 推荐奖励首次授权自动收款兼容方案（线上隔离开发环境）

- 新增 V60 `gold_referral_authorization`，按租户、用户、当前小程序 AppID、OpenID 和转账场景保存一次性微信商家转账收款授权；授权状态和授权编号服务端持久化，授权编号不返回给前端之外的日志或普通业务展示。
- 推荐人在俱乐部卡片点击“开通自动收款”后，服务端调用微信用户确认授权接口并返回 `package_info`，小程序调用 `wx.requestMerchantTransfer` 完成首次确认；授权通知经过 API v3 验签、解密和用户绑定校验后进入 `TAKING_EFFECT`。授权有效时，后续推荐奖励调用 `/v3/fund-app/mch-transfer/transfer-bills/transfer`，不再要求每笔用户确认。
- 兼容旧用户和异常状态：没有授权、授权处理中、授权被关闭或授权不完整时，推荐奖励继续调用原 `/v3/fund-app/mch-transfer/transfer-bills`，推荐人仍可在小程序中手动确认 `WAIT_USER_CONFIRM`；授权查询、通知回调和定时补偿均使用同一授权申请号幂等处理。
- 已部署的隔离环境配置授权通知地址为 `https://xingxuyuan.com/test-api/api/payments/wechat/merchant-transfer/authorization-notify`；V60 已由 Flyway 执行，远程 `rayk-server` 容器已重建并 healthy，回调 POST 入口公网可达。现有旧推荐订单不会被转换为自动授权转账，也不会重复发起转账。
- 本次线上隔离后端 Docker 构建通过；前端 `type-check`、ESLint、H5、微信开发包、线上隔离测试包和生产局域网包均已在本次构建中生成。真实“首次授权→新推荐订单→授权转账→推荐人到账”仍需推荐人使用最新隔离测试包实际点选授权并以微信状态 `SUCCESS` 验收，代码/容器健康不等同于真实到账。

## 2026-09-06 旧订单回调恢复与商家转账待用户确认（线上隔离开发环境）

- 已按用户确认恢复最近一笔推荐注册订单 `GBR2096177353089282049` 的标准虚拟支付回调：订单已变为 `PAID`，注册入账已完成，未重复扣款或重复注册。
- 平台第一次向推荐人发起商家转账时，微信返回 `INVALID_REQUEST`，原因为“此IP地址不允许调用接口”；订单当前为 `settlement_status=PENDING`，已保存原外部单号 `GBR2096177353089282049T`，微信侧尚未创建转账单。
- 服务器出口 IP 为 `62.234.44.238`。已补充 `NOT_FOUND` 查单补偿：转账创建因白名单失败后，查单发现原单号不存在时会使用同一外部单号重新发起，保持幂等，隔离服务已重建并 healthy，124 项 Maven 测试通过。
- 用户已将 `62.234.44.238` 加入微信支付商户平台 API 调用 IP 白名单；之后自动重试已能通过 IP 校验并到达商户转账接口。
- 用户已为商户运营账户补充 1 元；微信于 09:24:36 接受同一外部单号 `GBR2096177353089282049T`，商户流水显示“商家转账资金锁定”并扣减 1 元余额。该记录表示资金已锁定，不代表推荐人已经完成收款。
- 后续查单确认微信状态为 `WAIT_USER_CONFIRM`，同一转账单已保存微信返回的收款确认参数；订单仍为 `PAID`、结算仍为 `PENDING`，没有创建第二笔转账，`settled_at` 仍为空。
- 深度排查发现 SDK 响应解析误读了响应对象，导致首次创建成功后丢失 `package_info`；已修复 `WeChatPayClient` 从原始 HTTP 响应读取 JSON，并增加同外部单号幂等恢复逻辑。隔离服务已重建并 healthy，Maven 全量构建通过。
- 微信不会主动向推荐人发送“待收款”通知；推荐人必须用本人微信登录当前小程序，在“俱乐部/金豆会员”推荐奖励卡片点击“确认收款”，前端调用 `wx.requestMerchantTransfer` 后再查单。只有微信状态变为 `SUCCESS` 才记录推荐人已到账。

## 2026-09-06 虚拟支付标准消息推送结构修复（线上隔离开发环境）

- 深度排查确认微信并非没有发送回调：隔离 Nginx 在 `18:03` 测试后持续收到 `POST /api/payments/wechat/virtual/notify?signature=...&timestamp=...&nonce=...`，但 Java 日志显示 `xpay_goods_deliver_notify` 回调的 `hasPayload=false`、`hasPayEventSig=false`，接口因此返回 HTTP 400。订单未入账，平台未发起推荐人转账。
- 根因是标准小程序“消息推送”JSON 结构直接把 `Event`、`OpenId`、`OutTradeNo`、`Env`、`GoodsInfo`、`WeChatPayInfo` 放在根节点；原代码只支持另一种 `payload/payEventSig` 包装结构。现已兼容两种结构：标准结构必须通过 Token + `signature/timestamp/nonce` 完整验签，新结构仍必须通过 `payEventSig` HMAC 验签，商品、金额、环境、付款人绑定和幂等校验均保留。
- 修复已在隔离服务器 Java 21 Docker 构建并部署，Maven 全量 123 项测试通过（含标准 JSON 消息推送路由回归测试），`rayk-remote-dev-rayk-server-1` 已 healthy。部署后未再出现 400 回调；旧订单仍为 `PENDING`，没有人工置已支付或补发转账。
- 部署后使用不落库的标准根节点 JSON 探针完成公网回归：Token + `signature/timestamp/nonce` 验签通过，处理已推进到 `order_lookup`，对不存在的探针订单按预期返回业务 400；没有改动订单、没有扣款、没有创建转账。公网健康检查返回 HTTP 200。
- 你截图中的小程序后台“消息推送”配置本身是标准虚拟支付回调通道；微信官方接口文档示例也使用 `xpay_goods_deliver_notify` 的根节点字段，并要求成功响应 `returnCode=0`、`data=ok`。下一步用新订单测试，确认回调返回 200 后，订单才会进入 `PAID` 并创建推荐人商家转账单。

## 2026-09-05 推荐注册支付后平台转账闭环修复（线上隔离开发环境）

- 已修复推荐注册虚拟支付回调到推荐人商家转账之间的闭环：兼容实际到达的 `xpay_goods_deliver_notify` 回调字段组合，允许回调体缺少可选 `transactionId`，保留 `payEventSig`、商品、金额、环境、付款 OpenID、AppID 绑定和订单幂等校验；公共回调按无租户方式处理后再使用订单所属租户完成入账。
- 推荐注册支付成功后，平台使用微信商家转账接口 `/v3/fund-app/mch-transfer/transfer-bills` 向推荐人当前小程序 AppID 下的有效绑定 OpenID 发起 1 元转账；场景 `1005` 使用微信要求的固定报备字段“岗位类型”和“报酬说明”。每笔订单固定使用 `GBR...T` 作为外部转账单号，异常重试和补偿查单不会生成新单号，避免重复打款。
- 新增 V59 转账对账字段，保存微信转账状态、`package_info`、最近查单时间和下次重试时间；新增每 10 秒扫描的补偿任务，并为每笔补偿建立独立事务。只有微信状态 `SUCCESS` 才记录 `SETTLED`；`WAIT_USER_CONFIRM` 不视为到账，推荐人需在小程序“俱乐部/金豆会员”卡片点击“确认收款”，前端调用 `wx.requestMerchantTransfer` 后再由后端查单确认最终状态。转账权限或收款绑定短暂不可用时保留 `PENDING` 并自动重试，不再把付款成功的推荐奖励直接标成不可恢复的失败。
- 已在隔离服务器使用 Java 21 Docker 构建，Maven 全量 122 项测试通过（失败 0、错误 0）；远程容器已重建并 healthy，Flyway 已验证 V59，`https://xingxuyuan.com/test-api/health` 返回 HTTP 200。前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新生成。
- 本次仅部署线上隔离开发环境，未修改生产容器、生产数据库或生产静态资源；原有未支付 `PENDING` 推荐订单未被改为已支付，也未被强行发起转账。尚未用新的真实微信订单完成“支付回调→微信商家转账→推荐人确认/到账”验收，因此不能把代码和容器验证等同于真实到账。
- 真机验收请导入 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`：推荐人先用本人微信登录一次建立有效收款绑定，被推荐人用新订单完成 1 元虚拟支付；随后推荐人重新进入俱乐部，若出现“确认收款”则完成微信确认并刷新，直到状态显示“奖励已到账”。
- 18:03 的后续真机小额测试再次确认：新推荐注册订单仍为 `PENDING`，没有支付流水、回调时间、商家转账外部单号或微信转账状态；隔离应用及两层 Nginx 在该订单时段也未记录虚拟支付通知请求。因此平台尚未取得可验真的“支付成功”事实，按安全规则没有、也不应对推荐人发起转账。隔离环境的虚拟回调 URL、推送 Token、生产环境标识、登录 AppID 一致性、推荐转账开关、场景 `1005`、商户号、证书和 APIv3 Key 均已核对为有效；回调入口可从公网到达（无验签 GET 返回预期的 403）。剩余阻断点在微信虚拟支付侧：必须在虚拟支付对应的发货/支付结果推送配置中确认当前在线 `test_member` 商品实际启用了向该 URL 的推送，而不是仅保存小程序通用消息推送配置。旧 `PENDING` 订单不做人工置已支付或补打款。

## 2026-09-05 深度审计：虚拟支付回调仍在事件校验阶段失败（线上隔离开发环境）

- 16:51 的真实微信回调已到达隔离服务 3 次，事件为 `xpay_goods_deliver_notify`，但服务解析到的 `eventType` 为空，服务在事件校验阶段返回 HTTP 400；因此没有进入订单查询、注册入账或推荐人商家转账。远程数据库中的最近推荐注册订单仍为 `PENDING`，没有支付交易号和转账单号。
- 进一步核对发现当前商家转账客户端使用的是需要收款用户确认的 `/v3/fund-app/mch-transfer/transfer-bills` 链路，但服务端忽略了响应中的 `package_info`，前端也没有 `wx.requestMerchantTransfer`、确认页面或对应后端接口；即使后续回调放行，也不能据此承诺“平台转账后推荐人自动到账”。
- 当前隔离环境未配置商家转账 `notify_url`，代码中也没有定时对 `PENDING` 转账进行补偿查询的任务；立即查询失败或长期等待可能一直停留在 `PENDING`。`CANCELLED` 等终态也需要单独纳入状态机处理。
- 发现一个事件校验修复后的潜在阻断：虚拟支付公共回调在查询 `gold_member_order` 前没有像普通 JSAPI 回调那样显式执行无租户查询；该入口允许匿名访问，事件校验通过后可能因缺少 `TenantContext` 再次失败。
- 腾讯当前虚拟支付文档中 `transactionId` 标为可选，而现有处理器仍要求能解析出交易号；实际回调体需要在不记录敏感信息的前提下取样确认后再做兼容，不能直接删除验签、商品、金额、环境、OpenID 或订单幂等校验。
- 还存在需补强的可观测性和安全项：转账接口异常目前被压缩为通用失败，缺少微信错误码/请求标识的安全记录；会员虚拟支付分支尚未复用 `payEventSig` 校验；现有单测未覆盖 `WAIT_USER_CONFIRM`、`package_info`、`CANCELLED`、匿名回调租户上下文和真实回调字段组合。
- 本次审计只读检查了线上隔离开发环境，未修改生产容器、生产数据库、生产静态资源或测试订单。

## 2026-09-05 虚拟支付回调兼容修复（线上隔离开发环境）

- 最新一次 1 元推荐注册测试并非“已付款但转账失败”：隔离 Nginx 记录到微信向 `/api/payments/wechat/virtual/notify` 发起 POST，但服务返回 HTTP 400；Java 日志定位到回调的事件类型校验阶段，尚未进入订单查询、注册完成或推荐人商家转账。因此订单仍为 `PENDING`，没有向推荐人发起转账。
- 根因是原处理器只接受新版 `event=xpay_goods_deliver_notify` + `eventType=TRANSACTION.SUCCESS`，而微信虚拟支付文档还存在旧版小游戏商品发货回调组合 `Event=minigame_game_pay_goods_deliver_notify` + `EventType=event`；原代码没有兼容该格式。
- `GoldBeanPaymentService` 现在同时兼容上述两种事件组合，支持 `event_type` 别名，并在旧格式根节点没有订单号时从 `Payload` 的 `OutTradeNo`/`outTradeNo`/`out_trade_no` 回退读取；`payEventSig` HMAC、商品、金额、环境、付款 OpenID、交易号和当前 AppID 绑定校验仍然强制执行。
- 兼容版本已在隔离服务器 Java 21 Docker 构建成功，`rayk-remote-dev-rayk-server-1` 已重建并 healthy；部署后尚未收到新的微信回调，数据库中的两个最近推荐订单仍为 `PENDING`。需要使用最新远程隔离测试包创建新的虚拟支付订单，或等待微信对原订单重试，再观察是否进入商家转账阶段。
- 本次只修改线上隔离开发环境，未修改生产容器、生产数据库或正式 H5；发布构建跳过了全量测试，原因仍是既有 `VoiceReminderTextFactoryTest` 的睡眠提醒文案断言失败，Java 编译本身已通过。

## 2026-09-05 推荐注册切换为虚拟支付并保留推荐人结算（线上隔离开发环境）

- 推荐码注册订单 `GBR...` 已改为使用独立的微信虚拟商品 `GOLD_BEAN_REFERRAL_REGISTRATION_PRODUCT_ID`；线上隔离开发环境当前配置的测试商品 ID 为 `test_member`，价格保持 1 元（`100` 分）。平台注册码注册仍使用 `normal_member_998`，价格为 998 元（`99800` 分）。
- 小程序推荐注册点击后统一调用 `wx.requestVirtualPayment`，不再为推荐注册创建普通 JSAPI 收款参数。服务端创建支付参数时从订单读取商品和金额，虚拟支付回调会校验订单号、商品、数量、金额、环境、AppID、商户号、付款 OpenID 和交易号，校验通过后才开通被推荐用户的会员资格。
- 推荐注册虚拟支付成功后，服务端按现有页面业务语义向“提供推荐码的推荐人”结算：使用微信当前商家转账接口 `/v3/fund-app/mch-transfer/transfer-bills` 发起转账，并用持久化的外部单号 `GBR...T` 查询状态；只有微信状态为 `SUCCESS` 才标记 `SETTLED`，失败标记 `FAILED`，查询暂时失败保持 `PENDING`，不会用新单号自动重试造成重复打款。
- 转账场景报备字段由 `GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_SCENE_ID`、`...USER_RECV_PERCEPTION`、`...JOB_TYPE`、`...REWARD_DESCRIPTION` 和可选通知地址配置。创建推荐订单前仍要求推荐人已注册、推荐关系有效、当前 AppID 下存在有效微信收款绑定，以及平台商家转账配置完整；否则不生成虚拟支付参数。
- 已核对用户提供的 `test_member` 编辑弹窗：当前页面只有道具资料、价格、关联关系、图片和备注，没有单独的“发货推送”字段；不要继续在该弹窗里寻找或修改发货开关。当前以虚拟支付“基本配置/消息推送”中的全局回调配置为准，道具页面截图只能证明该道具已发布。
- 新增商家转账请求/查询单元测试后，Java 21 Docker 全量 Maven 测试为 119 项，失败 0、错误 0；隔离容器 `rayk-remote-dev-rayk-server-1` 已重建并 healthy，`https://xingxuyuan.com/test-api/health` 返回 HTTP 200。已生成 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`，该包只连接线上隔离 API。
- 生产容器、生产数据库和正式 H5 未修改。真实虚拟支付、微信回调和向真实推荐人转账仍需在微信开发者工具/真机使用已审核的虚拟商品、商家转账权限和真实收款绑定完成验收；代码通过不等于微信侧已批准该业务场景。
- 2026-09-05 真机小额验收发现：订单 `GBR...` 在隔离库仍为 `PENDING`，没有虚拟支付交易号、支付回调时间或商家转账单号；服务器只看到一次旧包请求 `/referral-wechat-pay`（500），以及一次新包 `/wechat-pay`（200），两次均未收到 `xpay_goods_deliver_notify`。因此未向推荐人发起转账，避免未验证交易造成误打款。当前应在微信小程序后台“商业化-虚拟支付-基本配置/消息推送”核对全局回调 URL、Token、明文/JSON 配置；用户截图所示道具编辑页没有单独发货字段。使用 `env=0` 的真实支付时要确保回调 URL 指向创建该订单的环境，不能只配置隔离测试 URL。已重新生成 `mp-weixin-dev-remote` 和 `mp-weixin-dev-remote-test`，两个包均不再包含旧支付接口。
- 本次沿用现有文案和数据模型的收款方向：新用户（被推荐人）支付 1 元虚拟商品，平台向推荐码对应的推荐人转账。如果“推荐人支付、被推荐人收款”是你的真实新规则，需要先确认后再调整收款人字段和转账对象，避免实际打款方向错误。
- 已为虚拟支付发货推送入口增加微信消息推送 URL 的 GET 验签处理，并将推送 Token 改为隔离环境密钥配置；POST 回调改为仅在携带标准消息推送查询参数时验签，直接回调 URL 的虚拟支付通知由业务体内的 `payEventSig` 验证，避免因缺少 GET 握手参数被误拒绝。隔离测试服务已重建，健康检查和 GET 握手均返回 HTTP 200，不带查询参数的无效 POST 返回业务 HTTP 400 而非 403；本次控制器测试 3 项通过。全量 122 项仍仅有既有的睡眠提醒文案断言失败，因此部署镜像采用跳过测试构建。微信后台表单使用隔离测试回调 URL、已配置 Token、“明文模式”和“JSON”；Token 不写入源码、文档或日志。

## 2026-09-05 平台注册费用调整为998元（线上隔离开发环境）

- 已定位平台注册虚拟支付失败的直接原因：平台商品 `normal_member_998` 按 998 元定价，但原注册下单共用推荐注册费配置 `100` 分，发送给微信虚拟支付的 `goodsPrice` 与商品后台价格不一致，因此返回 `GOODS_PRICE_INVALID`。
- 注册费用已拆分：平台注册码注册使用 `99,800` 分（998 元），推荐码注册继续使用 `100` 分（1 元）；订单金额、虚拟支付 `goodsPrice`、服务端完成回调和页面按钮/说明均按收款对象分别取值。
- 新增配置 `GOLD_BEAN_PLATFORM_REGISTRATION_FEE_CENT`，默认 `99800`；原 `GOLD_BEAN_REGISTRATION_FEE_CENT` 保留为推荐注册费配置并默认 `100`。生产配置和生产容器未修改。
- 已重建线上隔离开发服务 `rayk-remote-dev`，容器 healthy，实际环境变量核对为平台注册 `99800` 分、推荐注册 `100` 分，`https://xingxuyuan.com/test-api/health` 返回 200；金豆相关 Java 定向测试 31 项全部通过。
- 已对截图对应的旧失败订单 `GBR2096068313928085505` 做精确隔离清理：订单由 `PENDING` 关闭，授权码 `2096068230591459330` 恢复为可用；操作前查询备份位于 `/opt/zhiyu-health/backups/platform-registration-fee-test-order-before-20260905-1119/rows.tsv`。生产容器、生产数据库和生产静态资源未修改。
- 已重新生成 H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包；线上隔离验收优先使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。真实微信支付弹窗与回调仍需用户在微信端用新的 998 元平台注册订单实际验收。

## 2026-09-05 小程序支付能力限制的用户侧处理

- 已确认线上隔离环境的推荐注册 JSAPI 预下单返回微信支付 HTTP 200，前端实际调用 `requestPayment` 时由微信返回“支付能力已被限制”；前端包 AppID 与服务端 `WECHAT_PAY_APP_ID` 一致，不能通过改签名或切换按钮事件绕过 AppID 级限制。
- 用户完成推荐人绑定后再次核对：推荐人和付款人均有当前 AppID 的 ACTIVE 微信绑定；隔离库中最近 4 笔 1 元推荐注册订单均为 `PENDING`，没有 `payment_channel`、微信交易号、支付成功时间或支付回调，说明没有发生扣款，且问题已排除“推荐人收款绑定缺失”。
- 服务器在对应 4 次请求中均调用 `/v3/pay/transactions/jsapi` 并收到 HTTP 200；因此“商家转账场景 1005”属于支付成功后的出账步骤，不能解决当前买家侧的 JSAPI 收款权限问题。当前最可能的剩余原因是小程序经营场景/JSAPI 权限状态受限，或公众平台按交易/分销业务规则对 AppID 进行了处置；后者只能以公众平台通知中心的具体通知为准。
- 页面现在将该错误明确展示为“微信已限制本小程序支付能力，请管理员登录微信公众平台‘通知中心’按提示处理后再支付；当前不会扣款。”，并在确定为该错误时自动关闭未支付订单，避免重复点击堆积待支付订单。
- 微信官方排查路径是公众平台通知中心；交易类小程序还需核对服务类目及订单管理/发货管理要求。限制解除后重新进入页面即可恢复重试。当前仍未做真实扣款，生产环境未修改。

## 2026-09-05 推荐人支付无反馈修复（线上隔离开发环境）

- 已核对推荐注册支付链路：隔离环境支付开关和推荐人商家转账场景号已配置，但当前可用测试推荐人 `166****1137` 的有效微信收款绑定数为 0；后端原本会在创建订单前安全拒绝，前端只显示短暂 Toast，容易被误认为点击无效。
- 新增错误码 `60741`，明确提示“推荐人尚未完成微信收款绑定，请让推荐人先用本人微信手机号登录一次”；页面在注册卡片中增加持续显示的错误区域，支付处理中显示按钮加载状态，失败原因不会立即消失。
- 推荐人必须先用本人微信手机号登录一次，建立当前小程序 AppID 下的有效微信绑定；之后被推荐用户重新点击“向推荐人支付”，才会创建普通微信 JSAPI 支付并在支付回调后结算推荐人。没有真实 OpenID 时不会伪造收款绑定或发起错误转账。
- 前端已通过 `type-check`、ESLint、H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包构建；Java 21 远程镜像编译成功并重建 `rayk-remote-dev-rayk-server-1`，容器 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。生产容器、生产数据库和生产静态资源未修改。
- 本次新增后端错误码和推荐人绑定校验已同步到隔离服务；旧 Java 源码备份位于 `/opt/zhiyu-health/backups/referrer-payment-feedback-20260905`。真实微信支付弹窗和支付回调仍需在推荐人完成绑定后由用户使用小额订单验收，不能仅凭接口编译视为真实扣款成功。

## 2026-09-05 微信手机号首次登录异常修复（线上隔离开发环境）

- 已定位 `150****3671` 登录显示“系统内部错误”的根因：首次按手机号创建客户时，金豆账户初始化发生在 JWT 会话建立之前，MyBatis 租户拦截器错误地尝试从 `CurrentUser` 取租户，导致整个创建事务回滚。
- `GoldBeanApplicationService.initializeForCustomer` 现在会在登录前使用已确定的客户租户显式设置 `TenantContext`，完成金豆账户查询/创建后恢复原上下文，避免线程池复用时泄漏租户；微信手机号登录仍按 `CUSTOMER` 普通客户路径创建，不改变医生和平台管理员登录规则。
- 隔离数据库核对显示该手机号当前没有已提交的用户记录，符合上次事务已回滚的现象，因此没有做无依据的账号覆盖；下一次真实微信手机号授权成功后会自动创建 ACTIVE 的 `CUSTOMER` 账号、客户工作台、患者资料和金豆账户。
- 新增 `GoldBeanApplicationServiceTest` 覆盖“未建立认证会话时使用显式租户并清理上下文”，该测试通过。远程 Java 21 镜像已编译成功并重建 `rayk-remote-dev-rayk-server-1`，`https://xingxuyuan.com/test-api/health` 返回 200，启动日志无新的异常。
- 全量 Maven 测试共执行 117 项，其中本次新增测试及其他相关测试通过；既有 `VoiceReminderTextFactoryTest` 仍有 1 项文案断言失败（要求包含“睡觉/睡眠”等词，当前实现文案为“安心睡个好觉”），发布镜像按既有隔离流程使用跳过测试的构建参数。生产容器、生产数据库和生产静态资源未修改。
- 远程旧版 Java 源码已备份至 `/opt/zhiyu-health/backups/login-fix-20260905/GoldBeanApplicationService.java.before`。尚未使用真实微信一次性授权码完成真机登录验收，需用户重新点击手机号授权后再核对最终登录结果。

## 2026-09-05 推荐注册 1 元与一级代理测试数据已部署线上隔离开发环境

- 线上隔离开发环境 `rayk-remote-dev` 已将远端配置 `GOLD_BEAN_REGISTRATION_FEE_CENT` 设置为 `100`（1 元）；推荐注册商家转账开关仍保持已配置的开发验收状态，场景号为 `1005`。
- 已将手机号 `166****1137` 对应的开发客户账号设置为平台归属的一级代理：注册状态 `PAID`、费用归属 `PLATFORM`、注册费 1 元，并生成测试推荐码 `SYTESTREF166960`；保留普通会员等级，不授予钻石或区域权限。
- 该账号原有过期的待支付注册测试订单已标记为 `CLOSED`；此前为准备测试而写入的 `DEVELOPMENT_RECORD` 也已改为 `CLOSED` 并释放 `PLATFORM_ROOT` 槽位，避免占用平台管理员继续生成平台注册测试授权码。没有发起真实支付、商家转账或扣款。目标账号和远端配置的回滚备份位于 `/opt/zhiyu-health/backups/gold-referrer-registration-test-before-20260905-085633`。
- 重建 `rayk-server` 后已核对容器 healthy，`https://xingxuyuan.com/test-api/health` 返回 200；生产健康检查仍返回 200，生产容器、生产数据库和生产 H5 未修改。
- 推荐注册验收时使用该账号的推荐码创建被推荐人注册订单，订单金额应为 1 元；真实支付回调、平台商家转账到推荐人零钱及最终到账仍需用微信端实际支付链路验收，不能仅凭开发记录视为到账成功。
- 释放槽位后，已用隔离开发平台管理员实测“生成一次性授权码”和撤销接口，均返回成功；本次探测生成的授权码已撤销，不影响后续正式测试。

## 2026-09-05 健康树洞页面文案与布局优化

- 树洞页面顶部权益说明已改为“普通客户可免费体验7天，健康会员可无限使用，每七天树洞会给出一次总结反馈。”；底部输入框提示改为更短的单行文案“今天可记录身体、心情和压力”。
- 按需求移除树洞页面中独立的“7天树洞反馈”展示卡；七天反馈接口、周期生成和数据落库能力保留，后续仍可从其他入口承载反馈内容。
- 已通过 UniApp `type-check`、ESLint、H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包构建；本次仅更新前端源码和本地验收产物，未修改生产容器、生产 H5 或正式微信版本。线上隔离验收使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。

## 2026-09-05 金豆集市独立页面部署线上隔离开发环境

- 已将独立金豆集市对应的 Java 分页接口部署到服务器 `/opt/zhiyu-health` 的 `rayk-remote-dev` 隔离测试 Compose；俱乐部概览只保留入口，前端验收包使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。
- 部署前已备份远程三处 Java 源码至 `/opt/zhiyu-health/backups/gold-market-page-before-20260905-1788568652501/java-sources.tgz`；未执行 `down -v`，未删除或重建 MySQL、Redis、MinIO 数据卷，生产 Compose、生产数据库和生产 H5 未修改。
- 远程 Java 21 Docker 构建成功，Maven 全量测试 116 项全部通过；新 `rayk-server` 容器已重建并 healthy，Flyway 启动正常，当前数据库迁移保持 V58。
- 验收结果：`https://xingxuyuan.com/test-api/health` 返回 200；未登录访问 `/api/client/gold-bean/trade/market` 返回 401；使用隔离开发客户登录后，市场和我的挂单接口均返回 `PageResponse`，页大小 12，随后已注销测试会话。

## 2026-09-04 健康树洞首版接入线上隔离开发环境

- 首页客户区新增“健康树洞”入口，进入现有健康助手页面的独立模式；树洞与普通健康助手按 `conversation_type` 隔离，用户记录保存在原有助手消息表，不能互相混入历史或通过前端参数越权读取。
- 树洞文字问答复用已配置的 `qwen3.8-flash`，新增树洞陪伴提示词、七天反馈提示词和同一套急症安全边界；Java 负责客户身份、本人数据范围、消息落库和会员额度，Python 负责 Qwen 请求与结构化回复。
- 新增 Flyway `V57__health_tree_hole.sql`：为助手会话增加会话类型，并新增 `health_tree_hole_feedback` 七天反馈表；新增 `V58__health_tree_hole_entitlement.sql`，注册 `AI_HEALTH_TREE_HOLE / TRIAL_7D` 独立权益并同时绑定免费客户与年度健康会员。周期从本周期第一条树洞记录开始计算，满 7 个自然日后可生成反馈，同一用户同一周期通过唯一约束和查询幂等只生成一份。
- 新增客户接口：`GET/POST /api/client/health-tree-hole/feedback`；会话接口继续使用 `/api/client/medical-assistant/*`，通过 `mode=TREE_HOLE` 进入树洞模式。所有接口仍由 Java 的客户本人权限校验保护，未登录访问树洞反馈接口返回 401。
- 页面进入树洞时刷新反馈状态；到期反馈由后台定时任务自动尝试生成，反馈卡展示周期、记录天数、记录次数、阶段总结和下一步小行动，任务未生成时保留手动整理/重试按钮。当前没有微信订阅消息或主动推送。
- 树洞已改用独立的 `AI_HEALTH_TREE_HOLE` 权益：普通客户从首次有效记录起免费体验 7 天，年度健康会员期内不限使用；健康助手原有 `AI_MEDICAL_ASSISTANT` 3 次免费额度不受影响。首个七天周期的阶段反馈保留一次到期后的生成机会，之后普通客户需开通年度健康会员。
- 已在服务器 `rayk-remote-dev` 隔离开发 Compose 中备份相关文件至 `/opt/zhiyu-health/.codex-backups/health-tree-hole-before-20260904` 和 `/opt/zhiyu-health/.codex-backups/health-tree-hole-entitlement-before-20260904`，同步源码后用 Java 21 Docker 镜像构建并重启 `rayk-server`、`rayk-ai`；Java 编译构建成功，Flyway `58 / success=1`，数据库中的 `AI_HEALTH_TREE_HOLE` 已核对为 `TRIAL_7D`，隔离 Java、AI、Nginx、MySQL、Redis、MinIO 均 healthy。生产容器、生产数据库、生产 H5 和正式微信版本未修改。
- 本地验证：Python `compileall` 与健康助手测试 11 项通过；Java 21 隔离容器定向执行 `MembershipEntitlementServiceTest` 3 项全部通过；UniApp `type-check`、ESLint、H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包均重新构建成功。产物包括 `E:\health\rayk-miniapp\dist\build\h5`、`E:\health\rayk-miniapp\dist\release\mp-weixin-dev`、`E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test` 和 `E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan`。
- 尚未使用真实客户账号发起 Qwen 真实问答、七天跨日数据验收或微信真机体验；这些属于隔离环境后续验收，不应表述为生产已验收。

## 2026-09-04 金豆集市拆分为独立分页页面

- 俱乐部概览不再加载或展示大量挂单，只保留“金豆集市”入口卡片；普通会员和传奇人物均可从自己的俱乐部进入 `pages-customer/gold-bean-market/index`。
- 新增 `rayk-miniapp/src/components/GoldBeanMarketPanel.vue`，独立承载市场挂单、我的挂单、发布数量、出售来源、买入、下架和分页；每页 12 条，页面支持下拉刷新，避免挂单数量增长后撑长会员概览。
- `GET /api/client/gold-bean/trade/market` 与 `GET /api/client/gold-bean/trade/listings/mine` 已改为返回 `PageResponse`，服务端按 `page/size` 查询并限制单页最多 50 条；传奇人物数字银行挂单的可见性和买入鉴权仍由 Java 服务端执行，前端入口不作为权限依据。
- 已通过小程序 `type-check`、`lint`、`build:h5`、`build:mp-weixin:dev`、`build:mp-weixin:dev:remote`、`build:mp-weixin:dev:remote-test` 和 `build:mp-weixin`；H5、标准开发包、两个远程开发包和生产局域网包均已刷新。本机 Docker Desktop 和 Maven 未用于本次后端编译，后端已在 2026-09-05 远程 Java 21 Docker 构建链完成编译与全量测试。

## 2026-09-04 管理员俱乐部入口改为金豆会员运营

- 平台管理员进入底部第三个入口时，不再显示“俱乐部”空白占位卡，改为直接展示完整的“金豆会员运营”控制台；运营页已抽为可复用组件，工作台原有入口保持不变。
- 管理员底部入口动态显示为“金豆运营”，普通会员仍显示“俱乐部”，游客在金豆功能关闭时仍显示“消息”；普通会员和游客的原有俱乐部逻辑未改变。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；微信包仅刷新本地验收产物，未上传版本。

## 2026-09-04 区域奖励改为系统自动返利

- 平台管理员端已移除“区域盈利金豆结算”卡片、区域盈利手工录入流程及对应 `POST /api/v1/platform/gold-bean/region-profits` 接口；区域和区域返利记录接口仅保留只读审计用途。
- 区域内代理产生注册奖励、每日奖励或等级奖励等奖励金豆时，服务端自动识别最近的有效钻石区域并发放区域返利：开辟人获得 100%；开辟人与直接上级为 A→B 时，A 额外获得 20%；为 A→B→C 时，B 额外获得 5%、A 额外获得 15%。奖励复用数字银行/可交易双账本，按整枚金豆向下取整并使用幂等键防重复发放。
- 本次规则由 Java 后端统一鉴权和分配，平台不录入、不手工结算区域盈利；已补充自动分配单元测试。前端类型检查、Lint、H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包均已通过；线上隔离 `rayk-remote-dev` 已使用 Java 21 镜像重建并 healthy，Flyway 保持 V56，`https://xingxuyuan.com/test-api/health` 返回 200。未上传微信版本，未修改生产容器、生产数据库、生产 H5 或生产静态资源。

## 2026-09-04 移除管理员端运营提示卡

- 金豆会员运营页已移除“开发版运营台”提示卡，仅保留下方的平台首会员授权、传奇人物资格和数据管理功能；区域返利由服务端自动记录，管理员不可手工结算。
- 已重新通过前端类型检查、Lint、H5、微信开发包、线上开发包、线上隔离测试包和生产局域网包构建；未上传微信版本、未修改线上容器和生产环境。

## 2026-09-04 金豆会员按钮与流水记录折叠优化

- 金豆集市“发布”按钮和“我的推荐码”复制按钮已调整为更紧凑的尺寸、字号与圆角（分别为 104×76rpx、最小宽 136rpx×76rpx），降低按钮在卡片区域的突兀感，并用局部高优先级样式覆盖全局按钮尺寸规则。
- 最近金豆记录默认折叠，右侧操作已由“刷新”改为“展开/收起”按钮，点击按钮或折叠提示可查看完整流水；收起后仅保留记录数量提示，不改变流水加载逻辑。
- 已通过前端类型检查、Lint、H5、微信开发包、线上开发包、线上隔离测试包和生产局域网包构建；本次仅刷新本地验收产物，未上传微信版本、未修改线上容器和生产环境。

## 2026-09-04 机器人权益兑换说明文案调整

- 金豆会员页机器人权益卡片及兑换确认弹窗已统一调整为：每位用户仅可使用数字银行金豆兑换一次，消耗10000金豆，兑换后扫码加入微信群，由专人对接机器人的权益使用。
- 本次仅调整前端展示文案，兑换次数、数字银行扣豆、二维码入群和服务端鉴权逻辑均未改变。

## 2026-09-04 数字银行金豆兑换机器人权益已部署线上隔离开发环境

- 新增 Flyway `V55__gold_robot_entitlement_redemption.sql`、`V56__gold_robot_single_redemption.sql` 和 `gold_robot_redemption` 兑换记录表。已注册且状态有效的客户仅可兑换一次机器人权益，固定消耗 10000 枚数字银行金豆；每次兑换使用客户端请求号和金豆流水幂等，服务端锁定账户后扣减数字银行余额并保存兑换时的群名称、二维码地址快照。V56 的唯一约束覆盖软删除记录，不能通过重复请求或重建请求号再次兑换。
- 新增客户接口 `GET /api/client/gold-bean/robot/status` 和 `POST /api/client/gold-bean/robot/redeem`。兑换成功后小程序立即弹出企业微信客户群二维码，由专人对接；重复提交同一请求号不会二次扣豆。二维码未配置、余额不足、未完成注册或账户无效时，接口在扣豆前拒绝。
- 群入口采用企业微信客户群活码配置，而不是把二维码文件写死在前端。通过 `GOLD_BEAN_ROBOT_GROUP_NAME`、`GOLD_BEAN_ROBOT_GROUP_QR_IMAGE_URL` 配置；活码可以在运营侧轮换，历史兑换仍保留当时的地址快照。企业微信群活码不应承诺绝对永久有效，群满、群解散、管理员停用或规则变化仍可能要求更新二维码。
- 当时线上隔离 `rayk-remote-dev` 已重建并 healthy，Flyway 已执行到 V56，`https://xingxuyuan.com/test-api/health` 返回 200，未登录机器人接口返回 401；当时二维码变量为空，兑换入口保持不可用，未执行真实兑换、未扣除任何金豆。后续入口状态调整见本文最新的“机器人权益入口与余额不足提示”记录；生产容器、生产数据库和生产静态资源未修改。
- Java 金豆相关定向测试 39 项全部通过；远程发布构建按既有流程跳过全量测试，未把既有 `VoiceReminderTextFactoryTest` 文案断言问题误报为本次功能通过。前端已通过类型检查、Lint，并重新生成 H5、标准微信开发包、远程开发包、远程隔离测试包和生产局域网包；线上隔离验收优先使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。

## 2026-09-04 数字银行挂单仅限传奇人物购买已部署线上隔离开发环境

- 集市列表按买家资格隔离：普通用户只能看到并购买“可交易金”挂单；数字银行金挂单只对平台预先录入手机号、且已完成注册的传奇人物开放。
- 下单接口再次执行同样的服务端鉴权，普通用户直接提交数字银行挂单 ID 会返回“数字银行金豆挂单仅限传奇人物购买”；传奇人物购买数字银行挂单后，金豆全部进入数字银行。发布者仍可选择发布自己的数字银行余额。
- 新增错误码 `60736`，本次没有新增数据库结构，继续使用 V54 的 `buyer_credit_mode` 记录买家入账规则；历史未填该字段的交易仍按普通会员双账本规则兼容。
- 线上隔离 `rayk-remote-dev` 已重建并 healthy，数据库保持 V54，`https://xingxuyuan.com/test-api/health` 返回 200；金豆相关 Maven 定向测试 25 项全部通过。本次规则更新未再次上传微信开发版；此前误上传的 `2026.09.05` 未提交体验或发布，未修改生产容器、生产数据库或生产静态资源。
- 前端 H5、标准微信开发包、线上开发包、线上隔离测试包和生产局域网包已重新构建；微信包仅保留本地验收产物，不作为线上容器部署方式。

## 2026-09-04 金豆集市买家分账规则与页面精简已部署线上隔离开发环境

- 金豆集市卡片已删除交易说明、绿色规则说明、标题右侧刷新入口，以及“发布数量”“出售来源”两个可视字段标签；数量输入、来源选择、余额/限额校验提示和挂单列表仍保留。最近金豆记录区域的刷新入口仍保留。
- 金豆会员主面板和传奇人物俱乐部均已删除“传奇人物购买资格”提示卡；传奇人物独立俱乐部的数字银行购豆、支付和历史购买记录功能未删改。
- 新增 Flyway `V54__gold_trade_buyer_credit_mode.sql`。普通会员从集市买入后，买入数量按整数分配到两个账本：可交易金豆为向下取整的一半，数字银行为向上取整的一半；传奇人物只能看到并购买数字银行金挂单，成交后买入数量全部进入数字银行。卖家仍从所选来源账本扣减，买家分账规则在下单时落库，避免资格变化影响已创建订单。
- 服务端在集市查询、下单和成交三个环节校验传奇人物限制，不能通过绕过前端购买可交易金挂单；历史交易记录未填入新字段时按普通会员双账本分配兼容。
- 金豆相关 Maven 定向测试 24 项全部通过；远程 Java 21 镜像已重建，线上隔离库 Flyway 已从 V53 执行到 V54，六个服务均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。发布镜像构建使用跳过测试参数，未重新执行全量测试。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；线上隔离验收包为 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。线上隔离容器以 `https://xingxuyuan.com/test-api` 提供 API，前端包仅作为本地验收产物，不是容器部署方式；本次误上传的微信开发版本 `2026.09.05` 未提交体验或发布，不作为线上容器验收依据。未修改生产容器、生产数据库或生产静态资源。

## 2026-09-04 区域盈利金豆按三级推荐链自动分配已部署线上隔离开发环境

- 区域盈利的口径是“区域代理奖励产生的金豆数量”，不是人民币金额；奖励流水产生时由服务端自动识别所属区域并用幂等键记录一次自动返利，管理员不能录入或手工结算。
- 区域开辟人固定获得该笔区域盈利金豆的 100%；上级奖励在此基础上额外追加：A→B 时 A 额外 20%，A→B→C 时 B 额外 5%、A 额外 15%。每笔到账继续复用数字银行/可交易双账本分配，比例不足 1 枚时向下取整，不产生小数金豆。
- 区域开辟仍只允许钻石会员，一个账号最多一个区域；区域层级从实际直推链自动推导，不再信任客户端手填上级区域 ID；超过 A→B→C 的第四层、环路或不匹配的旧上级参数会被后端拒绝。
- 新增 Flyway `V53__gold_region_profit_distribution.sql`，扩展 `gold_region_profit` 并新增 `gold_region_profit_distribution`，保存开辟人 100% 和上级额外奖励的收款对象、比例、枚数及幂等流水；平台管理员接口为 `GET /api/v1/platform/gold-bean/regions`、`GET /api/v1/platform/gold-bean/region-profits`，仅供只读审计。
- 已补充 Java 回归测试覆盖 A→B（100% + 20%）、A→B→C（100% + 5% + 15%）和第四层拒绝；线上 Java 21 容器专项/完整金豆测试共 32 项全部通过。远程镜像已重建，Flyway 已从 V52 执行到 V53，六个隔离服务均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200；未登录平台金豆接口仍应返回 401。
- 管理员端区域结算卡片与客户端区域说明已同步；H5、标准开发包、线上开发包、线上隔离测试包和生产局域网包已重新构建。当前只部署线上隔离开发容器，区域返利由奖励流水自动触发，未发起现金支付，生产容器、生产数据库和生产静态资源未修改。

## 2026-09-04 掉星恢复、活跃保护与双账本交易权已部署线上隔离开发环境

- 已将掉星中直推恢复 1 个等级、历史最高星级封顶、7 天活跃保护、第 8 天按自然日继续掉级、历史最高等级只刷新保护期，以及数字银行/可交易金豆会员间交易权代码部署到 `/opt/zhiyu-health` 的 `rayk-remote-dev` 隔离开发项目。
- 本次部署前回滚备份为 `/opt/zhiyu-health/backups/gold-rank-protection-before-20260903-225201/source-config.tgz`；只备份源码、迁移和 Compose 配置，未修改或删除 MySQL、Redis、MinIO 数据卷，也未同步共享的生产 H5 目录。
- 远程 Java 21 Docker 镜像已成功构建并部署，Flyway 已成功执行 V52，`gold_member_trade` 与 `gold_member_trade_listing` 已存在；隔离 Java、AI、Nginx、MySQL、Redis、MinIO 六个服务均为 healthy。金豆相关 Maven 测试全部通过；全量测试仍有既有 `VoiceReminderTextFactoryTest` 文案断言失败，因此发布构建使用跳过测试参数。
- 验收结果：`https://xingxuyuan.com/test-api/health` 返回 200，未登录金豆摘要和交易接口返回 401；隔离开发客户登录后摘要、交易市场接口均返回业务成功，并已退出临时会话。会员交易真实支付开关仍保持关闭，未创建订单、未扣款。
- 前端 H5、微信开发包、远程开发包、远程隔离测试包和生产局域网包已于 2026-09-04 重新构建，线上隔离验收使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。生产容器、生产数据库、生产 H5 和正式微信包未修改。

## 2026-09-03 传奇人物数字银行购豆与双账本集市

- 新增 Flyway `V52__gold_bean_legendary_and_bucketed_trade.sql`：传奇人物资格使用平台管理员维护的手机号白名单（数据库只存 SHA-256 和掩码），不是新的登录角色；管理员可在“金豆会员运营”中录入、查看和撤销资格。
- 只有白名单手机号匹配到已注册客户时，传奇人物俱乐部才显示购豆入口；购豆订单类型为 `LEGENDARY_BANK_PURCHASE`，单价固定为 1 金豆 = ￥1.00，支付成功后全部进入数字银行。旧普通平台购豆入口不再作为新订单入口，后端仍保留旧订单回调兼容。
- 会员间金豆集市新增出售来源选择：可交易金或数字银行金，买家到账保持同一账本。服务端按两个账本合计余额计算当前交易额度，并叠加已有挂单/待成交量鉴权，不能通过拆分账本绕过 50% 限额；余额、账本和支付回调均由 Java 后端校验。
- 保护期结束后无论是否有直推，交易额度立即重新限制为 50%；等级仍按自然日继续掉级，直推成功后恢复 1 级并刷新 7 天保护期。已补充保护期过期限制测试、传奇手机号匹配测试和数字银行挂单测试。
- 本地已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；五个输出目录均已刷新。线上隔离开发容器已重建，V52 已执行，六个服务 healthy，`https://xingxuyuan.com/test-api/health` 返回 200，未登录传奇接口返回 401。
- 远程整包 Maven 测试中本次金豆相关测试通过；另有既有 `VoiceReminderTextFactoryTest` 文案断言失败，因此部署镜像使用跳过测试的发布构建。线上隔离环境会员/金豆虚拟支付开关已启用，会员间交易商家转账仍关闭；未创建真实订单或扣款，生产容器、生产数据库和生产静态资源未修改。

## 2026-09-03 掉星恢复、活跃保护与双账本交易权

- 推荐人直推注册成功时，先结算已到期的掉等级状态；如果当前等级低于本人历史最高星级，最多恢复 1 个等级，并严格封顶在 `historicalLevel`，不会因为一次推荐跨级恢复。
- 推荐成功后重新进入 7 天活跃保护期，保护期内不掉等级；第 4、5、6 天通过摘要接口和会员页提醒卡连续提醒推荐新人；第 8 天从保护期到期日开始按自然日掉等级。保护期结束后保留过期 `protectionUntil` 作为掉级锚点，后续访问会继续结算每日掉等级，不会因为第一次结算后清空时间字段而永久停止掉级。
- 已达到本人历史最高等级（包括钻石）的继续直推只刷新 7 天保护期，不再超过历史最高等级；直推人数和历史最高等级仍按真实直推阈值维护。
- 掉等级不注销账户，也不清空数字银行或可交易金豆；会员间交易接口已支持两个账本分别挂单和交割，交易限额仍按账户当前额度计算，已注册会员即使掉等级仍保留已有金豆的会员间交易权。
- 铜牌、银牌、金牌、钻石解锁奖励分别为 100、300、600、1000 金豆；已补充四档奖励、保护期第 4-6 天提醒、同日不重复掉级、跨日逐级掉级、普通会员限额、掉级中恢复 1 级和历史最高等级只刷新保护期的 Java 回归测试。线上隔离开发环境已部署并通过 Java 21 Docker 全量测试 95 项，服务 healthy，`https://xingxuyuan.com/test-api/health` 返回 200，未登录接口仍返回 401；真实商家转账开关仍未配置，生产环境未修改。
- 已重新生成并核对 `E:\health\rayk-miniapp\dist\build\h5`、`E:\health\rayk-miniapp\dist\release\mp-weixin-dev`、`E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test` 和 `E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan`；线上隔离 API 验收优先导入 `mp-weixin-dev-remote-test`，生产包未开启金豆入口。

## 2026-09-03 下线推荐奖励已扩展为全链路

- 推荐奖励现按完整下线链路计算：新会员注册成功时，直接推荐人获得 11 金豆；该直接推荐人的所有上级代理，无论相隔多少层，每人各获得 5 金豆。每个注册关系对每个上级最多触发一次奖励，不继续重复发放。
- 下线奖励复用现有双账本和幂等流水机制，5 金豆按数字银行 3、可交易 2 入账；`directReferralCount`、等级和活跃保护仍只按直推更新，不把下线人数计入直推统计。推荐链遍历带环路保护，遇到无效或断开的上级链路停止结算。
- 已更新会员页说明和平台管理员流水筛选，新增 `REFERRAL_DOWNLINE`（下游推荐奖励），保留 `REFERRAL_SECOND_LEVEL` 供历史流水查询；后端单测覆盖 A→B→C→D 链路、直推 11、所有上级各 5 及双账本拆分。Java 21 Docker 构建和 Maven 全量测试 80 项全部通过，`rayk-remote-dev-rayk-server-1` 已重建且 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。
- 已重新生成并核对 `E:\health\rayk-miniapp\dist\build\h5`、`mp-weixin-dev`、`mp-weixin-dev-remote`、`mp-weixin-dev-remote-test` 和 `mp-weixin-prod-lan`；线上隔离验收优先导入 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。生产容器、生产数据库和生产静态资源未修改，未发起真实扣款或金豆奖励数据修正。

## 2026-09-03 推荐注册商家转账场景已配置

- 按平台商户收款后转账给推荐人的方案，线上隔离开发环境已配置 `REMOTE_DEV_GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_ENABLED=true` 和 `REMOTE_DEV_GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_SCENE_ID=1005`；该场景为微信商户侧“佣金报酬”。
- 已备份远程 `.env.remote-dev` 后重建 `rayk-remote-dev-rayk-server-1`，容器内实际环境变量已核对为 `true`、`1005`，服务恢复 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。
- 本次只修改线上隔离开发容器配置，未发起真实支付或转账，生产容器、生产数据库和生产静态资源未修改。真实联调仍需确认商家转账 API 权限、推荐人收款绑定/实名及微信侧风控额度。

## 2026-09-03 注册成功后生成推荐码、按直推关系维护

- 未完成注册的金豆账号不再生成个人推荐码；新增 Flyway `V51__gold_referral_code_after_registration.sql` 将字段改为可空，并清理线上隔离库中未注册账号的旧推荐码。已完成注册但缺少历史推荐码的账号，会在注册成功事务中补生成推荐码；摘要接口对未注册账号也强制返回空值。
- 推荐关系、直推人数和等级仍只按直接推荐人维护；下线奖励规则见文档顶部最新记录。历史流水不做删除或倒账。
- 金豆会员页仅在注册状态为 `PAID` 且存在推荐码时展示“我的推荐码”卡片；新增“复制推荐码”按钮，复制成功/失败均有反馈，未注册状态不会显示空卡片。推荐说明已更新为直推 11 金豆、各级上级对非直推下游注册各奖励 5 金豆，页面不再展示“金豆不可提现”文案。
- 已同步到线上隔离开发容器：Java 21 Docker 构建和新增回归测试全部通过（80 项），`rayk-remote-dev-rayk-server-1` 已重建且 healthy，Flyway V51 已执行成功；`https://xingxuyuan.com/test-api/health` 返回 200。
- 已重新生成并核对 H5、`mp-weixin-dev`、`mp-weixin-dev-remote`、`mp-weixin-dev-remote-test` 和 `mp-weixin-prod-lan`。由于线上隔离 Nginx 与正式 Nginx 共用 H5 挂载目录，本次新 H5 只保留本地构建，服务器 H5 已从部署前备份恢复；线上隔离验收使用指向隔离 API 的 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。
- 本次部署前备份为 `/opt/zhiyu-health/backups/gold-referral-code-v51-before-20260903071023`；生产容器、生产数据库、生产 H5 和正式微信包未修改，未删除 Docker 数据卷。

## 2026-09-03 一级代理与推荐代理注册收款链路已完成

- 注册页面已改为先选择注册身份：一级代理只能填写平台管理员发放的一次性平台注册码；推荐代理只能填写已注册推荐人的推荐码；两种编码在客户端互斥展示，Java 后端也会拒绝同时提交或绕过页面提交。
- 一级代理的 `GBR...` 注册订单继续使用微信虚拟支付并归属平台；推荐代理的 `GBR...` 订单改用普通微信支付 JSAPI 付至平台商户号，支付回调确认成功后由平台商户通过“商家转账到零钱”向提供推荐码的推荐人绑定 OpenID 结算注册费。订单新增 `registration_referrer_id`、`settlement_status`、批次号、明细号、失败原因和结算时间，管理员支付订单页可查看推荐人及结算状态。
- 推荐注册订单创建前会强制检查：推荐人必须是同租户、已注册且状态有效；平台普通微信支付、商家转账场景 ID、推荐人微信收款绑定必须齐全。任一条件缺失时返回配置错误，不创建可误导用户扣款的支付参数；支付后若转账失败，会员开通和订单支付事实保留为已支付，结算标记为 `FAILED` 并提示平台处理，不伪造推荐人已收款。
- 新增 Flyway `V50__gold_referral_registration_settlement.sql`；线上隔离库已从 V49 成功执行到 V50。远程 Java 21 镜像全量编译测试通过，`rayk-remote-dev-rayk-server-1` 已重建且 healthy，`https://xingxuyuan.com/test-api/health` 返回 200，未登录金豆接口仍返回 401。
- 线上隔离环境新增 `REMOTE_DEV_GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_ENABLED`、`REMOTE_DEV_GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_SCENE_ID` 和可选 `REMOTE_DEV_GOLD_BEAN_REGISTRATION_REFERRAL_TRANSFER_NOTIFY_URL` 配置；部署初始状态为关闭/未配置，后续已按“佣金报酬”场景 `1005` 开启并部署。未发起真实推荐注册扣款或转账；真实联调仍必须由商户侧完成普通支付、商家转账权限/场景、收款绑定和退款/异常处理验收。
- 线上隔离 Nginx 的 H5 挂载点已确认并同步为本次构建版本；同步前的开发 H5 备份为 `/opt/zhiyu-health/backups/gold-referral-registration-v50-before-20260903-143547/h5-before.tgz`。开发网关根路径按当前配置不对外提供页面，微信线上开发验收使用远程隔离包。
- 本次部署前备份为 `/opt/zhiyu-health/backups/gold-referral-registration-v50-before-20260903-143547`；生产容器、生产数据库、生产 H5 和正式微信包未修改。
- 已重新生成并核对 `E:\health\rayk-miniapp\dist\build\h5`、`dist\release\mp-weixin-dev`、`dist/release/mp-weixin-dev-remote`、`dist\release\mp-weixin-dev-remote-test` 和 `dist\release\mp-weixin-prod-lan`。线上隔离验收优先导入 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`；生产局域网包仍关闭金豆入口。

## 2026-09-03 钻石会员平台购豆与城市区域权限

- 平台购豆资格为“已完成注册且为第一级代理（由平台提供注册码注册）或当前等级为钻石会员”；因此平台一级代理和推荐人体系中的钻石会员都可以购买。
- 创建订单和支付回调交付前都会刷新活跃保护期/等级状态；仅通过钻石等级获得的购豆资格在掉出钻石后取消，平台一级代理资格不受等级变化影响。
- 城市区域仍仅允许钻石会员申请，数据库唯一约束保证一个账号最多一个区域；等级掉出钻石后只关闭购豆和新增区域权限，不删除 `gold_region`，已开辟城市继续保留和展示。
- 已补充 Java 回归测试覆盖平台一级代理、推荐人钻石会员、普通推荐会员、钻石掉级后禁止购豆及已开辟区域保留；本次未执行真实支付。
- 已部署到线上隔离开发环境：远程 Java 21 Docker 镜像构建（含 Maven 全量测试）成功，重建 `rayk-remote-dev-rayk-server-1` 后六个隔离服务均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 HTTP 200；Flyway 校验 49 个迁移并确认远程开发库无需新增迁移。
- 已刷新线上开发 API 对应的微信验收包 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`；生产容器、生产数据库和生产 H5 未修改，未删除任何 Docker 数据卷。
- 本次部署前 Java 源码备份位于服务器 `/opt/zhiyu-health/backups/platform-purchase-eligibility-before-20260903-112315`。

## 2026-09-03 会员间可交易金豆真实人民币结算已部署线上隔离环境

- 新增 Flyway `V49__gold_bean_member_trade.sql`、挂单/成交实体和 `/api/client/gold-bean/trade/*` 接口：卖家只提交可交易金豆数量，单价固定为 1 金豆 = ￥1.00（后端按 100 分保存），买家创建 `GBT...` 订单并使用普通微信支付，挂单数量先锁定，取消/超时会释放。
- 支付回调确认买家付款后，Java 使用微信支付 API v3 的“商家转账到零钱”向卖家绑定 OpenID 发起结算；查询到转账明细 `SUCCESS` 后才扣卖家可交易金豆、给买家交割并写入双方账本。`TRANSFER_PENDING`、`REFUND_REQUIRED` 等异常状态不会伪造金豆到账，需平台后续补齐退款/运营处理。
- 开关为 `GOLD_BEAN_TRADE_PAYMENT_ENABLED`、`GOLD_BEAN_TRADE_TRANSFER_SCENE_ID` 和可选 `GOLD_BEAN_TRADE_TRANSFER_NOTIFY_URL`，默认关闭；普通微信支付商户证书和商家转账 API 权限、收款用户列表/实名认证、免密额度等外部条件尚未在本地完成真实验收。
- 已同步到线上隔离开发项目 `/opt/zhiyu-health`，本次部署前源码备份为 `/opt/zhiyu-health/backups/gold-bean-trade-v49-before-20260903-112057`；隔离数据库已执行 V49，`gold_member_trade` 和 `gold_member_trade_listing` 均已创建。远程 Java 21 镜像构建时 79 项测试全部通过，`rayk-remote-dev` 六个服务均 healthy，`https://xingxuyuan.com/test-api/health` 返回 200，未登录交易市场接口返回 401；生产容器、生产数据库和生产静态资源未修改。
- 小程序金豆页已增加“金豆集市”，发布表单只保留数量输入，客户端不再提交单价；服务端按当前可交易余额、交易限额并扣除已有挂单/待成交量鉴权，买入金额统一按 1 金豆 = ￥1.00 计算，使用普通 `uni.requestPayment`。本次已重新通过 `type-check`、`lint`、H5、标准开发包、远程开发包、远程隔离测试包和生产局域网包构建；线上隔离环境的三个交易开关因尚未配置商家转账场景 ID 仍为空/关闭，未创建真实交易、未扣款、未修改生产环境。

## 2026-09-03 修正旧会员重复初始金豆

- 已在线上开发容器数据库中修正 1 个受影响的已注册会员：余额由数字银行 90、可交易 90 调整为各 60；连续奖励天数保持第 2 天不变。
- 原注册逻辑产生的 2 条 `INITIAL_GRANT` 流水已做软删除，注册成功当天的第 1 天和第 2 天 `DAILY_REWARD` 共 4 条正常流水保留；未删除数据库记录，必要时可依据备份恢复。
- 修正前账本备份位于服务器 `/opt/zhiyu-health/backups/gold-bean-ledger-before-correction-20260903-094653.sql`；本次只处理线上开发容器，未修改生产环境。

## 2026-09-03 平台购豆按双账本分配、任意数量

- 平台购买金豆按“平台注册码注册的第一级代理或当前钻石会员”校验；具体资格规则见上方“钻石会员平台购豆与城市区域权限”。
- 购买金豆可填写任意正整数，支付成功后使用统一双账本入账逻辑，数字银行和可交易金豆尽量各入一半；奇数数量无法完全平分时，多出的 1 个进入数字银行，订单流水说明同步标记“双账本尽量各一半”。
- 客户端购买卡片已更新为“每个金豆 ￥1.00，可购买任意数量”及奇数分配提示，并保留无资格说明；验证用例覆盖双账本拆分、钻石等级资格和奇数数量分配。Vue 专项检索无匹配，界面按通用移动端禁用态、错误提示和触摸目标规范处理。
- 已部署到线上隔离开发容器：远程 Java 21 全量 76 个测试通过，`rayk-remote-dev-rayk-server-1` 重建后健康，六个隔离服务均为 healthy，`https://xingxuyuan.com/test-api/health` 返回 200。前端已刷新 H5、微信开发包、两个远程开发包和生产局域网包；本次未创建真实购买订单、未扣款、未修改生产环境。
- 本次部署前源码备份位于服务器 `/opt/zhiyu-health/backups/platform-purchase-any-quantity-before-20260903-103707`。

## 2026-09-03 注册成功当天计入连续奖励

- 普通会员无论通过开发版登记还是平台微信虚拟支付回调完成注册，注册成功当天只发放每日 60 金豆，并将其计为连续奖励第 1/20 天；不再另行发放一笔初始 60 金豆。
- 实现位置为 `rayk-server/src/main/java/com/rayk/health/goldbean/application/GoldBeanApplicationService.java`；复用原有按日期和幂等键结算逻辑，后续查询或购买不会重复发放当天奖励。
- 本次只修改 Java 服务端，未修改小程序源码、数据库结构或生产环境；本地单测与隔离容器内 Java 21 的 73 个 Maven 测试均全部通过。已在服务器 `/opt/zhiyu-health` 创建最新备份 `backups/gold-bean-registration-day1-single-reward-before-20260903-092200`，重建 `rayk-remote-dev-rayk-server-1`；六个隔离服务均 healthy，`https://xingxuyuan.com/test-api/health` 返回 200，未登录访问金豆接口返回预期 401。

## 2026-09-02 开发版俱乐部与消息入口调整

- 底部第三个入口已新增主包页面 `pages/club/index`：开发/隔离测试包在 `VITE_GOLD_BEAN_ENABLED=true` 时显示“俱乐部”，普通客户进入现有金豆会员页面；游客、医生和平台管理员不会请求客户金豆接口，会看到角色安全提示。
- 俱乐部的角色安全提示卡片已移除装饰性的“会”图标，并同步收紧卡片顶部间距，保留登录/返回工作台操作和功能逻辑不变。
- 已修复俱乐部页在微信开发者工具中整页空白的问题：原先误把 `pages-customer/gold-bean/index` 和 `pages/message/index` 两个页面直接嵌入为子组件，微信编译后会重复注册页面脚本；现已拆出 `GoldBeanPanel`、`MessagePanel` 可复用内容组件，原页面改为页面壳，路由和刷新逻辑保持不变。
- 修复后已重新通过前端类型检查、Lint、H5 构建、标准开发包、远程开发包、远程隔离测试包和生产局域网包构建；生成的俱乐部页只引用真正的组件，不再把页面脚本作为 `usingComponents` 加载。验收请重新导入 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test` 并点击“编译”。
- 原 `pages/message/index` 页面保留为普通页面，不再占用底部消息 Tab；工作台新增“消息中心”卡片，“我的”新增“我的消息”入口，二者均通过 `navigateTo` 打开原消息页面，角色数据范围和后端权限未改变。
- 生产构建的 `VITE_GOLD_BEAN_ENABLED=false` 不开放金豆功能：俱乐部容器改为展示原消息页，并由运行时把第三个底部标签恢复为“消息”；同步生产微信包时还会把 `app.json` 第三个 Tab 恢复为 `pages/message/index`，生产接口仍保持关闭。这样开发版可以验收俱乐部，生产包不会误显示金豆会员入口。
- 已重新通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；H5 构建脚本固定使用 esbuild 压缩以规避当前 Windows 页面文件不足导致的 Terser worker 内存崩溃。H5、标准开发包、两个远程开发包和生产局域网包均已刷新。本次只生成本地构建产物，未部署生产 H5、未重启生产服务、未上传微信版本。
- 交接验收优先导入 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`：登录普通客户后点底部“俱乐部”应打开金豆页；进入工作台应看到“消息中心”卡片；进入“我的”应看到“我的消息”。

## 2026-09-02 平台首会员一次性授权码控制

- 根据讨论确定的方案新增 Flyway `V48__gold_platform_registration_invite.sql`：平台管理员在开发版“金豆会员运营”页生成一次性平台注册授权码，可选绑定手机号并设置 1–168 小时有效期；数据库只保存 SHA-256，完整码仅在生成响应中显示一次，列表只返回掩码。
- 首个无推荐人的客户注册必须填写平台授权码；后端不接受“推荐码和授权码都留空”的注册订单。授权码在创建支付订单时进入 `RESERVED`，取消或过期释放，支付成功回调完成会员开通后变为 `CONSUMED`；`PLATFORM_ROOT` 唯一占位键和行锁共同防止并发产生多个平台首会员。取消/过期同时清空订单占位键，授权码可在未支付订单关闭后重新使用。
- 新增平台接口：`GET/POST /api/v1/platform/gold-bean/platform-invites`、`POST /api/v1/platform/gold-bean/platform-invites/{inviteId}/revoke`；客户新增 `POST /api/client/gold-bean/orders/{orderNo}/cancel`。管理员和客户页面已将“推荐码”与“平台注册授权码”拆为两个字段，避免空值语义混淆。
- 隔离线上容器 `/opt/zhiyu-health` 已同步源码、应用 V48 并重建 `rayk-remote-dev-rayk-server-1`；容器健康，V48 校验成功。接口回归验证：空授权码返回 `60714`；正确授权码创建订单后列表为 `RESERVED`；取消后为 `AVAILABLE`；再次创建成功并取消；管理员撤销后为 `REVOKED`。未发起真实扣款，生产容器、生产数据库和生产静态资源未修改。
- 本次远程跳过测试构建成功。完整 Maven 测试仍有 1 项既有 `VoiceReminderTextFactoryTest` 文案断言失败，其余通过；未修改语音提醒模块。前端 `type-check`、`lint`、`build:h5`、`build:mp-weixin:dev`、`build:mp-weixin:dev:remote`、`build:mp-weixin:dev:remote-test` 和 `build:mp-weixin` 均已通过，H5、标准开发包、远程包、隔离测试包和生产局域网包均已刷新；本次未部署生产静态资源。

## 2026-09-02 开发版金豆真实平台支付与购买链路

- 新增 Flyway `V47__gold_bean_payment.sql` 和 `gold_member_order` 订单表，区分平台注册费订单 `GBR...` 与金豆购买订单 `GBP...`；订单创建、商品 ID、金额、数量、支付渠道、平台交易号和到账时间均可追溯。
- 客户端金豆页已接入真实开发支付流程：支付开关关闭时仍是原记录型演示，开启并配置商品后，首个无推荐人的客户可向平台支付注册费；已注册客户可向平台购买金豆，购买金豆只入数字银行，不走可交易余额。带推荐码的注册不会进入平台收款链路，推荐人收款、结算和退款暂未实现。
- 服务端使用 `wx.requestVirtualPayment` 所需的服务端签名；支付回调按腾讯虚拟支付实际格式读取外层 `eventType/event/outTradeNo/payload/payEventSig`，校验 `payEventSig`、订单号、商品 ID、数量、单价、OpenID、绑定 AppID 和交易号后才开通会员或入账，并按订单/流水幂等处理。官方字段要求可参考 [腾讯云小程序支付文档](https://intl.cloud.tencent.com/zh/document/product/1219/70275) 和 [虚拟支付接口与回调](https://intl.cloud.tencent.com/zh/document/product/1219/67644)。
- 平台管理员金豆运营台新增只读“支付订单”页签和 `/api/v1/platform/gold-bean/orders` 接口，可查看脱敏交易号、订单状态、商品类型、金额、数量和会员归属；没有改账、打款、结算或退款操作。
- 2026-09-02 真机首次尝试时发现旧记录型演示账号被标记为 `PAID + PLATFORM`，导致新账号误收到“平台支付仅用于首个无推荐人的金豆会员注册”。已修正为只统计 `gold_member_order` 中真实微信虚拟支付成功的平台注册订单，不删除旧演示数据；修复后新账号可继续创建首个 `GBR...` 订单。
- 支付配置只在开发环境显式开启：本地 `compose.real-payment-dev.yml` 要求 `GOLD_BEAN_REGISTRATION_PRODUCT_ID`、`GOLD_BEAN_PRODUCT_ID` 和共享虚拟支付密钥；线上隔离容器通过 `REMOTE_DEV_GOLD_BEAN_PAYMENT_ENABLED`、`REMOTE_DEV_GOLD_BEAN_REGISTRATION_PRODUCT_ID`、`REMOTE_DEV_GOLD_BEAN_PRODUCT_ID` 注入，默认关闭。当前隔离环境已配置并发布注册商品 `normal_member_998` 和金豆商品 `gold_bean`，已明确开启金豆支付；不能使用年度健康会员商品 ID。
- 线上隔离开发容器已应用 V47，Java 21 构建的 72 个测试全部通过，六个隔离服务 healthy，`https://xingxuyuan.com/test-api/health` 返回 200；当前 `GOLD_BEAN_PAYMENT_ENABLED=true`，注册费为 99800 分、金豆单价为 100 分，支付与购买入口已通过摘要接口确认开启，尚未产生真实订单或扣款。回调兼容修正已同步并重建容器；生产容器、生产数据库和生产静态资源未修改。
- 交接验收包为 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。用全新客户账号且推荐码留空测试注册，再用同一账号测试购买金豆；分别核对客户端订单状态、会员页数字银行余额/流水、管理员“支付订单”页和回调日志。若需要回滚，先恢复隔离环境备份并关闭 `REMOTE_DEV_GOLD_BEAN_PAYMENT_ENABLED`。

## 2026-09-02 修复远程开发包缺少平台金豆页面

- 微信开发者工具报错 `/pages-platform/gold-bean/index.wxml not found` 的根因是当时导入的 `dist/release/mp-weixin-dev-remote` 仍是 2026-08-27 的旧包，旧 `app.json` 和目录都没有新增的 `pages-platform/gold-bean` 分包页面；不是 Vue 页面或后端接口渲染错误。
- 已重新执行 `npm run build:mp-weixin:dev:remote` 和 `npm run build:mp-weixin:dev:remote-test`，当前 `mp-weixin-dev-remote`、`mp-weixin-dev-remote-test`、`mp-weixin-dev`、`mp-weixin-prod-lan` 四个包均包含 `pages-platform/gold-bean/index.js/json/wxml/wxss`，且 `app.json` 已登记 `gold-bean/index`。
- 线上隔离开发容器回归仍通过：`https://xingxuyuan.com/test-api/health` 返回 200，平台管理员登录及金豆管理员接口返回成功，测试环境保持 `developmentMode=true`、`recordOnly=true`；未修改生产容器。
- 微信开发者工具需重新导入或重新编译当前目录 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`（隔离线上容器验收）或 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote`（线上 HTTPS 验收），不要继续使用旧的开发工具缓存/旧目录。

## 2026-09-02 开发版金豆会员管理员运营台第一阶段

- 根据 `C:\Users\An'an\Desktop\需求梳理.docx`，已开始实现开发环境管理员端第一阶段：规则快照、会员账户、推荐关系、注册费用归属和金豆流水只读查询。
- 服务端新增 `/api/v1/platform/gold-bean/overview`、`/accounts`、`/referrals`、`/ledger` 接口，仅允许现有 `PLATFORM_ADMIN` 通过既有 `platform:tenant:list` 权限访问；跨机构读取仍在后端执行，未新增角色。
- 小程序新增开发版菜单和页面 `rayk-miniapp/src/pages-platform/gold-bean/index.vue`，由 `VITE_GOLD_BEAN_ENABLED` 控制，正式包保持隐藏；页面支持关键词、注册状态和流水类型筛选。
- 当前页面明确为开发环境只读运营台：现有注册流程仍是记录型演示，未接入真实收款、向推荐人个人打款、结算、退款或管理员改账操作。
- 前端 `type-check`、`lint`、`build:h5`、`build:mp-weixin:dev`、`build:mp-weixin:dev:remote-test` 和 `build:mp-weixin` 均通过；H5、微信开发包、隔离测试包和生产局域网包已按本次源码刷新。
- 线上隔离开发容器 `rayk-remote-dev-rayk-server-1` 已用 Java 21 编译并启动，跳过测试构建成功且六个隔离服务均 healthy。完整 Maven 测试首次为 70 项中 69 项通过、1 项既有 `VoiceReminderTextFactoryTest` 文案断言失败，与本次金豆管理员改动无关；未修改提醒模块。
- 已通过 `https://xingxuyuan.com/test-api` 实测：开发平台管理员登录成功，规则快照、账户、推荐关系和流水接口均返回 200；`registrationStatus=ALL` 修复后返回 2 个账户；开发客户访问平台接口返回 403。测试环境返回 `recordOnly=true`、`developmentMode=true`，未触发真实收款或结算。
- 远程服务源码变更前备份目录为 `/opt/zhiyu-health/backups/platform-gold-bean-admin-before-20260902-094600`，筛选修复前备份目录为 `/opt/zhiyu-health/backups/platform-gold-bean-admin-filter-before-20260902-095200`；生产容器、生产数据库和生产 H5 未修改。

## 2026-09-02 开发版首页健康管理进度卡片隐藏

- 首页底部“健康管理进度 / 健康档案已完善”卡片已在开发模式下暂时隐藏，生产模式继续保留；上方“今日概览”中的档案完整度卡片未修改。
- 通过 `homeProfileProgressCardEnabled` 按构建模式控制显示，未删除原有代码和路由。已重新生成并核对 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`；开发包开关为 `false`，生产局域网包为 `true`。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`。本次未部署或发布线上版本，开发版仍需导入微信开发者工具/真机确认视觉效果。

## 2026-09-01 首页今日概览同步线上

- 已将首页游客“今日概览”替换为“档案完整度＋健康报告＋健康随访”概览结构：游客仅显示 0/登录提示等安全空值，点击个人数据卡片会先引导登录；已登录客户继续显示真实数据。生产 H5 已同步到服务器 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5`。
- 切换前的线上 H5 已备份至 `/opt/zhiyu-health/backups/home-overview-before-20260901-182814/h5.tgz`，旧目录保留为 `rayk-miniapp/dist/build/h5.previous-home-overview-before-20260901-182814`，可用于回滚。生产 Nginx 已重建并通过健康检查，`https://xingxuyuan.com/health` 返回 `{"status":"UP"}`，线上首页 bundle 已核对包含“档案完整度”和“登录后可完善健康档案”。
- 使用本次构建的 `dist/release/mp-weixin-prod-lan` 上传微信小程序体验版本 `2026.09.01.2`，描述为 `home-overview-dashboard-20260901`，AppID 为现有生产小程序。该版本已上传但未提交审核或正式发布，正式发布仍需在微信平台完成后续流程。
- 本次仅更新前端静态资源和小程序体验包，未修改数据库、Java/Python 镜像或生产密钥；生产静态目录与隔离测试网关共用，因此测试 H5 也同步到同一份新页面。

## 2026-09-01 首页品牌标识与今日概览视觉调整

- 首页主视觉右侧头像位不再使用游客默认字母 `R`：游客或无显示姓名的账号使用现有品牌资源 `src/assets/ui/login/brand-logo-sheep.png`，有显示姓名的账号继续显示姓名首字，避免影响已登录用户辨识。
- 首页“今日概览”游客状态改为与客户一致的“档案完整度＋健康报告＋健康随访”概览结构；游客只显示 0/登录提示等安全空值，不调用个人数据接口，已登录客户继续显示真实数据。
- 游客首页常用服务两行卡片已扩大纵向触控区域并移除卡片末尾多余外边距，减少首屏底部留白；服务数量、路由和游客预览逻辑不变。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`；四套前端产物已在本次修改后重新生成并核对。

## 2026-09-01 微信审核整改：登录改为主动选择

- 审核拒绝原因为：小程序打开后直接进入“授权手机号并登录”页面，未先让用户浏览功能服务；审核要求先体验，再由用户自行选择授权登录。当前整改只修改了小程序入口和未登录展示逻辑，未放宽后端鉴权、未请求头像/昵称授权，也未改变手机号登录校验。
- `rayk-miniapp/src/pages.json` 已将 `pages/home/index` 调整为首个页面。未登录用户进入首页时使用普通首页布局（主视觉、今日概览、常用服务和工作台入口），不再显示额外的游客引导卡；手机号授权只会在用户主动点击登录入口后触发。登录页仅保留主动登录和协议确认内容，不再重复放置浏览按钮，公开浏览统一从首页进入。
- 首页、工作台、消息、我的和帮助与反馈页都增加了游客状态：游客可以浏览完整 10 项客户服务说明、工作台服务卡片、常见问题和登录提示；首页今日概览显示普通空状态，消息页保留无图标的“登录后查看你的健康动态”提示卡；这些状态不会调用受保护的个人数据接口。点击需要个人数据的服务时先显示说明，可继续浏览或主动选择“去登录”。登录后原有三角色工作台和数据权限逻辑保持不变。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`。当次产物已刷新到 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`；生成包 `app.json` 首页已核对为 `pages/home/index`。
- 当前仅完成代码和开发/隔离测试包构建，未修改、未重启、未发布生产 H5、生产 Java 或微信正式/体验版本。下一步应先把 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test` 导入微信开发者工具，在真机以全新会话验证：首次打开为首页、可浏览工作台/服务卡片/常见问题，点击“选择登录”后才进入手机号授权；确认通过后再按微信审核流程上传新的体验版本。

## 2026-09-01 金豆会员刷新按钮样式修复

- 金豆会员页“最近金豆记录”的刷新按钮已改为页面自绘的可点击控件，绕开微信原生按钮对直接文本和全局 `.elder-page button` 规则的特殊渲染；按钮固定为 88rpx 触控区域，文字使用绝对定位对齐按钮中心。
- 保留原有 `loadLedger` 刷新逻辑，增加按压反馈、禁用态和重复请求保护；已重新生成 H5、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`。本次未部署线上，真机视觉效果仍需导入最新微信包后重新编译确认。

## 2026-09-01 开发版手机号授权校验修复

- 开发包登录失败的可观测性根因是 Java `WeChatPhoneNumberClient` 将手机号接口、access_token 获取和缓存异常全部压缩为同一个 10205，既无法确认微信实际返回的 `errcode/errmsg`，也不会在缓存 token 失效时刷新重试。当前已改为读取并安全记录 HTTP 状态、微信错误码/消息、是否返回手机号信息、请求 ID 和脱敏的 AppID 哈希；绝不记录手机号、手机号授权 code、access_token、AppSecret 或其他密钥。
- access_token 缓存改为按 AppID 哈希隔离；微信返回无效/过期 token（40001、40014、42001，或 HTTP 401/403）时自动删除缓存并用新 token 重试一次；Redis 仅作为缓存，临时读写故障会降级为直接请求微信，不再因缓存故障直接阻断授权。
- 开发包 `pages/login/index.vue` 增加了手机号授权事件的前置校验：微信没有返回一次性手机号凭证时不再向后端发送空 code，而是提示重新点击授权并在真机预览；取消授权仍显示可操作提示。正常手机号 code 仍只交给后端真实校验。
- 服务器隔离测试环境已备份旧源码至 `/opt/zhiyu-health/backups/phone-auth-before-20260901-114905`（登录服务）和 `/opt/zhiyu-health/backups/phone-auth-before-20260901-115248`（最终手机号客户端），重建并强制重建 `rayk-remote-dev-rayk-server-1`，70 个 Maven 测试通过，容器健康。使用服务器当前微信配置做无效 code 探针返回 `errcode=40029`（invalid code），说明 AppID/AppSecret 与微信手机号接口连通且凭证有效；真实登录仍需在开发包中由用户重新点击并授权一次性 code。
- 已重新生成 H5、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-dev-remote-test` 和 `dist/release/mp-weixin-prod-lan`。本次未修改、未重启、未发布生产环境；正式生产仍使用原 Java 镜像和配置。
- 接手测试时优先导入 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`（请求地址为 `https://xingxuyuan.com/test-api`），在真机上重新点击手机号授权并让微信生成新的 `getPhoneNumber` code；旧 code 或旧页面不要重复提交。标准开发包 `mp-weixin-dev` 请求本机局域网 `http://192.168.0.100:8088`，只有 Docker Desktop 启动并按本地 Compose 启动 Java 后才能使用，本次会话未完成本机 Docker 运行验证。
- 当前未验证项：尚未用真实用户在设备上完成一次成功手机号登录；若仍失败，先看隔离测试 Java 日志中的 `hasPhoneCode`、HTTP 状态、微信 `errcode/errmsg` 和请求 ID，再区分“客户端未返回一次性 code”和“微信接口拒绝 code”。生产 H5、生产微信包和生产 Java 均未因本次修复变更。

## 2026-09-01 用户服务协议与隐私政策阅读页

- 登录页的《用户服务协议》和《隐私政策》已从静态文案改为可点击入口，统一打开 `/pages/legal/index`，通过 `type=service|privacy` 切换两份正文；正文使用中老年友好的字号、行距和明显链接色，并可在页面底部互相切换。
- 新增 `rayk-miniapp/src/pages/legal/index.vue` 和 `pages/legal/index` 路由，内容覆盖健康数据、体检报告、AI 健康管理辅助边界、第三方受托处理、访问/更正/删除/撤回授权、未成年人和安全事件等说明。页面不再展示生效日期，正式发布前仍需运营方核对主体、联系方式、第三方服务清单并完成法务审核。
- 隐私授权新记录版本统一为 `2026.09`（前端 `src/api/privacy.ts` 与 Java `PrivacyConsentService.CURRENT_POLICY_VERSION`）；历史授权记录不改动，重新授权时会记录新版本。仅修改协议入口与版本标记，未改变登录鉴权和健康数据权限。

## 2026-09-01 登录前主动同意协议

- 登录页协议区域不再使用默认勾选的静态对勾，改为用户主动点击的勾选按钮；同意状态按隐私政策版本保存在本机，版本变化后会重新要求确认。
- 微信手机号登录在未同意时渲染为普通按钮，不会提前触发微信手机号授权；点击登录会先打开简洁的协议确认弹窗。弹窗点击“同意并继续”后，手机号登录需再次点击授权按钮以遵守微信原生授权触发要求；开发调试登录和无需手机号授权的微信登录会在确认后继续原操作。
- 登录页协议链接保留点击打开完整协议的能力，但取消下划线并压缩为同一行；协议弹窗只保留“登录前请阅读并同意《用户服务协议》和《隐私政策》。”及“暂不/同意并继续”操作。按钮点击区域不小于 88rpx，并保留按压反馈；未改变后端登录接口。
- 用户服务协议第 8 节的政策更新文案已改为“我们可能因服务内容、法律法规或安全要求变化而更新本协议，请您及时查看，重大变化会通过适当方式提示。继续使用服务即视为接受更新后的协议。”，不再展示具体生效日期。

## 2026-09-01 登录协议改动同步线上

- 已将本次登录页主动同意协议、无下划线紧凑协议入口、协议确认弹窗和用户协议正文更新同步到生产 H5：服务器 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5`。切换前的 H5 保存在 `/opt/zhiyu-health/backups/login-consent-before-20260901-100738/h5.tgz`，当前旧目录保留为 `rayk-miniapp/dist/build/h5.previous-login-consent-20260901-100738`，可用于回滚。
- Nginx 已按新静态目录重建，生产 Java、AI、MySQL、Redis、MinIO 和 Nginx 容器均通过健康检查；`https://xingxuyuan.com/` 返回 HTTP 200，公网健康接口、登录页 bundle 和协议页 bundle 均核验为本次版本，旧弹窗“你可以点击下方链接查看完整内容”计数为 0。
- 使用生产配置包 `rayk-miniapp/dist/release/mp-weixin-prod-lan` 上传微信小程序体验版本 `2026.09.01.1`，上传描述为 `login-consent-legal-ui-20260901`，AppID 为现有生产小程序。该操作仅上传体验版本，不代表已提交审核或正式发布；正式发布仍需在微信开发者工具/平台完成后续流程。
- 本次未重新构建或更换 Java 镜像、未执行数据库迁移、未修改生产密钥；切换 Nginx 时 Compose 重新创建了依赖的 Java 容器，健康检查已通过。微信原生手机号授权在首次同意协议后需用户再次点击授权按钮，属于微信授权触发限制。

## 2026-08-31 开发环境金豆会员需求 V1

- 已依据 `C:\Users\An'an\Desktop\需求梳理.docx` 在现有小程序基础上新增开发环境专用金豆会员演示能力：普通会员登记状态、60 金豆初始奖励、每日 60 金豆 20 天、直推等级（普通/铜牌/银牌/金牌/钻石）、直推奖励、7 天活跃保护期、降级提醒、双账本和钻石区域演示。后端以 Java 校验和追加账本流水，前端只负责展示和操作入口。
- 数据库新增 Flyway `V46__gold_bean_membership.sql`，包含账户、推荐关系、金豆账本、区域和分润记录基础表；未完成开发注册的账号保持 `UNPAID`，不会提前获得初始或每日金豆。真实收款、数字银行购买、机器人能力、交易撮合和区域分润结算未伪造实现，待业务/支付/合规口径明确后再开发。
- 功能隔离：`compose.dev.yml` 与 `compose.remote-dev.yml` 开启 `GOLD_BEAN_ENABLED`、`GOLD_BEAN_DEVELOPMENT_MODE`；生产配置保持关闭。小程序 `.env.development` 开启 `VITE_GOLD_BEAN_ENABLED`，`.env.production` 关闭，因此生产包不显示入口，生产容器和线上版本本次未修改、未重启、未发布。
- 前端页面为 `rayk-miniapp/src/pages-customer/gold-bean/index.vue`，入口仅对开发客户显示；接口前缀为 `/api/client/gold-bean`。已生成并同步 H5、微信开发包、生产局域网包，以及服务器隔离测试包 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。
- 验证结果：小程序 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin`、`npm run build:mp-weixin:dev:remote-test` 均通过；开发/隔离测试包的功能开关为 `true`，生产局域网包为 `false`。服务器 `rayk-remote-dev` 使用 Java 21 构建，68 个测试通过，V46 已在隔离测试库执行，`rayk-server` 与全套远程测试服务运行正常。
- 验收入口：本地 Docker 使用 `dist\release\mp-weixin-dev`；服务器隔离测试使用 `dist\release\mp-weixin-dev-remote-test`。两者均为开发包，不得上传到生产体验版本；正式生产开关继续保持关闭。

### 2026-08-31 金豆注册地区两级选择

- 开发版金豆注册卡片的“所在地区”已改为微信原生 `picker mode="region" level="city"`，用户只能选择省和市，不再显示城市自由输入框，也不会出现区/县选项。
- 提交前强制完成省、市选择，接口继续使用现有 `city` 字段并保存为“省 / 市”标签；未新增数据库迁移，不影响已存在的 V46 数据和生产开关。
- 本次前端检查与构建均通过：`npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin`、`npm run build:mp-weixin:dev:remote-test`。仅刷新本地构建产物，未部署或重启线上生产服务。

### 2026-08-31 金豆会员首屏加载修复

- 隔离测试包进入“金豆会员”显示“加载遇到问题 / 系统内部错误”的根因是 `GoldBeanApplicationService.createReferralCode` 对短用户 ID 生成的 9 字符前缀强制执行 `substring(0, 20)`，触发 `StringIndexOutOfBoundsException`；与微信登录、权限、支付和 V46 迁移无关。
- 已改为推荐码至少保留完整生成值、超过数据库字段长度时才截断，并新增短 ID/长 ID 回归测试。服务器隔离测试 `rayk-server` 已使用 Java 21 重建，70 个 Maven 测试全部通过并仅重启隔离测试服务。
- 使用隔离测试开发账号实际请求 `/api/client/gold-bean/summary` 已返回 HTTP 200，首次账号状态为 `UNPAID`，摘要不返回个人推荐码；远程测试六个服务均 healthy。生产容器和生产配置未修改。

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

## 2026-08-24 实物商品商城首版完成

- 用户明确商城销售实物产品。现有会员虚拟支付保持原链路不变，商城新增独立的商品、收货地址、订单、库存和普通微信支付 API v3 JSAPI 链路。
- 新增 Flyway `V45__physical_product_mall.sql`，包含 `mall_product`、`mall_address`、`mall_order` 和 `mall_order_item`。商品目录不预置虚构商品，由平台管理员发布后客户才会看到；订单保存商品和地址快照，库存下单时原子扣减，取消或超时释放库存。
- 新增客户接口 `/api/mall/**` 和平台管理员商品接口 `/api/v1/platform/mall/products`。Java 后端校验客户本人地址/订单范围和 `PLATFORM_ADMIN` 商品管理权限；共享商品目录固定使用平台目录租户，更新时也会校验目录租户和未删除状态。
- 实物订单号使用 `G` 前缀，普通微信支付回调按订单号分流到商城；会员标准支付回调保持原处理，会员虚拟支付回调完全不受影响。
- 小程序新增商城、商品详情、地址、结算和订单页面；底部导航调整为“首页、工作台、商城、我的”，消息中心改由工作台快捷入口进入；平台工作台新增“商城商品”入口。商城页面不提供模拟支付。
- `MALL_ENABLED` 默认开启，`MALL_PAYMENT_ENABLED` 默认关闭；生产只有在配置普通微信支付 API v3 商户证书、私钥、API v3 密钥、支付回调地址，并完成发货、退款和售后验收后才能显式开启。该配置不能复用会员虚拟支付 AppKey、OfferID 或 ProductID，也未把任何密钥写入源码。
- 已完成前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包构建；Docker 中 Java 21 后端编译成功，66 项 Maven 测试全部通过。商城上线前仍需在目标环境执行 V45 迁移，并进行真实商品、库存、支付回调、发货和退款联调。
- 后端已按当前 `wechatpay-java 0.2.17` 模型 API 完成普通微信支付 JSAPI 请求与返回参数兼容修正；开发/生产 Compose 配置校验和 `git diff --check` 均已通过。
- 当前完成的是代码集成与本地构建验证，尚未在目标环境执行 V45 数据库迁移、发布商城镜像或开启 `MALL_PAYMENT_ENABLED`；真实支付、库存对账、发货、退款和售后流程仍需在目标环境验收后再上线。

## 2026-08-24 修复商城原生输入裁切并完善实物支付验收

- 平台管理员商品编辑表单和客户收货地址表单的原生 `<input>` 现在使用固定高度与同高行高，不再让微信 Android 将占位文字和数字上半部分裁切；多行文本框保持独立行高。
- 实物订单的小程序支付面板返回成功后，不再直接把订单提示为“支付已完成”。客户端会轮询本人订单，只有服务端通过微信支付回调将订单写为 `PAID` 才显示成功；回调尚未抵达时明确提示“支付结果确认中”。
- `compose.real-payment-dev.yml` 真实支付验收配置现强制开启商城普通微信支付，并在 Compose 启动时要求标准微信支付 API v3 的 AppID、商户号、商户证书序列号、API v3 Key 和回调地址；商户私钥仍只从只读证书挂载提供。生产 Compose 则保持默认关闭并读取服务器 `.env` 的显式 `MALL_PAYMENT_ENABLED=true` 开关，避免无验收配置时误收费。该配置同时保留会员虚拟支付所需的独立密钥校验，不能用会员虚拟支付配置替代商城支付配置。
- 验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均已完成；Compose 真实支付验收配置已用非敏感占位值通过语法校验，Java Docker Maven 测试构建通过。仍需在微信开发者工具/Android 真机确认输入文字完整显示。目标验收服务器需先配置未提交的支付密钥、私钥挂载和公网 HTTPS 回调，执行 V45 迁移并部署当前 Java 镜像后，才能由真实客户完成一笔小额支付、回调、订单 `PAID`、库存和取消/超时释放的全链路验收。

## 2026-08-24 修复本地商城商品发布系统内部错误

- 根因是小程序已更新到商城页面，但本地运行的 `rayk-server` 仍是商城控制器加入前的旧容器。对 `/api/v1/platform/mall/products` 的读取和发布请求被 Spring 当成静态资源，导致 500 和“系统内部错误”。
- 已使用当前 `rayk-a1-server:dev` 镜像无卷重建 Java 服务，Flyway 已将本地数据库从 V44 升级到 V45，商城商品、地址、订单和订单项表已创建；MySQL、Redis、MinIO 数据卷未删除。
- 验证：`rayk-server`、`rayk-ai` 和本地网关均 healthy，`/health` 返回 200；已认证开发管理员读取商品目录返回 HTTP 200，故意无效的发布请求返回预期 HTTP 400 校验错误，不再返回静态资源 500。仍需在微信开发者工具或真机重新点击“发布商品”完成一条真实商品的业务验收。

## 2026-08-25 生产商城与普通微信支付部署

- 已将商城后端、V45 迁移、H5 页面和生产 Compose 配置部署到服务器 `/opt/zhiyu-health`；变更前的受影响源码、H5 和服务器 `.env` 已备份至 `/opt/zhiyu-health/backups/mall-deploy-20260825-1630`，未删除 MySQL、Redis、MinIO 或其他数据卷。
- 商城普通微信支付的标准 API v3 配置仅从服务器受限 `.env` 读取，商户私钥以只读挂载提供给 Java 运行用户；容器中已确认支付开关、所需参数和私钥可读，但未回显任何密钥或证书内容。
- 生产 Java 镜像已完成 Maven 测试构建并重建；Flyway 已成功执行 V45，`mall_product`、`mall_address`、`mall_order`、`mall_order_item` 均存在。`rayk-server`、AI、Nginx 均为 healthy，公网 H5 可访问；未登录访问平台商城接口返回 401，表明商城路由已加载并受到鉴权保护。
- 尚未发起真实扣款。下一步由平台管理员发布一件小额测试实物商品，再由真实客户在微信小程序中完成支付；以微信回调后订单变为 `PAID`、库存扣减和后台订单记录为准。退款、发货和售后仍需按真实业务流程单独验收。

## 2026-08-25 商城支付诊断与按钮热修复

- 已核对普通微信支付服务器到微信支付 API 的 HTTPS 连通性、API v3 Key 长度、商户证书序列号和商户私钥/证书公钥匹配；此前支付失败发生在订单创建后、向微信获取 JSAPI 预支付参数时，不是商品、地址、库存或支付开关缺失。
- 原 `WeChatPayClient` 会将 SDK 的所有运行时错误直接映射为“商城支付服务暂时不可用”，且不留下原因。现改为只记录异常类型、HTTP 状态和微信机器错误码；不记录签名原串、OpenID、订单号、商户号、API v3 Key 或证书内容。下一次支付调起即可据此确认是商户权限、JSAPI 授权还是配置拒绝。
- 付款页现在显示“创建订单 / 获取微信支付 / 打开微信支付 / 确认结果”的阶段文案；同一地址和数量的重试会复用当前待支付订单，避免连续点击反复占用库存。地址页操作按钮、数量加减按钮和主按钮统一为明确的 Android 触控尺寸、居中内容、禁用态和无默认伪边框样式。
- H5 和线上 Java/Nginx 已完成热部署，线上容器与公网 H5 均为 healthy；前端类型检查、Lint、H5、微信开发包和微信生产局域网包均已重新构建，线上 Java Maven 测试构建通过。微信体验版需要在开发者工具开启服务端口后另行上传，未自动随服务器 H5 更新。

## 2026-08-25 商城体验版上传

- 微信开发者工具服务端口和登录状态已由用户确认开启；已将 `rayk-miniapp/dist/release/mp-weixin-prod-lan` 上传为微信小程序开发版本 `0.1.1`，描述为“商城支付诊断与移动端按钮修复”，包体约 2.1 MB。
- 该上传包含商城页面、付款阶段反馈、待支付订单页面内复用、按钮样式修复和安全的微信预支付失败诊断；不等同于正式发布，也没有替用户发起付款。
- 仍需由具备微信小程序后台权限的账号将该开发版本设为体验版（或更新现有体验版），然后用客户微信账号发起一次支付调起；服务器日志会仅输出安全错误类别，以便确定并处理微信商户侧阻断条件。

## 2026-08-25 商城真实支付阻断原因确认

- 真实客户付款请求已经到达微信支付 JSAPI 预支付接口；微信返回机器错误码 `RESOURCE_NOT_EXISTS`。请求地址、服务器到微信的 HTTPS 连通性、商户证书与私钥匹配、API v3 Key 长度均已核对，因此这不是商城商品、地址、库存、前端按钮或服务器网络问题。
- 后端现将该明确错误安全映射为“商城微信支付尚未完成小程序关联或 JSAPI 开通，请联系管理员”，不再笼统提示服务暂不可用；线上 Java 服务已使用新镜像重建并通过容器健康检查。本次远程临时源码包已删除，受限备份保留用于回滚。
- 真实扣款仍受微信支付商户平台配置阻断：需由有商户平台权限的人员确认当前小程序已关联至该商户号，并已开通/启用普通微信支付 JSAPI（小程序支付）产品。代码无法绕过微信支付返回的该权限/资源状态；完成商户平台设置后，使用体验版 0.1.1 的客户账号再次发起一笔小额支付验证回调和订单 `PAID` 状态。
- 本次生产构建的 Java 编译成功；全量 Maven 测试另有既有的睡眠提醒文案测试断言失败，与商城支付改动无关。为部署明确的错误提示，生产镜像本次以 `MAVEN_SKIP_TESTS=true` 构建；该既有测试失败仍需后续单独修复后恢复全量绿灯发布。

## 2026-08-25 商户身份信息（待以原始下单响应确认）

- 用户在微信支付商户平台确认当前商户号类型为“特约商户”；这是账户信息，不能单凭该信息推断本次下单失败的具体原因，也不能据此切换支付接口。
- 当前后端实际配置和调用路径须以本次微信支付下单的原始 HTTP 响应为准。未取得请求 URL、响应 `code/message` 和 `Wechatpay-Request-Id` 前，不再对支付模式、权限状态或服务商配置作结论。

## 2026-08-25 微信支付平台公钥进一步排查

- 线上当前未配置微信支付平台公钥 ID 或平台公钥文件；现有 Java SDK 使用 `RSAAutoCertificateConfig` 自动下载平台证书。官方 SDK 说明表明，若该商户号无可用平台证书，初始化下载会返回 `RESOURCE_NOT_EXISTS`，并提示改用微信支付公钥。
- 因此此前仅按错误码判断“预支付接口被拒绝”并不充分：错误可能发生于 SDK 初始化下载平台证书，尚未发送真实的 JSAPI 下单请求。用户提供的 `PUB_KEY_ID_*` 是微信支付平台公钥 ID，单独配置该 ID 不够，仍需对应的微信支付平台公钥 PEM 文件，并将 SDK 改为 `RSAPublicKeyConfig`（或等价的公钥模式）。
- 平台公钥模式切换后，仍须通过一次真实下单取得微信的完整原始 HTTP 响应，才能确认剩余阻断条件；不能仅依据商户类型或单一机器错误码推断原因。

## 2026-08-25 微信支付平台公钥模式已部署

- 已将用户下载的微信支付平台公钥以受限文件方式安装到服务器，并写入对应公钥 ID 与容器只读路径；私钥、公钥及 API v3 Key 均未写入源码、日志或 Git。远程上传暂存文件已删除，部署前备份保留在服务器受限备份目录。
- Java 后端在同时提供平台公钥 ID 和 PEM 时使用 `RSAPublicKeyConfig` 发起 API 请求，并使用 `RSAPublicKeyNotificationConfig` 验证支付回调；未提供公钥配置的环境仍保留自动下载平台证书的兼容路径。
- 线上 Java 镜像已通过完整 Maven 测试构建并重建，容器为 healthy，容器内已核对平台公钥文件可读和公钥配置存在。尚未为了诊断主动创建新的支付订单；下一次客户支付会验证此前的“无可用平台证书”阻断是否已经消除。
- 当前商户号仍是特约商户，平台公钥模式只解决 SDK 初始化/验签配置；是否需要服务商授权或接口调整必须以真实下单响应和商户平台确认结果为准，不得预设。

## 2026-08-25 公钥模式后支付错误（待原始响应确认）

- 客户在平台公钥模式上线后重新发起商城支付，原有安全日志仅记录到微信支付 `HTTP 403 / NO_AUTH`；它没有记录请求 URL、微信 `message` 或 `Wechatpay-Request-Id`，因此不足以确认失败点或支付接入模式。
- 已部署更完整且脱敏的 HTTP 响应日志：仅记录请求 URL（无查询参数）、HTTP 状态、微信 `code/message`、`Wechatpay-Request-Id` 和实际 API 路径；不记录 API v3 Key、私钥、签名、订单信息或请求体。下一次客户点击支付后，以该条原始响应日志为唯一诊断依据，不切换支付接口。
- 生产镜像的诊断代码编译成功并已重建为 healthy。完整 Maven 测试仍有既有的睡眠提醒文案断言波动，和本次诊断改动无关；本次镜像以跳过测试方式构建，后续仍应单独恢复全量绿灯。
- 后续一次客户点击记录到 `java.lang.NullPointerException`，且请求 URL、HTTP 状态、微信 `code/message`、请求 ID 和 API 路径均为 `unavailable`。这表明异常发生在 HTTP 请求创建或 SDK 调用前，尚未向微信支付发出可确认的 JSAPI 下单请求；不能据此推断支付模式或微信侧权限原因。
- 根因已定位为：为接入脱敏 HTTP 日志而改用 SDK 自定义 `HttpClient` 时，`JsapiServiceExtension` 构造器未同时保留 `Config`。SDK 在生成小程序调起所需的支付签名参数时读取该配置，因而在发出 HTTP 请求前触发空指针。已改为同时注入 `Config` 和自定义 HTTP 客户端；不改变任何下单 API 路径或支付模式。下一次点击应产生可用的 HTTP 原始响应日志。
- 修复后真实下单已发出 HTTP 请求：`POST /v3/pay/transactions/jsapi` 返回 `HTTP 403 / NO_AUTH`，微信 `message` 为“商户号该产品权限未开通，请前往商户平台>产品中心检查后重试”，且返回 `Wechatpay-Request-Id`。这证实当前实际路径是普通 JSAPI，不是服务商路径；前端此前“服务商授权”提示与微信原始响应不一致，已改为与微信原文一致的产品权限提示。该产品权限需由商户平台侧开通，代码无法绕过。

## 2026-08-25 新微信支付商户配置已切换

- 已按用户提供的新商户资料，在本地受限 `.env` 与服务器受限 `.env` 更新商户号、商户 API 证书序列号、API v3 Key、平台公钥 ID 与容器内证书路径；敏感值未写入源码、日志、交接文档或 Git。
- 新商户私钥和微信支付平台公钥均已以受限文件方式安装在本地和服务器证书目录；服务器文件权限保持运行用户可读的最小权限。已验证两份 PEM 可被 OpenSSL 解析，Compose 配置校验通过，Java 服务已重建并为 healthy，运行用户可读取两份挂载证书。
- 现有小程序 AppID 因用户未提供新的值而保持不变；新商户必须已在微信支付侧关联该 AppID，才能完成真实小程序支付。未为本次配置变更主动发起扣款；下一次客户支付将验证新商户的微信侧产品权限、回调和订单状态。
- 新商户实际 JSAPI 下单已返回 HTTP 200，说明商户配置、签名和预支付接口已成功；失败发生在小程序客户端 `requestPayment:fail banned`。微信官方将该错误定义为小程序支付能力被公众平台限制，需在微信公众平台通知中心核实原因并按指引申诉/整改；更换商户号不能绕过该 AppID 级限制。小程序现将原始英文错误改为简短中文指引，订单保持待支付，待公众平台解除限制后可直接重试。
- 前端修复已完成 `type-check`、Lint、H5、微信开发包和微信生产局域网包构建；H5 已同步到线上 Nginx 挂载目录并通过公网访问校验。微信体验版尚需上传刚生成的生产微信包后才会显示新的中文提示；上传不是正式发布。

## 2026-08-26 首页视频播放栏

- 首页原客户健康关怀文案卡已替换为 `HomeVideoCard`，所有工作台共用同一块视频播放栏；视频存在时使用原生 `<video controls>`，支持播放、暂停、进度拖动和全屏。
- 视频地址和可选封面图分别读取 `VITE_HOME_VIDEO_URL`、`VITE_HOME_VIDEO_POSTER`，未配置或加载失败时显示明确的可配置/失败占位，不会伪造已上线的视频内容。
- 当前仓库和服务器没有可直接发布的视频文件或链接；正式微信验收前需提供 HTTPS 视频地址，将视频域名加入小程序业务域名白名单，并重新生成开启视频功能的微信包。本次上传的是关闭视频/商城入口的隐藏版。

## 2026-08-26 修复平台管理员商城浏览权限

- 根因：底部“商城”进入的是客户商品目录接口 `/api/mall/products`，原控制器在类级别要求 `self:health-record` 且当前工作台必须为 `CUSTOMER`，平台管理员浏览商品时被统一返回 403。
- 已将商品列表和商品详情改为允许 `PLATFORM_ADMIN` 或 `CUSTOMER` 读取；收货地址、订单创建/查询/取消和微信支付仍逐方法限制为客户工作台，平台管理员不会获得客户订单或支付权限。
- 管理员在底部商城可只读浏览在售商品，商品详情页隐藏购买数量和下单按钮；平台工作台的“商城商品”仍用于管理员维护商品。
- 验证：前端 `type-check`、ESLint、H5、微信开发包和微信生产局域网包均已重新完成，`git diff --check` 通过；当前主机未安装 Maven，未在本机直接运行 Java 测试，也尚未部署线上。管理员会话下的商品列表/详情 200 及客户地址、订单权限仍需在运行中的后端或线上验收。

## 2026-08-26 生产隐藏视频和商城功能版本

- 因首页视频素材、视频域名白名单以及商城商品/履约材料尚未准备好，新增前端构建开关 `VITE_HOME_VIDEO_ENABLED` 和 `VITE_MALL_ENABLED`。当前本地开发和生产构建均显式关闭两个开关；后续材料齐备时可在目标构建环境分别打开。视频组件、商城页面、平台商品管理、Java 接口和 V45 迁移均未删除。
- 生产隐藏版底部导航恢复为“首页、工作台、消息、我的”；工作台不再展示“商城商品”，首页在视频开关关闭时恢复原有健康关怀文案卡。商城源码仍可在后续材料齐备后通过开关和底部入口重新启用。
- 服务器生产 Compose 的 `MALL_ENABLED` 默认改为关闭，防止旧小程序或直接接口调用继续创建商城订单；准备重新开放时需同时设置服务端 `MALL_ENABLED=true`、前端 `VITE_MALL_ENABLED=true`，并恢复底部商城入口后重新构建发布。`MALL_PAYMENT_ENABLED` 仍独立保持关闭。
- 本版发布标识：`release-20260826-hidden-video-mall`。该标识用于本次隐藏版前端发布清单；当前工作区包含此前功能开发的未提交改动，发布清单会标记 `gitDirty=true`，不能当作干净提交版号。
- 已将隐藏版 H5 部署到线上 Nginx 挂载目录，服务器 `rayk-server` 已重建并健康运行；线上 `.env` 的 `MALL_ENABLED` 与 `MALL_PAYMENT_ENABLED` 均已设为 `false`。公网首页和 `/health` 返回 200，未授权访问 `/api/mall/products` 返回 401。
- 服务器回滚备份：`/opt/zhiyu-health/backups/release-20260826-hidden-video-mall-before-20260826-174751`。备份包含替换前 H5、生产 Compose 和受限 `.env`；临时上传目录已仅用于本次同步，未修改数据库、Redis 或 MinIO 数据卷。
- 线上 Java 容器继续使用既有 `rayk-a1-server:dev` 镜像，因此 `/api/system/version` 的运行时镜像标识仍可能显示 `dev-local`；本次正式标记以 H5 `release-manifest.json` 和本条发布记录为准，后续若构建正式服务镜像再同步 Java 版本元数据。
- 已用已登录的微信开发者工具 CLI 上传隐藏版微信包，AppID 为现有项目 AppID，微信版本号 `2026.08.26`，上传描述包含发布标识 `release-20260826-hidden-video-mall`；上传成功但未自动提交审核或发布，正式体验/线上发布仍由微信公众平台的版本管理流程控制。

## 2026-08-27 帮助与反馈中老年可读性调整

- 删除帮助与反馈页中已不再适用的“如何登录并识别身份？”常见问题，保留其他问题和反馈提交/历史记录逻辑。
- FAQ 展开答案改用中老年页面统一的 `32rpx` 正文字号，行高调整为 `1.8`，并提高文字对比度；源码未删除其他帮助功能。
- 已通过 `type-check`、ESLint、H5、微信开发包和微信生产局域网包构建；三个前端输出目录均已按本次修改同步。此次仅更新本地源码和构建产物，未自动替换线上 H5 或重新上传微信版本。

## 2026-08-27 修复微信开发者工具预览缺页竞态

- 开发者工具曾报 `app.json: ["subPackages"][2]["pages"][0] could not find the corresponding file: "pages-tenant/dashboard/index.wxml"`。源码和三个构建输出实际都包含该文件，根因是发布目录同步时可能先写入 `app.json`，开发者工具监听到中间状态后立即预览。
- 已调整 `scripts/sync-mp-weixin-release.mjs`：页面及静态资源先完成镜像，`app.json`、`project.config.json` 等清单文件最后写入；保留被开发者工具占用时的原地同步逻辑，避免监听目录出现短暂不完整项目。
- 已重新生成并核对 `dist/build/mp-weixin`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-prod-lan`，三处 `pages-tenant/dashboard/index.wxml` 均存在且与当前构建一致。`type-check`、ESLint 和两个微信包构建通过；H5 不受本次同步脚本影响，已有构建产物保持可用，当前机器重新执行 H5 时受 Uni 编译器 Node 内存限制未完成。

## 2026-08-27 修复开发包误用生产环境

- 开发者工具窗口虽显示 `mp-weixin-dev`，但其请求模块曾注入生产地址 `https://xingxuyuan.com`。根因是开发构建未完整结束后，复用了同一 `dist/build/mp-weixin` 目录中的生产产物进行开发包同步。
- 已重新完成开发构建并同步，当前 `dist/release/mp-weixin-dev/utils/request.js` 注入局域网 API 地址 `http://192.168.0.100:8088`；`mp-weixin-prod-lan` 仍注入生产地址，两个包环境已分离核对。
- `build-mp-weixin-dev.mjs` 新增环境校验：构建成功后必须确认请求模块包含开发 API 地址，否则直接失败并拒绝同步旧产物。后续开发包应使用 `npm run build:mp-weixin:dev`，不要在生产构建后手动执行开发同步。

## 2026-08-27 报告处理中状态中文化

- 报告详情页状态标签此前未覆盖后端枚举 `AI_PROCESSING`，因此直接显示英文内部值。
- `StatusTag.vue` 已增加 `AI_PROCESSING: 评估中` 映射；H5、微信开发包和微信生产局域网包均已重新生成并核对。
- 已通过 `type-check`、ESLint；开发包使用局域网 API，生产局域网包使用生产 API，环境未混用。此次未部署线上 H5 或上传新的微信版本。

## 2026-08-27 远程后端开发包

- 为避免本机 Docker 服务占满内存，新增 `npm run build:mp-weixin:dev:remote`。该命令仍使用 development 构建优化，但将 API 指向线上 `https://xingxuyuan.com`，并关闭仅本地可用的开发身份入口；线上容器和数据库未被修改。
- 当前 `dist/release/mp-weixin-dev` 已由该命令生成，已核对请求模块使用线上 HTTPS 地址；`dist/release/mp-weixin-prod-lan` 同样使用线上 HTTPS 地址。恢复本机局域网联调时运行 `npm run build:mp-weixin:dev`，会重新使用 `.env.development` 的局域网地址。
- 远程开发包必须使用线上真实授权手机号登录，不能依赖本地开发模拟登录；线上商城/支付开关仍以服务器配置为准。本次仅更新本地构建包，未重新上传微信版本。
- 为释放本机内存，已执行 `docker compose stop` 停止本地 MySQL、Redis、MinIO、AI、Java 和 Nginx 容器；未删除任何数据卷。线上 `/health` 检查返回 200。恢复本地服务可运行 `docker compose up -d`（按需再叠加 `compose.dev.yml`）。

## 2026-08-27 开发包与远程开发包目录隔离

- 修复远程开发构建覆盖标准开发包的问题：`npm run build:mp-weixin:dev` 始终同步到 `dist/release/mp-weixin-dev`，保留 `.env.development` 的局域网 API 和开发登录界面；`npm run build:mp-weixin:dev:remote` 改为同步到独立的 `dist/release/mp-weixin-dev-remote`。
- 远程包仍访问线上 HTTPS，但因线上后端关闭 `RAYK_DEVELOPMENT_LOGIN_ENABLED`，只支持真实微信手机号登录，不能使用本地模拟身份。微信开发者工具若要看到开发身份入口，应导入 `mp-weixin-dev` 并启动本地开发后端；若要验收线上 Docker，应导入 `mp-weixin-dev-remote` 或 `mp-weixin-prod-lan`。
- 若 `mp-weixin-dev` 显示“网络连接失败”且 Network 中出现 `mock-login (failed)`，应先确认本机 Docker 是否启动；该包默认请求 `http://192.168.0.100:8088`。本机 Docker 停止时请改用 `mp-weixin-dev-remote`，不要把线上地址临时写回标准开发包。

## 2026-08-27 服务器隔离远程开发环境

- 新增 `compose.remote-dev.yml` 与 `deploy/nginx/nginx.remote-dev.conf`，服务器使用独立 Compose 项目 `rayk-remote-dev`、独立 MySQL/Redis/MinIO/日志/OCR 卷和 localhost 网关端口 `18081`；测试 Java 环境开启 `RAYK_DEVELOPMENT_LOGIN_ENABLED` 与 `MEMBERSHIP_DEVELOPMENT_MODE`，关闭商城和支付。
- 生产 HTTPS 仅新增 `/test-api/` 反代前缀，转发到测试网关并剥离前缀；生产 `/api/`、生产数据库、支付密钥和正式容器不复用。测试环境通过 `https://xingxuyuan.com/test-api` 访问，前端包使用 `npm run build:mp-weixin:dev:remote-test` 生成到 `dist/release/mp-weixin-dev-remote-test`。
- 已部署到腾讯云 `/opt/zhiyu-health`：测试网关仅绑定 Docker bridge `172.17.0.1:18081`，生产 Nginx 通过 `host.docker.internal` 反代；测试 Java、AI、MySQL、Redis、MinIO 均 healthy，测试 `health` 和 `mock-login` 分别返回 200。生产 `/health` 同样返回 200。生产配置备份位于 `/opt/zhiyu-health/backups/remote-dev-before-20260827-162317`，测试 `.env.remote-dev` 为 root-only，未回显或复制生产密钥。

## 2026-08-29 隔离测试环境健康助手配置

- 线上隔离测试环境此前 `QWEN_ASSISTANT_ENABLED=false` 且没有 Qwen API Key，健康助手接口虽然返回 HTTP 200，但按安全降级逻辑显示“健康助手暂未完成服务配置”。
- 已先备份 `/opt/zhiyu-health/.env.remote-dev`，再仅在隔离测试环境启用 Qwen 助手，并复用服务器已有 Qwen 凭据；密钥保持 root-only，不进入代码、日志或本交接文档。生产 Java/AI 容器和支付配置未修改。
- 已重建 `rayk-remote-dev-rayk-ai-1` 与 `rayk-remote-dev-rayk-server-1`，两者及测试 Nginx、MySQL、Redis、MinIO 均 healthy。非流式和 SSE 流式助手请求均已返回 `qwen3.7-flash-2026-07-15` 的成功回答；临时验收会话已清理。

## 2026-08-29 修复健康助手额度错误与 SSE 降级

- 用户实际点击请求已进入 `/test-api`，根因不是 Qwen 上游故障，而是测试客户的 `AI_MEDICAL_ASSISTANT` 免费权益已用完；Java 在建立 SSE 前抛出 `MEMBERSHIP_BENEFIT_NOT_AVAILABLE (60401)`。
- 旧控制器将 SSE 的 `Accept`/响应协商与全局 JSON 异常处理冲突，导致服务端再次抛出 `No acceptable representation`，小程序只能显示“健康助手无法回答”。
- `MedicalAssistantController` 现对建立流前的 `BusinessException` 返回标准 SSE `error` 事件（包含错误码和提示，不含密钥或健康内容），前端已有解析和会员引导逻辑可正常工作。
- 隔离测试客户已切回年度测试会员；实测正常请求收到 `delta` 与 `done`，模拟免费额度不足收到 `type=error/code=60401`。服务器测试 Java 镜像构建通过 66 项 Maven 测试，测试 Java、AI、Nginx、MySQL、Redis、MinIO 均 healthy。
- 本次仅更新隔离测试环境，生产服务、生产会员权益和支付配置未修改。服务器旧控制器备份位于 `/opt/zhiyu-health/backups/assistant-sse-error-20260829-105743`。

## 2026-08-29 隔离测试环境启用同商户会员支付

- 按用户确认，隔离测试环境改为复用生产环境当前使用的微信支付商户配置；支付私钥和平台公钥复制到独立的 `secrets/remote-dev` 目录，容器内仍以只读方式挂载，未把敏感值写入源码或交接文档。
- 已同时同步标准 JSAPI 的 `WECHAT_PAY_*` 与会员当前代码路径使用的 `WECHAT_VIRTUAL_*` 配置；会员接口仍按现有实现调用 `requestVirtualPayment`，未擅自改成另一种支付模式。
- 测试回调地址单独设置为 `https://xingxuyuan.com/test-api/api/payments/wechat/notify`，由生产 Nginx 的 `/test-api/` 前缀转发至隔离测试 Java；测试数据库、Redis、MinIO 和日志卷保持独立。
- `compose.remote-dev.yml` 改为仅在 `.env.remote-dev` 显式设置 `REMOTE_DEV_MEMBERSHIP_PAYMENT_ENABLED=true` 时开启会员支付；当前测试会员支付已开启，商城和商城支付仍关闭，与生产当前开关一致。测试开发登录和会员模拟开关仍保留为测试专用差异。
- 测试 Java 容器已强制重建并 healthy；容器内支付配置项和两份证书均可由运行用户读取，会员摘要接口返回 `paymentEnabled=true`。本次未创建微信支付订单、未发起扣款。
- 本次变更前备份位于 `/opt/zhiyu-health/backups/remote-dev-shared-payment-before-20260829-111549`。真实支付验收必须使用小额订单，并单独核对微信回调、订单状态和退款流程；生产环境未重启、未改配置。

## 2026-08-29 修复隔离测试微信手机号登录配置

- 根因：隔离测试 `.env.remote-dev` 中 `WECHAT_APP_SECRET` 为空，真实微信登录请求在服务端未进入微信校验就返回 `10202 / 微信小程序身份服务尚未配置`；不是手机号组件或支付配置导致。
- 已从生产受限 `.env` 同步小程序 AppSecret 到测试受限 `.env.remote-dev`，测试 AppID 保持不变；测试开发身份开关仍保留，真实授权手机号登录不改为 Mock。
- 已强制重建测试 Java 容器并确认 healthy；运行时 AppSecret 可用但未打印。无效凭证安全探针返回 `10201 / 微信登录凭证校验失败，请重试`，不再返回 `10202`，说明身份服务配置已生效。
- 本次备份位于 `/opt/zhiyu-health/backups/remote-dev-wechat-auth-before-20260829-113413`；生产容器和生产配置未修改。

## 2026-08-29 Qwen 模型统一切换为 qwen3.8-flash

- 按用户要求，Qwen OCR、Vision 和健康助手的默认模型统一改为精确模型 ID `qwen3.8-flash`；移除了活动代码、测试和当前 README 中的 `qwen3.7-flash`（含旧日期后缀）引用。
- `qwen3.7-plus` 在当前项目源码、Compose 配置和文档中未发现引用，因此没有额外替换项。Qwen3.5-OCR 仍作为 PDF 失败页的独立备用模型保留。
- 已更新 `compose.yml`、根目录和 AI 服务 `.env.example`、Python 默认值、Java 健康助手固定值及相关回归断言；本机 `.env` 仅将 `QWEN_OCR_MODEL` 与 `QWEN_VISION_MODEL` 改为新模型，未读取、打印或修改任何密钥。
- 当前仅完成源码、默认配置和本机模型字段更新，未重建或重启线上/隔离测试 AI 容器。本机 Docker 引擎当前未运行；部署环境的 `.env` 若显式设置了 `QWEN_OCR_MODEL`、`QWEN_VISION_MODEL` 或 `QWEN_ASSISTANT_MODEL`，仍需在受限部署配置中将对应值改为 `qwen3.8-flash` 后重建 AI/Java 容器并做真实请求验收。

## 2026-08-29 统一普通客户会员权益用尽提示

- 后端仍统一以 `60401 / MEMBERSHIP_BENEFIT_NOT_AVAILABLE` 拦截权益不足；新增小程序 `src/utils/membership.ts`，统一弹出“权益次数已用完”会员引导，并提供“开通会员”与“暂不”选项。
- 已覆盖 AI 健康助手、AI 健康评估/报告（含异步失败报告和重新评估）、健康拍、吃饭/睡眠语音提醒（页面试听与后台定时试听）、首次健康随访和持续健康随访。初始随访因额度不足而被自动跳过时，随访列表会根据报告和权益状态补充提示。
- 前端只展示后端返回的额度错误，不绕过会员限制；跳转目标为现有 `/pages-customer/member/subscribe` 开通页。非额度错误仍保留原有网络或服务失败提示。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev` 和 `npm run build:mp-weixin`；H5、微信开发包、微信生产局域网包均已重新同步。此次仅更新本地源码和构建产物，未自动部署线上或上传新的微信版本。

## 2026-08-29 生产与隔离测试同步 qwen3.8-flash

- 按用户确认，将线上生产和服务器隔离测试两套运行环境中活动配置的 `qwen3.7-flash`（含日期后缀）统一替换为 `qwen3.8-flash`；未发现 `qwen3.7-plus`。生产原本显式使用的 OCR 专用 `qwen3.5-ocr` 保持不变，作为独立 OCR 配置。
- 已先在服务器创建回滚备份 `/opt/zhiyu-health/backups/qwen38-20260829-153812`，随后只修改 Compose 默认值、Qwen Vision 默认值、健康助手 Java 模型常量及两套受限环境中对应模型字段；未输出或写入交接文档任何密钥。
- 已分别使用生产 Compose 与 `rayk-remote-dev` Compose 重建 `rayk-ai`、`rayk-server`（未停止或删除 MySQL、Redis、MinIO 数据卷）。生产助手/视觉运行时为 `qwen3.8-flash`，测试助手/OCR/视觉运行时均为 `qwen3.8-flash`；Java 健康助手常量已核对为新模型。
- 重建后生产和隔离测试的 Java、AI、Nginx、MySQL、Redis、MinIO 均 healthy；生产 `/health` 与测试网关 `/health` 均返回 HTTP 200。真实 AI 请求和支付扣款未在本次部署中发起。

## 2026-08-29 隔离测试 OCR 与生产配置对齐

- 根据用户复核，隔离测试 OCR 不应使用通用 `qwen3.8-flash`，已恢复为生产使用的专用 `qwen3.5-ocr`；测试助手和视觉仍保持 `qwen3.8-flash`。
- 已更新 `compose.remote-dev.yml` 的测试覆盖配置，并在服务器受限 `.env.remote-dev` 显式写入 `QWEN_OCR_MODEL=qwen3.5-ocr`；生产配置未改动。
- 已备份测试原配置至 `/opt/zhiyu-health/backups/remote-dev-ocr-rollback-20260829-154642`，仅重建隔离测试 `rayk-ai` 容器。测试 AI、Java、Nginx、MySQL、Redis、MinIO 均 healthy；生产和测试健康检查仍返回 HTTP 200。两套 AI 容器运行时模型已核对一致为：助手/视觉 `qwen3.8-flash`，OCR `qwen3.5-ocr`。

## 2026-08-29 生产与隔离测试全量更新重启

- 按用户确认，已先备份当前 Compose、Nginx 和受限环境配置至 `/opt/zhiyu-health/backups/full-restart-before-20260829-155143`，随后分别对生产和 `rayk-remote-dev` 执行 `docker compose up -d --build --force-recreate`；未执行 `down -v`、未删除任何数据卷。
- 生产与隔离测试的 Java、AI、Nginx、MySQL、Redis、MinIO 全部重建/重启后均为 healthy；生产和测试健康检查均返回 HTTP 200。
- 两套 AI 容器运行时模型仍一致：助手/视觉 `qwen3.8-flash`，OCR `qwen3.5-ocr`。生产与测试数据卷均保留，未发起真实支付扣款或额外数据迁移。

## 2026-08-29 帮助与反馈页同步线上与隔离测试端

- 根因：帮助与反馈页删除登录 FAQ、放大答案字体的修改已存在于源码和本地构建，但线上 Nginx 挂载的 H5 目录仍是旧静态包，所以线上继续显示“如何登录并识别身份？”。提交反馈按钮及其接口逻辑未因本次同步而改变。
- 已重新通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin` 和 `npm run build:mp-weixin:dev:remote-test`。H5、微信开发包、生产局域网包和隔离测试包均确认不含旧 FAQ，并保留新的三个 FAQ；答案字体规则仍为中老年适配字号。
- 已先备份线上 H5 至 `/opt/zhiyu-health/backups/help-feedback-h5-before-20260829-161526/h5.tgz`，再同步到 `/opt/zhiyu-health/rayk-miniapp/dist/build/h5`。线上实际访问的 `pages-support-index.BxsMi4Cl.js` 已核对旧 FAQ 不存在、新 FAQ 存在。
- 已按生产 Compose 重载线上 Nginx；生产 Java、AI、Nginx、MySQL、Redis、MinIO 均 running，`https://xingxuyuan.com/health` 返回 HTTP 200。隔离测试各服务均 running，`https://xingxuyuan.com/test-api/health` 返回 HTTP 200。
- 隔离测试微信包已生成到 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`，请求地址核对为 `https://xingxuyuan.com/test-api`；该包需在微信开发者工具中重新导入或编译，未上传到同一 AppID，避免测试接口进入线上体验包。生产配置包已按 AppID `wxf6f4549c8c962948` 上传为微信开发/体验版本 `2026.08.29`，上传成功但未提交审核或发布。

## 2026-08-29 修复帮助与反馈提交按钮 UI

- 根因：帮助与反馈页提交按钮只设置了颜色和圆角，未覆盖微信原生按钮的默认宽度、内边距、边框和伪元素样式，导致出现白色外壳包裹绿色按钮的错位 UI；点击事件和后端提交接口本身无需改动。
- 已为 `src/pages/support/index.vue` 的提交按钮补齐全宽、固定高度、Flex 居中、内边距归零、边框/伪元素重置和轻量阴影；同步保留旧登录 FAQ 删除和答案字号调整。
- 已通过 `npm run type-check`、`npm run lint`、`npm run build:h5`、`npm run build:mp-weixin:dev`、`npm run build:mp-weixin:dev:remote-test` 和 `npm run build:mp-weixin`。四个前端输出目录均已生成新按钮样式，且不含旧登录 FAQ。
- 已先备份线上 H5 至 `/opt/zhiyu-health/backups/help-feedback-button-ui-before-20260829-164749/h5.tgz`，再更新生产与隔离测试网关共同挂载的 H5 目录。公网实际加载的 `index-Dz4Rolo-.css` 已包含全宽和固定高度规则，`/health` 返回 200；临时上传文件已清理。
- 已将生产配置微信包上传至现有 AppID 的开发/体验版本 `2026.08.29.1`，描述为 `fix-support-feedback-button-ui-20260829`，上传成功但未提交审核或发布。隔离测试包仍在 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`，需在开发者工具中重新导入/编译，避免测试接口进入生产体验包。
## 2026-09-06 推荐奖励收款状态文案

- 将待用户确认的推荐奖励状态文案由“等待你确认收款”统一调整为“等待确认收款”。
- 已重新生成前端 H5、微信开发包、线上隔离测试包和生产局域网包。
## 2026-09-06 删除推荐奖励自动收款提示卡片

- 删除“推荐奖励自动收款”独立提示卡片，自动收款入口继续保留在推荐奖励收款列表第一条记录的操作区，与“确认收款/刷新状态”并排显示。
- 已重新生成前端 H5、微信开发包、线上隔离测试包和生产局域网包。
## 2026-09-06 推荐奖励收款状态去重

- 推荐奖励待确认时，业务状态与微信转账状态相同的“等待确认收款”只显示一次；其他不同状态仍保留状态补充信息。
- 已重新生成前端 H5、微信开发包、线上隔离测试包和生产局域网包。

## 2026-09-06 金豆集市改为虚拟支付并接通卖家结算

- 金豆集市 `GBT...` 买单现在调用已配置的微信虚拟道具 `gold_bean`，按 `buyQuantity` 传递购买数量；虚拟支付回调会校验订单、商品、数量、单价、环境、商户号、买家 OpenID 和交易号，未通过校验不会交割金豆。
- 买家支付确认后，Java 服务使用新版“商家转账到零钱”接口向卖家微信 OpenID 结算，新的转账单使用固定 `out_bill_no` 幂等重试；`SUCCESS` 才扣卖家对应账本并给买家入账，`WAIT_USER_CONFIRM` 会把确认参数展示给卖家，失败/处理中保留订单状态并由定时补偿恢复。旧版已有的 `D` 批量转账单继续走旧查询路径，避免重复打款。
- 新增 `V61__gold_member_trade_transfer_reconciliation.sql` 保存微信转账状态、收款确认参数、查单时间和下次重试时间；新增卖家收款查询/查单接口 `/api/client/gold-bean/trade/payouts` 和 `/api/client/gold-bean/trade/payouts/{tradeNo}/sync`，小程序增加“集市卖家收款”卡片，支持确认收款、刷新状态和已到账展示。
- 本次已在远端 Java 21 Docker 构建链通过 126 项 Maven 测试，V61 已在 `rayk_health_remote_dev` 执行，隔离 Java 容器当前 healthy；前端 `type-check`、ESLint、H5、微信开发包、生产局域网包和远程隔离测试包均已生成，远程隔离 H5 已同步到 `h5-remote-dev`。
- 远程隔离环境在本次后续配置前仍是安全默认值：`GOLD_BEAN_PRODUCT_ID=gold_bean` 已注入，虚拟支付回调地址已配置，但 `REMOTE_DEV_GOLD_BEAN_TRADE_PAYMENT_ENABLED` 未开启、`REMOTE_DEV_GOLD_BEAN_TRADE_TRANSFER_SCENE_ID` 未配置，因此当时没有发起真实卖家转账。要做真实联调，还需在服务器受限 `.env.remote-dev` 配置已在商户平台审核通过的金豆交易转账场景 ID，并显式开启交易支付开关；不能用普通微信 JSAPI 或只配置虚拟道具替代商家转账能力。

## 2026-09-06 启用金豆集市“佣金报酬”转账场景（线上隔离开发环境）

- 按用户确认，线上隔离开发环境复用微信商户平台“佣金报酬”转账场景 ID `1005`，用于金豆集市买家虚拟道具支付成功后的卖家商家转账；只修改受限的 `.env.remote-dev`，修改前备份位于 `/opt/zhiyu-health/.codex-backups/trade-chain-config-before-scene1005-20260906-122500/.env.remote-dev`。
- 已启用 `REMOTE_DEV_GOLD_BEAN_TRADE_PAYMENT_ENABLED=true`、`REMOTE_DEV_GOLD_BEAN_TRADE_TRANSFER_SCENE_ID=1005`，重建隔离 `rayk-server` 后核对容器实际环境为 `GOLD_BEAN_PRODUCT_ID=gold_bean`、交易开关 `true`、场景 `1005`；容器 healthy，`https://xingxuyuan.com/test-api/health` 返回 `status=UP`。V61 已执行，生产容器、生产数据库和生产静态资源未修改。
- 本次部署没有创建订单、发起真实支付或转账。联调时应使用新金豆集市订单：买家完成 `gold_bean` 虚拟支付后，服务端才发起卖家转账；若微信返回 `WAIT_USER_CONFIRM`，卖家需在小程序“集市卖家收款”中确认，只有最终 `SUCCESS` 才会给买家交割金豆。

## 2026-09-06 金豆集市按注册区域城市限制交易

- 新增 Flyway `V62__gold_member_trade_region_scope.sql`：挂单增加 `region_city` 区域快照，并按卖家已注册城市回填历史挂单；新挂单必须有注册区域城市，避免出现无区域归属的普通挂单。
- 普通客户的 `/api/client/gold-bean/trade/market` 只返回与本人注册区域城市一致的挂单，`/trade/listings/{listingId}/buy` 也在服务端再次校验区域，跨区域请求返回“普通会员只能购买本注册区域的金豆挂单”；未完成城市注册的普通客户返回区域必填错误。
- 传奇资格客户不增加区域过滤，可购买全国各地挂单；继续保留既有规则：传奇只能购买数字银行金豆，普通客户只能购买可交易金豆。小程序集市顶部和每个挂单显示当前/挂单区域，明确告知用户交易范围。
- 远程 Java 21 Docker 构建完整 Maven 测试 `128` 项通过，V62 已在 `rayk_health_remote_dev` 执行，隔离容器 healthy，`https://xingxuyuan.com/test-api/health` 返回 `status=UP`。前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新生成；生产环境未修改，未创建真实订单。

## 2026-09-06 机器人权益入口与余额不足提示

- 机器人权益入口不再因为数字银行余额未满 10000 或服务群二维码未配置而置灰；已注册、状态有效且尚未兑换的客户可以点击入口，服务端仍在事务内校验余额、群配置和一次性兑换条件。
- 数字银行余额不足时新增错误码 `60744`，小程序弹出“数字银行金豆余额不足，请先积累至10000金豆后再兑换”，不扣除任何金豆；兑换成功后原有幂等扣豆、兑换记录和企业微信群二维码弹窗逻辑不变，按钮显示“已兑换”。
- 线上隔离 `rayk-remote-dev` 已重建并 healthy，`https://xingxuyuan.com/test-api/health` 返回 `status=UP`；远程 Java 21 Docker 构建完整 Maven 测试 `129` 项通过。当前隔离环境的服务群二维码变量仍为空，因此余额不足账号可验证提示，余额达到 10000 前仍需配置真实企业微信群活码才能完成兑换；生产环境未修改。
- 前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新生成，验收优先使用 `E:\health\rayk-miniapp\dist\release\mp-weixin-dev-remote-test`。

## 2026-09-06 机器人权益卡片视觉优化

- 移除机器人权益卡片内嵌的“数字银行金豆余额不足，还需多少豆；点击兑换会提示余额不足”动态文案；余额不足提示仍保留在点击兑换后的中文弹窗中，不改变服务端扣豆前校验和不扣豆约束。
- 兑换按钮由深色金色改为浅色品牌绿色、深色文字、轻边框和按压态，增加与浅金色卡片的层次协调；兑换完成后的“已兑换”状态不变。
- 本次前端类型检查、Lint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新生成；生产环境未修改。

## 2026-09-07 全局金豆统一支持六位小数

- 新增 Flyway `V65__gold_bean_decimal_amounts.sql`，将账户余额、金豆流水、平台购豆订单、集市挂单/成交数量、区域返利和机器人兑换成本统一迁移为 `DECIMAL(24,6)`；历史整数值会自动转换为带六位小数的定点值，人民币金额字段仍按分保存。
- Java 账本、平台购豆、传奇人物购豆、集市交易、区域返利、卖家结算和机器人兑换统一使用 `BigDecimal`；金额计算固定 6 位小数，双账本拆分不丢失小数，虚拟支付商品仍按微信要求编码为整数数量并对小数订单使用“单个商品、总价”表达。
- 小程序新增统一金豆格式化和输入校验，平台购豆、集市发布/购买、余额、流水、返利和管理员统计均支持最多 6 位小数，最小输入为 `0.000001` 金豆。
- 本机 Java 21 完整 Maven 测试全部通过；前端 `type-check`、ESLint、H5、微信开发包、远程隔离测试包和生产局域网包均已重新生成。尚未在生产数据库执行 V65、未创建真实小数支付订单；上线前需先迁移数据库并核对微信虚拟支付回调、卖家转账和账本对账。

## 2026-09-07 删除平台购豆数量辅助文案

- 删除金豆会员“向平台购买金豆”数量输入框下方的辅助文案；购买数量输入、金额计算、支付和到账逻辑不变。
- 已重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；生产环境未修改。

## 2026-09-07 修复推荐码复制失败

- 推荐码复制统一规范为字符串，并增加跨端剪贴板处理：微信小程序优先调用微信原生剪贴板 API，其他小程序运行时回退到 `uni.setClipboardData`，H5 使用浏览器剪贴板并提供 textarea 兼容方案。
- 复制成功仍提示“推荐码已复制”，失败才提示“推荐码复制失败”；推荐码生成、展示和推荐关系逻辑不变。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；生产环境未修改。

## 2026-09-07 补充微信剪贴板隐私授权处理

- 针对微信开发者工具仍返回剪贴板失败的问题，复制前增加 `wx.requirePrivacyAuthorize`；复制失败会保留微信返回原因，并对未声明隐私范围的情况提示管理员配置“剪贴板”。推荐码文本支持长按选择复制作为兜底。
- 微信公众平台必须在“用户隐私保护指引”中声明剪贴板用途并生效；该项属于微信后台配置，不能由本地代码绕过。配置完成后请重新打开开发者工具并重新导入远程测试包。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；生产环境未修改。

## 2026-09-07 推荐码改为长按复制提示

- 移除金豆会员推荐码右侧的“复制推荐码”按钮和自动剪贴板调用，改为显示“长按推荐码即可复制”；推荐码文本保留可选择能力，推荐码生成和推荐关系逻辑不变。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；生产环境未修改。

## 2026-09-08 平台金豆运营界面紧凑化与中文显示

- “最近授权码”默认折叠，标题显示记录数量并支持展开/收起，授权码生成、撤销逻辑不变。
- 平台会员账户、推荐关系、金豆流水和支付订单显示最新健康资料姓名；无资料时回退账号昵称，管理员重新进入或刷新后生效。
- 金豆流水类型统一显示中文，覆盖初始金豆、每日奖励、推荐奖励、等级奖励、平台/传奇购豆、机器人权益和集市买入/卖出等类型；未知类型显示“其他金豆流水”。
- 已通过前端 `type-check`、ESLint，并重新生成 H5、微信开发包、远程隔离测试包和生产局域网包；本机未安装 Maven 且 Docker 未运行，后端 Java 测试未执行，生产环境未修改。

## 2026-09-08 会员注册费调整为 1000 元

- 平台注册码注册和推荐码注册的业务/结算基准统一调整为 1000 元（100000 分）；12% 虚拟支付加价后的用户应付金额统一为 1120 元（112000 分）。
- 推荐码注册支付成功后，商家转账仍读取订单业务金额，因此推荐人实际收款为 1000 元，不会收到加价部分；已支付账户、已创建订单和历史推荐结算金额保持不变。
- 新增 Flyway `V66__gold_registration_fee_1000.sql`，更新数据库默认值，并将未注册账户的待注册费用切换为 1000 元。微信后台对应的平台注册/推荐注册虚拟商品仍需分别配置为 1120 元，商品 ID 保持不变。
- 本次仅修改代码、默认配置和迁移文件，已通过前端 `type-check`、ESLint、H5、微信开发包和生产局域网包构建，并同步 `dist/build/h5`、`dist/release/mp-weixin-dev`、`dist/release/mp-weixin-prod-lan`；未执行数据库迁移或真实支付，未修改生产环境。后端 Java 测试因本机未安装 Maven 且 Docker 未运行暂未执行。

## 2026-09-08 健康树洞七天反馈后台定时任务与总结卡片

- 新增 `HealthTreeHoleFeedbackScheduler`，默认每天北京时间 02:15 扫描已有树洞记录的客户；通过显式租户上下文和客户本人身份执行，服务端继续按本人数据范围读取，避免要求客户打开页面才能生成反馈。
- 新增 `generateTreeHoleFeedbackIfDue(patientId)` 后台入口：只处理已完成七天周期且当前周期尚未生成反馈的客户，沿用 `AI_HEALTH_TREE_HOLE` 权益预占/确认/失败释放、AI 空结果失败和数据库唯一约束；免费体验已结束等预期状态不会刷满错误日志。定时任务可用 `RAYK_HEALTH_TREE_HOLE_FEEDBACK_CRON` 覆盖默认时间。
- 健康树洞“树洞记录”入口下方恢复“七天反馈总结”卡片，支持同步中、记录进度、待整理、已生成、风险提醒、下一周小行动、隐私说明和失败重试；页面进入/返回时只刷新状态，不再把页面访问作为主要生成触发器。
- 已通过 Java 21 Maven 编译和 `-DforkCount=0 test`（156 项通过），前端 `type-check`、ESLint、H5、微信开发包和生产局域网包均通过并已同步；本次未执行数据库迁移或真实 AI/微信端跨日验收，生产环境未修改。

## 2026-09-08 远程隔离测试包重新同步

- 针对微信开发者工具仍打开旧包的问题，重新执行 `build:mp-weixin:dev:remote-test`，已将最新树洞页面同步到 `rayk-miniapp/dist/release/mp-weixin-dev-remote-test`。
- 已核对包内 `pages-customer/medical-assistant/index.wxml` 包含“七天反馈总结”；本次后续重建后的远程测试包目录更新时间为 2026-09-08 11:10:42。微信开发者工具仍可能保留旧编译缓存，导入同一目录后需点击“编译”，必要时执行“清缓存并重新编译”。

## 2026-09-08 修复首页档案完整度动态刷新

- 首页概览请求增加时间戳参数，避免 GET 缓存继续显示旧完整度；客户首页同时读取最新健康档案接口，以服务端刚计算的 `profileCompleteness` 覆盖概览卡片数值。
- 增加刷新序列号，页面返回、定时刷新和手动刷新并发时只接受最后一次请求，旧请求不能覆盖新数据；页面隐藏时会使未完成请求失效。
- 已通过前端 `type-check`、ESLint、H5、微信开发包、微信生产局域网包和远程隔离测试包构建；四套产物均已重新同步，生产环境未修改。

## 2026-09-08 远程隔离测试包纳入每次前端交付

- 后续每次修改 `rayk-miniapp` 前端后，必须执行 `npm run build:mp-weixin:dev:remote-test`，将最新源码重新生成到 `dist/release/mp-weixin-dev-remote-test`；该包与 H5、开发包、生产局域网包一样纳入交付核对。
- 本次已重新构建该包，首页与接口文件均已和 `dist/build/mp-weixin` 逐文件核对哈希一致；微信开发者工具仍需点击“编译”，必要时清缓存并重新编译。

## 2026-09-08 修复档案完整度仍显示 0%

- 根因：服务端原完整度只统计 24 项健康问卷；用户已经填写的姓名、性别、出生日期属于 `health_patient`，未被计入，因此截图中的档案详情会显示 0%，不是首页请求缓存问题。
- 服务端展示口径现统一为姓名、性别、出生日期 + 24 项健康问卷，共 27 项；读取和保存健康档案时都会按患者最新身份资料重新计算。前端首页、档案详情和编辑页预览使用同一口径，远程测试包在旧服务端尚未重启时也不会把已填写身份显示成 0%。
- 无数据库结构变化，不修改或重排已执行 Flyway 迁移；未部署生产后端、未创建生产数据。交付前需通过 Java 测试、前端检查，并重新生成 H5、微信开发包、`mp-weixin-dev-remote-test` 和生产局域网包。
