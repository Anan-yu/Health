param([Parameter(Mandatory = $true)][string]$BackupFile)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) { throw "备份文件不存在：$BackupFile" }
Write-Warning '恢复会向当前 rayk_health 数据库写入备份内容，不会删除命名卷。请先另做当前数据库备份。'
$confirmation = Read-Host '输入 RESTORE 确认继续'
if ($confirmation -cne 'RESTORE') { Write-Host '已取消恢复'; exit 0 }
Get-Content -LiteralPath $BackupFile -Raw -Encoding utf8 | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"'
if ($LASTEXITCODE -ne 0) { throw 'MySQL 恢复失败' }
Write-Host '恢复完成。'

