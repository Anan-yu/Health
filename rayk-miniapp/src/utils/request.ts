import type { ApiResponse } from '@/types/api'

export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

export const getApiBaseUrl = () => import.meta.env.VITE_API_BASE_URL || ''

export function getRequestHeaders() {
  const token = uni.getStorageSync('rayk_access_token') as string
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    'X-Request-Id': `${Date.now()}-${Math.random().toString(16).slice(2)}`,
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

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: `${getApiBaseUrl()}${options.url}`,
      header: {
        ...options.header,
        ...getRequestHeaders(),
      },
      success: (response) => {
        const body = response.data as ApiResponse<T>
        if (response.statusCode === 401) {
          uni.reLaunch({ url: '/pages/login/index?expired=1' })
          reject(new ApiError(401, '登录已失效'))
          return
        }
        if (response.statusCode === 403) {
          uni.navigateTo({ url: '/pages/no-permission/index' })
          reject(new ApiError(403, '无权限'))
          return
        }
        if (response.statusCode >= 400 || body.code !== 0) {
          reject(new ApiError(body.code, body.message))
          return
        }
        resolve(body.data)
      },
      fail: () => {
        uni.navigateTo({ url: '/pages/error/index' })
        reject(new ApiError(-1, '网络连接失败'))
      },
    })
  })
}
