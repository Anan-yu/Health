import { defineStore } from 'pinia'
import type { AuthData, Role } from '@/types/api'
import { mockLogin, logout, weChatAdminLogin, weChatLogin, weChatStaffLogin } from '@/api/auth'
import { switchWorkbench } from '@/api/workbench'

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null as AuthData | null, currentWorkbench: '' as Role | '' }),
  getters: {
    isLoggedIn: (state) =>
      Boolean(state.user?.accessToken || uni.getStorageSync('rayk_access_token')),
    permissions: (state) => state.user?.permissions || [],
    roles: (state) => state.user?.roles || [],
  },
  actions: {
    async login(username: string, password: string) {
      const data = await mockLogin(username, password)
      this.saveSession(data)
    },
    async loginWithWeChat(code: string, phoneCode?: string) {
      const data = await weChatLogin(code, phoneCode)
      this.saveSession(data)
      return data
    },
    async loginWithWeChatInvite(code: string, inviteCode: string) {
      const data = await weChatStaffLogin(code, inviteCode)
      this.saveSession(data)
      return data
    },
    async loginWithWeChatAdmin(code: string, username: string, password: string) {
      const data = await weChatAdminLogin(code, username, password)
      this.saveSession(data)
      return data
    },
    saveSession(data: AuthData) {
      this.user = data
      this.currentWorkbench = data.defaultWorkbench
      uni.setStorageSync('rayk_access_token', data.accessToken)
      uni.setStorageSync('rayk_user', data)
      uni.setStorageSync('rayk_workbench', data.defaultWorkbench)
    },
    hydrate() {
      const user = uni.getStorageSync('rayk_user') as AuthData | ''
      if (user) {
        this.user = user
        this.currentWorkbench =
          (uni.getStorageSync('rayk_workbench') as Role) || user.defaultWorkbench
      }
    },
    async changeWorkbench(code: Role) {
      await switchWorkbench(code)
      this.currentWorkbench = code
      uni.setStorageSync('rayk_workbench', code)
    },
    async signOut() {
      const accessToken = uni.getStorageSync('rayk_access_token') as string
      // Clear local state first so in-flight page requests cannot redirect
      // back to the login page with a misleading expiration banner.
      this.$reset()
      uni.removeStorageSync('rayk_access_token')
      uni.removeStorageSync('rayk_user')
      uni.removeStorageSync('rayk_workbench')
      try {
        await logout(accessToken)
      } catch {
        // Logout is best effort; local credentials must still be removed.
      } finally {
        uni.removeStorageSync('rayk_access_token')
        uni.removeStorageSync('rayk_user')
        uni.removeStorageSync('rayk_workbench')
      }
    },
  },
})
