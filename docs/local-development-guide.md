# 智能三羊本地开发启动指南


## 一、开始前的准备

### 必需软件

- Git
- Windows 10/11
- Docker Desktop，建议使用 WSL2 后端
- Node.js 和 npm（建议使用当前 LTS 版本）
- PowerShell 5.1 或更高版本

Java、Python、MySQL、Redis、MinIO、Nginx 都由 Docker Compose 启动，普通本地联调不需要单独安装 Java 或 Python。

建议 Docker Desktop 的磁盘镜像、项目目录和 Docker 数据放在 E 盘。项目根目录固定为 E:\health。

## 二、拉取代码

将 <仓库地址> 替换成实际 Git 仓库地址：

~~~powershell
Set-Location E:\
git clone <仓库地址> health
Set-Location E:\health
git status --short
~~~

如果团队使用指定分支，请在拉取后切换：

~~~powershell
git fetch --all
git switch <分支名>
git pull --ff-only
~~~

不要使用 git reset --hard 覆盖其他人的本地修改。

## 三、创建本地环境配置

首次运行从模板创建 .env：

~~~powershell
Set-Location E:\health
Copy-Item .env.example .env
~~~

.env 已被 Git 忽略，只保存在本机。可以根据需要修改数据库密码、端口和外部服务开关；不要把真实 API Key、微信 Secret 或生产密码提交到 Git。

本地默认配置已经开启开发联调所需的 Mock 微信开关。DeepSeek、Qwen OCR、健康拍和腾讯云 TTS 默认关闭，不配置外部密钥也可以启动基础功能和三角色调试。

## 四、启动 Docker 开发环境

先启动 Docker Desktop，确认状态为 Running，然后在项目根目录执行：

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
~~~

等价命令：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build
~~~

第一次启动会下载基础镜像、安装 Java/Python 依赖，并可能下载 PaddleOCR 模型，耗时较长。检查状态：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml ps
~~~

MySQL、Redis、MinIO、rayk-ai、rayk-server、Nginx 均应显示 healthy。首次加载 OCR 模型时可以等待几分钟，再查看状态。

查看日志：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml logs -f rayk-ai rayk-server nginx
~~~

## 五、启动和访问 H5

### 方案 A：Docker Nginx 静态 H5

本地 H5 必须使用开发构建，才能显示三角色调试入口：

~~~powershell
Set-Location E:\health\rayk-miniapp
npm ci
npm run type-check
npm run lint
npm run build:h5:dev
~~~

打开：

http://localhost:8088/#/pages/login/index

如果仍看到“请在微信中使用”，说明浏览器缓存了旧的生产构建。关闭页面重新打开，或按 Ctrl + F5 强制刷新。

本地 H5 在 localhost 或 127.0.0.1 下会自动使用当前页面的同源 API，因此不依赖旧的局域网 IP。

### 方案 B：H5 热更新开发服务器

需要频繁修改前端时，可以使用 UniApp 开发服务器：

~~~powershell
Set-Location E:\health\rayk-miniapp
npm run dev:h5
~~~

打开终端提示的地址，通常是：

http://localhost:5173/#/

该方式支持前端热更新，API 请求通过 Vite 代理转到本地 Docker 服务。停止时在终端按 Ctrl + C。

## 六、三个角色的本地调试

在 H5 登录页向下查看“开发调试身份”卡片，选择角色后点击“进入智能三羊”：

| 角色 | 用途 |
| --- | --- |
| 平台管理员 | 合作医院、医生预录入、全平台报告和随访管理 |
| 医生 | 体检者查询、报告查看和医生工作台 |
| 客户 | 个人健康档案、报告、健康检测和随访 |

页面会根据当前开发配置自动填充开发账号和密码。这里的账号只用于本地 Mock 登录，不代表生产账号；如果本地数据库被重新初始化，以登录页当前预填值和数据库初始化数据为准。

也可以直接检查 Mock 登录接口（将占位符替换为当前登录页显示的本地开发密码）：

~~~powershell
$body = @{ username = 'doctor'; password = '<当前开发密码>' } | ConvertTo-Json
Invoke-WebRequest -Uri 'http://localhost:8088/api/v1/auth/mock-login' -Method Post -ContentType 'application/json' -Body $body
~~~

## 七、微信开发者工具联调

构建微信开发包：

~~~powershell
Set-Location E:\health\rayk-miniapp
npm run build:mp-weixin:dev
~~~

在微信开发者工具中导入：

E:\health\rayk-miniapp\dist\build\mp-weixin

也可以导入已同步的：

E:\health\rayk-miniapp\dist\release\mp-weixin-dev

开发者工具联调时按需勾选“不校验合法域名、TLS 版本及 HTTPS 证书”。真机不能访问 127.0.0.1，必须使用电脑的局域网 IP；手机和电脑需要在同一个 Wi-Fi，且 Windows 防火墙允许 8088 端口。

## 八、前端修改后的同步构建

修改 rayk-miniapp 前端后，至少执行：

~~~powershell
Set-Location E:\health\rayk-miniapp
npm run type-check
npm run lint
npm run build:h5:dev
npm run build:mp-weixin:dev
Copy-Item -LiteralPath dist\build\mp-weixin\* -Destination dist\release\mp-weixin-dev -Recurse -Force
npm run build:mp-weixin
Copy-Item -LiteralPath dist\build\mp-weixin\* -Destination dist\release\mp-weixin-prod-lan -Recurse -Force
~~~

三个输出目录分别是：

- E:\health\rayk-miniapp\dist\build\h5
- E:\health\rayk-miniapp\dist\release\mp-weixin-dev
- E:\health\rayk-miniapp\dist\release\mp-weixin-prod-lan

mp-weixin-prod-lan 仅用于局域网验收，不是可直接提交微信审核的正式生产包。

## 九、后端代码修改后的处理

Python AI 服务在开发 Compose 中挂载了 rayk-ai/app，修改 Python 后重启 AI 容器：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml up -d --no-deps rayk-ai
~~~

Java 服务需要重新构建镜像：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml up -d --build rayk-server
~~~

修改 Dockerfile、依赖或基础镜像时，重新构建对应服务。

## 十、停止服务和保留数据

停止开发环境但保留 MySQL、Redis、MinIO 和 OCR 模型数据：

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev.ps1
~~~

再次启动：

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
~~~

禁止使用 docker compose down -v 作为日常停止命令，该命令会删除持久化数据卷。

## 十一、常见问题

### 1. 浏览器显示连接被拒绝

~~~powershell
docker info
docker compose -f compose.yml -f compose.dev.yml ps
~~~

如果 Docker 未运行，先启动 Docker Desktop；如果 Nginx 未启动，查看 rayk-ai 和 rayk-server 日志。确认 8088 端口没有被其他程序占用。

### 2. H5 只有“请在微信中使用”

这是生产构建的登录页。执行 npm run build:h5:dev，再强制刷新浏览器；不要使用 npm run build:h5 生成本地调试 H5。

### 3. 登录提示网络连接失败

- H5：确认访问 localhost:8088 或 localhost:5173，并确认 Docker 中 Java/Nginx 为 healthy。
- 微信开发者工具：确认导入的是 mp-weixin-dev，并核对开发 API 地址和“不校验合法域名”设置。
- 真机：使用电脑 LAN IP，不要使用 127.0.0.1 或 localhost。

### 4. AI 服务长时间不健康

首次 PaddleOCR 模型下载可能需要较长时间：

~~~powershell
docker compose -f compose.yml -f compose.dev.yml logs -f rayk-ai
~~~

模型保存在命名卷中，后续启动会复用。不要删除 rayk_ocr_models_data 卷。

### 5. 修改后页面仍是旧版本

确认重新执行了对应构建命令，检查 dist\build\h5\index.html 的修改时间，然后在浏览器执行 Ctrl + F5。微信开发者工具中使用“编译”或清除缓存后重新编译。

## 十二、安全提醒

- .env、数据库密码、微信 Secret、DeepSeek/Qwen/TTS/健康拍密钥不得提交 Git。
- 不要把生产环境账号密码写入代码、文档或截图。
- 健康数据属于敏感个人信息，测试数据也应按最小范围使用。
- AI 结果仅用于健康管理和辅助参考，不替代医生诊断。
- 发现已有凭据曾在聊天或截图中暴露时，应在上线前轮换。
