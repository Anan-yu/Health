import { defineStore } from 'pinia'
import type { AuthData, Role } from '@/types/api'
import { mockLogin, logout } from '@/api/auth'
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
      try {
        await logout()
      } finally {
        this.$reset()
        uni.removeStorageSync('rayk_access_token')
        uni.removeStorageSync('rayk_user')
        uni.removeStorageSync('rayk_workbench')
      }
    },
  },
})
