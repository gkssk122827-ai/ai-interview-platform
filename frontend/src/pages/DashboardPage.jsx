import { Link } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { BUTTON_LABELS } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useAuthStore from '../store/authStore.js'
import useAppStore from '../store/useAppStore.js'

function DashboardPage() {
  usePageTitle('대시보드')
  const user = useAuthStore((state) => state.user)
  const recentInterviewSessions = useAppStore((state) => state.recentInterviewSessions)
  const scoreTrend = useAppStore((state) => state.scoreTrend)
  const weaknessTags = useAppStore((state) => state.weaknessTags)
  const recommendedNextActions = useAppStore((state) => state.recommendedNextActions)

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">사용자 대시보드</p>
        <h2 className="page-card__title">안녕하세요, {user?.name ?? user?.email ?? '사용자'}님</h2>
        <p className="page-card__description">최근 면접 기록을 확인하고 지원자료, 모의면접, 학습 화면으로 바로 이동해 보세요.</p>
      </div>

      <div className="button-row">
        <Link className="button" to="/profile-documents">지원자료 관리</Link>
        <Link className="button button--secondary" to="/interview/setup">{BUTTON_LABELS.startInterview}</Link>
        <Link className="button button--secondary" to="/learning">{BUTTON_LABELS.startLearning}</Link>
      </div>

      <div className="dashboard-grid">
        <section className="panel dashboard-chart-card">
          <div className="panel__header"><div><h3 className="panel__title">면접 점수 추이</h3><p className="panel__subtitle">최근 모의면접 점수를 날짜별로 확인할 수 있습니다.</p></div></div>
          <div className="dashboard-chart">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={scoreTrend} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(18, 18, 18, 0.08)" />
                <XAxis dataKey="date" tickLine={false} axisLine={false} />
                <YAxis domain={[60, 100]} tickLine={false} axisLine={false} width={36} />
                <Tooltip />
                <Line type="monotone" dataKey="score" stroke="#2563EB" strokeWidth={2.5} dot={{ r: 4, fill: '#2563EB' }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="panel">
          <div className="panel__header"><div><h3 className="panel__title">보완 포인트</h3><p className="panel__subtitle">최근 세션에서 반복적으로 보이는 개선 항목입니다.</p></div></div>
          <div className="tag-list">{weaknessTags.map((tag) => <span key={tag} className="dashboard-tag">{tag}</span>)}</div>
        </section>

        <section className="panel dashboard-recent-card">
          <div className="panel__header"><div><h3 className="panel__title">최근 면접 세션</h3><p className="panel__subtitle">날짜, 회사명, 종합 점수를 함께 확인할 수 있습니다.</p></div></div>
          <div className="dashboard-session-list">
            {recentInterviewSessions.map((session) => (
              <article key={session.id} className="dashboard-session-item">
                <div className="dashboard-session-item__row"><strong>{session.date}</strong><span className="dashboard-score-chip">점수 {session.score}</span></div>
                <p className="dashboard-session-item__meta">{session.companyName}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel__header"><div><h3 className="panel__title">다음 추천 학습</h3><p className="panel__subtitle">현재 기록 기준으로 우선 진행하면 좋은 항목입니다.</p></div></div>
          <div className="dashboard-action-list">{recommendedNextActions.map((action) => <article key={action} className="dashboard-action-item"><strong>추천 항목</strong><p>{action}</p></article>)}</div>
        </section>
      </div>
    </section>
  )
}

export default DashboardPage
