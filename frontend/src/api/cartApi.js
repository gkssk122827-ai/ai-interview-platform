import apiClient, { extractApiErrorMessage } from './client.js'
import { extractPayload } from './apiUtils.js'

const cartApi = {
  async getCart() {
    try {
      const response = await apiClient.get('/cart')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '장바구니를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async addItem(bookId, quantity = 1) {
    try {
      const response = await apiClient.post('/cart', { bookId, quantity })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '장바구니에 담는 중 오류가 발생했습니다.'))
    }
  },

  async updateItem(bookId, quantity) {
    try {
      const response = await apiClient.put(`/cart/${bookId}`, { quantity })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '수량을 변경하는 중 오류가 발생했습니다.'))
    }
  },

  async removeItem(bookId) {
    try {
      const response = await apiClient.delete(`/cart/${bookId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '장바구니 항목을 삭제하는 중 오류가 발생했습니다.'))
    }
  },
}

export default cartApi
