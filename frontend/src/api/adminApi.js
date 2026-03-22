import apiClient, { extractApiErrorMessage } from './client.js'
import { extractPayload } from './apiUtils.js'

const adminApi = {
  async getDashboard() {
    try {
      const response = await apiClient.get('/admin/dashboard')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '관리자 대시보드를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async getUsers(params = {}) {
    try {
      const response = await apiClient.get('/admin/users', { params })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '회원 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async updateUserStatus(userId, status) {
    try {
      const response = await apiClient.patch(`/admin/users/${userId}/status`, { status })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '회원 상태를 변경하는 중 오류가 발생했습니다.'))
    }
  },

  async getPayments() {
    try {
      const response = await apiClient.get('/admin/payments')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '결제 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async getSubscriptions() {
    try {
      const response = await apiClient.get('/admin/subscriptions')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '구독 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },
}

export default adminApi
