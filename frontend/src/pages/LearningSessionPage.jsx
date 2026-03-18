import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextAreaField from '../components/forms/TextAreaField.jsx'
import learningApi from '../api/learningApi.js'
import { BUTTON_LABELS, EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function LearningSessionPage() {
  usePageTitle('학습 세션')
  const location = useLocation()
  const navigate = useNavigate()
  const problems = location.state?.problems ?? []
  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectedChoice, setSelectedChoice] = useState('')
  const [shortAnswer, setShortAnswer] = useState('')
  const [feedback, setFeedback] = useState(null)
  const [error, setError] = useState('')
  const [submittedCount, setSubmittedCount] = useState(0)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const currentProblem = problems[currentIndex]
  const isFinished = submittedCount >= problems.length && problems.length > 0
  const answerValue = useMemo(() => (currentProblem?.type === 'MULTIPLE' ? selectedChoice : shortAnswer), [currentProblem?.type, selectedChoice, shortAnswer])

  if (!currentProblem && !isFinished) {
    return (
      <section className="workspace-page">
        <EmptyState title={EMPTY_MESSAGES.learningProblems.title} description={EMPTY_MESSAGES.learningProblems.description} action={<button className="button" type="button" onClick={() => navigate('/learning')}>{BUTTON_LABELS.goToSetup}</button>} />
      </section>
    )
  }

  async function handleSubmit() {
    if (!currentProblem || !answerValue.trim()) {
      setError('답변을 입력하거나 선택해 주세요.')
      return
    }

    setError('')
    setIsSubmitting(true)
    try {
      const result = await learningApi.grade({
        question: currentProblem.question,
        correctAnswer: currentProblem.answer,
        userAnswer: answerValue,
        explanation: currentProblem.explanation,
      })
      setFeedback(result)
      setSubmittedCount((countValue) => countValue + 1)
    } catch (gradeError) {
      setError(gradeError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleNext() {
    setFeedback(null)
    setSelectedChoice('')
    setShortAnswer('')

    if (currentIndex >= problems.length - 1) {
      navigate('/learning')
      return
    }

    setCurrentIndex((index) => index + 1)
  }

  if (isFinished) {
    return (
      <section className="workspace-page">
        <div className="workspace-page__hero">
          <p className="page-card__eyebrow">학습 결과</p>
          <h2 className="page-card__title">학습을 완료했습니다.</h2>
          <p className="page-card__description">총 {submittedCount}문제를 제출했습니다.</p>
        </div>
        <div className="button-row"><button className="button" type="button" onClick={() => navigate('/learning')}>{BUTTON_LABELS.startLearning}</button></div>
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">학습 세션</p>
        <h2 className="page-card__title">문제 {currentIndex + 1} / {problems.length}</h2>
        <p className="page-card__description">문제를 풀고 AI 채점 결과를 확인해 보세요.</p>
      </div>

      <StatusMessage variant="error" message={error} />

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">{currentProblem.question}</h3>
            <p className="panel__subtitle">유형: {currentProblem.type === 'MULTIPLE' ? '객관식' : '주관식'}</p>
          </div>
        </div>

        {currentProblem.type === 'MULTIPLE'
          ? <div className="resource-list">{currentProblem.choices.map((choice) => <button key={choice} type="button" className={choice === selectedChoice ? 'resource-list__item resource-list__item--active' : 'resource-list__item'} onClick={() => setSelectedChoice(choice)}><strong>{choice}</strong></button>)}</div>
          : <TextAreaField label="답안" rows={6} value={shortAnswer} onChange={(event) => setShortAnswer(event.target.value)} placeholder="정답을 입력해 주세요." />}

        <div className="button-row">
          <button className="button" type="button" onClick={handleSubmit} disabled={isSubmitting}>{isSubmitting ? STATUS_MESSAGES.loadingLearningResult : '제출'}</button>
          {feedback ? <button className="button button--secondary" type="button" onClick={handleNext}>{currentIndex >= problems.length - 1 ? '학습 종료' : '다음 문제'}</button> : null}
        </div>
      </article>

      {feedback ? <article className="panel"><div className="panel__header"><div><h3 className="panel__title">채점 결과</h3></div></div><p className="panel__subtitle">{feedback.isCorrect ? '정답입니다.' : '오답입니다.'}</p><p className="panel__subtitle">{feedback.aiFeedback}</p></article> : null}
    </section>
  )
}

export default LearningSessionPage