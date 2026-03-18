import apiClient, { extractApiErrorMessage } from './client.js'
import { extractPayload } from './apiUtils.js'

const bookApi = {
  async list({ keyword = '', page = 0, size = 8 } = {}) {
    try {
      const response = await apiClient.get('/books', { params: { keyword: keyword || undefined, page, size } })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '도서 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },
}

export default bookApi
