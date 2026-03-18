import { extractPayload } from './apiUtils.js'
import apiClient from './client.js'
import { createStubCrud } from './stubDatabase.js'

const stubApi = createStubCrud('coverLetters', 'cover-letter')
const useStubApi = import.meta.env.VITE_USE_API_STUB !== 'false'

const coverLetterApi = {
  async list() {
    if (useStubApi) {
      return stubApi.list()
    }

    const response = await apiClient.get('/profiles/cover-letters')
    return extractPayload(response)
  },
  async create(payload) {
    if (useStubApi) {
      return stubApi.create(payload)
    }

    const response = await apiClient.post('/profiles/cover-letters', payload)
    return extractPayload(response)
  },
  async update(id, payload) {
    if (useStubApi) {
      return stubApi.update(id, payload)
    }

    const response = await apiClient.put(`/profiles/cover-letters/${id}`, payload)
    return extractPayload(response)
  },
  async remove(id) {
    if (useStubApi) {
      return stubApi.remove(id)
    }

    const response = await apiClient.delete(`/profiles/cover-letters/${id}`)
    return extractPayload(response)
  },
}

export default coverLetterApi
