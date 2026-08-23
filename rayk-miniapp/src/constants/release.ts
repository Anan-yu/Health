const buildEnv = import.meta.env

export const RELEASE_INFO = Object.freeze({
  releaseId: String(buildEnv.VITE_RELEASE_ID || 'local-unreleased'),
  gitCommit: String(buildEnv.VITE_GIT_COMMIT || 'unknown'),
  gitDirty: String(buildEnv.VITE_GIT_DIRTY || 'unknown'),
  buildTime: String(buildEnv.VITE_BUILD_TIME || 'unknown'),
})
