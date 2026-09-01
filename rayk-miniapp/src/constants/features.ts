/**
 * Build-time feature switches for capabilities that are not ready for every
 * release. Keep the implementation and routes in the bundle so a later
 * release can enable them without restoring deleted code.
 */
const isEnabled = (value: unknown) => String(value || '').toLowerCase() === 'true'

export const homeVideoEnabled = isEnabled(import.meta.env.VITE_HOME_VIDEO_ENABLED)
export const mallEnabled = isEnabled(import.meta.env.VITE_MALL_ENABLED)
// 金豆会员需求梳理目前只在开发环境验收；正式包保持关闭，代码和路由仍保留。
export const goldBeanEnabled = isEnabled(import.meta.env.VITE_GOLD_BEAN_ENABLED)
