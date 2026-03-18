import { useEffect, useMemo, useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextAreaField from '../components/forms/TextAreaField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import adminApi from '../api/adminApi.js'
import jobPostingApi from '../api/jobPostingApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useCrudResource from '../hooks/useCrudResource.js'
import { BUTTON_LABELS, EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'

const emptyJobPosting = {
  companyName: '',
  positionTitle: '',
  description: '',
  jobUrl: '',
  deadline: '',
}

function toJobPostingForm(item) {
  return {
    companyName: item?.companyName ?? '',
    positionTitle: item?.positionTitle ?? '',
    description: item?.description ?? '',
    jobUrl: item?.jobUrl ?? '',
    deadline: item?.deadline ?? '',
  }
}

function AdminPage() {
  usePageTitle('관리자')
  const [dashboard, setDashboard] = useState(null)
  const [isDashboardLoading, setIsDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')
  const {
    items: jobPostings,
    selectedId,
    setSelectedId,
    isLoading: isJobPostingLoading,
    isSaving,
    error: jobPostingError,
    setError: setJobPostingError,
    successMessage,
    setSuccessMessage,
    saveItem,
    deleteItem,
  } = useCrudResource(jobPostingApi)
  const [jobPostingForm, setJobPostingForm] = useState(emptyJobPosting)

  const selectedJobPosting = useMemo(
    () => jobPostings.find((item) => item.id === selectedId) ?? null,
    [jobPostings, selectedId],
  )

  useEffect(() => {
    async function loadDashboard() {
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
    }

    loadDashboard()
  }, [])

  useEffect(() => {
    setJobPostingForm(toJobPostingForm(selectedJobPosting))
    setSuccessMessage('')
    setJobPostingError('')
  }, [selectedJobPosting, setJobPostingError, setSuccessMessage])

  function updateJobPostingField(name, value) {
    setJobPostingForm((current) => ({ ...current, [name]: value }))
  }

  function handleCreateJobPosting() {
    setSelectedId(null)
    setJobPostingForm(emptyJobPosting)
    setSuccessMessage('')
    setJobPostingError('')
  }

  async function handleSaveJobPosting(event) {
    event.preventDefault()
    await saveItem(jobPostingForm)
  }

  async function handleDeleteJobPosting() {
    if (!selectedId) return
    await deleteItem(selectedId)
    setJobPostingForm(emptyJobPosting)
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">관리자</p>
        <h2 className="page-card__title">운영 지표와 채용공고를 한 곳에서 관리해 보세요.</h2>
        <p className="page-card__description">일반 사용자는 채용공고를 조회만 할 수 있으며, 등록·수정·삭제는 관리자만 가능합니다.</p>
      </div>

      <section className="workspace-grid">
        <article className="panel"><h3 className="panel__title">전체 유저 수</h3><p className="score-chip">{dashboard?.totalUsers ?? 0}</p></article>
        <article className="panel"><h3 className="panel__title">전체 지원자료 수</h3><p className="score-chip">{dashboard?.totalApplicationDocuments ?? 0}</p></article>
        <article className="panel"><h3 className="panel__title">전체 채용공고 수</h3><p className="score-chip">{dashboard?.totalJobPostings ?? 0}</p></article>
        <article className="panel"><h3 className="panel__title">진행 중 면접 수</h3><p className="score-chip">{dashboard?.ongoingInterviews ?? 0}</p></article>
        <article className="panel"><h3 className="panel__title">전체 주문 수</h3><p className="score-chip">{dashboard?.totalOrders ?? 0}</p></article>
        <article className="panel"><h3 className="panel__title">총 매출</h3><p className="score-chip">{dashboard?.totalRevenue ?? 0}원</p></article>
      </section>

      <section className="workspace-grid">
        <article className="panel panel--wide dashboard-chart-card">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">최근 7일 가입 추이</h3>
              <p className="panel__subtitle">관리자 화면에서 전체 서비스 유입 흐름을 빠르게 확인합니다.</p>
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
                  <Line type="monotone" dataKey="count" stroke="#0f766e" strokeWidth={2.5} dot={{ r: 4, fill: '#0f766e' }} activeDot={{ r: 6 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.dailySignups?.length ?? 0) === 0 ? (
            <EmptyState title={EMPTY_MESSAGES.adminDailySignups.title} description={EMPTY_MESSAGES.adminDailySignups.description} />
          ) : null}
        </article>

        <article className="panel panel--wide">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">채용공고 관리</h3>
              <p className="panel__subtitle">관리자가 등록·수정·삭제할 수 있는 채용공고 목록입니다.</p>
            </div>
            <button className="button button--secondary" type="button" onClick={handleCreateJobPosting}>{BUTTON_LABELS.newItem}</button>
          </div>
          {isJobPostingLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingList} /> : null}
          {!isJobPostingLoading && jobPostingError && jobPostings.length === 0 ? <ErrorBlock message={jobPostingError} /> : null}
          {!isJobPostingLoading && !jobPostingError && jobPostings.length === 0 ? (
            <EmptyState title="등록된 채용공고가 없습니다." description="첫 채용공고를 등록하면 일반 사용자가 면접 설정에서 선택할 수 있습니다." />
          ) : null}
          {!isJobPostingLoading && jobPostings.length > 0 ? (
            <div className="resource-list">
              {jobPostings.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={item.id === selectedId ? 'resource-list__item resource-list__item--active' : 'resource-list__item'}
                  onClick={() => setSelectedId(item.id)}
                >
                  <strong>{item.positionTitle}</strong>
                  <span>{item.companyName} · {item.deadline || '상시 채용'}</span>
                  <span>{item.jobUrl || 'URL 없음'}</span>
                </button>
              ))}
            </div>
          ) : null}
        </article>
      </section>

      <section className="workspace-grid">
        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">최근 가입 사용자</h3><p className="panel__subtitle">최근 생성된 사용자 5건</p></div></div>
          {isDashboardLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingAdminDashboard} /> : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentUsers?.length ?? 0) > 0 ? (
            <div className="resource-list">
              {dashboard.recentUsers.map((user) => (
                <div key={user.id} className="resource-list__item resource-list__item--static">
                  <strong>{user.name}</strong>
                  <span>{user.email}</span>
                  <span>{user.role} · {user.createdAt ?? '-'}</span>
                </div>
              ))}
            </div>
          ) : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentUsers?.length ?? 0) === 0 ? (
            <EmptyState title={EMPTY_MESSAGES.adminRecentUsers.title} description={EMPTY_MESSAGES.adminRecentUsers.description} />
          ) : null}
        </article>

        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">최근 지원자료</h3><p className="panel__subtitle">최근 등록된 지원자료 5건</p></div></div>
          {isDashboardLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingAdminDashboard} /> : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentApplicationDocuments?.length ?? 0) > 0 ? (
            <div className="resource-list">
              {dashboard.recentApplicationDocuments.map((document) => (
                <div key={document.id} className="resource-list__item resource-list__item--static">
                  <strong>{document.title}</strong>
                  <span>{document.userName} · {document.userEmail}</span>
                  <span>{document.createdAt ?? '-'}</span>
                </div>
              ))}
            </div>
          ) : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentApplicationDocuments?.length ?? 0) === 0 ? (
            <EmptyState title={EMPTY_MESSAGES.adminRecentDocuments.title} description={EMPTY_MESSAGES.adminRecentDocuments.description} />
          ) : null}
        </article>
      </section>

      <section className="workspace-grid">
        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">최근 면접 세션</h3><p className="panel__subtitle">최근 생성된 면접 세션 5건</p></div></div>
          {isDashboardLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingAdminDashboard} /> : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentInterviewSessions?.length ?? 0) > 0 ? (
            <div className="resource-list">
              {dashboard.recentInterviewSessions.map((session) => (
                <div key={session.id} className="resource-list__item resource-list__item--static">
                  <strong>{session.title}</strong>
                  <span>{session.userName} · {session.userEmail}</span>
                  <span>{session.positionTitle} · {session.status}</span>
                </div>
              ))}
            </div>
          ) : null}
          {!isDashboardLoading && !dashboardError && (dashboard?.recentInterviewSessions?.length ?? 0) === 0 ? (
            <EmptyState title={EMPTY_MESSAGES.adminRecentSessions.title} description={EMPTY_MESSAGES.adminRecentSessions.description} />
          ) : null}
        </article>

        <article className="panel panel--wide">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{selectedId ? '채용공고 수정' : '채용공고 등록'}</h3>
              <p className="panel__subtitle">일반 사용자는 이 채용공고를 면접 설정에서 조회만 할 수 있습니다.</p>
            </div>
          </div>
          <StatusMessage variant="error" message={jobPostings.length > 0 ? jobPostingError : ''} />
          <StatusMessage variant="success" message={successMessage} />
          <form className="editor-form" onSubmit={handleSaveJobPosting}>
            <TextInput label="회사명" value={jobPostingForm.companyName} onChange={(event) => updateJobPostingField('companyName', event.target.value)} placeholder="AIMentor" />
            <TextInput label="공고 제목" value={jobPostingForm.positionTitle} onChange={(event) => updateJobPostingField('positionTitle', event.target.value)} placeholder="백엔드 개발자" />
            <TextAreaField label="공고 설명" value={jobPostingForm.description} onChange={(event) => updateJobPostingField('description', event.target.value)} placeholder="주요 업무, 요구 역량, 우대 사항을 입력해 주세요." rows={8} />
            <TextInput label="공고 URL" value={jobPostingForm.jobUrl} onChange={(event) => updateJobPostingField('jobUrl', event.target.value)} placeholder="https://example.com/jobs/backend" />
            <TextInput label="마감일" value={jobPostingForm.deadline} onChange={(event) => updateJobPostingField('deadline', event.target.value)} placeholder="2026-03-31" />
            <div className="button-row">
              <button className="button" type="submit" disabled={isSaving}>{isSaving ? STATUS_MESSAGES.saving : BUTTON_LABELS.save}</button>
              <button className="button button--secondary" type="button" onClick={handleCreateJobPosting}>{BUTTON_LABELS.reset}</button>
              <button className="button button--danger" type="button" onClick={handleDeleteJobPosting} disabled={!selectedId}>{BUTTON_LABELS.delete}</button>
            </div>
          </form>
        </article>
      </section>
    </section>
  )
}

export default AdminPage