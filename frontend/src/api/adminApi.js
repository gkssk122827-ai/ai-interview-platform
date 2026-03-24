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

  async getSubscriptionPlans() {
    try {
      const response = await apiClient.get('/admin/subscription-plans')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '구독 종류 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async createSubscriptionPlan(payload) {
    try {
      const response = await apiClient.post('/admin/subscription-plans', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '구독 종류를 추가하는 중 오류가 발생했습니다.'))
    }
  },

  async updateSubscriptionPlan(planId, payload) {
    try {
      const response = await apiClient.put(`/admin/subscription-plans/${planId}`, payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '구독 종류를 수정하는 중 오류가 발생했습니다.'))
    }
  },

  async deleteSubscriptionPlan(planId) {
    try {
      const response = await apiClient.delete(`/admin/subscription-plans/${planId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '구독 종류를 삭제하는 중 오류가 발생했습니다.'))
    }
  },
}

export default adminApi