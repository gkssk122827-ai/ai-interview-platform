import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import AnswerInput from '../components/interview/AnswerInput.jsx'
import FeedbackCard from '../components/interview/FeedbackCard.jsx'
import QuestionCard from '../components/interview/QuestionCard.jsx'
import interviewApi from '../api/interviewApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function InterviewSessionPage() {
  usePageTitle('면접 진행')
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const sessionId = searchParams.get('sessionId')
  const [session, setSession] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [pageError, setPageError] = useState('')
  const [statusMessage, setStatusMessage] = useState(location.state?.sessionNotice ?? '')
  const [statusVariant, setStatusVariant] = useState(location.state?.sessionFallbackUsed ? 'error' : 'success')
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0)
  const [answer, setAnswer] = useState('')

  useEffect(() => {
    async function loadSession() {
      if (!sessionId) {
        setPageError('면접 세션 정보가 없습니다.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setPageError('')

      try {
        const loadedSession = await interviewApi.getSession(sessionId)
        setSession(loadedSession)
        const nextIndex = loadedSession.questions.findIndex((question) => !question.answered)
        const initialIndex = nextIndex >= 0 ? nextIndex : 0
        setCurrentQuestionIndex(initialIndex)
        setAnswer(loadedSession.questions[initialIndex]?.answerText ?? '')
      } catch (error) {
        setPageError(error.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadSession()
  }, [sessionId])

  const currentQuestion = useMemo(
    () => session?.questions?.[currentQuestionIndex] ?? null,
    [session, currentQuestionIndex],
  )
  const feedbackMessage = session?.feedback?.improvements ?? session?.feedback?.weakPoints ?? ''

  async function handleSubmitAnswer() {
    if (!currentQuestion || !answer.trim()) return

    setIsSubmitting(true)
    setStatusMessage('')
    setPageError('')

    try {
      await interviewApi.saveAnswer(sessionId, {
        questionId: currentQuestion.id,
        answerText: answer.trim(),
        audioUrl: null,
      })
      const updatedSession = await interviewApi.getSession(sessionId)
      setSession(updatedSession)
      setAnswer(updatedSession.questions[currentQuestionIndex]?.answerText ?? answer.trim())
      setStatusVariant('success')
      setStatusMessage('답변이 저장되었습니다.')
    } catch (error) {
      setPageError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleVoicePlaceholder() {
    setStatusVariant('success')
    setStatusMessage('음성 녹음 기능은 현재 텍스트 답변 흐름에 맞춰 순차적으로 연결 중입니다.')
  }

  async function handleNextQuestion() {
    if (!session) return

    const isLastQuestion = currentQuestionIndex >= session.questions.length - 1
    if (isLastQuestion) {
      try {
        await interviewApi.endSession(sessionId)
      } catch {
      }
      navigate(`/interview/result?sessionId=${sessionId}`)
      return
    }

    const nextIndex = currentQuestionIndex + 1
    setCurrentQuestionIndex(nextIndex)
    setAnswer(session.questions[nextIndex]?.answerText ?? '')
    setStatusMessage('')
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingInterviewSession} /></section>
  }

  if (pageError && !session) {
    return <section className="workspace-page"><ErrorBlock message={pageError} /></section>
  }

  if (!currentQuestion) {
    return (
      <section className="workspace-page">
        <div className="workspace-page__hero">
          <p className="page-card__eyebrow">면접 세션</p>
          <h2 className="page-card__title">진행 중인 질문을 찾을 수 없습니다.</h2>
          <p className="page-card__description">면접 설정 화면에서 새 세션을 시작해 주세요.</p>
        </div>
        <div className="button-row">
          <button className="button" type="button" onClick={() => navigate('/interview/setup')}>
            {BUTTON_LABELS.goToSetup}
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">면접 진행</p>
        <h2 className="page-card__title">질문에 답변하고 다음 질문으로 차근차근 진행해 보세요.</h2>
        <p className="page-card__description">
          완료율 {session?.completionRate ?? 0}% · 답변 {session?.answeredQuestions ?? 0}/{session?.totalQuestions ?? 0}
        </p>
      </div>

      <StatusMessage variant="error" message={session ? pageError : ''} />
      <StatusMessage variant={statusVariant} message={statusMessage} />

      <div className="workspace-page">
        <QuestionCard question={currentQuestion} index={currentQuestionIndex} total={session.questions.length} mode={session.positionTitle} />
        <AnswerInput
          answer={answer}
          onChange={setAnswer}
          onSubmit={handleSubmitAnswer}
          onRecordPlaceholder={handleVoicePlaceholder}
          isSubmitting={isSubmitting}
        />
        <FeedbackCard
          feedback={feedbackMessage}
          title="현재 피드백"
          description="저장된 답변을 기준으로 현재까지의 개선 방향을 보여 줍니다."
        />
        <div className="button-row">
          <button className="button" type="button" onClick={handleNextQuestion}>
            {currentQuestionIndex >= session.questions.length - 1 ? BUTTON_LABELS.viewResult : BUTTON_LABELS.nextQuestion}
          </button>
        </div>
      </div>
    </section>
  )
}

export default InterviewSessionPage