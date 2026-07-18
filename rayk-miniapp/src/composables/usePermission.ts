import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/api'

export function usePermission() {
  const auth = useAuthStore()
  const hasRole = (role: Role) => auth.roles.includes(role)
  const hasPermission = (permission: string) => auth.permissions.includes(permission)
  return { hasRole, hasPermission }
}
