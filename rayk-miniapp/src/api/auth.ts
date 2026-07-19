import type { AuthData, WeChatBinding } from '@/types/api'
import { request } from '@/utils/request'

export const mockLogin = (username: string, password: string) =>
  request<AuthData>({
    url: '/api/v1/auth/mock-login',
    method: 'POST',
    data: { username, password },
  })
export const weChatLogin = (code: string) =>
  request<AuthData>({ url: '/api/v1/auth/wechat-login', method: 'POST', data: { code } })
export const bindWeChat = (code: string) =>
  request<WeChatBinding>({ url: '/api/v1/auth/wechat-bind', method: 'POST', data: { code } })
export const logout = () => request<void>({ url: '/api/v1/auth/logout', method: 'POST' })
