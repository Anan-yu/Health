import { readFileSync } from 'node:fs'
import { networkInterfaces } from 'node:os'
import { resolve } from 'node:path'
import { spawnSync } from 'node:child_process'

const parseEnvFile = (filePath) => {
  const values = {}
  for (const line of readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*(VITE_[A-Z0-9_]+)\s*=\s*(.*?)\s*$/)
    if (!match) continue
    values[match[1]] = match[2].replace(/^['"]|['"]$/g, '')
  }
  return values
}

const fileValues = parseEnvFile(resolve('.env.development'))
const env = { ...process.env, ...fileValues }

// 允许临时覆盖局域网地址，但不会误读生产 VITE_API_BASE_URL。
if (process.env.VITE_DEV_API_BASE_URL) env.VITE_API_BASE_URL = process.env.VITE_DEV_API_BASE_URL

// 微信开发者工具和真机不能访问 localhost。局域网 DHCP 地址变化后，自动把开发包
// 指向当前电脑的 RFC1918 IPv4，避免继续使用上一次构建时保存的旧地址。
if (!process.env.VITE_DEV_API_BASE_URL && env.VITE_API_BASE_URL) {
  const isPrivateIpv4 = (address) =>
    /^10\./.test(address) ||
    /^192\.168\./.test(address) ||
    /^172\.(1[6-9]|2\d|3[01])\./.test(address)
  const lanAddress = Object.values(networkInterfaces())
    .flatMap((items) => items || [])
    .find((item) => item && !item.internal && item.family === 'IPv4' && isPrivateIpv4(item.address))
    ?.address
  if (lanAddress) {
    const apiUrl = new URL(env.VITE_API_BASE_URL)
    apiUrl.hostname = lanAddress
    env.VITE_API_BASE_URL = apiUrl.toString().replace(/\/$/, '')
  }
}
if (!env.VITE_API_BASE_URL) {
  throw new Error('开发微信包缺少 VITE_API_BASE_URL，请在 rayk-miniapp/.env.development 中配置局域网 API 地址')
}

const bin = resolve('node_modules/.bin/uni' + (process.platform === 'win32' ? '.cmd' : ''))
const result = spawnSync(bin, ['build', '-p', 'mp-weixin', '--mode', 'development'], {
  env,
  stdio: 'inherit',
  shell: process.platform === 'win32',
})

process.exit(result.status ?? 1)
