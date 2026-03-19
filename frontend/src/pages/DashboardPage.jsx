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

function DashboardPage() {
  usePageTitle('\uB300\uC2DC\uBCF4\uB4DC')

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
        setSessions(response ?? [])
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadSessions()
  }, [])

  const learningRecords = useMemo(() => readLearningRecords(), [])

  const completedInterviewSessions = useMemo(
    () => sessions.filter((session) => session.status === 'COMPLETED'),
    [sessions],
  )

  const recentInterviewSessions = useMemo(() => sessions.slice(0, 5), [sessions])

  const latestInterviewScore = useMemo(() => {
    const latestSession = [...completedInterviewSessions]
      .filter((session) => Number.isFinite(Number(session.feedback?.overallScore)))
      .sort((left, right) => new Date(right.endedAt ?? right.startedAt) - new Date(left.endedAt ?? left.startedAt))[0]

    if (!latestSession) {
      return null
    }

    return Number(latestSession.feedback?.overallScore)
  }, [completedInterviewSessions])

  const latestLearningScore = useMemo(() => {
    const latestRecord = [...learningRecords]
      .sort((left, right) => new Date(right.completedAt) - new Date(left.completedAt))[0]

    if (!latestRecord) {
      return null
    }

    return Number(latestRecord.score)
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
          timestamp,
          date: formatShortDate(endedAt),
          fullDate: formatFullDate(endedAt),
          interviewScore: score,
          learningScore: null,
          type: '\uBA74\uC811',
          title: session.title || session.positionTitle || '\uBA74\uC811 \uC138\uC158',
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
          timestamp,
          date: formatShortDate(record.completedAt),
          fullDate: formatFullDate(record.completedAt),
          interviewScore: null,
          learningScore: Number(record.score),
          type: '\uD559\uC2B5',
          title: `\uD559\uC2B5 ${record.correctCount}/${record.totalCount}`,
        }
      })
      .filter(Boolean)

    const points = [...interviewPoints, ...learningPoints]
      .sort((left, right) => left.timestamp - right.timestamp)
      .filter((point) => (minTimestamp === null ? true : point.timestamp >= minTimestamp))
      .slice(-50)

    return points.map((point, index) => ({
      ...point,
      indexLabel: `${index + 1}`,
    }))
  }, [completedInterviewSessions, learningRecords, range])

  const weaknessTags = useMemo(() => {
    const source = completedInterviewSessions
      .map((session) => session.feedback?.weakPoints)
      .filter(Boolean)
      .join(' ')

    const tags = []
    if (source.includes('\uAD6C\uCCB4') || source.includes('\uC218\uCE58') || source.includes('\uC9C0\uD45C')) {
      tags.push('\uAD6C\uCCB4\uC131')
    }
    if (source.includes('STAR') || source.includes('\uAD6C\uC870') || source.includes('\uC21C\uC11C')) {
      tags.push('\uB2F5\uBCC0 \uAD6C\uC870')
    }
    if (source.includes('\uC9C1\uBB34') || source.includes('\uC801\uD569')) {
      tags.push('\uC9C1\uBB34 \uC801\uD569\uC131')
    }
    if (learningRecords.some((record) => Number(record.score) < 70)) {
      tags.push('\uD559\uC2B5 \uC815\uB2F5\uB960')
    }

    return tags.slice(0, 4)
  }, [completedInterviewSessions, learningRecords])

  const recommendedNextActions = useMemo(() => {
    const actions = completedInterviewSessions
      .map((session) => session.feedback?.improvements)
      .filter(Boolean)
      .slice(0, 2)

    if (latestLearningScore !== null && latestLearningScore < 70) {
      actions.push('\uCD5C\uADFC \uD559\uC2B5 \uC624\uB2F5 \uBB38\uC81C\uB97C \uBA3C\uC800 \uBCF5\uC2B5\uD558\uACE0 \uB2E4\uC2DC \uD480\uC5B4\uBCF4\uC138\uC694.')
    }

    if (actions.length > 0) {
      return actions.slice(0, 3)
    }

    return [
      '\uBA74\uC811 \uB2F5\uBCC0\uC740 \uC0C1\uD669-\uD589\uB3D9-\uACB0\uACFC \uC21C\uC11C\uB85C \uC7AC\uAD6C\uC131\uD574 \uC5F0\uC2B5\uD574\uBCF4\uC138\uC694.',
      '\uD559\uC2B5 \uC138\uC158\uC5D0\uC11C \uD2C0\uB9B0 \uBB38\uC81C\uB97C \uC624\uB2F5\uB178\uD2B8\uB85C \uBA3C\uC800 \uBCF5\uC2B5\uD558\uC138\uC694.',
      '\uB2E4\uC74C \uBAA9\uD45C\uB294 \uD559\uC2B5 \uC815\uB2F5\uB960 80% \uC774\uC0C1\uC73C\uB85C \uC124\uC815\uD574\uBCF4\uC138\uC694.',
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
          label: '\uBA74\uC811',
          title: session.title || session.positionTitle || '\uBA74\uC811 \uC138\uC158',
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
          label: '\uD559\uC2B5',
          title: `\uD559\uC2B5 ${record.correctCount}/${record.totalCount}`,
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
    {
      label: '\uCD1D \uBA74\uC811 \uD69F\uC218',
      value: `${sessions.length}\uD68C`,
    },
    {
      label: '\uCD1D \uD559\uC2B5 \uC138\uC158',
      value: `${learningRecords.length}\uD68C`,
    },
    {
      label: '\uCD5C\uADFC \uBA74\uC811 \uC810\uC218',
      value: latestInterviewScore === null ? '-' : `${latestInterviewScore}\uC810`,
    },
    {
      label: '\uCD5C\uADFC \uD559\uC2B5 \uC810\uC218',
      value: latestLearningScore === null ? '-' : `${latestLearningScore}\uC810`,
    },
    {
      label: '\uC5F0\uC18D \uD559\uC2B5 \uC77C\uC218',
      value: `${learningStreak}\uC77C`,
    },
  ]), [sessions.length, learningRecords.length, latestInterviewScore, latestLearningScore, learningStreak])

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">{`\uC0AC\uC6A9\uC790 \uB300\uC2DC\uBCF4\uB4DC`}</p>
        <h2 className="page-card__title">{`\uC548\uB155\uD558\uC138\uC694, ${user?.name ?? user?.email ?? '\uC0AC\uC6A9\uC790'}\uB2D8`}</h2>
        <p className="page-card__description">
          {'\uBA74\uC811\uACFC \uD559\uC2B5 \uC810\uC218\uB97C \uD68C\uCC28\uBCC4\uB85C \uD655\uC778\uD558\uACE0, \uB2E4\uC74C \uD560 \uC77C\uC744 \uBC14\uB85C \uC9C4\uD589\uD574\uBCF4\uC138\uC694.'}
        </p>
      </div>

      <div className="button-row">
        <Link className="button" to="/profile-documents">{'\uC9C0\uC6D0\uC790\uB8CC \uAD00\uB9AC'}</Link>
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
                  <h3 className="panel__title">{'\uD68C\uCC28\uBCC4 \uC131\uACFC \uCD94\uC774 (\uBA74\uC811/\uD559\uC2B5)'}</h3>
                  <p className="panel__subtitle">{'\uAC19\uC740 \uADF8\uB798\uD504\uC5D0 \uBA74\uC811\uACFC \uD559\uC2B5 \uC810\uC218\uB97C \uC0C9\uC0C1\uC73C\uB85C \uAD6C\uBD84\uD574 \uD45C\uC2DC\uD569\uB2C8\uB2E4.'}</p>
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
                          const score = typeof value === 'number' ? `${value}\uC810` : value
                          if (name === 'interviewScore') {
                            return [score, '\uBA74\uC811 \uC810\uC218']
                          }
                          if (name === 'learningScore') {
                            return [score, '\uD559\uC2B5 \uC810\uC218']
                          }
                          return [score, name]
                        }}
                      />
                      <Line
                        type="monotone"
                        dataKey="interviewScore"
                        stroke="#2563EB"
                        strokeWidth={2.5}
                        dot={{ r: 4, fill: '#2563EB' }}
                        activeDot={{ r: 6 }}
                        connectNulls={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="learningScore"
                        stroke="#16A34A"
                        strokeWidth={2.5}
                        dot={{ r: 4, fill: '#16A34A' }}
                        activeDot={{ r: 6 }}
                        connectNulls={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <EmptyState
                    title={'\uD45C\uC2DC\uD560 \uC810\uC218 \uB370\uC774\uD130\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.'}
                    description={'\uBA74\uC811\uC744 \uC644\uB8CC\uD558\uACE0 \uD559\uC2B5\uC744 \uC9C4\uD589\uD558\uBA74 \uD68C\uCC28\uBCC4 \uCD94\uC774\uB97C \uD655\uC778\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.'}
                  />
                )}
              </div>
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">{'\uBCF4\uC644 \uC0AC\uC778'}</h3>
                  <p className="panel__subtitle">{'\uCD5C\uADFC \uAE30\uB85D\uC5D0\uC11C \uBC18\uBCF5\uB418\uB294 \uAC1C\uC120 \uD3EC\uC778\uD2B8\uC785\uB2C8\uB2E4.'}</p>
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
                  title={'\uBCF4\uC644 \uC0AC\uC778\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.'}
                  description={'\uCD94\uAC00 \uC138\uC158\uC744 \uC9C4\uD589\uD558\uBA74 \uC790\uB3D9\uC73C\uB85C \uBD84\uC11D\uB429\uB2C8\uB2E4.'}
                />
              )}
            </section>

            <section className="panel dashboard-recommend-card">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">{'\uB2E4\uC74C \uCD94\uCC9C \uC791\uC5C5'}</h3>
                  <p className="panel__subtitle">{'\uD604\uC7AC \uAE30\uB85D \uAE30\uBC18 \uCD5C\uC6B0\uC120 \uC791\uC5C5\uC785\uB2C8\uB2E4.'}</p>
                </div>
              </div>
              <div className="dashboard-action-list">
                {recommendedNextActions.map((action) => (
                  <article key={action} className="dashboard-action-item">
                    <strong>{'\uCD94\uCC9C \uD56D\uBAA9'}</strong>
                    <p>{action}</p>
                  </article>
                ))}
              </div>
            </section>

            <section className="panel dashboard-recent-card">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">{'\uCD5C\uADFC \uD65C\uB3D9 \uD0C0\uC784\uB77C\uC778'}</h3>
                  <p className="panel__subtitle">{'\uBA74\uC811\uACFC \uD559\uC2B5 \uAE30\uB85D\uC744 \uD568\uAED8 \uD655\uC778\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.'}</p>
                </div>
              </div>
              {recentActivities.length > 0 ? (
                <div className="dashboard-session-list">
                  {recentActivities.map((activity) => (
                    <article key={activity.id} className="dashboard-session-item">
                      <div className="dashboard-session-item__row">
                        <strong>{activity.title}</strong>
                        <span className="dashboard-score-chip">{`${activity.label} ${activity.score}\uC810`}</span>
                      </div>
                      <p className="dashboard-session-item__meta">{activity.meta}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title={'\uCD5C\uADFC \uD65C\uB3D9\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.'}
                  description={'\uBA74\uC811 \uB610\uB294 \uD559\uC2B5\uC744 \uC9C4\uD589\uD558\uBA74 \uC5EC\uAE30\uC5D0 \uC790\uB3D9 \uD45C\uC2DC\uB429\uB2C8\uB2E4.'}
                />
              )}
            </section>

            <section className="panel">
              <div className="panel__header">
                <div>
                  <h3 className="panel__title">{'\uCD5C\uADFC \uBA74\uC811 \uC138\uC158'}</h3>
                  <p className="panel__subtitle">{'\uAC01 \uC138\uC158\uC758 \uC810\uC218\uC640 \uC0C1\uD0DC\uB97C \uBC14\uB85C \uD655\uC778\uD569\uB2C8\uB2E4.'}</p>
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
                          <strong>{session.title || session.positionTitle || '\uBA74\uC811 \uC138\uC158'}</strong>
                          <span className="dashboard-score-chip">
                            {score > 0 ? `\uC810\uC218 ${score}` : '\uACB0\uACFC \uB300\uAE30'}
                          </span>
                        </div>
                        <p className="dashboard-session-item__meta">
                          {`${formatShortDate(session.startedAt)} \u00B7 ${session.positionTitle || '\uC9C1\uBB34 \uBBF8\uC124\uC815'}`}
                        </p>
                        <div className="button-row">
                          <Link
                            className="button button--secondary"
                            to={isCompleted ? `/interview/result?sessionId=${session.id}` : `/interview/session?sessionId=${session.id}`}
                          >
                            {isCompleted ? '\uACB0\uACFC \uBCF4\uAE30' : '\uC774\uC5B4\uC11C \uC9C4\uD589'}
                          </Link>
                        </div>
                      </article>
                    )
                  })}
                </div>
              ) : (
                <EmptyState
                  title={'\uCD5C\uADFC \uBA74\uC811 \uC138\uC158\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.'}
                  description={'\uBAA8\uC758\uBA74\uC811\uC744 \uC2DC\uC791\uD558\uBA74 \uC5EC\uAE30\uC5D0 \uC138\uC158\uC774 \uB204\uC801\uB429\uB2C8\uB2E4.'}
                />
              )}
            </section>
          </div>
        </>
      ) : null}
    </section>
  )
}

export default DashboardPage
