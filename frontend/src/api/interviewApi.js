import apiClient, { extractApiErrorMessage } from './client.js'
import { cloneData, createId, delay, extractPayload } from './apiUtils.js'
import { difficultyPlan, stubQuestionTemplates } from './interviewStubQuestions.js'

const useStubApi = import.meta.env.VITE_USE_API_STUB === 'true'
const stubSessionStore = new Map()

function normalizeSessionListPayload(payload) {
  if (Array.isArray(payload)) {
    return payload
  }
  if (Array.isArray(payload?.sessions)) {
    return payload.sessions
  }
  if (Array.isArray(payload?.content)) {
    return payload.content
  }
  return []
}

function resolveRole(positionTitle = '') {
  const normalized = positionTitle.toLowerCase()
  if (
    normalized.includes('front')
    || normalized.includes('frontend')
    || normalized.includes('front-end')
    || normalized.includes('react')
    || normalized.includes('next')
    || normalized.includes('vue')
    || normalized.includes('angular')
    || normalized.includes('javascript')
    || normalized.includes('typescript')
    || normalized.includes('html')
    || normalized.includes('css')
    || normalized.includes('ui')
    || normalized.includes('web')
    || normalized.includes('프론트')
    || normalized.includes('프론트엔드')
    || normalized.includes('웹')
  ) {
    return 'FRONTEND'
  }
  return 'BACKEND'
}

function normalizeMode(mode = '') {
  return stubQuestionTemplates[mode] ? mode : 'COMPREHENSIVE'
}

function buildSeed(payload, role, mode) {
  const source = [
    payload.title ?? '',
    payload.positionTitle ?? '',
    payload.applicationDocumentId ?? '',
    payload.jobPostingId ?? '',
    mode,
    role,
    Date.now(),
  ].join('|')

  return Array.from(source).reduce((accumulator, character) => {
    return ((accumulator * 31) + character.charCodeAt(0)) >>> 0
  }, 7)
}

function createSeededRandom(seed) {
  let state = seed >>> 0
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0
    return state / 0x100000000
  }
}

function shuffle(items, random) {
  const copied = [...items]
  for (let index = copied.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(random() * (index + 1))
    ;[copied[index], copied[swapIndex]] = [copied[swapIndex], copied[index]]
  }
  return copied
}

function createStubQuestions(payload, sessionId) {
  const role = resolveRole(payload.positionTitle)
  const mode = normalizeMode(payload.mode)
  const questionCount = Math.max(1, payload.questionCount ?? difficultyPlan.length)
  const plannedDifficulties = difficultyPlan.slice(0, questionCount)
  const random = createSeededRandom(buildSeed(payload, role, mode))
  const questionPoolByDifficulty = Object.fromEntries(
    ['EASY', 'MEDIUM', 'HARD'].map((difficulty) => [
      difficulty,
      shuffle(stubQuestionTemplates[mode][role][difficulty], random),
    ]),
  )
  const usageCountByDifficulty = { EASY: 0, MEDIUM: 0, HARD: 0 }
  const usedQuestionTexts = new Set()

  return plannedDifficulties.map((difficulty, index) => {
    const pool = questionPoolByDifficulty[difficulty]
    const startIndex = usageCountByDifficulty[difficulty]
    let selected = null

    for (let cursor = 0; cursor < pool.length; cursor += 1) {
      const candidate = pool[(startIndex + cursor) % pool.length]
      if (!usedQuestionTexts.has(candidate.questionText)) {
        selected = candidate
        usageCountByDifficulty[difficulty] += 1
        break
      }
    }

    if (!selected) {
      selected = pool[startIndex % pool.length]
      usageCountByDifficulty[difficulty] += 1
    }

    usedQuestionTexts.add(selected.questionText)

    return {
      id: createId('question'),
      sequenceNumber: index + 1,
      questionText: index === 0 ? `${payload.positionTitle} 직무를 기준으로 답변해 주세요. ${selected.questionText}` : selected.questionText,
      answerText: null,
      audioUrl: null,
      answered: false,
      answerLength: 0,
      createdAt: new Date().toISOString(),
    }
  })
}

function createStubFeedback(questions) {
  const answeredQuestions = questions.filter((question) => question.answered)
  const totalLength = answeredQuestions.reduce((sum, question) => sum + (question.answerText?.trim().length ?? 0), 0)
  const averageLength = answeredQuestions.length > 0 ? Math.round(totalLength / answeredQuestions.length) : 0
  const logicScore = Math.min(100, 45 + answeredQuestions.length * 10 + Math.round(averageLength / 10))
  const relevanceScore = Math.min(100, 48 + answeredQuestions.length * 9 + Math.round(averageLength / 12))
  const specificityScore = Math.min(100, 40 + answeredQuestions.length * 8 + Math.round(averageLength / 9))
  const overallScore = Math.round((logicScore + relevanceScore + specificityScore) / 3)

  return {
    logicScore,
    relevanceScore,
    specificityScore,
    overallScore,
    weakPoints: averageLength < 60
      ? '답변 길이가 짧아 근거와 결과가 충분히 드러나지 않았습니다.'
      : '답변 구조는 안정적이지만 선택 이유와 성과를 더 구체화하면 좋습니다.',
    improvements: averageLength < 60
      ? '상황, 행동, 결과를 순서대로 설명하고 수치나 근거를 함께 제시해 보세요.'
      : '기술 선택 이유와 대안 비교를 덧붙이면 더 설득력 있는 답변이 됩니다.',
    recommendedAnswer: '질문의 핵심을 먼저 짚고, 본인의 역할, 해결 과정, 결과, 배운 점 순서로 답변해 보세요.',
  }
}

function createStubResult(session) {
  const questions = session.questions ?? []
  const answeredQuestions = questions.filter((question) => question.answered).length
  const unansweredQuestions = questions.length - answeredQuestions
  const completionRate = questions.length > 0 ? Math.round((answeredQuestions * 100) / questions.length) : 0
  const feedback = createStubFeedback(questions)

  return {
    sessionId: session.id,
    userId: 'stub-user',
    applicationDocumentId: session.applicationDocumentId ?? null,
    resumeId: session.resumeId ?? null,
    coverLetterId: session.coverLetterId ?? null,
    jobPostingId: session.jobPostingId ?? null,
    title: session.title,
    positionTitle: session.positionTitle,
    mode: session.mode,
    status: completionRate === 100 ? 'COMPLETED' : 'ONGOING',
    startedAt: session.startedAt,
    endedAt: session.endedAt ?? null,
    totalQuestions: questions.length,
    answeredQuestions,
    unansweredQuestions,
    completionRate,
    completed: completionRate === 100,
    durationMinutes: 8,
    summary: answeredQuestions === 0
      ? '아직 저장된 답변이 없어 결과를 분석할 수 없습니다.'
      : `총 ${questions.length}개 질문 중 ${answeredQuestions}개에 답변했고, 현재 종합 점수는 ${feedback.overallScore}점입니다.`,
    highlights: [
      `전체 ${questions.length}개 질문 중 ${answeredQuestions}개에 답변했습니다.`,
      completionRate === 100 ? '면접이 정상적으로 종료되었습니다.' : '아직 답변하지 않은 질문이 남아 있습니다.',
      `현재 종합 점수는 ${feedback.overallScore}점입니다.`,
    ],
    learningRecommendations: [
      {
        focusArea: '답변 구조',
        reason: '면접 답변을 더 명확하게 전달하려면 구조화가 필요합니다.',
        recommendedAction: '상황, 행동, 결과 순서로 나누어 말하는 연습을 해 보세요.',
      },
    ],
    questions,
    feedback,
  }
}

const interviewApi = {
  async startSession(payload) {
    if (useStubApi) {
      await delay()
      const sessionId = createId('interview-session')
      const questionCount = Math.max(1, payload.questionCount ?? difficultyPlan.length)
      const session = cloneData({
        id: sessionId,
        title: payload.title,
        positionTitle: payload.positionTitle,
        mode: normalizeMode(payload.mode),
        status: 'ONGOING',
        applicationDocumentId: payload.applicationDocumentId,
        resumeId: payload.resumeId ?? null,
        coverLetterId: payload.coverLetterId ?? null,
        jobPostingId: payload.jobPostingId ?? null,
        startedAt: new Date().toISOString(),
        endedAt: null,
        questions: createStubQuestions({ ...payload, questionCount }, sessionId),
        feedback: null,
        answeredQuestions: 0,
        totalQuestions: questionCount,
        unansweredQuestions: questionCount,
        completionRate: 0,
        questionGenerationSource: 'MOCK',
        questionGenerationFallbackUsed: true,
        questionGenerationMessage: '스텁 환경에서는 모드와 직무를 반영한 기본 질문 세트로 면접을 시작합니다.',
      })
      stubSessionStore.set(session.id, session)
      return cloneData(session)
    }

    try {
      const response = await apiClient.post('/interviews/sessions', payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 세션을 시작하는 중 오류가 발생했습니다.'))
    }
  },

  async listSessions() {
    if (useStubApi) {
      await delay(150)
      return cloneData(Array.from(stubSessionStore.values()).sort((left, right) => new Date(right.startedAt) - new Date(left.startedAt)))
    }

    try {
      const response = await apiClient.get('/interviews/sessions')
      return normalizeSessionListPayload(extractPayload(response))
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 세션 목록을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async getSession(sessionId) {
    if (useStubApi) {
      await delay(120)
      const session = stubSessionStore.get(sessionId)
      if (!session) {
        throw new Error('면접 세션을 찾을 수 없습니다.')
      }
      return cloneData(session)
    }

    try {
      const response = await apiClient.get(`/interviews/sessions/${sessionId}`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 세션을 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async saveAnswer(sessionId, payload) {
    if (useStubApi) {
      await delay(120)
      const session = stubSessionStore.get(sessionId)
      if (!session) {
        throw new Error('면접 세션을 찾을 수 없습니다.')
      }
      session.questions = session.questions.map((question) => {
        if (question.id !== payload.questionId) return question
        const answerText = payload.answerText?.trim() ?? ''
        return {
          ...question,
          answerText,
          audioUrl: payload.audioUrl ?? null,
          answered: Boolean(answerText),
          answerLength: answerText.length,
          updatedAt: new Date().toISOString(),
        }
      })
      session.answeredQuestions = session.questions.filter((question) => question.answered).length
      session.totalQuestions = session.questions.length
      session.unansweredQuestions = session.totalQuestions - session.answeredQuestions
      session.completionRate = session.totalQuestions > 0 ? Math.round((session.answeredQuestions * 100) / session.totalQuestions) : 0
      session.feedback = createStubFeedback(session.questions)
      stubSessionStore.set(sessionId, session)
      return cloneData({
        id: createId('answer'),
        answerText: payload.answerText?.trim() ?? '',
        audioUrl: payload.audioUrl ?? null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    }

    try {
      const response = await apiClient.post(`/interviews/sessions/${sessionId}/answers`, payload)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '답변을 저장하는 중 오류가 발생했습니다.'))
    }
  },

  async endSession(sessionId) {
    if (useStubApi) {
      await delay(120)
      const session = stubSessionStore.get(sessionId)
      if (!session) {
        throw new Error('면접 세션을 찾을 수 없습니다.')
      }
      session.endedAt = new Date().toISOString()
      session.status = 'COMPLETED'
      stubSessionStore.set(sessionId, session)
      return cloneData(session)
    }

    try {
      const response = await apiClient.post(`/interviews/sessions/${sessionId}/end`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접을 종료하는 중 오류가 발생했습니다.'))
    }
  },

  async getResult(sessionId) {
    if (useStubApi) {
      await delay(150)
      const session = stubSessionStore.get(sessionId)
      if (!session) {
        throw new Error('면접 결과를 조회할 수 없습니다.')
      }
      return cloneData(createStubResult(session))
    }

    try {
      const response = await apiClient.get(`/interviews/sessions/${sessionId}/full-report`)
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '면접 결과를 불러오는 중 오류가 발생했습니다.'))
    }
  },
}

export default interviewApi
