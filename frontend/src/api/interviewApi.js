import apiClient, { extractApiErrorMessage } from './client.js'
import { cloneData, createId, delay, extractPayload } from './apiUtils.js'

const useStubApi = import.meta.env.VITE_USE_API_STUB === 'true'

function createStubQuestions(positionTitle = '백엔드 개발자', questionCount = 3) {
  return Array.from({ length: questionCount }, (_, index) => ({
    id: createId('question'),
    sequenceNumber: index + 1,
    questionText: `${positionTitle} 직무와 관련된 경험을 설명해 주세요. (${index + 1})`,
    answerText: null,
    audioUrl: null,
  }))
}

const interviewApi = {
  async startSession(payload) {
    if (useStubApi) {
      await delay()
      return cloneData({
        id: createId('interview-session'),
        title: payload.title,
        positionTitle: payload.positionTitle,
        applicationDocumentId: payload.applicationDocumentId,
        jobPostingId: payload.jobPostingId ?? null,
        questions: createStubQuestions(payload.positionTitle, payload.questionCount ?? 3),
        feedback: null,
        questionGenerationSource: 'MOCK',
        questionGenerationFallbackUsed: true,
        questionGenerationMessage: '스텁 환경에서는 기본 질문으로 면접을 시작합니다.',
      })
    }

    try {
      const response = await apiClient.post('/interviews/sessions', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 세션을 시작하는 중 오류가 발생했습니다.'))
    }
  },

  async getSession(sessionId) {
    try {
      const response = await apiClient.get(`/interviews/sessions/${sessionId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 세션을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async saveAnswer(sessionId, payload) {
    try {
      const response = await apiClient.post(`/interviews/sessions/${sessionId}/answers`, payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '답변을 저장하는 중 오류가 발생했습니다.'))
    }
  },

  async endSession(sessionId) {
    try {
      const response = await apiClient.post(`/interviews/sessions/${sessionId}/end`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접을 종료하는 중 오류가 발생했습니다.'))
    }
  },

  async getResult(sessionId) {
    try {
      const response = await apiClient.get(`/interviews/sessions/${sessionId}/full-report`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 결과를 불러오는 중 오류가 발생했습니다.'))
    }
  },
}

export default interviewApi