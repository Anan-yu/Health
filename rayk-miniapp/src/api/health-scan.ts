import type { ApiResponse, HealthScanResult, HealthScanSession } from '@/types/api'
import { ApiError, getApiBaseUrl, getRequestHeaders, request } from '@/utils/request'

export const createHealthScanSession = () =>
  request<HealthScanSession>({
    url: '/api/v1/me/health-scans/session',
    method: 'POST',
  })

export const getMyHealthScans = () =>
  request<HealthScanResult[]>({
    url: '/api/v1/me/health-scans',
    method: 'GET',
  })

export const getHealthScan = (id: string) =>
  request<HealthScanResult>({
    url: `/api/v1/me/health-scans/${id}`,
    method: 'GET',
  })

export const uploadHealthScanVideo = (
  id: string,
  filePath: string,
  onProgress?: (progress: number) => void,
) =>
  new Promise<HealthScanResult>((resolve, reject) => {
    const task = uni.uploadFile({
      url: `${getApiBaseUrl()}/api/v1/me/health-scans/${id}/video`,
      filePath,
      name: 'video',
      header: getRequestHeaders(),
      success: (response) => {
        try {
          const body = JSON.parse(response.data) as ApiResponse<HealthScanResult>
          if (response.statusCode === 401) {
            uni.reLaunch({ url: '/pages/login/index?expired=1' })
            reject(new ApiError(401, '登录已失效'))
            return
          }
          if (response.statusCode >= 400 || body.code !== 0) {
            reject(new ApiError(body.code, body.message || '健康检测视频上传失败'))
            return
          }
          resolve(body.data)
        } catch {
          reject(new ApiError(-1, '健康检测服务返回格式异常'))
        }
      },
      fail: () => reject(new ApiError(-1, '视频上传失败，请检查网络连接')),
    })
    task.onProgressUpdate((event) => onProgress?.(event.progress))
  })
