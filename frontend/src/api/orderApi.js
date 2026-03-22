import apiClient, { extractApiErrorMessage } from './client.js'
import { extractPayload } from './apiUtils.js'

const orderApi = {
  async list() {
    try {
      const response = await apiClient.get('/orders')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '주문 내역을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async create(payload) {
    const requestBody = typeof payload === 'string' ? { address: payload } : payload
    try {
      const response = await apiClient.post('/orders', requestBody)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '주문하는 중 오류가 발생했습니다.'))
    }
  },

  async pay(orderId, payload) {
    try {
      const response = await apiClient.post(`/orders/${orderId}/pay`, payload ?? {})
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '결제 처리 중 오류가 발생했습니다.'))
    }
  },

  async readyKakao(orderId, payload) {
    try {
      const response = await apiClient.post(`/orders/${orderId}/payments/kakao/ready`, payload ?? {})
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '카카오페이 결제 준비 중 오류가 발생했습니다.'))
    }
  },

  async approveKakao(orderId, payload) {
    try {
      const response = await apiClient.post(`/orders/${orderId}/payments/kakao/approve`, payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '카카오페이 승인 처리 중 오류가 발생했습니다.'))
    }
  },

  async cancelKakao(orderId) {
    try {
      const response = await apiClient.post(`/orders/${orderId}/payments/kakao/cancel`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '카카오페이 취소 처리 중 오류가 발생했습니다.'))
    }
  },

  async failKakao(orderId) {
    try {
      const response = await apiClient.post(`/orders/${orderId}/payments/kakao/fail`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '카카오페이 실패 처리 중 오류가 발생했습니다.'))
    }
  },

  async get(orderId) {
    try {
      const response = await apiClient.get(`/orders/${orderId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '주문 상세를 불러오는 중 오류가 발생했습니다.'))
    }
  },
}

export default orderApi
