import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const target = process.argv[2]
const targets = {
  dev: 'mp-weixin-dev',
  'dev-remote': 'mp-weixin-dev-remote',
  'dev-remote-test': 'mp-weixin-dev-remote-test',
  prod: 'mp-weixin-prod-lan',
}

if (!targets[target]) {
  console.error('Usage: node scripts/sync-mp-weixin-release.mjs <dev|dev-remote|dev-remote-test|prod>')
  process.exit(1)
}

const root = fileURLToPath(new URL('..', import.meta.url))
const buildDir = resolve(root, 'dist', 'build', 'mp-weixin')
const releaseDir = resolve(root, 'dist', 'release', targets[target])
const memberStaticSourceDir = resolve(root, 'src', 'pages-customer', 'static', 'member')
const memberStaticBuildDir = resolve(buildDir, 'pages-customer', 'static', 'member')
// WeChat DevTools watches the release directory. Updating app.json before all
// referenced page files are present creates a short-lived invalid project and
// can make auto-preview fail with "could not find ... index.wxml". Keep the
// project manifest as the final write so DevTools only reloads after the mirror
// is complete.
const manifestFiles = new Set([
  'app.json',
  'project.config.json',
  'project.private.config.json',
  'sitemap.json',
])

const isEnabled = (value) => String(value || '').trim().toLowerCase() === 'true'

const productionGoldBeanEnabled = () => {
  if (process.env.VITE_GOLD_BEAN_ENABLED !== undefined) {
    return isEnabled(process.env.VITE_GOLD_BEAN_ENABLED)
  }
  const envPath = resolve(root, '.env.production')
  if (!existsSync(envPath)) return false
  const match = readFileSync(envPath, 'utf8').match(/^\s*VITE_GOLD_BEAN_ENABLED\s*=\s*(.*?)\s*$/m)
  return isEnabled(match?.[1])
}

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
  const sourceEntryNames = readdirSync(sourceDir)
  const sourceEntries = new Set(sourceEntryNames)
  const entries = [...sourceEntryNames].sort((a, b) => {
    const aManifest = manifestFiles.has(a) ? 1 : 0
    const bManifest = manifestFiles.has(b) ? 1 : 0
    return aManifest - bManifest || a.localeCompare(b)
  })

  for (const entry of entries) {
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

// Always mirror in place. This keeps a DevTools-watched directory present and
// lets syncInPlace remove stale files only after current assets are copied;
// app.json is still the final root-level write.
syncInPlace(buildDir, releaseDir)

// When production builds explicitly disable the club feature, keep the legacy
// message tab in the generated package. An enabled production build keeps the
// club tab from pages.json so the package and runtime feature switch agree.
if (target === 'prod' && !productionGoldBeanEnabled()) {
  const manifestPath = resolve(releaseDir, 'app.json')
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
  const tab = manifest.tabBar?.list?.[2]
  if (!tab) throw new Error(`生产微信包缺少第三个底部导航项：${manifestPath}`)
  tab.pagePath = 'pages/message/index'
  tab.text = '消息'
  writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`)
}

const label =
  target === 'dev'
    ? '开发'
    : target === 'dev-remote'
      ? '远程开发'
      : target === 'dev-remote-test'
        ? '远程隔离测试'
        : '生产局域网'
console.log(`已同步微信${label}包：${releaseDir}`)
