import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextAreaField from '../components/forms/TextAreaField.jsx'
import learningApi from '../api/learningApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

const LEARNING_RESULT_STORAGE_KEY = 'aimentor.learning.results'

function saveLearningResult(totalCount, correctCount) {
  if (typeof window === 'undefined') {
    return
  }

  const nextRecord = {
    id: `learning-${Date.now()}`,
    completedAt: new Date().toISOString(),
    totalCount,
    correctCount,
    score: totalCount > 0 ? Math.round((correctCount * 100) / totalCount) : 0,
  }

  try {
    const storedValue = window.localStorage.getItem(LEARNING_RESULT_STORAGE_KEY)
    const records = storedValue ? JSON.parse(storedValue) : []
    const nextRecords = [nextRecord, ...(Array.isArray(records) ? records : [])].slice(0, 200)
    window.localStorage.setItem(LEARNING_RESULT_STORAGE_KEY, JSON.stringify(nextRecords))
  } catch {
    // Ignore local storage write errors and continue.
  }
}

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
  const [gradeResults, setGradeResults] = useState([])
  const [showResult, setShowResult] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const currentProblem = problems[currentIndex]
  const isFinished = showResult && submittedCount >= problems.length && problems.length > 0
  const correctCount = gradeResults.filter((result) => result.isCorrect).length
  const wrongNotes = gradeResults.filter((result) => !result.isCorrect)
  const answerValue = useMemo(
    () => (currentProblem?.type === 'MULTIPLE' ? selectedChoice : shortAnswer),
    [currentProblem?.type, selectedChoice, shortAnswer],
  )

  if (!currentProblem && !isFinished) {
    return (
      <section className="workspace-page">
        <EmptyState
          title={'생성된 학습 문제가 없습니다.'}
          description={'학습 설정으로 돌아가 과목과 난이도를 다시 선택해 주세요.'}
          action={<button className="button" type="button" onClick={() => navigate('/learning')}>{'설정 화면으로 이동'}</button>}
        />
      </section>
    )
  }

  async function handleSubmit() {
    if (feedback) {
      return
    }

    if (!currentProblem || !answerValue.trim()) {
      setError('답안을 입력하거나 보기를 선택해 주세요.')
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
      setGradeResults((resultList) => [
        ...resultList,
        {
          question: currentProblem.question,
          userAnswer: answerValue,
          correctAnswer: currentProblem.answer,
          explanation: currentProblem.explanation,
          isCorrect: result.isCorrect,
          aiFeedback: result.aiFeedback,
        },
      ])
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
      saveLearningResult(problems.length, correctCount)
      setShowResult(true)
      return
    }

    setCurrentIndex((index) => index + 1)
  }

  if (isFinished) {
    return (
      <section className="workspace-page">
        <div className="workspace-page__hero">
          <p className="page-card__eyebrow">{'학습 결과'}</p>
          <h2 className="page-card__title">{'학습이 완료되었습니다.'}</h2>
          <p className="page-card__description">
            {`총 ${problems.length}문제 중 ${correctCount}문제를 맞추셨습니다.`}
          </p>
        </div>

        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{'전체 문제 요약'}</h3>
            </div>
          </div>
          <p className="panel__subtitle">{`전체 문제: ${problems.length}`}</p>
          <p className="panel__subtitle">{`정답 문제: ${correctCount}`}</p>
          <p className="panel__subtitle">{`오답 문제: ${wrongNotes.length}`}</p>
        </article>

        {wrongNotes.length > 0 ? (
          <article className="panel">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">{'오답노트'}</h3>
              </div>
            </div>
            <div className="resource-list">
              {wrongNotes.map((note, index) => (
                <div key={`${note.question}-${index}`} className="resource-list__item resource-list__item--static">
                  <strong>{`${index + 1}. ${note.question}`}</strong>
                  <p className="panel__subtitle">{`내 답안: ${note.userAnswer}`}</p>
                  <p className="panel__subtitle">{`정답: ${note.correctAnswer}`}</p>
                  <p className="panel__subtitle">{`해설: ${note.explanation}`}</p>
                  <p className="panel__subtitle">{note.aiFeedback}</p>
                </div>
              ))}
            </div>
          </article>
        ) : null}

        <div className="button-row">
          <button className="button" type="button" onClick={() => navigate('/learning')}>
            {'학습 다시 시작'}
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">{'학습 세션'}</p>
        <h2 className="page-card__title">{`문제 ${currentIndex + 1} / ${problems.length}`}</h2>
        <p className="page-card__description">{'문제를 풀고 AI 채점 결과를 확인해 보세요.'}</p>
      </div>

      <StatusMessage variant="error" message={error} />

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">{currentProblem.question}</h3>
            <p className="panel__subtitle">{'유형: 객관식'}</p>
          </div>
        </div>

        {currentProblem.type === 'MULTIPLE' ? (
          <div className="resource-list">
            {currentProblem.choices.map((choice) => (
              <button
                key={choice}
                type="button"
                className={choice === selectedChoice ? 'resource-list__item resource-list__item--active' : 'resource-list__item'}
                onClick={() => setSelectedChoice(choice)}
              >
                <strong>{choice}</strong>
              </button>
            ))}
          </div>
        ) : (
          <TextAreaField
            label={'답안'}
            rows={6}
            value={shortAnswer}
            onChange={(event) => setShortAnswer(event.target.value)}
            placeholder={'정답을 입력해 주세요.'}
          />
        )}

        <div className="button-row">
          <button className="button" type="button" onClick={handleSubmit} disabled={isSubmitting || Boolean(feedback)}>
            {isSubmitting ? '채점 결과를 불러오는 중입니다.' : '제출'}
          </button>
          {feedback ? (
            <button className="button button--secondary" type="button" onClick={handleNext}>
              {currentIndex >= problems.length - 1 ? '학습 결과 보기' : '다음 문제'}
            </button>
          ) : null}
        </div>
      </article>

      {feedback ? (
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{'채점 결과'}</h3>
            </div>
          </div>
          <p className="panel__subtitle">{feedback.isCorrect ? '정답입니다.' : '오답입니다.'}</p>
          <p className="panel__subtitle">{feedback.aiFeedback}</p>
        </article>
      ) : null}
    </section>
  )
}

export default LearningSessionPage
