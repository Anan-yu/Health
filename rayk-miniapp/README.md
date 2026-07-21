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

微信小程序联调时，运行 `npm run dev:mp-weixin` 并保持进程运行，然后让微信开发者工具导入 `dist/dev/mp-weixin`。一次性构建的默认输出目录是 `dist/build/mp-weixin`，不要在开发者工具中混用两个目录，否则真机可能继续运行旧的 API 地址。

登录页已接入 `uni.login`，报告页已接入真实 PDF/JPG/PNG 文件选择与上传进度。当前使用 `touristappid` 进行本机开发；发布前需要替换正式 AppID，并配置 API 和对象存储的 HTTPS 合法域名。

## 当前微信双包

- `dist/release/mp-weixin-dev`：development 开发联调包，API 为 `http://192.168.0.100:8088`，保留五角色开发登录。
- `dist/release/mp-weixin-prod-lan`：production 优化的局域网验收包，API 同样为 `http://192.168.0.100:8088`，按当前验收要求保留开发登录。
- `dist/build/mp-weixin`：同步为当前 development 联调包，便于原有微信开发者工具项目继续使用。

这两份包用于同一 Wi-Fi 下的开发和验收。`mp-weixin-prod-lan` 虽然使用 production 构建优化，但仍包含 HTTP 局域网地址和开发身份入口，**不能直接提交微信审核或正式上线**。

正式发布前必须把 `VITE_API_BASE_URL` 改成已备案的 HTTPS 合法域名，把 `VITE_ENABLE_DEVELOPMENT_LOGIN` 设为 `false`，关闭后端模拟微信登录，再重新执行 `npm run build:mp-weixin`。
