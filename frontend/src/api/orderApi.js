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

  async create(address) {
    try {
      const response = await apiClient.post('/orders', { address })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '주문하는 중 오류가 발생했습니다.'))
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
