/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_USE_MOCK: string
  readonly VITE_ENABLE_DEVELOPMENT_LOGIN?: string
  readonly VITE_HOME_VIDEO_ENABLED?: string
  readonly VITE_HOME_VIDEO_URL?: string
  readonly VITE_HOME_VIDEO_POSTER?: string
  readonly VITE_MALL_ENABLED?: string
}
