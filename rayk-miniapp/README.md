# RayK A1 统一小程序

同一套 uni-app 源码承载 B 端机构人员和 C 端客户。登录后根据角色、权限和当前工作台生成卡片式功能入口；服务端仍是权限判断的最终依据。

```bash
npm install
npm run dev:h5
npm run type-check
npm run build:h5
npm run build:mp-weixin
```

微信开发者工具导入 `dist/build/mp-weixin`。当前使用 `touristappid`，接入真实微信登录前替换为正式 AppID。

