import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import StatusMessage from '../components/common/StatusMessage.jsx'
import adminApi from '../api/adminApi.js'
import bookApi from '../api/bookApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

const emptyBookForm = {
  title: '',
  author: '',
  publisher: '',
  price: '0',
  stock: '0',
  coverUrl: '',
  description: '',
}

function AdminPage() {
  usePageTitle('관리자')

  const [dashboard, setDashboard] = useState(null)
  const [users, setUsers] = useState([])
  const [books, setBooks] = useState([])
  const [payments, setPayments] = useState([])
  const [subscriptions, setSubscriptions] = useState([])
  const [bookForm, setBookForm] = useState(emptyBookForm)
  const [editingBookId, setEditingBookId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadAll() {
    setError('')
    try {
      const [dashboardResult, usersResult, booksResult, paymentsResult, subscriptionsResult] = await Promise.all([
        adminApi.getDashboard(),
        adminApi.getUsers(),
        bookApi.list({ page: 0, size: 50 }),
        adminApi.getPayments(),
        adminApi.getSubscriptions(),
      ])
      setDashboard(dashboardResult)
      setUsers(usersResult)
      setBooks(booksResult.content ?? [])
      setPayments(paymentsResult)
      setSubscriptions(subscriptionsResult)
    } catch (loadError) {
      setError(loadError.message)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  async function handleUserStatusChange(userId, status) {
    setError('')
    setNotice('')
    try {
      await adminApi.updateUserStatus(userId, status)
      setNotice('회원 상태가 변경되었습니다.')
      await loadAll()
    } catch (changeError) {
      setError(changeError.message)
    }
  }

  function beginBookEdit(book) {
    setEditingBookId(book.id)
    setBookForm({
      title: book.title ?? '',
      author: book.author ?? '',
      publisher: book.publisher ?? '',
      price: String(book.price ?? '0'),
      stock: String(book.stock ?? '0'),
      coverUrl: book.coverUrl ?? '',
      description: book.description ?? '',
    })
  }

  async function handleBookSubmit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    try {
      const payload = {
        ...bookForm,
        price: Number(bookForm.price || 0),
        stock: Number(bookForm.stock || 0),
      }
      if (editingBookId) {
        await bookApi.update(editingBookId, payload)
        setNotice('도서가 수정되었습니다.')
      } else {
        await bookApi.create(payload)
        setNotice('도서가 등록되었습니다.')
      }
      setEditingBookId(null)
      setBookForm(emptyBookForm)
      await loadAll()
    } catch (submitError) {
      setError(submitError.message)
    }
  }

  async function handleBookDelete(bookId) {
    setError('')
    setNotice('')
    try {
      await bookApi.remove(bookId)
      setNotice('도서가 삭제되었습니다.')
      await loadAll()
    } catch (deleteError) {
      setError(deleteError.message)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">관리자</p>
        <h2 className="page-card__title">회원, 도서, 결제, 구독 현황을 한 화면에서 관리하세요.</h2>
        <p className="page-card__description">기존 관리자 대시보드에 회원 상태 변경, 도서 CRUD, 결제/구독 조회를 확장했습니다.</p>
      </div>

      <StatusMessage variant="success" message={notice} />
      <StatusMessage variant="error" message={error} />

      <section className="admin-summary-grid">
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">전체 회원</p><h3 className="admin-summary-card__value">{dashboard?.totalUsers ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">지원자료</p><h3 className="admin-summary-card__value">{dashboard?.totalApplicationDocuments ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">채용공고</p><h3 className="admin-summary-card__value">{dashboard?.totalJobPostings ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">주문</p><h3 className="admin-summary-card__value">{dashboard?.totalOrders ?? 0}</h3></article>
      </section>

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">채용공고 관리</h3>
            <p className="panel__subtitle">채용공고 등록과 수정은 관리자 전용 편집 화면에서 진행합니다.</p>
          </div>
          <Link className="button" to="/admin/job-postings/new">채용공고 등록</Link>
        </div>
      </article>

      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">회원관리</h3></div></div>
          <div className="resource-list">
            {users.map((user) => (
              <div key={user.id} className="resource-list__item resource-list__item--static">
                <strong>{user.name}</strong>
                <span>{user.email}</span>
                <span>{user.role} · {user.status}</span>
                <div className="button-row">
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'ACTIVE')}>정상</button>
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'SUSPENDED')}>정지</button>
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'WITHDRAWN')}>탈퇴</button>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">도서관리</h3></div></div>
          <form className="editor-form" onSubmit={handleBookSubmit}>
            <input className="input" value={bookForm.title} placeholder="제목" onChange={(event) => setBookForm((current) => ({ ...current, title: event.target.value }))} required />
            <input className="input" value={bookForm.author} placeholder="저자" onChange={(event) => setBookForm((current) => ({ ...current, author: event.target.value }))} required />
            <input className="input" value={bookForm.publisher} placeholder="출판사" onChange={(event) => setBookForm((current) => ({ ...current, publisher: event.target.value }))} required />
            <input className="input" value={bookForm.price} placeholder="가격" onChange={(event) => setBookForm((current) => ({ ...current, price: event.target.value }))} required />
            <input className="input" value={bookForm.stock} placeholder="재고" onChange={(event) => setBookForm((current) => ({ ...current, stock: event.target.value }))} required />
            <input className="input" value={bookForm.coverUrl} placeholder="이미지 URL" onChange={(event) => setBookForm((current) => ({ ...current, coverUrl: event.target.value }))} />
            <textarea className="input input--textarea" rows="5" value={bookForm.description} placeholder="설명" onChange={(event) => setBookForm((current) => ({ ...current, description: event.target.value }))} />
            <div className="button-row">
              <button className="button" type="submit">{editingBookId ? '도서 수정' : '도서 등록'}</button>
              {editingBookId ? <button className="button button--secondary" type="button" onClick={() => { setEditingBookId(null); setBookForm(emptyBookForm) }}>취소</button> : null}
            </div>
          </form>
          <div className="resource-list">
            {books.map((book) => (
              <div key={book.id} className="resource-list__item resource-list__item--static">
                <strong>{book.title}</strong>
                <span>{book.author} · {book.publisher}</span>
                <span>{Number(book.price).toLocaleString('ko-KR')}원 · 재고 {book.stock}</span>
                <div className="button-row">
                  <button className="button button--secondary" type="button" onClick={() => beginBookEdit(book)}>수정</button>
                  <button className="button button--secondary" type="button" onClick={() => handleBookDelete(book.id)}>삭제</button>
                </div>
              </div>
            ))}
          </div>
        </article>
      </div>

      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">결제관리</h3></div></div>
          <div className="resource-list">
            {payments.map((payment, index) => (
              <div key={`${payment.sourceType}-${payment.sourceId}-${index}`} className="resource-list__item resource-list__item--static">
                <strong>{payment.title}</strong>
                <span>{payment.sourceType} · {payment.status}</span>
                <span>{payment.userEmail || payment.userId}</span>
                <span>{Number(payment.amount).toLocaleString('ko-KR')}원</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">구독관리</h3></div></div>
          <div className="resource-list">
            {subscriptions.map((subscription) => (
              <div key={subscription.id} className="resource-list__item resource-list__item--static">
                <strong>{subscription.planName}</strong>
                <span>회원 #{subscription.userId}</span>
                <span>{subscription.status} · {subscription.startDate} ~ {subscription.endDate}</span>
              </div>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}

export default AdminPage
