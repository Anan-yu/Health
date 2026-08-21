$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
    Write-Warning '已从 .env.example 创建开发 .env。正式部署前必须更换全部密码和 JWT_SECRET。'
}
docker compose -f compose.yml -f compose.dev.yml up -d --build
# Docker Desktop 在 WLAN/WSL 网络切换或后端服务重建后，可能保留 Nginx 的旧端口转发状态。
# 每次通过启动脚本进入开发环境时只重建网关容器，避免 8088 建立连接后直接断开；业务容器和数据卷不受影响。
docker compose -f compose.yml -f compose.dev.yml up -d --force-recreate nginx
$nginxPort = 8088
$nginxPortLine = Get-Content -LiteralPath '.env' -ErrorAction SilentlyContinue |
    Where-Object { $_ -match '^NGINX_PORT=\d+$' } | Select-Object -First 1
if ($nginxPortLine -match '^NGINX_PORT=(\d+)$') { $nginxPort = [int]$Matches[1] }
$health = Invoke-WebRequest -Uri "http://127.0.0.1:$nginxPort/health" -UseBasicParsing -TimeoutSec 10
if ($health.StatusCode -ne 200) { throw "Nginx 网关健康检查失败：HTTP $($health.StatusCode)" }
docker compose -f compose.yml -f compose.dev.yml ps
