import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import usePageTitle from '../hooks/usePageTitle.js'

const LEARNING_RESULT_STORAGE_KEY = 'aimentor.learning.results'

function readLearningResults() {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const storedValue = window.localStorage.getItem(LEARNING_RESULT_STORAGE_KEY)
    const records = storedValue ? JSON.parse(storedValue) : []
    return Array.isArray(records) ? records : []
  } catch {
    return []
  }
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function LearningResultPage() {
  usePageTitle('학습 결과')

  const [searchParams] = useSearchParams()
  const resultId = searchParams.get('resultId')

  const result = useMemo(() => {
    if (!resultId) {
      return null
    }

    return readLearningResults().find((record) => record?.id === resultId) ?? null
  }, [resultId])

  if (!resultId || !result) {
    return (
      <section className="workspace-page">
        <EmptyState
          title="학습 결과를 찾을 수 없습니다."
          description="저장된 학습 결과가 없거나 이미 삭제된 기록입니다."
          action={<Link className="button" to="/learning">학습하러 가기</Link>}
        />
      </section>
    )
  }

  const totalCount = Number(result.totalCount) || 0
  const correctCount = Number(result.correctCount) || 0
  const wrongCount = Math.max(0, totalCount - correctCount)
  const score = Number(result.score) || 0

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">학습 결과</p>
        <h2 className="page-card__title">저장된 학습 결과</h2>
        <p className="page-card__description">
          {`${formatDateTime(result.completedAt)}에 완료한 학습 기록입니다.`}
        </p>
      </div>

      <section className="admin-summary-grid">
        <article className="panel admin-summary-card">
          <p className="admin-summary-card__label">점수</p>
          <p className="admin-summary-card__value">{`${score}점`}</p>
        </article>
        <article className="panel admin-summary-card">
          <p className="admin-summary-card__label">전체 문제</p>
          <p className="admin-summary-card__value">{`${totalCount}문제`}</p>
        </article>
        <article className="panel admin-summary-card">
          <p className="admin-summary-card__label">정답</p>
          <p className="admin-summary-card__value">{`${correctCount}문제`}</p>
        </article>
        <article className="panel admin-summary-card">
          <p className="admin-summary-card__label">오답</p>
          <p className="admin-summary-card__value">{`${wrongCount}문제`}</p>
        </article>
      </section>

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">결과 요약</h3>
            <p className="panel__subtitle">대시보드의 기간별 성과 추이에서 바로 이동한 기록입니다.</p>
          </div>
        </div>
        <p className="panel__subtitle">{`완료 시각: ${formatDateTime(result.completedAt)}`}</p>
        <p className="panel__subtitle">{`정답률: ${score}%`}</p>
      </article>

      <div className="button-row">
        <Link className="button" to="/learning">새 학습 시작</Link>
        <Link className="button button--secondary" to="/dashboard">대시보드로 돌아가기</Link>
      </div>
    </section>
  )
}

export default LearningResultPage
