import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import interviewApi from '../api/interviewApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useAuthStore from '../store/authStore.js'

function formatShortDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' })
}

function formatFullDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

function toDateKey(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function DashboardPage() {
  usePageTitle('대시보드')

  const user = useAuthStore((state) => state.user)
  const [sessions, setSessions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadSessions() {
      setIsLoading(true)
      setError('')

      try {
        const response = await interviewApi.listSessions()
        setSessions(response ?? [])
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadSessions()
  }, [])

  const recentInterviewSessions = useMemo(() => sessions.slice(0, 5), [sessions])

  const scoreTrend = useMemo(() => {
    const grouped = new Map()

    sessions
      .filter((session) => session.status === 'COMPLETED')
      .filter((session) => Boolean(session.endedAt))
      .forEach((session) => {
        const dateKey = toDateKey(session.endedAt)
        if (!dateKey) {
          return
        }

        const score = Number(session.feedback?.overallScore ?? 0)
        if (!Number.isFinite(score)) {
          return
        }

        const current = grouped.get(dateKey) ?? {
          dateKey,
          displayDate: formatShortDate(session.endedAt),
          fullDate: formatFullDate(session.endedAt),
          scoreTotal: 0,
          count: 0,
        }

        current.scoreTotal += score
        current.count += 1
        grouped.set(dateKey, current)
      })

    return Array.from(grouped.values())
      .sort((left, right) => left.dateKey.localeCompare(right.dateKey))
      .slice(-7)
      .map((item) => ({
        dateKey: item.dateKey,
        date: item.displayDate,
        fullDate: item.fullDate,
        averageScore: Number((item.scoreTotal / item.count).toFixed(1)),
        sessionCount: item.count,
      }))
  }, [sessions])

  const weaknessTags = useMemo(() => {
    const source = sessions
      .map((session) => session.feedback?.weakPoints)
      .filter(Boolean)
      .join(' ')

    const keywordMap = [
      ['구체성', ['구체', '수치', '지표']],
      ['답변 구조', ['구조', '순서', 'STAR']],
      ['직무 적합성', ['직무', '채용공고', '적합']],
      ['기술 설명', ['기술', '설계', '구현']],
    ]

    return keywordMap
      .filter(([, keywords]) => keywords.some((keyword) => source.includes(keyword)))
      .map(([label]) => label)
      .slice(0, 4)
  }, [sessions])

  const recommendedNextActions = useMemo(() => {
    const actions = sessions
      .map((session) => session.feedback?.improvements)
      .filter(Boolean)
      .slice(0, 3)

    if (actions.length > 0) {
      return actions
    }

    return [
      '최근 프로젝트 경험을 상황, 행동, 결과 순서로 다시 정리해 보세요.',
      '답변마다 성과나 수치 근거를 한 가지 이상 포함하는 연습을 해 보세요.',
      '지원 직무와 연결되는 경험을 두세 개 정도 미리 정리해 두면 좋습니다.',
    ]
  }, [sessions])

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">사용자 대시보드</p>
        <h2 className="page-card__title">안녕하세요, {user?.name ?? user?.email ?? '사용자'}님</h2>
        <p className="page-card__description">
          최근 면접 기록을 확인하고 지원자료 관리, 모의면접, 학습 화면으로 바로 이동해 보세요.
        </p>
      </div>

      <div className="button-row">
        <Link className="button" to="/profile-documents">지원자료 관리</Link>
        <Link className="button button--secondary" to="/interview/setup">{BUTTON_LABELS.startInterview}</Link>
        <Link className="button button--secondary" to="/learning">{BUTTON_LABELS.startLearning}</Link>
      </div>

      {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingDashboard} /> : null}
      {!isLoading && error ? <ErrorBlock message={error} /> : null}

      {!isLoading && !error ? (
        <div className="dashboard-grid">
          <section className="panel dashboard-chart-card">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">최근 완료된 면접의 날짜별 평균 점수</h3>
                <p className="panel__subtitle">
                  완료된 면접 세션만 집계하여 종료일 기준으로 날짜별 평균 점수를 보여줍니다.
                </p>
              </div>
            </div>
            <div className="dashboard-chart">
              {scoreTrend.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={scoreTrend} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(18, 18, 18, 0.08)" />
                    <XAxis dataKey="date" tickLine={false} axisLine={false} />
                    <YAxis domain={[0, 100]} tickLine={false} axisLine={false} width={36} />
                    <Tooltip
                      formatter={(value, name, item) => {
                        const score = typeof value === 'number' ? `${value}점` : value
                        const count = item?.payload?.sessionCount ?? 0
                        return [`${score} · ${count}건 평균`, '평균 점수']
                      }}
                      labelFormatter={(label, payload) => payload?.[0]?.payload?.fullDate ?? label}
                    />
                    <Line
                      type="monotone"
                      dataKey="averageScore"
                      stroke="#2563EB"
                      strokeWidth={2.5}
                      dot={{ r: 4, fill: '#2563EB' }}
                      activeDot={{ r: 6 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState
                  title="아직 완료된 면접 결과가 없습니다."
                  description="모의면접을 완료하면 날짜별 평균 점수 추이를 이곳에서 확인할 수 있습니다."
                />
              )}
            </div>
          </section>

          <section className="panel">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">보완 포인트</h3>
                <p className="panel__subtitle">최근 면접 결과에서 반복적으로 보이는 개선 항목입니다.</p>
              </div>
            </div>
            {weaknessTags.length > 0 ? (
              <div className="tag-list">
                {weaknessTags.map((tag) => (
                  <span key={tag} className="dashboard-tag">{tag}</span>
                ))}
              </div>
            ) : (
              <EmptyState
                title="보완 포인트가 없습니다."
                description="면접 결과가 쌓이면 반복적으로 나타나는 약점을 이곳에 정리해 드립니다."
              />
            )}
          </section>

          <section className="panel dashboard-recommend-card">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">다음 추천 학습</h3>
                <p className="panel__subtitle">현재 기록을 기준으로 우선 진행하면 좋은 학습 항목입니다.</p>
              </div>
            </div>
            <div className="dashboard-action-list">
              {recommendedNextActions.map((action) => (
                <article key={action} className="dashboard-action-item">
                  <strong>추천 항목</strong>
                  <p>{action}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="panel dashboard-recent-card">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">최근 면접 세션</h3>
                <p className="panel__subtitle">세션 상태를 확인하고 바로 이어서 진행하거나 결과를 볼 수 있습니다.</p>
              </div>
            </div>
            {recentInterviewSessions.length > 0 ? (
              <div className="dashboard-session-list">
                {recentInterviewSessions.map((session) => {
                  const isCompleted = session.status === 'COMPLETED'
                  const score = session.feedback?.overallScore ?? 0

                  return (
                    <article key={session.id} className="dashboard-session-item">
                      <div className="dashboard-session-item__row">
                        <strong>{session.title || session.positionTitle || '면접 세션'}</strong>
                        <span className="dashboard-score-chip">
                          {score > 0 ? `점수 ${score}` : '결과 대기'}
                        </span>
                      </div>
                      <p className="dashboard-session-item__meta">
                        {formatShortDate(session.startedAt)} · {session.positionTitle || '직무 미지정'}
                      </p>
                      <p className="dashboard-session-item__meta">
                        답변 {session.answeredQuestions ?? 0}/{session.totalQuestions ?? 0} · 완료율 {session.completionRate ?? 0}%
                      </p>
                      <div className="button-row">
                        <Link
                          className="button button--secondary"
                          to={isCompleted ? `/interview/result?sessionId=${session.id}` : `/interview/session?sessionId=${session.id}`}
                        >
                          {isCompleted ? '결과 보기' : '이어서 진행'}
                        </Link>
                      </div>
                    </article>
                  )
                })}
              </div>
            ) : (
              <EmptyState
                title="최근 면접 세션이 없습니다."
                description="모의면접을 시작하면 이곳에서 진행 상태와 결과를 확인할 수 있습니다."
              />
            )}
          </section>
        </div>
      ) : null}
    </section>
  )
}

export default DashboardPage