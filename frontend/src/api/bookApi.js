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

  async get(bookId) {
    try {
      const response = await apiClient.get(`/books/${bookId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '도서 정보를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async create(payload) {
    try {
      const response = await apiClient.post('/books', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '도서를 등록하는 중 오류가 발생했습니다.'))
    }
  },

  async update(bookId, payload) {
    try {
      const response = await apiClient.put(`/books/${bookId}`, payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '도서를 수정하는 중 오류가 발생했습니다.'))
    }
  },

  async remove(bookId) {
    try {
      const response = await apiClient.delete(`/books/${bookId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '도서를 삭제하는 중 오류가 발생했습니다.'))
    }
  },
}

export default bookApi
