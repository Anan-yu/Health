$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$backupRoot = if ($env:RAYK_BACKUP_DIR) { $env:RAYK_BACKUP_DIR } else { 'E:\DockerData\RayKA1\backups\mysql' }
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backupFile = Join-Path $backupRoot "rayk_health_$stamp.sql"
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers "$MYSQL_DATABASE"' | Out-File -LiteralPath $backupFile -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'mysqldump 备份失败' }
Write-Host "备份已创建：$backupFile"

