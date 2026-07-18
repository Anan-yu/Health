import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { installPermissionDirective } from '@/guards/permission'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  installPermissionDirective(app)
  return { app }
}
