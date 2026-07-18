$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
docker compose -f compose.yml -f compose.dev.yml config --quiet
docker compose -f compose.yml -f compose.dev.yml build
Push-Location rayk-miniapp
try {
    npm install
    npm run type-check
    npm run build:h5
    npm run build:mp-weixin
} finally {
    Pop-Location
}

