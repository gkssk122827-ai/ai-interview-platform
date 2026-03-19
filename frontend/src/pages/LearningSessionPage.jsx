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
  usePageTitle('\uD559\uC2B5 \uC138\uC158')
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
          title={'\uC0DD\uC131\uB41C \uD559\uC2B5 \uBB38\uC81C\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.'}
          description={'\uD559\uC2B5 \uC124\uC815\uC73C\uB85C \uB3CC\uC544\uAC00 \uACFC\uBAA9\uACFC \uB09C\uC774\uB3C4\uB97C \uB2E4\uC2DC \uC120\uD0DD\uD574 \uC8FC\uC138\uC694.'}
          action={<button className="button" type="button" onClick={() => navigate('/learning')}>{'\uC124\uC815 \uD654\uBA74\uC73C\uB85C \uC774\uB3D9'}</button>}
        />
      </section>
    )
  }

  async function handleSubmit() {
    if (feedback) {
      return
    }

    if (!currentProblem || !answerValue.trim()) {
      setError('\uB2F5\uC548\uC744 \uC785\uB825\uD558\uAC70\uB098 \uBCF4\uAE30\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694.')
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
          <p className="page-card__eyebrow">{'\uD559\uC2B5 \uACB0\uACFC'}</p>
          <h2 className="page-card__title">{'\uD559\uC2B5\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.'}</h2>
          <p className="page-card__description">
            {`\uCD1D ${problems.length}\uBB38\uC81C \uC911 ${correctCount}\uBB38\uC81C\uB97C \uB9DE\uCD94\uC168\uC2B5\uB2C8\uB2E4.`}
          </p>
        </div>

        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{'\uC804\uCCB4 \uBB38\uC81C \uC694\uC57D'}</h3>
            </div>
          </div>
          <p className="panel__subtitle">{`\uC804\uCCB4 \uBB38\uC81C: ${problems.length}`}</p>
          <p className="panel__subtitle">{`\uC815\uB2F5 \uBB38\uC81C: ${correctCount}`}</p>
          <p className="panel__subtitle">{`\uC624\uB2F5 \uBB38\uC81C: ${wrongNotes.length}`}</p>
        </article>

        {wrongNotes.length > 0 ? (
          <article className="panel">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">{'\uC624\uB2F5\uB178\uD2B8'}</h3>
              </div>
            </div>
            <div className="resource-list">
              {wrongNotes.map((note, index) => (
                <div key={`${note.question}-${index}`} className="resource-list__item resource-list__item--static">
                  <strong>{`${index + 1}. ${note.question}`}</strong>
                  <p className="panel__subtitle">{`\uB0B4 \uB2F5\uC548: ${note.userAnswer}`}</p>
                  <p className="panel__subtitle">{`\uC815\uB2F5: ${note.correctAnswer}`}</p>
                  <p className="panel__subtitle">{`\uD574\uC124: ${note.explanation}`}</p>
                  <p className="panel__subtitle">{note.aiFeedback}</p>
                </div>
              ))}
            </div>
          </article>
        ) : null}

        <div className="button-row">
          <button className="button" type="button" onClick={() => navigate('/learning')}>
            {'\uD559\uC2B5 \uB2E4\uC2DC \uC2DC\uC791'}
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">{'\uD559\uC2B5 \uC138\uC158'}</p>
        <h2 className="page-card__title">{`\uBB38\uC81C ${currentIndex + 1} / ${problems.length}`}</h2>
        <p className="page-card__description">{'\uBB38\uC81C\uB97C \uD480\uACE0 AI \uCC44\uC810 \uACB0\uACFC\uB97C \uD655\uC778\uD574 \uBCF4\uC138\uC694.'}</p>
      </div>

      <StatusMessage variant="error" message={error} />

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">{currentProblem.question}</h3>
            <p className="panel__subtitle">{'\uC720\uD615: \uAC1D\uAD00\uC2DD'}</p>
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
            label={'\uB2F5\uC548'}
            rows={6}
            value={shortAnswer}
            onChange={(event) => setShortAnswer(event.target.value)}
            placeholder={'\uC815\uB2F5\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694.'}
          />
        )}

        <div className="button-row">
          <button className="button" type="button" onClick={handleSubmit} disabled={isSubmitting || Boolean(feedback)}>
            {isSubmitting ? '\uCC44\uC810 \uACB0\uACFC\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4.' : '\uC81C\uCD9C'}
          </button>
          {feedback ? (
            <button className="button button--secondary" type="button" onClick={handleNext}>
              {currentIndex >= problems.length - 1 ? '\uD559\uC2B5 \uACB0\uACFC \uBCF4\uAE30' : '\uB2E4\uC74C \uBB38\uC81C'}
            </button>
          ) : null}
        </div>
      </article>

      {feedback ? (
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{'\uCC44\uC810 \uACB0\uACFC'}</h3>
            </div>
          </div>
          <p className="panel__subtitle">{feedback.isCorrect ? '\uC815\uB2F5\uC785\uB2C8\uB2E4.' : '\uC624\uB2F5\uC785\uB2C8\uB2E4.'}</p>
          <p className="panel__subtitle">{feedback.aiFeedback}</p>
        </article>
      ) : null}
    </section>
  )
}

export default LearningSessionPage
