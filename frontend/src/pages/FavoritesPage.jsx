import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import StatusMessage from '../components/common/StatusMessage.jsx'
import favoriteApi from '../api/favoriteApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

const tabs = [
  { value: 'JOB_POSTING', label: '채용공고' },
  { value: 'COMPANY', label: '관심기업' },
  { value: 'BOOK', label: '도서관심' },
]

function FavoritesPage() {
  usePageTitle('찜목록')
  const [searchParams, setSearchParams] = useSearchParams()
  const initialType = searchParams.get('type') || 'JOB_POSTING'
  const [selectedType, setSelectedType] = useState(initialType)
  const [items, setItems] = useState([])
  const [form, setForm] = useState({ title: '', targetValue: '', targetUrl: '' })
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadItems(type) {
    setError('')
    try {
      const result = await favoriteApi.list(type)
      setItems(result)
    } catch (loadError) {
      setError(loadError.message)
    }
  }

  useEffect(() => {
    loadItems(selectedType)
    setSearchParams({ type: selectedType }, { replace: true })
  }, [selectedType, setSearchParams])

  const placeholder = useMemo(() => {
    if (selectedType === 'COMPANY') return '예: 오픈AI 코리아'
    if (selectedType === 'BOOK') return '예: 리액트 실전 가이드'
    return '예: 프론트엔드 개발자 채용'
  }, [selectedType])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    try {
      await favoriteApi.create({ type: selectedType, ...form })
      setForm({ title: '', targetValue: '', targetUrl: '' })
      setNotice('찜 항목이 저장되었습니다.')
      await loadItems(selectedType)
    } catch (submitError) {
      setError(submitError.message)
    }
  }

  async function handleDelete(id) {
    setError('')
    setNotice('')
    try {
      await favoriteApi.remove(id)
      setNotice('찜 항목이 삭제되었습니다.')
      await loadItems(selectedType)
    } catch (deleteError) {
      setError(deleteError.message)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">찜목록</p>
        <h2 className="page-card__title">채용공고, 관심기업, 도서관심을 한 곳에서 관리하세요.</h2>
        <p className="page-card__description">각 유형별로 저장한 항목을 확인하고 필요 없는 항목은 바로 삭제할 수 있습니다.</p>
      </div>

      <StatusMessage variant="success" message={notice} />
      <StatusMessage variant="error" message={error} />

      <article className="panel">
        <div className="button-row">
          {tabs.map((tab) => (
            <button
              key={tab.value}
              type="button"
              className={selectedType === tab.value ? 'button' : 'button button--secondary'}
              onClick={() => setSelectedType(tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form className="editor-form" onSubmit={handleSubmit}>
          <input className="input" value={form.title} placeholder={placeholder} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} required />
          <input className="input" value={form.targetValue} placeholder="부가 정보" onChange={(event) => setForm((current) => ({ ...current, targetValue: event.target.value }))} />
          <input className="input" value={form.targetUrl} placeholder="관련 URL" onChange={(event) => setForm((current) => ({ ...current, targetUrl: event.target.value }))} />
          <div className="button-row">
            <button className="button" type="submit">추가하기</button>
          </div>
        </form>
      </article>

      <article className="panel panel--wide">
        <div className="panel__header"><div><h3 className="panel__title">저장된 항목</h3></div></div>
        <div className="resource-list">
          {items.map((item) => (
            <div key={item.id} className="resource-list__item resource-list__item--static">
              <strong>{item.title}</strong>
              <span>{item.targetValue || '-'}</span>
              <span>{item.targetUrl || '-'}</span>
              <div className="button-row">
                <button className="button button--secondary" type="button" onClick={() => handleDelete(item.id)}>삭제</button>
              </div>
            </div>
          ))}
        </div>
      </article>
    </section>
  )
}

export default FavoritesPage
