import { cpSync, existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'
import {
  MINIAPP_DIR,
  ROOT_DIR,
  assertDirectory,
  assertSafeReleaseId,
  getGitState,
  hashDirectory,
  latestMigration,
  parseArgs,
} from './release-utils.mjs'

const args = parseArgs(process.argv.slice(2))
const allowDirty = args['allow-dirty'] === true
const skipBuild = args['skip-build'] === true

function run() {
  const git = getGitState()
  if (git.dirty && !allowDirty) {
    throw new Error(
      '发布构建要求 Git 工作区干净。请先提交当前改动；仅用于本地验证时可显式添加 --allow-dirty。',
    )
  }

  const defaultReleaseId = git.dirty
    ? `local-dirty-${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`
    : `release-${git.commit.slice(0, 12)}`
  const releaseId = assertSafeReleaseId(String(args['release-id'] || defaultReleaseId))
  const buildTime = new Date().toISOString()
  const databaseMigration = String(args['database-migration'] || latestMigration())
  const buildEnv = {
    ...process.env,
    VITE_RELEASE_ID: releaseId,
    VITE_GIT_COMMIT: git.commit,
    VITE_GIT_DIRTY: String(git.dirty),
    VITE_BUILD_TIME: buildTime,
  }

  if (!skipBuild) {
    const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm'
    for (const script of ['build:h5', 'build:mp-weixin:dev', 'build:mp-weixin']) {
      console.log(`\n> npm run ${script}`)
      const result = spawnSync(npm, ['run', script], {
        cwd: MINIAPP_DIR,
        env: buildEnv,
        stdio: 'inherit',
        shell: process.platform === 'win32',
      })
      if (result.error) throw result.error
      if (result.status !== 0) throw new Error(`前端脚本失败：${script}`)
    }
  }

  const artifactDirectories = {
    h5: resolve(MINIAPP_DIR, 'dist', 'build', 'h5'),
    mpWeixinDev: resolve(MINIAPP_DIR, 'dist', 'release', 'mp-weixin-dev'),
    mpWeixinProdLan: resolve(MINIAPP_DIR, 'dist', 'release', 'mp-weixin-prod-lan'),
  }
  for (const [name, directory] of Object.entries(artifactDirectories)) {
    assertDirectory(directory, `前端${name}构建目录`)
  }

  const manifest = {
    schemaVersion: 1,
    releaseId,
    gitCommit: git.commit,
    gitDirty: git.dirty,
    buildTime,
    databaseMigration,
    artifacts: Object.fromEntries(
      Object.entries(artifactDirectories).map(([name, directory]) => [name, hashDirectory(directory)]),
    ),
  }

  const releaseDirectory = resolve(ROOT_DIR, 'build', 'release')
  mkdirSync(releaseDirectory, { recursive: true })
  const manifestText = `${JSON.stringify(manifest, null, 2)}\n`
  const manifestPath = resolve(releaseDirectory, 'release-manifest.json')
  writeFileSync(manifestPath, manifestText, 'utf8')

  for (const directory of Object.values(artifactDirectories)) {
    writeFileSync(resolve(directory, 'release-manifest.json'), manifestText, 'utf8')
  }

  const env = {
    RAYK_RELEASE_ID: releaseId,
    RAYK_GIT_COMMIT: git.commit,
    RAYK_GIT_DIRTY: String(git.dirty),
    RAYK_BUILD_TIME: buildTime,
    RAYK_DATABASE_MIGRATION: databaseMigration,
    RAYK_FRONTEND_H5_SHA256: manifest.artifacts.h5,
    RAYK_FRONTEND_MP_WEIXIN_DEV_SHA256: manifest.artifacts.mpWeixinDev,
    RAYK_FRONTEND_MP_WEIXIN_PROD_LAN_SHA256: manifest.artifacts.mpWeixinProdLan,
  }
  writeFileSync(
    resolve(releaseDirectory, 'release.env'),
    `${Object.entries(env)
      .map(([key, value]) => `${key}=${value}`)
      .join('\n')}\n`,
    'utf8',
  )

  console.log(`\n发布清单已生成：${manifestPath}`)
  console.log(`Compose 环境文件：${resolve(releaseDirectory, 'release.env')}`)
  console.log(`releaseId=${releaseId} gitCommit=${git.commit} gitDirty=${git.dirty}`)
}

try {
  run()
} catch (error) {
  console.error(`发布构建失败：${error instanceof Error ? error.message : String(error)}`)
  process.exit(1)
}
