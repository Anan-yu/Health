export const GOLD_BEAN_SCALE = 6
export const GOLD_BEAN_MINIMUM = 0.000001

export function formatGoldBeanAmount(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '0'
  return amount.toLocaleString('zh-CN', { maximumFractionDigits: GOLD_BEAN_SCALE })
}

export function formatGoldBeanBalance(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '0'
  return amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export function parseGoldBeanAmount(value: unknown): number | undefined {
  const text = String(value ?? '').trim()
  if (!/^\d+(?:\.\d{1,6})?$/.test(text)) return undefined
  const amount = Number(text)
  return Number.isFinite(amount) ? amount : undefined
}

export function isValidGoldBeanAmount(value: unknown, maximum?: number): value is number {
  const amount = typeof value === 'number' ? value : parseGoldBeanAmount(value)
  if (amount === undefined || !Number.isFinite(amount) || amount < GOLD_BEAN_MINIMUM) return false
  if (Math.abs(Number(amount.toFixed(GOLD_BEAN_SCALE)) - amount) > Number.EPSILON * 10) return false
  return maximum === undefined || amount <= maximum
}

export function isValidIntegerGoldBeanAmount(value: number, maximum?: number): boolean {
  return Number.isInteger(value) && isValidGoldBeanAmount(value, maximum)
}
