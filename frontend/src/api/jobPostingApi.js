import { extractPayload } from './apiUtils.js'
import apiClient, { extractApiErrorMessage } from './client.js'
import { createStubCrud } from './stubDatabase.js'

const stubApi = createStubCrud('jobPostings', 'job-posting')
const useStubApi = import.meta.env.VITE_USE_API_STUB === 'true'

function normalizePayload(payload) {
  return {
    ...payload,
    companyName: payload.companyName?.trim() ?? '',
    positionTitle: payload.positionTitle?.trim() ?? '',
    description: payload.description?.trim() ?? '',
    siteName: payload.siteName?.trim() ? payload.siteName.trim() : null,
    jobUrl: payload.jobUrl?.trim() ? payload.jobUrl.trim() : null,
    deadline: payload.deadline?.trim() ? payload.deadline : null,
  }
}

const jobPostingApi = {
  async list(keyword = '') {
    if (useStubApi) {
      return stubApi.list()
    }

    try {
      const response = await apiClient.get('/profiles/job-postings', {
        params: { keyword: keyword || undefined },
      })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async get(id) {
    if (useStubApi) {
      const items = await stubApi.list()
      const item = items.find((candidate) => String(candidate.id) === String(id))
      if (!item) throw new Error('채용공고를 찾을 수 없습니다.')
      return item
    }

    try {
      const response = await apiClient.get(`/profiles/job-postings/${id}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고 상세를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async previewUrl(url) {
    try {
      const response = await apiClient.post('/profiles/job-postings/preview', { url })
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고 URL을 분석하는 중 오류가 발생했습니다.'))
    }
  },

  async create(payload) {
    if (useStubApi) {
      return stubApi.create(payload)
    }

    try {
      const response = await apiClient.post('/profiles/job-postings', normalizePayload(payload))
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고를 저장하는 중 오류가 발생했습니다.'))
    }
  },

  async update(id, payload) {
    if (useStubApi) {
      return stubApi.update(id, payload)
    }

    try {
      const response = await apiClient.put(`/profiles/job-postings/${id}`, normalizePayload(payload))
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고를 수정하는 중 오류가 발생했습니다.'))
    }
  },

  async remove(id) {
    if (useStubApi) {
      return stubApi.remove(id)
    }

    try {
      const response = await apiClient.delete(`/profiles/job-postings/${id}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '채용공고를 삭제하는 중 오류가 발생했습니다.'))
    }
  },
}

export default jobPostingApi
