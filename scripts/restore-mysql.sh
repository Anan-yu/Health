#!/usr/bin/env sh
set -eu
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then echo '用法：restore-mysql.sh <backup.sql>'; exit 1; fi
cd "$(dirname "$0")/.."
echo '恢复会写入当前数据库；请先另做当前数据库备份。输入 RESTORE 确认：'
read -r confirmation
if [ "$confirmation" != 'RESTORE' ]; then echo '已取消恢复'; exit 0; fi
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < "$1"
echo '恢复完成。'

