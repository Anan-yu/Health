import type {
  MallAddress,
  MallOrder,
  MallPaymentParams,
  MallProduct,
} from '@/types/api'
import { request } from '@/utils/request'

export const getMallProducts = () =>
  request<MallProduct[]>({ url: '/api/mall/products', method: 'GET' })

export const getMallProduct = (productId: string) =>
  request<MallProduct>({
    url: `/api/mall/products/${encodeURIComponent(productId)}`,
    method: 'GET',
  })

export const getMallAddresses = () =>
  request<MallAddress[]>({ url: '/api/mall/addresses', method: 'GET' })

export const createMallAddress = (data: Omit<MallAddress, 'id'>) =>
  request<MallAddress>({ url: '/api/mall/addresses', method: 'POST', data })

export const updateMallAddress = (addressId: string, data: Omit<MallAddress, 'id'>) =>
  request<MallAddress>({
    url: `/api/mall/addresses/${encodeURIComponent(addressId)}`,
    method: 'PUT',
    data,
  })

export const deleteMallAddress = (addressId: string) =>
  request<void>({
    url: `/api/mall/addresses/${encodeURIComponent(addressId)}`,
    method: 'DELETE',
  })

export const createMallOrder = (data: { productId: string; addressId: string; quantity: number }) =>
  request<MallOrder>({ url: '/api/mall/orders', method: 'POST', data })

export const getMallOrders = () =>
  request<MallOrder[]>({ url: '/api/mall/orders', method: 'GET' })

export const getMallOrder = (orderNo: string) =>
  request<MallOrder>({ url: `/api/mall/orders/${encodeURIComponent(orderNo)}`, method: 'GET' })

/**
 * `uni.requestPayment` only confirms the client payment sheet. The callback
 * notification is the source of truth for a paid physical-goods order.
 */
export const waitForMallPaymentConfirmation = async (orderNo: string, attempts = 8) => {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    try {
      const order = await getMallOrder(orderNo)
      if (order.status !== 'PENDING_PAYMENT') return order
    } catch {
      // A transient refresh failure must not be reported as a failed payment.
    }
    if (attempt < attempts - 1) await new Promise((resolve) => setTimeout(resolve, 1500))
  }
  return undefined
}

export const cancelMallOrder = (orderNo: string) =>
  request<void>({
    url: `/api/mall/orders/${encodeURIComponent(orderNo)}/cancel`,
    method: 'POST',
  })

export const createWechatMallPayment = (orderNo: string) =>
  request<MallPaymentParams>({
    url: `/api/mall/orders/${encodeURIComponent(orderNo)}/wechat-pay`,
    method: 'POST',
  })

export const getMallAdminProducts = () =>
  request<MallProduct[]>({ url: '/api/v1/platform/mall/products', method: 'GET' })

export const createMallAdminProduct = (data: Omit<MallProduct, 'id' | 'soldCount'>) =>
  request<MallProduct>({ url: '/api/v1/platform/mall/products', method: 'POST', data })

export const updateMallAdminProduct = (
  productId: string,
  data: Omit<MallProduct, 'id' | 'soldCount'>,
) =>
  request<MallProduct>({
    url: `/api/v1/platform/mall/products/${encodeURIComponent(productId)}`,
    method: 'PUT',
    data,
  })

export const disableMallAdminProduct = (productId: string) =>
  request<void>({
    url: `/api/v1/platform/mall/products/${encodeURIComponent(productId)}`,
    method: 'DELETE',
  })
