# 部署目录

`nginx/nginx.conf` 是统一入口配置。生产环境应通过 `.env` 注入强密码，并在入口前增加 HTTPS、WAF 和可信代理配置。

