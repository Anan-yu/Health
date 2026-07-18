import type { App, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function installPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
      if (!useAuthStore().permissions.includes(binding.value)) el.style.display = 'none'
    },
  })
}
