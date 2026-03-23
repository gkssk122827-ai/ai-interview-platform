import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import authApi from '../api/authApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

function MyPage() {
  usePageTitle('마이페이지')

  const [form, setForm] = useState({ name: '', phone: '', email: '', role: '', status: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    async function loadMe() {
      setIsLoading(true)
      setError('')
      try {
        const result = await authApi.getMe()
        setForm({
          name: result.name ?? '',
          phone: result.phone ?? '',
          email: result.email ?? '',
          role: result.role ?? '',
          status: result.status ?? '',
        })
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadMe()
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setNotice('')
    try {
      await authApi.updateMe({ name: form.name, phone: form.phone })
      setNotice('회원 정보가 수정되었습니다.')
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="회원 정보를 불러오는 중입니다." /></section>
  }

  if (error && !form.email) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">마이페이지</p>
        <h2 className="page-card__title">내 정보와 구매·구독 상태를 관리하세요.</h2>
        <p className="page-card__description">회원정보 수정, 이력서 관리, 구매관리, 구독관리, 찜목록, 채용공고 메뉴로 빠르게 이동할 수 있습니다.</p>
      </div>

      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">회원 정보</h3>
              <p className="panel__subtitle">기본 정보와 계정 상태를 확인하세요.</p>
            </div>
          </div>
          <StatusMessage variant="success" message={notice} />
          <StatusMessage variant="error" message={form.email ? error : ''} />
          <form className="editor-form" onSubmit={handleSubmit}>
            <TextInput label="이름" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required />
            <TextInput label="전화번호" value={form.phone} onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} required />
            <TextInput label="이메일" value={form.email} disabled />
            <TextInput label="권한" value={form.role} disabled />
            <TextInput label="상태" value={form.status} disabled />
            <div className="button-row">
              <button className="button" type="submit" disabled={isSaving}>{isSaving ? '저장 중...' : '회원수정 저장'}</button>
            </div>
          </form>
        </article>

        <article className="panel panel--wide">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">바로가기</h3>
              <p className="panel__subtitle">자주 사용하는 개인 메뉴를 한 번에 이동할 수 있습니다.</p>
            </div>
          </div>
          <div className="resource-list">
            <Link to="/profile-documents" className="resource-list__item">
              <strong>이력서 관리</strong>
              <span>지원자료와 파일 업로드를 관리합니다.</span>
            </Link>
            <Link to="/orders" className="resource-list__item">
              <strong>구매관리</strong>
              <span>도서 주문과 결제 이력을 확인합니다.</span>
            </Link>
            <Link to="/subscriptions" className="resource-list__item">
              <strong>구독관리</strong>
              <span>현재 구독 상태와 결제 이력을 확인합니다.</span>
            </Link>
            <Link to="/favorites" className="resource-list__item">
              <strong>찜목록</strong>
              <span>채용공고, 관심기업, 도서관심 목록을 관리합니다.</span>
            </Link>
            <Link to="/job-posting" className="resource-list__item">
              <strong>채용공고</strong>
              <span>URL 등록과 수동 보완 입력으로 공고를 관리합니다.</span>
            </Link>
          </div>
        </article>
      </div>
    </section>
  )
}

export default MyPage
