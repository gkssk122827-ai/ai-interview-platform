import { useEffect, useState } from 'react'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import jobPostingApi from '../api/jobPostingApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

function JobPostingPage() {
  usePageTitle('채용공고')

  const [items, setItems] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadItems() {
      setIsLoading(true)
      setError('')
      try {
        const result = await jobPostingApi.list()
        setItems(result)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadItems()
  }, [])

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="채용공고를 불러오는 중입니다." /></section>
  }

  if (error) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">채용공고</p>
        <h2 className="page-card__title">등록된 채용공고를 조회하고 면접 준비에 활용하세요.</h2>
        <p className="page-card__description">채용공고 등록과 수정은 관리자 페이지에서만 가능합니다. 일반 회원은 공고를 조회하고 면접 설정에 활용할 수 있습니다.</p>
      </div>

      <article className="panel panel--wide">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">채용공고 목록</h3>
            <p className="panel__subtitle">회사명, 사이트명, 마감일, 링크 정보를 확인할 수 있습니다.</p>
          </div>
        </div>
        {items.length === 0 ? (
          <EmptyState title="등록된 채용공고가 없습니다." description="관리자 페이지에서 채용공고를 등록하면 이곳에 표시됩니다." />
        ) : (
          <div className="resource-list">
            {items.map((item) => (
              <div key={item.id} className="resource-list__item resource-list__item--static">
                <strong>{item.positionTitle}</strong>
                <span>{item.companyName} · {item.siteName || '수동입력'}</span>
                <span>{item.deadline ? `마감일 ${item.deadline}` : '마감일 미정'}</span>
                <span>{item.jobUrl || '링크 없음'}</span>
              </div>
            ))}
          </div>
        )}
      </article>
    </section>
  )
}

export default JobPostingPage
