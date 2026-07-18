import type { AuthData } from '@/types/api'
import { request } from '@/utils/request'

export const mockLogin = (username: string, password: string) =>
  request<AuthData>({
    url: '/api/v1/auth/mock-login',
    method: 'POST',
    data: { username, password },
  })
export const logout = () => request<void>({ url: '/api/v1/auth/logout', method: 'POST' })
