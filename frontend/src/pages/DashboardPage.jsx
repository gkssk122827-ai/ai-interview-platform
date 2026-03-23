import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import interviewApi from '../api/interviewApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useAuthStore from '../store/authStore.js'

const LEARNING_RESULT_STORAGE_KEY = 'aimentor.learning.results'

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

function daysBetween(leftDateKey, rightDateKey) {
  const left = new Date(`${leftDateKey}T00:00:00`)
  const right = new Date(`${rightDateKey}T00:00:00`)
  if (Number.isNaN(left.getTime()) || Number.isNaN(right.getTime())) {
    return Number.POSITIVE_INFINITY
  }
  return Math.round((left.getTime() - right.getTime()) / 86400000)
}

function readLearningRecords() {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const storedValue = window.localStorage.getItem(LEARNING_RESULT_STORAGE_KEY)
    const records = storedValue ? JSON.parse(storedValue) : []
    if (!Array.isArray(records)) {
      return []
    }

    return records
      .map((record, index) => {
        const completedAt = record?.completedAt
        const timestamp = completedAt ? new Date(completedAt).getTime() : Number.NaN
        const score = Number(record?.score)
        const totalCount = Number(record?.totalCount)
        const correctCount = Number(record?.correctCount)

        if (!Number.isFinite(timestamp) || !Number.isFinite(score)) {
          return null
        }

        return {
          id: record?.id ?? `learning-record-${index}`,
          completedAt,
          score: Math.max(0, Math.min(100, score)),
          totalCount: Number.isFinite(totalCount) ? totalCount : 0,
          correctCount: Number.isFinite(correctCount) ? correctCount : 0,
        }
      })
      .filter(Boolean)
  } catch {
    return []
  }
}

function normalizeSessions(payload) {
  if (!Array.isArray(payload)) {
    return []
  }

  return payload.filter(Boolean)
}

function ClickableDot({ cx, cy, payload, stroke, onSelect }) {
  if (!payload || !Number.isFinite(cx) || !Number.isFinite(cy)) {
    return null
  }

  return (
    <g
      style={{ cursor: 'pointer' }}
      onClick={() => onSelect(payload)}
    >
      <circle
        cx={cx}
        cy={cy}
        r={14}
        fill="transparent"
        pointerEvents="all"
      />
      <circle
        cx={cx}
        cy={cy}
        r={4}
        fill={stroke}
        stroke="#ffffff"
        strokeWidth={2}
        pointerEvents="none"
      />
    </g>
  )
}

function DashboardPage() {
  usePageTitle('대시보드')

  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const [sessions, setSessions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [range, setRange] = useState('30D')

  useEffect(() => {
    async function loadSessions() {
      setIsLoading(true)
      setError('')

      try {
        const response = await interviewApi.listSessions()
        console.log('[Dashboard] interview sessions response:', response)
        setSessions(normalizeSessions(response))
      } catch (loadError) {
        console.error('[Dashboard] interview sessions load failed:', loadError)
        setSessions([])
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadSessions()
  }, [])

  const learningRecords = useMemo(() => readLearningRecords(), [])
  const sessionList = useMemo(() => normalizeSessions(sessions), [sessions])

  const completedInterviewSessions = useMemo(
    () => sessionList.filter((session) => session.status === 'COMPLETED'),
    [sessionList],
  )

  const recentInterviewSessions = useMemo(
    () => [...sessionList].sort((left, right) => new Date(right.startedAt ?? 0) - new Date(left.startedAt ?? 0)).slice(0, 5),
    [sessionList],
  )

  const latestInterviewScore = useMemo(() => {
    const latestSession = [...completedInterviewSessions]
      .filter((session) => Number.isFinite(Number(session.feedback?.overallScore)))
      .sort((left, right) => new Date(right.endedAt ?? right.startedAt) - new Date(left.endedAt ?? left.startedAt))[0]

    return latestSession ? Number(latestSession.feedback?.overallScore) : null
  }, [completedInterviewSessions])

  const latestLearningScore = useMemo(() => {
    const latestRecord = [...learningRecords].sort((left, right) => new Date(right.completedAt) - new Date(left.completedAt))[0]
    return latestRecord ? Number(latestRecord.score) : null
  }, [learningRecords])

  const learningStreak = useMemo(() => {
    const uniqueDateKeys = Array.from(new Set(learningRecords.map((record) => toDateKey(record.completedAt)).filter(Boolean)))
      .sort((left, right) => right.localeCompare(left))

    if (uniqueDateKeys.length === 0) {
      return 0
    }

    let streak = 1
    for (let index = 1; index < uniqueDateKeys.length; index += 1) {
      const gap = daysBetween(uniqueDateKeys[index - 1], uniqueDateKeys[index])
      if (gap !== 1) {
        break
      }
      streak += 1
    }

    return streak
  }, [learningRecords])

  const scoreTrend = useMemo(() => {
    const now = Date.now()
    const periodDays = range === '7D' ? 7 : range === '30D' ? 30 : null
    const minTimestamp = periodDays === null ? null : now - (periodDays * 24 * 60 * 60 * 1000)

    const interviewPoints = completedInterviewSessions
      .map((session) => {
        const endedAt = session.endedAt ?? session.startedAt
        const timestamp = new Date(endedAt).getTime()
        const score = Number(session.feedback?.overallScore)

        if (!Number.isFinite(timestamp) || !Number.isFinite(score)) {
          return null
        }

        return {
          id: `interview-${session.id}`,
          sessionId: session.id,
          timestamp,
          date: formatShortDate(endedAt),
          fullDate: formatFullDate(endedAt),
          interviewScore: score,
          learningScore: null,
          title: session.title || session.positionTitle || '면접 세션',
        }
      })
      .filter(Boolean)

    const learningPoints = learningRecords
      .map((record) => {
        const timestamp = new Date(record.completedAt).getTime()
        if (!Number.isFinite(timestamp)) {
          return null
        }

        return {
          id: `learning-${record.id}`,
          resultId: record.id,
          timestamp,
          date: formatShortDate(record.completedAt),
          fullDate: formatFullDate(record.completedAt),
          interviewScore: null,
          learningScore: Number(record.score),
          title: `학습 ${record.correctCount}/${record.totalCount}`,
        }
      })
      .filter(Boolean)

    return [...interviewPoints, ...learningPoints]
      .sort((left, right) => left.timestamp - right.timestamp)
      .filter((point) => (minTimestamp === null ? true : point.timestamp >= minTimestamp))
      .slice(-50)
      .map((point, index) => ({
        ...point,
        indexLabel: `${index + 1}`,
      }))
  }, [completedInterviewSessions, learningRecords, range])

  const weaknessTags = useMemo(() => {
    const weakPointText = completedInterviewSessions
      .map((session) => session.feedback?.weakPoints)
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    const tags = []
    if (weakPointText.includes('구체') || weakPointText.includes('수치') || weakPointText.includes('근거')) {
      tags.push('구체성 보완')
    }
    if (weakPointText.includes('star') || weakPointText.includes('구조') || weakPointText.includes('순서')) {
      tags.push('답변 구조화')
    }
    if (weakPointText.includes('직무') || weakPointText.includes('적합')) {
      tags.push('직무 연관성')
    }
    if (learningRecords.some((record) => Number(record.score) < 70)) {
      tags.push('학습 오답 복습')
    }

    return tags.slice(0, 4)
  }, [completedInterviewSessions, learningRecords])

  const recommendedNextActions = useMemo(() => {
    const actions = completedInterviewSessions
      .map((session) => session.feedback?.improvements)
      .filter(Boolean)
      .slice(0, 2)

    if (latestLearningScore !== null && latestLearningScore < 70) {
      actions.push('최근 학습 오답 문제를 먼저 복습하고 다시 풀어보세요.')
    }

    if (actions.length > 0) {
      return actions.slice(0, 3)
    }

    return [
      '면접 답변은 상황, 행동, 결과 순서로 다시 구조화해 보세요.',
      '학습 세션에서 틀린 문제를 먼저 복습해 보세요.',
      '다음 목표를 학습 정답률 80% 이상으로 잡아보세요.',
    ]
  }, [completedInterviewSessions, latestLearningScore])

  const recentActivities = useMemo(() => {
    const interviewActivities = completedInterviewSessions
      .map((session) => {
        const endedAt = session.endedAt ?? session.startedAt
        const timestamp = new Date(endedAt).getTime()
        const score = Number(session.feedback?.overallScore)
        if (!Number.isFinite(timestamp) || !Number.isFinite(score)) {
          return null
        }

        return {
          id: `activity-interview-${session.id}`,
          timestamp,
          label: '면접',
          title: session.title || session.positionTitle || '면접 세션',
          meta: formatFullDate(endedAt),
          score,
        }
      })
      .filter(Boolean)

    const learningActivities = learningRecords
      .map((record) => {
        const timestamp = new Date(record.completedAt).getTime()
        if (!Number.isFinite(timestamp)) {
          return null
        }

        return {
          id: `activity-learning-${record.id}`,
          timestamp,
          label: '학습',
          title: `학습 ${record.correctCount}/${record.totalCount}`,
          meta: formatFullDate(record.completedAt),
          score: Number(record.score),
        }
      })
      .filter(Boolean)

    return [...interviewActivities, ...learningActivities]
      .sort((left, right) => right.timestamp - left.timestamp)
      .slice(0, 6)
  }, [completedInterviewSessions, learningRecords])

  const scoreCards = useMemo(() => ([
    { label: '총 면접 횟수', value: `${sessionList.length}회` },
    { label: '총 학습 세션', value: `${learningRecords.length}회` },
    { label: '최근 면접 점수', value: latestInterviewScore === null ? '-' : `${latestInterviewScore}점` },
    { label: '최근 학습 점수', value: latestLearningScore === null ? '-' : `${latestLearningScore}점` },
    { label: '연속 학습 일수', value: `${learningStreak}일` },
  ]), [sessionList.length, learningRecords.length, latestInterviewScore, latestLearningScore, learningStreak])

  function handleTrendPointSelect(point) {
    if (!point) {
      return
    }

    if (point.sessionId) {
      navigate(`/interview/result?sessionId=${point.sessionId}`)
      return
    }

    if (point.resultId) {
      navigate(`/learning/result?resultId=${point.resultId}`)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero dashboard-hero">
        <p className="page-card__eyebrow">사용자 대시보드</p>
        <h2 className="page-card__title dashboard-greeting-title">{`안녕하세요, ${user?.name ?? user?.email ?? '사용자'}님`}</h2>
        <p className="page-card__description">
          면접과 학습 점수를 한눈에 확인하고, 다음 액션을 바로 이어서 진행해 보세요.
        </p>
      </div>

      <div className="button-row">
        <Link className="button" to="/profile-documents">지원 자료 관리</Link>
        <Link className="button button--secondary" to="/interview/setup">{BUTTON_LABELS.startInterview}</Link>
        <Link className="button button--secondary" to="/learning">{BUTTON_LABELS.startLearning}</Link>
      </div>

      {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingDashboard} /> : null}
      {!isLoading && error ? <ErrorBlock message={error} /> : null}

      {!isLoading && !error ? (
        <>
          <section className="admin-summary-grid">
            {scoreCards.map((card) => (
              <article key={card.label} className="panel admin-summary-card">
                <p className="admin-summary-card__label">{card.label}</p>
                <p className="admin-summary-card__value">{card.value}</p>
              </article>
            ))}
          </section>

          <div className="dashboard-grid">
            <section className="panel dashboard-chart-card">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">기간별 성과 추이</h3>
                  <p className="panel__subtitle">면접 점수와 학습 점수를 한 차트에서 비교합니다.</p>
                </div>
              </div>
              <div className="button-row">
                <button className={range === '7D' ? 'button' : 'button button--secondary'} type="button" onClick={() => setRange('7D')}>7D</button>
                <button className={range === '30D' ? 'button' : 'button button--secondary'} type="button" onClick={() => setRange('30D')}>30D</button>
                <button className={range === 'ALL' ? 'button' : 'button button--secondary'} type="button" onClick={() => setRange('ALL')}>ALL</button>
              </div>
              <div className="dashboard-chart">
                {scoreTrend.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={scoreTrend} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(18, 18, 18, 0.08)" />
                      <XAxis dataKey="indexLabel" tickLine={false} axisLine={false} />
                      <YAxis domain={[0, 100]} tickLine={false} axisLine={false} width={36} />
                      <Tooltip
                        labelFormatter={(label, payload) => payload?.[0]?.payload?.fullDate ?? label}
                        formatter={(value, name) => {
                          const score = typeof value === 'number' ? `${value}점` : value
                          if (name === 'interviewScore') {
                            return [score, '면접 점수']
                          }
                          if (name === 'learningScore') {
                            return [score, '학습 점수']
                          }
                          return [score, name]
                        }}
                      />
                      <Line
                        type="monotone"
                        dataKey="interviewScore"
                        stroke="#2563EB"
                        strokeWidth={2.5}
                        dot={(props) => <ClickableDot {...props} onSelect={handleTrendPointSelect} />}
                        activeDot={{ r: 6 }}
                        connectNulls={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="learningScore"
                        stroke="#16A34A"
                        strokeWidth={2.5}
                        dot={(props) => <ClickableDot {...props} onSelect={handleTrendPointSelect} />}
                        activeDot={{ r: 6 }}
                        connectNulls={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState title="아직 표시할 데이터가 없습니다." description="면접이나 학습을 완료하면 추이 차트가 표시됩니다." />
                )}
              </div>
              {scoreTrend.length > 0 ? (
                <p className="panel__subtitle">차트의 점수를 누르면 해당 면접 결과 또는 학습 결과 화면으로 이동합니다.</p>
              ) : null}
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">보완 포인트</h3>
                  <p className="panel__subtitle">최근 결과를 바탕으로 반복되는 보완 포인트를 정리했습니다.</p>
                </div>
              </div>
              {weaknessTags.length > 0 ? (
                <div className="tag-list">
                  {weaknessTags.map((tag) => (
                    <span key={tag} className="dashboard-tag">{tag}</span>
                  ))}
                </div>
              ) : (
                <EmptyState title="보완 포인트가 아직 없습니다." description="면접 결과가 쌓이면 자동으로 분석합니다." />
              )}
            </section>

            <section className="panel dashboard-recommend-card">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">다음 추천 작업</h3>
                  <p className="panel__subtitle">현재 기록을 기준으로 우선순위가 높은 작업입니다.</p>
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
                  <h3 className="panel__title">최근 활동 타임라인</h3>
                  <p className="panel__subtitle">면접과 학습 기록을 최신순으로 보여줍니다.</p>
                </div>
              </div>
              {recentActivities.length > 0 ? (
                <div className="dashboard-session-list">
                  {recentActivities.map((activity) => (
                    <article key={activity.id} className="dashboard-session-item">
                      <div className="dashboard-session-item__row">
                        <strong>{activity.title}</strong>
                        <span className="dashboard-score-chip">{`${activity.label} ${activity.score}점`}</span>
                      </div>
                      <p className="dashboard-session-item__meta">{activity.meta}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState title="최근 활동이 없습니다." description="면접 또는 학습을 진행하면 여기에 바로 반영됩니다." />
              )}
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">최근 면접 세션</h3>
                  <p className="panel__subtitle">최근 세션의 상태와 결과를 빠르게 확인할 수 있습니다.</p>
                </div>
              </div>
              {recentInterviewSessions.length > 0 ? (
                <div className="dashboard-session-list">
                  {recentInterviewSessions.map((session) => {
                    const isCompleted = session.status === 'COMPLETED'
                    const score = Number(session.feedback?.overallScore ?? 0)

                    return (
                      <article key={session.id} className="dashboard-session-item">
                        <div className="dashboard-session-item__row">
                          <strong>{session.title || session.positionTitle || '면접 세션'}</strong>
                          <span className="dashboard-score-chip">
                            {score > 0 ? `점수 ${score}` : session.status}
                          </span>
                        </div>
                        <p className="dashboard-session-item__meta">
                          {`${formatShortDate(session.startedAt)} · ${session.positionTitle || '직무 미설정'}`}
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
                <EmptyState title="최근 면접 세션이 없습니다." description="모의면접을 시작하면 대시보드에 바로 반영됩니다." />
              )}
            </section>
          </div>
        </>
      ) : null}
    </section>
  )
}

export default DashboardPage
