import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { resolve, relative } from 'node:path'

export const ROOT_DIR = resolve(fileURLToPath(new URL('../..', import.meta.url)))
export const MINIAPP_DIR = resolve(ROOT_DIR, 'rayk-miniapp')

export function gitOutput(args) {
  return execFileSync('git', args, {
    cwd: ROOT_DIR,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim()
}

export function getGitState() {
  const commit = gitOutput(['rev-parse', 'HEAD'])
  const status = gitOutput(['status', '--porcelain=v1'])
  return {
    commit,
    dirty: Boolean(status),
  }
}

export function latestMigration() {
  const migrationDir = resolve(ROOT_DIR, 'database', 'migrations')
  const versions = readdirSync(migrationDir)
    .map((name) => name.match(/^V(\d+)__.*\.sql$/i)?.[1])
    .filter(Boolean)
    .map(Number)
  return versions.length ? `V${Math.max(...versions)}` : 'unknown'
}

export function hashDirectory(directory) {
  const hash = createHash('sha256')
  const root = resolve(directory)

  const visit = (current) => {
    for (const entry of readdirSync(current, { withFileTypes: true }).sort((a, b) =>
      a.name.localeCompare(b.name),
    )) {
      const absolutePath = resolve(current, entry.name)
      const relativePath = relative(root, absolutePath).replaceAll('\\', '/')
      if (relativePath === 'release-manifest.json') continue

      if (entry.isDirectory()) {
        visit(absolutePath)
        continue
      }
      if (!entry.isFile()) continue

      hash.update(relativePath)
      hash.update('\0')
      hash.update(readFileSync(absolutePath))
      hash.update('\0')
    }
  }

  visit(root)
  return hash.digest('hex')
}

export function assertSafeReleaseId(value) {
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{1,79}$/.test(value)) {
    throw new Error('release id 只能包含字母、数字、点、下划线和短横线，长度为 2-80')
  }
  return value
}

export function parseArgs(argv) {
  const options = { _: [] }
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index]
    if (!value.startsWith('--')) {
      options._.push(value)
      continue
    }

    const separator = value.indexOf('=')
    if (separator > 2) {
      options[value.slice(2, separator)] = value.slice(separator + 1)
      continue
    }

    const key = value.slice(2)
    const next = argv[index + 1]
    if (next && !next.startsWith('--')) {
      options[key] = next
      index += 1
    } else {
      options[key] = true
    }
  }
  return options
}

export function assertDirectory(path, label) {
  if (!statSync(path, { throwIfNoEntry: false })?.isDirectory()) {
    throw new Error(`${label}不存在：${path}`)
  }
}
