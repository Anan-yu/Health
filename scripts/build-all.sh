#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
docker compose -f compose.yml -f compose.dev.yml config --quiet
docker compose -f compose.yml -f compose.dev.yml build
cd rayk-miniapp
npm install
npm run type-check
npm run build:h5
npm run build:mp-weixin

