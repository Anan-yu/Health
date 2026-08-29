import type { MallPaymentParams } from '@/types/api'

type WechatPaymentFailure = {
  errMsg?: unknown
}

function paymentFailureMessage(failure: WechatPaymentFailure): string {
  const errMsg = typeof failure.errMsg === 'string' ? failure.errMsg : ''
  if (/requestPayment:fail\s+(banned|no permission|access denied|jsapi has no permission)/i.test(errMsg)) {
    return '小程序支付能力受限，请查看公众平台通知'
  }
  if (/requestPayment:fail\s+cancel/i.test(errMsg)) {
    return '已取消支付'
  }
  return '微信支付未完成，请稍后重试'
}

/**
 * Start a normal WeChat JSAPI payment returned by the physical-goods order API.
 * The membership page uses its own virtual-payment flow and must not call this
 * helper.
 */
export function requestWechatPayment(params: MallPaymentParams): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: params.timeStamp,
      nonceStr: params.nonceStr,
      package: params.packageValue,
      signType: params.signType,
      paySign: params.paySign,
      success: () => resolve(),
      fail: (error) => reject(new Error(paymentFailureMessage(error as WechatPaymentFailure))),
    })
    // #endif

    // #ifndef MP-WEIXIN
    reject(new Error('请在微信小程序中完成实物订单支付'))
    // #endif
  })
}
