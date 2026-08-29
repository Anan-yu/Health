import { spawnSync } from 'node:child_process'

const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm'
const env = {
  ...process.env,
  VITE_DEV_API_BASE_URL: process.env.VITE_DEV_API_BASE_URL || 'https://xingxuyuan.com',
  VITE_DEV_ENABLE_DEVELOPMENT_LOGIN: process.env.VITE_DEV_ENABLE_DEVELOPMENT_LOGIN || 'false',
}
const releaseTarget = process.env.VITE_DEV_RELEASE_TARGET || 'dev-remote'

// Build the same development-mode source, but mirror it to a separate release
// directory so an online-backend package can never overwrite the normal LAN
// development package (which intentionally contains the development login UI).
const buildResult = spawnSync(npm, ['run', 'build:mp-weixin:dev:source'], {
  env,
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

if ((buildResult.status ?? 1) !== 0) process.exit(buildResult.status ?? 1)

const syncResult = spawnSync(
  npm,
  ['run', 'sync:mp-weixin:release', '--', releaseTarget],
  {
    env,
    stdio: 'inherit',
    shell: process.platform === 'win32',
  },
)

process.exit(syncResult.status ?? 1)
