/**
 * Build-time feature switches for capabilities that are not ready for every
 * release. Keep the implementation and routes in the bundle so a later
 * release can enable them without restoring deleted code.
 */
const isEnabled = (value: unknown) => String(value || '').toLowerCase() === 'true'

export const homeVideoEnabled = isEnabled(import.meta.env.VITE_HOME_VIDEO_ENABLED)
export const mallEnabled = isEnabled(import.meta.env.VITE_MALL_ENABLED)
// 金豆会员入口由构建环境开关控制；开启时必须同时配置服务端能力与支付链路。
export const goldBeanEnabled = isEnabled(import.meta.env.VITE_GOLD_BEAN_ENABLED)
// 首页健康管理进度卡片先在开发版隐藏，生产版继续保留。
export const homeProfileProgressCardEnabled = import.meta.env.MODE !== 'development'
