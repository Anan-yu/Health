import type { ApiResponse } from '@/types/api'
import { RELEASE_INFO } from '@/constants/release'

export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

export const getApiBaseUrl = () => {
  const configured = import.meta.env.VITE_API_BASE_URL || ''
  // The local H5 preview is served by the same Nginx container as the API.
  // Use the current origin there so an old LAN address in .env.development
  // cannot prevent local role debugging from reaching Docker.
  if (
    typeof window !== 'undefined' &&
    (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
  ) {
    return window.location.origin
  }
  return configured
}

export function getRequestHeaders() {
  const token = uni.getStorageSync('rayk_access_token') as string
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    'X-Request-Id': `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    'X-Client-Release-Id': RELEASE_INFO.releaseId,
    'X-Client-Git-Commit': RELEASE_INFO.gitCommit,
  }
}

/** Open a protected binary file in the H5 browser without exposing MinIO directly. */
export async function openProtectedFileInBrowser(
  path: string,
  target: '_blank' | '_self' = '_blank',
) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    headers: getRequestHeaders(),
  })
  if (!response.ok) {
    throw new ApiError(response.status, '文件打开失败，请稍后重试')
  }
  const objectUrl = URL.createObjectURL(await response.blob())
  if (target === '_self') {
    globalThis.location.assign(objectUrl)
    globalThis.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
    return
  }
  const previewWindow = globalThis.open(objectUrl, '_blank')
  if (previewWindow) {
    previewWindow.opener = null
  } else {
    globalThis.location.assign(objectUrl)
  }
  globalThis.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
}

/** Download a protected binary file in H5 with a stable filename. */
export async function downloadProtectedFileInBrowser(path: string, filename: string) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    headers: getRequestHeaders(),
  })
  if (!response.ok) {
    throw new ApiError(response.status, '文件下载失败，请稍后重试')
  }

  const objectUrl = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  link.rel = 'noopener'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()

  // Keep the object URL alive long enough for mobile browsers to enqueue it.
  globalThis.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
}

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    // Authentication endpoints must remain usable after a previous session
    // expires or the user switches roles.  In particular, WeChat/mock login
    // requests are public endpoints; forwarding an old Bearer token can make
    // the gateway reject or stall the request before the new credentials are
    // processed.
    const isAuthRequest = options.url.startsWith('/api/v1/auth/')
    const requestHeaders = isAuthRequest
      ? {
          'X-Request-Id': `${Date.now()}-${Math.random().toString(16).slice(2)}`,
          'X-Client-Release-Id': RELEASE_INFO.releaseId,
          'X-Client-Git-Commit': RELEASE_INFO.gitCommit,
        }
      : getRequestHeaders()
    uni.request({
      ...options,
      url: `${getApiBaseUrl()}${options.url}`,
      header: {
        ...options.header,
        ...requestHeaders,
      },
      success: (response) => {
        const body = response.data as Partial<ApiResponse<T>> | null
        const bodyRecord = body !== null && typeof body === 'object' ? body : undefined
        const bodyMessage = typeof bodyRecord?.message === 'string' ? bodyRecord.message.trim() : ''
        const statusMessage =
          response.statusCode === 502
            ? '网关暂时无法连接服务，请稍后重试'
            : response.statusCode === 503
              ? '服务暂时不可用，请稍后重试'
              : response.statusCode >= 500
                ? '服务器暂时不可用，请稍后重试'
                : `请求失败（${response.statusCode}）`
        if (response.statusCode === 401) {
          // A request can finish after an explicit logout. In that case the
          // local session has already been cleared and redirecting with the
          // "expired" banner would make a normal logout look like a timeout.
          if (uni.getStorageSync('rayk_access_token')) {
            uni.reLaunch({ url: '/pages/login/index?expired=1' })
          }
          reject(new ApiError(401, '登录已失效'))
          return
        }
        if (response.statusCode === 403) {
          uni.navigateTo({ url: '/pages/no-permission/index' })
          reject(new ApiError(403, '无权限'))
          return
        }
        if (response.statusCode >= 400) {
          reject(new ApiError(response.statusCode, bodyMessage || statusMessage))
          return
        }
        if (!bodyRecord || bodyRecord.code !== 0) {
          reject(new ApiError(typeof bodyRecord?.code === 'number' ? bodyRecord.code : -1, bodyMessage || '服务返回异常，请稍后重试'))
          return
        }
        resolve(bodyRecord.data as T)
      },
      fail: () => {
        uni.navigateTo({ url: '/pages/error/index' })
        reject(new ApiError(-1, '网络连接失败'))
      },
    })
  })
}
