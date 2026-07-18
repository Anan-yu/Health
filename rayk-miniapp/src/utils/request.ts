import type { ApiResponse } from '@/types/api'

export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  const token = uni.getStorageSync('rayk_access_token') as string
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: `${import.meta.env.VITE_API_BASE_URL || ''}${options.url}`,
      header: {
        ...options.header,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        'X-Request-Id': `${Date.now()}-${Math.random().toString(16).slice(2)}`,
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
