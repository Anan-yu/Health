/**
 * Build-time feature switches for capabilities that are not ready for every
 * release. Keep the implementation and routes in the bundle so a later
 * release can enable them without restoring deleted code.
 */
const isEnabled = (value: unknown) => String(value || '').toLowerCase() === 'true'

export const homeVideoEnabled = isEnabled(import.meta.env.VITE_HOME_VIDEO_ENABLED)
export const mallEnabled = isEnabled(import.meta.env.VITE_MALL_ENABLED)
