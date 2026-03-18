import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import ResultSummary from '../components/interview/ResultSummary.jsx'
import interviewApi from '../api/interviewApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function InterviewResultPage() {
  usePageTitle('면접 결과')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const sessionId = searchParams.get('sessionId')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    async function loadResult() {
      if (!sessionId) {
        setError('면접 결과를 조회할 세션 ID가 없습니다.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setError('')
      try {
        const response = await interviewApi.getResult(sessionId)
        setResult(response)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadResult()
  }, [sessionId])

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingInterviewResult} /></section>
  }

  if (error) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">면접 결과</p>
        <h2 className="page-card__title">모의면접이 완료되었습니다. 결과를 확인해 보세요.</h2>
        <p className="page-card__description">세션 종료 후 저장된 답변과 피드백을 바탕으로 결과를 표시합니다.</p>
      </div>
      <ResultSummary result={result} />
      <div className="button-row">
        <button className="button" type="button" onClick={() => navigate('/interview/setup')}>{BUTTON_LABELS.startInterview}</button>
        <button className="button button--secondary" type="button" onClick={() => navigate('/learning')}>{BUTTON_LABELS.startLearning}</button>
      </div>
    </section>
  )
}

export default InterviewResultPage