$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
docker compose -f compose.yml -f compose.dev.yml down
Write-Host '服务已停止；命名卷和业务数据已保留。'

