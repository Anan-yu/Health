#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
docker compose -f compose.yml -f compose.dev.yml down
echo '服务已停止；命名卷和业务数据已保留。'

