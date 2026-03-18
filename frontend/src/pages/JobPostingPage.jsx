import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import jobPostingApi from '../api/jobPostingApi.js'
import { STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function JobPostingPage() {
  usePageTitle('채용공고')
  const [jobPostings, setJobPostings] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadJobPostings() {
      setIsLoading(true)
      setError('')
      try {
        const items = await jobPostingApi.list()
        setJobPostings(items)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadJobPostings()
  }, [])

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">채용공고</p>
        <h2 className="page-card__title">면접 연습에 활용할 채용공고를 확인해 보세요.</h2>
        <p className="page-card__description">일반 사용자는 목록과 상세를 조회할 수 있고, 관리 기능은 관리자 화면에서만 제공합니다.</p>
      </div>

      <section className="panel panel--wide">
        {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingList} /> : null}
        {!isLoading && error ? <ErrorBlock message={error} /> : null}
        {!isLoading && !error && jobPostings.length === 0 ? (
          <EmptyState title="등록된 채용공고가 없습니다." description="관리자가 채용공고를 등록하면 이곳에 표시됩니다." />
        ) : null}
        {!isLoading && !error && jobPostings.length > 0 ? (
          <div className="resource-list">
            {jobPostings.map((item) => (
              <Link key={item.id} className="resource-list__item" to={`/job-posting/${item.id}`}>
                <strong>{item.positionTitle}</strong>
                <span>{item.companyName}</span>
                <span>{item.deadline ? `마감일: ${item.deadline}` : '상시 채용 또는 마감일 미정'}</span>
              </Link>
            ))}
          </div>
        ) : null}
      </section>
    </section>
  )
}

export default JobPostingPage