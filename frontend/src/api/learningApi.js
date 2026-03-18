import apiClient, { extractApiErrorMessage } from './client.js'
import { extractPayload } from './apiUtils.js'

const learningApi = {
  async generate(payload) {
    try {
      const response = await apiClient.post('/learning/generate', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '학습 문제를 생성하는 중 오류가 발생했습니다.'))
    }
  },

  async grade(payload) {
    try {
      const response = await apiClient.post('/learning/grade', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '문제를 채점하는 중 오류가 발생했습니다.'))
    }
  },
}

export default learningApi
