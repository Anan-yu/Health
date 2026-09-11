import type { GoldBeanPaymentParams } from '@/api/gold-bean'

export type WechatVirtualPaymentError = Error & { errCode?: number; errMsg?: string }

export const VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE =
  '微信虚拟支付能力未开通或已被限制，请管理员在小程序后台核对虚拟支付权限；当前不会扣款。'

type WechatVirtualPaymentApi = {
  requestVirtualPayment: (options: {
    mode: 'short_series_goods' | 'short_series_coin' | string
    signData: string
    paySig: string
    signature: string
    success?: () => void
    fail?: (error: { errMsg?: string; errCode?: number }) => void
  }) => void
}

/** Opens the native WeChat virtual-goods payment sheet. */
export function requestWechatVirtualPayment(params: GoldBeanPaymentParams): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const wechat = (globalThis as unknown as { wx?: WechatVirtualPaymentApi }).wx
    if (!wechat?.requestVirtualPayment) {
      reject(new Error('当前微信基础库不支持虚拟支付，请升级微信后重试'))
      return
    }
    wechat.requestVirtualPayment({
      mode: params.mode,
      signData: params.signData,
      paySig: params.paySig,
      signature: params.signature,
      success: () => resolve(),
      fail: (failure) => {
        const code = failure.errCode
        const rawMessage = failure.errMsg || ''
        const restricted = code === -15005
          || /(banned|no permission|access denied|permission.*(restrict|limit|ban)|限制|封禁|无权限)/i.test(rawMessage)
        const message = code === -2
          ? '支付已取消'
          : restricted
            ? VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE
            : rawMessage || '微信虚拟支付失败'
        reject(Object.assign(new Error(message), { errCode: code, errMsg: failure.errMsg }) as WechatVirtualPaymentError)
      },
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error('请在微信小程序中完成支付'))
    // #endif
  })
}

export const isCancelledWechatVirtualPayment = (cause: unknown) => {
  if (!(cause instanceof Error)) return false
  const paymentError = cause as WechatVirtualPaymentError
  return paymentError.errCode === -2 || `${paymentError.message} ${paymentError.errMsg || ''}`.toUpperCase().includes('CANCEL')
}

export const isWechatVirtualPaymentCapabilityRestricted = (cause: unknown) =>
  cause instanceof Error && cause.message === VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE
