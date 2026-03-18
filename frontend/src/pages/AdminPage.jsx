import { useCallback, useEffect, useMemo, useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import adminApi from '../api/adminApi.js'
import jobPostingApi from '../api/jobPostingApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'

const EDITOR_WINDOW_NAME = 'adminJobPostingEditor'
const EDITOR_WINDOW_FEATURES = 'popup=yes,width=960,height=860,scrollbars=yes,resizable=yes'

function formatDeadline(deadline) {
  return deadline || '상시 채용'
}

function formatRevenue(value) {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat('ko-KR').format(amount)
}

function openEditorWindow(path) {
  return window.open(path, EDITOR_WINDOW_NAME, EDITOR_WINDOW_FEATURES)
}

function SummaryCard({ title, value, description }) {
  return (
    <article className="panel admin-summary-card">
      <div>
        <p className="admin-summary-card__label">{title}</p>
        <h3 className="admin-summary-card__value">{value}</h3>
      </div>
      <p className="panel__subtitle">{description}</p>
    </article>
  )
}

function RecentListPanel({ title, description, items, emptyState, renderItem, isLoading, error }) {
  return (
    <article className="panel admin-list-panel">
      <div className="panel__header">
        <div>
          <h3 className="panel__title">{title}</h3>
          <p className="panel__subtitle">{description}</p>
        </div>
      </div>

      {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingAdminDashboard} /> : null}
      {!isLoading && error ? <ErrorBlock message={error} /> : null}
      {!isLoading && !error && items.length === 0 ? (
        <EmptyState title={emptyState.title} description={emptyState.description} />
      ) : null}
      {!isLoading && !error && items.length > 0 ? (
        <div className="resource-list">
          {items.map(renderItem)}
        </div>
      ) : null}
    </article>
  )
}

function AdminPage() {
  usePageTitle('관리자')

  const [dashboard, setDashboard] = useState(null)
  const [isDashboardLoading, setIsDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')

  const [jobPostings, setJobPostings] = useState([])
  const [isJobPostingLoading, setIsJobPostingLoading] = useState(true)
  const [jobPostingError, setJobPostingError] = useState('')
  const [jobPostingNotice, setJobPostingNotice] = useState('')

  const loadDashboard = useCallback(async () => {
    setIsDashboardLoading(true)
    setDashboardError('')

    try {
      const result = await adminApi.getDashboard()
      setDashboard(result)
    } catch (error) {
      setDashboardError(error.message)
    } finally {
      setIsDashboardLoading(false)
    }
  }, [])

  const loadJobPostings = useCallback(async () => {
    setIsJobPostingLoading(true)
    setJobPostingError('')

    try {
      const items = await jobPostingApi.list()
      setJobPostings(items)
    } catch (error) {
      setJobPostingError(error.message)
    } finally {
      setIsJobPostingLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDashboard()
    loadJobPostings()
  }, [loadDashboard, loadJobPostings])

  useEffect(() => {
    function handleEditorMessage(event) {
      if (event.origin !== window.location.origin) {
        return
      }

      if (event.data?.type === 'jobPosting:saved') {
        setJobPostingNotice('채용공고가 저장되었습니다.')
        loadJobPostings()
        loadDashboard()
      }

      if (event.data?.type === 'jobPosting:deleted') {
        setJobPostingNotice('채용공고가 삭제되었습니다.')
        loadJobPostings()
        loadDashboard()
      }
    }

    function handleWindowFocus() {
      loadJobPostings()
      loadDashboard()
    }

    window.addEventListener('message', handleEditorMessage)
    window.addEventListener('focus', handleWindowFocus)

    return () => {
      window.removeEventListener('message', handleEditorMessage)
      window.removeEventListener('focus', handleWindowFocus)
    }
  }, [loadDashboard, loadJobPostings])

  const summaryCards = useMemo(() => [
    {
      title: '전체 사용자',
      value: `${dashboard?.totalUsers ?? 0}명`,
      description: '현재 서비스에 가입한 전체 사용자 수입니다.',
    },
    {
      title: '지원자료',
      value: `${dashboard?.totalApplicationDocuments ?? 0}건`,
      description: '이력서와 자기소개서를 포함한 전체 지원자료 수입니다.',
    },
    {
      title: '채용공고',
      value: `${dashboard?.totalJobPostings ?? 0}건`,
      description: '일반 사용자 화면에 노출되는 전체 채용공고 수입니다.',
    },
    {
      title: '진행 중 면접',
      value: `${dashboard?.ongoingInterviews ?? 0}건`,
      description: '아직 종료되지 않은 모의 면접 세션 수입니다.',
    },
    {
      title: '주문',
      value: `${dashboard?.totalOrders ?? 0}건`,
      description: '학습 콘텐츠 결제를 포함한 전체 주문 수입니다.',
    },
    {
      title: '총 매출',
      value: `${formatRevenue(dashboard?.totalRevenue)}원`,
      description: '누적 결제 금액 기준 총 매출입니다.',
    },
  ], [dashboard])

  function handleOpenCreateWindow() {
    setJobPostingNotice('')
    const popup = openEditorWindow('/admin/job-postings/new')

    if (!popup) {
      setJobPostingError('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해 주세요.')
    }
  }

  function handleOpenEditWindow(jobPostingId) {
    setJobPostingNotice('')
    const popup = openEditorWindow(`/admin/job-postings/${jobPostingId}/edit`)

    if (!popup) {
      setJobPostingError('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해 주세요.')
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">관리자</p>
        <h2 className="page-card__title">운영 현황과 채용공고를 한곳에서 관리하세요.</h2>
        <p className="page-card__description">
          최근 가입 사용자, 지원자료, 면접 세션 흐름을 빠르게 확인하고 채용공고를 별도 편집 창에서 관리할 수 있습니다.
        </p>
      </div>

      <section className="admin-summary-grid">
        {summaryCards.map((card) => (
          <SummaryCard
            key={card.title}
            title={card.title}
            value={card.value}
            description={card.description}
          />
        ))}
      </section>

      <section className="admin-overview-grid">
        <article className="panel admin-chart-panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">최근 7일 가입 추이</h3>
              <p className="panel__subtitle">관리자 대시보드에서 최근 가입 사용자 흐름을 한눈에 확인할 수 있습니다.</p>
            </div>
          </div>

          {isDashboardLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingAdminDashboard} /> : null}
          {!isDashboardLoading && dashboardError ? <ErrorBlock message={dashboardError} /> : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.dailySignups?.length ?? 0) > 0 ? (
            <div className="dashboard-chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={dashboard.dailySignups} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(18, 18, 18, 0.08)" />
                  <XAxis dataKey="date" tickLine={false} axisLine={false} />
                  <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={36} />
                  <Tooltip />
                  <Line
                    type="monotone"
                    dataKey="count"
                    stroke="#0f766e"
                    strokeWidth={2.5}
                    dot={{ r: 4, fill: '#0f766e' }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.dailySignups?.length ?? 0) === 0 ? (
            <EmptyState
              title={EMPTY_MESSAGES.adminDailySignups.title}
              description={EMPTY_MESSAGES.adminDailySignups.description}
            />
          ) : null}
        </article>

        <article className="panel admin-job-posting-panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">채용공고 관리</h3>
              <p className="panel__subtitle">공고를 클릭하면 별도 편집 창이 열리고, 새 공고도 같은 방식으로 등록합니다.</p>
            </div>
            <button className="button button--secondary" type="button" onClick={handleOpenCreateWindow}>
              새 채용공고 등록
            </button>
          </div>

          <StatusMessage variant="success" message={jobPostingNotice} />
          <StatusMessage variant="error" message={jobPostings.length > 0 ? jobPostingError : ''} />

          {isJobPostingLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingList} /> : null}
          {!isJobPostingLoading && jobPostingError && jobPostings.length === 0 ? <ErrorBlock message={jobPostingError} /> : null}
          {!isJobPostingLoading && !jobPostingError && jobPostings.length === 0 ? (
            <EmptyState
              title="등록된 채용공고가 없습니다."
              description="새 채용공고를 등록하면 일반 사용자가 면접 설정 화면에서 선택할 수 있습니다."
            />
          ) : null}
          {!isJobPostingLoading && jobPostings.length > 0 ? (
            <div className="resource-list">
              {jobPostings.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className="resource-list__item"
                  onClick={() => handleOpenEditWindow(item.id)}
                >
                  <strong>{item.positionTitle}</strong>
                  <span>{item.companyName} · {formatDeadline(item.deadline)}</span>
                  <span>{item.jobUrl || '링크가 없는 공고입니다.'}</span>
                </button>
              ))}
            </div>
          ) : null}
        </article>
      </section>

      <section className="admin-list-stack">
        <RecentListPanel
          title="최근 가입 사용자"
          description="최근 가입한 사용자 정보를 확인할 수 있습니다."
          items={dashboard?.recentUsers ?? []}
          emptyState={EMPTY_MESSAGES.adminRecentUsers}
          isLoading={isDashboardLoading}
          error={dashboardError}
          renderItem={(user) => (
            <div key={user.id} className="resource-list__item resource-list__item--static">
              <strong>{user.name}</strong>
              <span>{user.email}</span>
              <span>{user.role} · {user.createdAt ?? '-'}</span>
            </div>
          )}
        />

        <RecentListPanel
          title="최근 지원자료"
          description="최근 등록된 지원자료를 빠르게 확인할 수 있습니다."
          items={dashboard?.recentApplicationDocuments ?? []}
          emptyState={EMPTY_MESSAGES.adminRecentDocuments}
          isLoading={isDashboardLoading}
          error={dashboardError}
          renderItem={(document) => (
            <div key={document.id} className="resource-list__item resource-list__item--static">
              <strong>{document.title}</strong>
              <span>{document.userName} · {document.userEmail}</span>
              <span>{document.createdAt ?? '-'}</span>
            </div>
          )}
        />

        <RecentListPanel
          title="최근 면접 세션"
          description="최근 생성된 면접 세션 상태를 확인할 수 있습니다."
          items={dashboard?.recentInterviewSessions ?? []}
          emptyState={EMPTY_MESSAGES.adminRecentSessions}
          isLoading={isDashboardLoading}
          error={dashboardError}
          renderItem={(session) => (
            <div key={session.id} className="resource-list__item resource-list__item--static">
              <strong>{session.title}</strong>
              <span>{session.userName} · {session.userEmail}</span>
              <span>{session.positionTitle} · {session.status}</span>
            </div>
          )}
        />
      </section>
    </section>
  )
}

export default AdminPage
