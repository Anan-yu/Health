import { cpSync, existsSync, mkdirSync, readdirSync, rmSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const target = process.argv[2]
const targets = {
  dev: 'mp-weixin-dev',
  prod: 'mp-weixin-prod-lan',
}

if (!targets[target]) {
  console.error('Usage: node scripts/sync-mp-weixin-release.mjs <dev|prod>')
  process.exit(1)
}

const root = fileURLToPath(new URL('..', import.meta.url))
const buildDir = resolve(root, 'dist', 'build', 'mp-weixin')
const releaseDir = resolve(root, 'dist', 'release', targets[target])
const memberStaticSourceDir = resolve(root, 'src', 'pages-customer', 'static', 'member')
const memberStaticBuildDir = resolve(buildDir, 'pages-customer', 'static', 'member')

if (!existsSync(buildDir)) {
  console.error(`微信构建目录不存在：${buildDir}`)
  process.exit(1)
}

// UniApp's MP-WEIXIN adapter does not copy a nested pages-* static directory
// consistently. The member pages deliberately use a package-local absolute
// path, so make that contract explicit before mirroring either release build.
// This keeps the large member illustrations out of the main package while
// ensuring production and development packages contain the same resources.
if (!existsSync(memberStaticSourceDir)) {
  console.error(`会员静态资源目录不存在：${memberStaticSourceDir}`)
  process.exit(1)
}
rmSync(memberStaticBuildDir, { recursive: true, force: true })
cpSync(memberStaticSourceDir, memberStaticBuildDir, { recursive: true })

const syncInPlace = (sourceDir, targetDir) => {
  mkdirSync(targetDir, { recursive: true })
  const sourceEntries = new Set(readdirSync(sourceDir))

  for (const entry of sourceEntries) {
    const sourcePath = resolve(sourceDir, entry)
    const targetPath = resolve(targetDir, entry)
    if (statSync(sourcePath).isDirectory()) {
      syncInPlace(sourcePath, targetPath)
    } else {
      cpSync(sourcePath, targetPath, { force: true })
    }
  }

  for (const entry of readdirSync(targetDir)) {
    if (sourceEntries.has(entry)) continue
    rmSync(resolve(targetDir, entry), { recursive: true, force: true })
  }
}

// Release directories are generated artifacts. Remove the exact target first so
// hashed files from an earlier build cannot remain alongside the current assets.
// If WeChat DevTools is watching the directory, Windows may reject removing the
// root. Fall back to an in-place mirror so unlocked files still update without
// silently leaving stale files behind.
try {
  rmSync(releaseDir, { recursive: true, force: true })
  mkdirSync(releaseDir, { recursive: true })
  cpSync(buildDir, releaseDir, { recursive: true })
} catch (error) {
  if (!['EPERM', 'EBUSY', 'EACCES'].includes(error?.code)) throw error
  console.warn(`目标目录被占用，改为原地同步：${releaseDir}`)
  syncInPlace(buildDir, releaseDir)
}
console.log(`已同步微信${target === 'dev' ? '开发' : '生产局域网'}包：${releaseDir}`)
