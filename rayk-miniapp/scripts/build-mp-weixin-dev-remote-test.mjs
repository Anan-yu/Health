import { spawnSync } from 'node:child_process'

const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm'
const env = {
  ...process.env,
  VITE_DEV_API_BASE_URL:
    process.env.VITE_DEV_API_BASE_URL || 'https://xingxuyuan.com/test-api',
  VITE_DEV_ENABLE_DEVELOPMENT_LOGIN: 'true',
  VITE_DEV_RELEASE_TARGET: 'dev-remote-test',
}

const result = spawnSync(npm, ['run', 'build:mp-weixin:dev:remote'], {
  env,
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

process.exit(result.status ?? 1)
