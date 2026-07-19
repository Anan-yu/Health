import type {
  ApiResponse,
  Indicator,
  LabReport,
  LabReportFile,
  LabReportUpload,
  OcrTask,
  PageResponse,
} from '@/types/api'
import { ApiError, getApiBaseUrl, getRequestHeaders, request } from '@/utils/request'

export const getLabReports = () =>
  request<PageResponse<LabReport>>({ url: '/api/v1/lab-reports', method: 'GET' })
export const getMyLabReports = () =>
  request<LabReport[]>({ url: '/api/v1/me/lab-reports', method: 'GET' })
export const getLabReport = (id: string) =>
  request<LabReport>({ url: `/api/v1/lab-reports/${id}`, method: 'GET' })
export const getOcrTask = (id: string) =>
  request<OcrTask>({ url: `/api/v1/lab-reports/${id}/ocr-task`, method: 'GET' })
export const retryOcrTask = (id: string) =>
  request<OcrTask>({ url: `/api/v1/lab-reports/${id}/ocr-task/retry`, method: 'POST' })
export const getReportFiles = (id: string) =>
  request<LabReportFile[]>({ url: `/api/v1/lab-reports/${id}/files`, method: 'GET' })
export const getFileDownloadUrl = (reportId: string, fileId: string) =>
  request<LabReportFile>({
    url: `/api/v1/lab-reports/${reportId}/files/${fileId}/download-url`,
    method: 'POST',
  })
export const uploadLabReport = (
  filePath: string,
  patientId: string,
  reportName: string,
  reportDate: string,
  onProgress?: (progress: number) => void,
) =>
  new Promise<LabReportUpload>((resolve, reject) => {
    const task = uni.uploadFile({
      url: `${getApiBaseUrl()}/api/v1/lab-reports/upload`,
      filePath,
      name: 'file',
      header: getRequestHeaders(),
      formData: { patientId, reportName, reportDate },
      success: (response) => {
        try {
          const body = JSON.parse(response.data) as ApiResponse<LabReportUpload>
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
            reject(new ApiError(body.code, body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch {
          reject(new ApiError(-1, '服务端返回格式异常'))
        }
      },
      fail: () => reject(new ApiError(-1, '文件上传失败，请检查网络连接')),
    })
    task.onProgressUpdate((event) => onProgress?.(event.progress))
  })
export const saveIndicators = (id: string, indicators: Indicator[]) =>
  request<LabReport>({
    url: `/api/v1/lab-reports/${id}/indicators`,
    method: 'PUT',
    data: { indicators },
  })
export const confirmIndicators = (id: string) =>
  request<LabReport>({ url: `/api/v1/lab-reports/${id}/confirm`, method: 'POST' })
export const submitAi = (id: string) =>
  request<unknown>({ url: `/api/v1/lab-reports/${id}/submit-ai`, method: 'POST' })
