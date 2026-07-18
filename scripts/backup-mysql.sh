#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
backup_root="${RAYK_BACKUP_DIR:-/mnt/e/DockerData/RayKA1/backups/mysql}"
mkdir -p "$backup_root"
backup_file="$backup_root/rayk_health_$(date +%Y%m%d_%H%M%S).sql"
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers "$MYSQL_DATABASE"' > "$backup_file"
echo "备份已创建：$backup_file"

