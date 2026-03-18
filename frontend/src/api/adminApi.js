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
}

export default adminApi
