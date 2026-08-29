# RayK A1 统一小程序

同一套 uni-app 源码承载 B 端机构人员和 C 端客户。登录后根据角色、权限和当前工作台生成卡片式功能入口；服务端仍是权限判断的最终依据。

```bash
npm install
npm run dev:h5
npm run dev:mp-weixin
npm run type-check
npm run build:h5
npm run build:mp-weixin
```

微信小程序联调时，运行 `npm run dev:mp-weixin` 并保持进程运行，然后让微信开发者工具导入 `dist/dev/mp-weixin`。一次性构建开发包请运行 `npm run build:mp-weixin:dev`，脚本会强制读取 `.env.development`，避免误加载生产域名。默认输出目录是 `dist/build/mp-weixin`，不要在开发者工具中混用两个目录，否则真机可能继续运行旧的 API 地址。

登录页已接入 `uni.login` 与微信手机号快速验证。企业主体包通过授权手机号匹配客户、平台预录入医生和平台管理员；平台工作台可维护管理员手机号，登录页不再使用账号密码或绑定码。微信包已配置企业主体 AppID `wxf6f4549c8c962948`；正式发布前还需要在部署环境配置对应 AppSecret、开通手机号快速验证能力，并配置 API 和对象存储的 HTTPS 合法域名。

## 当前微信双包

- `dist/release/mp-weixin-dev`：development 开发联调包，默认 API 使用 `.env.development` 中的局域网地址，保留开发登录。
- `dist/release/mp-weixin-dev-remote`：远程 development 验收包，API 指向线上 HTTPS，关闭本地开发身份入口，使用真实微信手机号登录。
- `dist/release/mp-weixin-dev-remote-test`：服务器隔离测试包，API 使用线上域名的 `/test-api/` 前缀，开启开发身份入口，只连接隔离测试数据库。
- `dist/release/mp-weixin-prod-lan`：production 优化包，API 使用线上 HTTPS 服务 `https://xingxuyuan.com`；线上 Docker 的开发专用登录和商城支付开关仍由服务端配置控制。
- `dist/build/mp-weixin`：UniApp 临时构建目录，只能作为同步源，不要直接作为长期微信开发者工具项目目录。

如果本机 Docker 服务内存不足，可运行 `npm run build:mp-weixin:dev:remote` 生成 `dist/release/mp-weixin-dev-remote`。该包仍使用 development 构建优化，但 API 指向线上 `https://xingxuyuan.com`，并关闭仅本地可用的开发身份入口，需使用线上已授权的真实微信手机号登录；它不会覆盖 `mp-weixin-dev`，也不会修改线上容器或上传版本。恢复局域网联调时重新运行 `npm run build:mp-weixin:dev` 即可。

服务器隔离测试环境准备好后，可运行 `npm run build:mp-weixin:dev:remote-test` 生成 `dist/release/mp-weixin-dev-remote-test`。该包通过 `https://xingxuyuan.com/test-api` 访问服务器上的独立 Docker 项目，开发身份选择器可用；测试环境不接入生产数据库、支付或商城。若服务器尚未部署该环境，不要导入这个包。

开发包会员验收时，客户进入“健康会员”页面即可使用“开发调试”卡片在免费客户和年度会员之间切换；该入口只在 development 登录构建中显示，后端也会校验开发模式，生产包不会开放。

会员页面已按普通客户、年度会员和开通会员三种状态重做：页面使用会员接口返回的权益和有效期，保留原有订单、微信虚拟支付和开发调试流程；会员中心不再展示权益使用记录入口，开通页在权益对比上方展示脱敏动态播报。会员 SVG 资源放在 `src/pages-customer/static/member/`，作为 `pages-customer` 分包静态资源发布；开通页四个小组件的替换 SVG 放在该目录的 `replacements/` 下，不会进入微信主包。

检验报告上传支持同一份报告连续添加多张 PDF/图片文件；系统会逐页保存并在提交后合并 OCR，选择器单次达到平台上限时可再次点击添加，不设应用层总张数限制。

默认开发包用于同一 Wi-Fi 下的局域网联调；远程开发包和 `mp-weixin-prod-lan` 直接访问线上 HTTPS 服务。两者都不是可直接提交审核的正式互联网生产包，正式发布仍需使用关闭开发入口并经过审核配置的独立生产构建。

正式发布前必须把 `VITE_API_BASE_URL` 改成已备案的 HTTPS 合法域名，把 `VITE_ENABLE_DEVELOPMENT_LOGIN` 设为 `false`、`VITE_WECHAT_PHONE_LOGIN` 设为 `true`，关闭后端模拟微信登录，并在 Java 服务设置 `WECHAT_PHONE_LOGIN_REQUIRED=true`，再重新执行 `npm run build:mp-weixin`。如果管理员账号还没有手机号，可临时在 Java 部署环境设置 `WECHAT_PLATFORM_ADMIN_PHONE` 和 `WECHAT_PLATFORM_ADMIN_USERNAME=admin` 完成首次进入，之后在平台工作台维护手机号；真实号码不能写入源码或 Git。

首页视频播放栏代码读取 `VITE_HOME_VIDEO_URL`，可选封面图读取 `VITE_HOME_VIDEO_POSTER`；`VITE_HOME_VIDEO_ENABLED` 控制是否显示播放栏。当前生产隐藏版关闭该开关并显示原健康关怀文案卡，素材准备好后再打开并重新生成 H5 和微信包。视频地址和封面图都应配置为 HTTPS，并将视频域名加入微信小程序业务域名白名单；视频加载失败时可点击“重新加载”。仓库不提交视频文件。

本地 H5 三角色调试请使用 `npm run build:h5:dev`，然后访问 `http://localhost:8088/`；完整的首次拉取、Docker 启动、H5 和微信开发者工具步骤见根目录 `docs/local-development-guide.md`。

## 微信代码包体与性能检查

微信开发包和局域网验收包由当次构建产物完整同步生成，不能直接复制旧的 `dist` 目录。推荐执行：

```powershell
npm run build:mp-weixin:dev
npm run build:mp-weixin
```

两个构建命令都会把当次构建完整同步到对应的 release 目录，并在微信开发者工具占用目录时递归镜像清理子目录，避免旧哈希资源重复进入主包或分包。开发构建完成后还会校验 `utils/request.js` 中确实注入 `.env.development` 的 API 地址，防止失败或过期的生产产物混入开发包。会员 SVG 会在同步阶段显式复制到 `pages-customer/static/member/`，确保微信包与 H5 使用相同资源。只有确认对应模式构建已成功后才可单独运行 `npm run sync:mp-weixin:dev` 或 `npm run sync:mp-weixin:prod`；当前登录页图片均已压缩到 200 KB 以下，`pages.json` 已开启组件按需注入，`manifest.json` 已开启脚本、WXML、WXSS 压缩。

在微信开发者工具中仍需打开“详情 > 本地设置”，勾选“上传代码时自动压缩脚本文件”“上传代码时自动压缩 WXML 文件”和“上传代码时自动压缩 WXSS 文件”。代码质量扫描出现“无依赖文件”时，先确认该文件不是 `app.json/pages.json` 配置页面，再按扫描建议处理；不要把旧 release 目录继续作为项目目录使用。
