import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { ROOT_DIR, parseArgs } from './release-utils.mjs'

const args = parseArgs(process.argv.slice(2))
const baseUrl = args._[0]
const manifestPath = resolve(
  ROOT_DIR,
  String(args.manifest || 'build/release/release-manifest.json'),
)

if (!baseUrl) {
  console.error('用法：node scripts/release/verify-release.mjs <线上地址> [--manifest <清单路径>]')
  process.exit(1)
}

async function run() {
  const expected = JSON.parse(readFileSync(manifestPath, 'utf8'))
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/system/version`)
  const body = await response.json()
  if (!response.ok || !body?.data) {
    throw new Error(`线上版本接口返回 HTTP ${response.status}`)
  }

  const actual = body.data
  const expectedValues = {
    releaseId: expected.releaseId,
    gitCommit: expected.gitCommit,
    gitDirty: expected.gitDirty,
    buildTime: expected.buildTime,
    databaseMigration: expected.databaseMigration,
    frontendH5Sha256: expected.artifacts?.h5,
    frontendMpWeixinDevSha256: expected.artifacts?.mpWeixinDev,
    frontendMpWeixinProdLanSha256: expected.artifacts?.mpWeixinProdLan,
  }
  const mismatches = Object.keys(expectedValues).filter(
    (key) => String(actual[key]) !== String(expectedValues[key]),
  )

  if (mismatches.length) {
    console.error('线上版本与本地发布清单不一致：')
    for (const key of mismatches) {
      console.error(`- ${key}: local=${expectedValues[key]} online=${actual[key]}`)
    }
    process.exit(2)
  }

  console.log(`版本一致：${expected.releaseId}`)
  console.log(`Git：${expected.gitCommit}`)
  console.log(`数据库迁移：${expected.databaseMigration}`)
}

run().catch((error) => {
  console.error(`线上版本校验失败：${error instanceof Error ? error.message : String(error)}`)
  process.exit(1)
})
