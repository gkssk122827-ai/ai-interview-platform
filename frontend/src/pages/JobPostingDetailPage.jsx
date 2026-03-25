import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import jobPostingApi from '../api/jobPostingApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function JobPostingDetailPage() {
  usePageTitle('채용공고 상세')
  const navigate = useNavigate()
  const { jobPostingId } = useParams()

  const [jobPosting, setJobPosting] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadJobPosting() {
      if (!jobPostingId) {
        setError('채용공고 ID가 없습니다.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setError('')
      try {
        const item = await jobPostingApi.get(jobPostingId)
        setJobPosting(item)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadJobPosting()
  }, [jobPostingId])

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingData} /></section>
  }

  if (error) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  if (!jobPosting) {
    return (
      <section className="workspace-page">
        <EmptyState
          title="채용공고를 찾을 수 없습니다."
          description="목록에서 다른 채용공고를 선택해 주세요."
          action={<button className="button" type="button" onClick={() => navigate('/job-posting')}>목록으로 돌아가기</button>}
        />
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">채용공고 상세</p>
        <h2 className="page-card__title">{jobPosting.positionTitle}</h2>
        <p className="page-card__description">{jobPosting.companyName} 채용공고 상세 정보를 확인하고, 면접 준비로 바로 이동할 수 있습니다.</p>
      </div>

      <div className="workspace-grid workspace-grid--single">
        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">공고 정보</h3></div></div>
          <p className="panel__subtitle"><strong>회사명</strong> {jobPosting.companyName ?? '-'}</p>
          <p className="panel__subtitle"><strong>직무</strong> {jobPosting.positionTitle ?? '-'}</p>
          <p className="panel__subtitle"><strong>사이트</strong> {jobPosting.siteName ?? '수동입력'}</p>
          <p className="panel__subtitle"><strong>마감일</strong> {jobPosting.deadline ?? '상시채용 또는 미정'}</p>
          <p className="panel__subtitle"><strong>출처 URL</strong> {jobPosting.jobUrl ?? '-'}</p>

          <div className="panel__header"><div><h3 className="panel__title">상세 설명</h3></div></div>
          <p className="panel__subtitle">{jobPosting.description || '상세 설명이 없습니다.'}</p>
        </article>

        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">바로 이동</h3></div></div>
          <div className="button-row">
            <button className="button" type="button" onClick={() => navigate('/interview/setup')}>{BUTTON_LABELS.startInterview}</button>
            <Link className="button button--secondary" to="/job-posting">목록 보기</Link>
          </div>
          {jobPosting.jobUrl ? (
            <a className="button button--secondary" href={jobPosting.jobUrl} target="_blank" rel="noreferrer">원본 공고 열기</a>
          ) : null}
        </article>
      </div>
    </section>
  )
}

export default JobPostingDetailPage
