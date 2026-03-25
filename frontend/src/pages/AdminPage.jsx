import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import StatusMessage from '../components/common/StatusMessage.jsx'
import adminApi from '../api/adminApi.js'
import bookApi from '../api/bookApi.js'
import jobPostingApi from '../api/jobPostingApi.js'
import orderApi from '../api/orderApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { toKoreanPaymentStatus, toKoreanSubscriptionStatus } from '../utils/paymentStatus.js'

const emptyBookForm = {
  title: '',
  author: '',
  publisher: '',
  price: '0',
  stock: '0',
  coverUrl: '',
  description: '',
}

const emptySubscriptionPlanForm = {
  code: '',
  name: '',
  price: '0',
  durationDays: '30',
  description: '',
}

const ADMIN_TABS = [
  { key: 'overview', label: '채용공고 관리' },
  { key: 'users', label: '회원 관리' },
  { key: 'books', label: '도서 관리' },
  { key: 'payments', label: '결제 관리' },
  { key: 'subscriptions', label: '구독 관리' },
]

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('ko-KR')
}

function resolveUserLabel(users, userId) {
  const user = users.find((item) => String(item.id) === String(userId))
  if (!user) return `회원 #${userId}`
  return user.name || user.email || `회원 #${userId}`
}

function AdminPage() {
  usePageTitle('관리자')

  const [activeTab, setActiveTab] = useState('overview')
  const [dashboard, setDashboard] = useState(null)
  const [users, setUsers] = useState([])
  const [books, setBooks] = useState([])
  const [jobPostings, setJobPostings] = useState([])
  const [payments, setPayments] = useState([])
  const [subscriptions, setSubscriptions] = useState([])
  const [subscriptionPlans, setSubscriptionPlans] = useState([])

  const [bookForm, setBookForm] = useState(emptyBookForm)
  const [editingBookId, setEditingBookId] = useState(null)
  const [isBookModalOpen, setIsBookModalOpen] = useState(false)

  const [subscriptionPlanForm, setSubscriptionPlanForm] = useState(emptySubscriptionPlanForm)
  const [editingSubscriptionPlanId, setEditingSubscriptionPlanId] = useState(null)
  const [isSubscriptionPlanModalOpen, setIsSubscriptionPlanModalOpen] = useState(false)

  const [selectedPayment, setSelectedPayment] = useState(null)
  const [paymentOrderItems, setPaymentOrderItems] = useState([])
  const [isPaymentOrderLoading, setIsPaymentOrderLoading] = useState(false)
  const [paymentOrderError, setPaymentOrderError] = useState('')

  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadAll() {
    setError('')
    try {
      const [
        dashboardResult,
        usersResult,
        booksResult,
        jobPostingsResult,
        paymentsResult,
        subscriptionsResult,
        subscriptionPlansResult,
      ] = await Promise.all([
        adminApi.getDashboard(),
        adminApi.getUsers(),
        bookApi.list({ page: 0, size: 50 }),
        jobPostingApi.list(),
        adminApi.getPayments(),
        adminApi.getSubscriptions(),
        adminApi.getSubscriptionPlans(),
      ])

      setDashboard(dashboardResult)
      setUsers(usersResult)
      setBooks(booksResult.content ?? [])
      setJobPostings(Array.isArray(jobPostingsResult) ? jobPostingsResult : [])
      setPayments(paymentsResult)
      setSubscriptions(subscriptionsResult)
      setSubscriptionPlans(subscriptionPlansResult)
    } catch (loadError) {
      setError(loadError.message)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  useEffect(() => {
    async function loadOrderItemsForPayment() {
      setPaymentOrderItems([])
      setPaymentOrderError('')

      if (!selectedPayment) {
        setIsPaymentOrderLoading(false)
        return
      }

      const sourceType = String(selectedPayment.sourceType ?? '').toUpperCase()
      if (sourceType !== 'ORDER' || !selectedPayment.sourceId) {
        setIsPaymentOrderLoading(false)
        return
      }

      setIsPaymentOrderLoading(true)
      try {
        const order = await orderApi.get(selectedPayment.sourceId)
        setPaymentOrderItems(Array.isArray(order?.items) ? order.items : [])
      } catch (loadError) {
        setPaymentOrderError(loadError.message)
      } finally {
        setIsPaymentOrderLoading(false)
      }
    }

    loadOrderItemsForPayment()
  }, [selectedPayment])

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
    setIsBookModalOpen(true)
  }

  function beginBookCreate() {
    setEditingBookId(null)
    setBookForm(emptyBookForm)
    setIsBookModalOpen(true)
  }

  function closeBookModal() {
    setIsBookModalOpen(false)
    setEditingBookId(null)
    setBookForm(emptyBookForm)
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

      closeBookModal()
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

  function beginSubscriptionPlanEdit(plan) {
    setEditingSubscriptionPlanId(plan.id)
    setSubscriptionPlanForm({
      code: plan.code ?? '',
      name: plan.name ?? '',
      price: String(plan.price ?? '0'),
      durationDays: String(plan.durationDays ?? '30'),
      description: plan.description ?? '',
    })
    setIsSubscriptionPlanModalOpen(true)
  }

  function beginSubscriptionPlanCreate() {
    setEditingSubscriptionPlanId(null)
    setSubscriptionPlanForm(emptySubscriptionPlanForm)
    setIsSubscriptionPlanModalOpen(true)
  }

  function resetSubscriptionPlanForm() {
    setEditingSubscriptionPlanId(null)
    setSubscriptionPlanForm(emptySubscriptionPlanForm)
  }

  function closeSubscriptionPlanModal() {
    setIsSubscriptionPlanModalOpen(false)
    resetSubscriptionPlanForm()
  }

  async function handleSubscriptionPlanSubmit(event) {
    event.preventDefault()
    setError('')
    setNotice('')

    try {
      const payload = {
        code: subscriptionPlanForm.code.trim(),
        name: subscriptionPlanForm.name.trim(),
        price: Number(subscriptionPlanForm.price || 0),
        durationDays: Number(subscriptionPlanForm.durationDays || 0),
        description: subscriptionPlanForm.description.trim(),
      }

      if (editingSubscriptionPlanId) {
        await adminApi.updateSubscriptionPlan(editingSubscriptionPlanId, payload)
        setNotice('구독 종류가 수정되었습니다.')
      } else {
        await adminApi.createSubscriptionPlan(payload)
        setNotice('구독 종류가 추가되었습니다.')
      }

      closeSubscriptionPlanModal()
      await loadAll()
    } catch (submitError) {
      setError(submitError.message)
    }
  }

  async function handleSubscriptionPlanDelete(planId) {
    setError('')
    setNotice('')
    try {
      await adminApi.deleteSubscriptionPlan(planId)
      setNotice('구독 종류가 삭제되었습니다.')
      if (String(editingSubscriptionPlanId) === String(planId)) {
        resetSubscriptionPlanForm()
      }
      await loadAll()
    } catch (deleteError) {
      setError(deleteError.message)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">관리자</p>
        <h2 className="page-card__title">회원, 도서, 결제, 구독 데이터를 한 번에 관리하세요.</h2>
        <p className="page-card__description">탭을 전환하고 필요한 관리 영역만 집중해서 처리할 수 있습니다.</p>
      </div>

      <StatusMessage variant="success" message={notice} />
      <StatusMessage variant="error" message={error} />

      <section className="admin-summary-grid">
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">전체 회원</p><h3 className="admin-summary-card__value">{dashboard?.totalUsers ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">지원 자료</p><h3 className="admin-summary-card__value">{dashboard?.totalApplicationDocuments ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">채용공고</p><h3 className="admin-summary-card__value">{dashboard?.totalJobPostings ?? 0}</h3></article>
        <article className="panel admin-summary-card"><p className="admin-summary-card__label">주문</p><h3 className="admin-summary-card__value">{dashboard?.totalOrders ?? 0}</h3></article>
      </section>

      <div className="button-row admin-tab-row">
        {ADMIN_TABS.map((tab) => (
          <button
            key={tab.key}
            className={activeTab === tab.key ? 'button' : 'button button--secondary'}
            type="button"
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' ? (
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">채용공고 관리</h3>
              <p className="panel__subtitle">요약 카드 아래에서 채용공고 목록을 확인하고 등록/수정할 수 있습니다.</p>
            </div>
            <Link className="button" to="/admin/job-postings/new">채용공고 등록</Link>
          </div>
          <div className="resource-list">
            {jobPostings.map((jobPosting) => (
              <div key={jobPosting.id} className="resource-list__item resource-list__item--static">
                <strong>{jobPosting.companyName} · {jobPosting.positionTitle}</strong>
                <span>{jobPosting.deadline ? `마감일 ${jobPosting.deadline}` : '마감일 미정'}</span>
                <span>{jobPosting.siteName || '출처 미정'}</span>
                <div className="button-row">
                  <Link className="button button--secondary" to={`/admin/job-postings/${jobPosting.id}/edit`}>수정</Link>
                </div>
              </div>
            ))}
            {jobPostings.length === 0 ? (
              <div className="resource-list__item resource-list__item--static">
                <strong>등록된 채용공고가 없습니다.</strong>
                <span>오른쪽 상단의 채용공고 등록 버튼으로 새 공고를 추가해 주세요.</span>
              </div>
            ) : null}
          </div>
        </article>
      ) : null}

      {activeTab === 'users' ? (
        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">회원 관리</h3></div></div>
          <div className="resource-list">
            {users.map((user) => (
              <div key={user.id} className="resource-list__item resource-list__item--static">
                <strong>{user.name}</strong>
                <span>{user.email}</span>
                <span>{user.role} · {user.status}</span>
                <div className="button-row">
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'ACTIVE')}>활성</button>
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'SUSPENDED')}>정지</button>
                  <button className="button button--secondary" type="button" onClick={() => handleUserStatusChange(user.id, 'WITHDRAWN')}>탈퇴</button>
                </div>
              </div>
            ))}
          </div>
        </article>
      ) : null}

      {activeTab === 'books' ? (
        <article className="panel panel--wide">
          <div className="panel__header">
            <div><h3 className="panel__title">도서 관리</h3></div>
            <button className="button" type="button" onClick={beginBookCreate}>도서 등록</button>
          </div>
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
      ) : null}

      {activeTab === 'books' && isBookModalOpen ? (
        <div className="admin-book-modal-backdrop" role="presentation" onClick={closeBookModal}>
          <article className="panel panel--wide admin-book-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="panel__header">
              <div><h3 className="panel__title">{editingBookId ? '도서 수정' : '도서 등록'}</h3></div>
            </div>
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
                <button className="button button--secondary" type="button" onClick={closeBookModal}>취소</button>
              </div>
            </form>
          </article>
        </div>
      ) : null}

      {activeTab === 'payments' ? (
        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">결제 관리</h3></div></div>
          <div className="resource-list">
            {payments.map((payment, index) => (
              <button
                key={`${payment.sourceType}-${payment.sourceId}-${index}`}
                type="button"
                className="resource-list__item"
                onClick={() => setSelectedPayment(payment)}
              >
                <strong>{payment.title}</strong>
                <span>{payment.sourceType} · {toKoreanPaymentStatus(payment.status)}</span>
                <span>{payment.userEmail || payment.userId}</span>
                <span>{Number(payment.amount).toLocaleString('ko-KR')}원</span>
                <span>결제일 {formatDateTime(payment.paidAt ?? payment.approvedAt ?? payment.requestedAt ?? payment.createdAt)}</span>
              </button>
            ))}
          </div>
        </article>
      ) : null}

      {activeTab === 'payments' && selectedPayment ? (
        <div className="admin-book-modal-backdrop" role="presentation" onClick={() => setSelectedPayment(null)}>
          <article className="panel panel--wide admin-book-modal admin-payment-receipt-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="panel__header">
              <div><h3 className="panel__title">결제 상세 정보</h3></div>
            </div>
            <div className="payment-receipt">
              <div className="payment-receipt__head">
                <p className="payment-receipt__store">AI Mentor 결제 내역</p>
                <h4 className="payment-receipt__title">{selectedPayment.title}</h4>
                <p className="payment-receipt__date">{formatDateTime(selectedPayment.paidAt ?? selectedPayment.approvedAt ?? selectedPayment.requestedAt ?? selectedPayment.createdAt)}</p>
              </div>

              <div className="payment-receipt__rows">
                <div className="payment-receipt__row"><span>유형</span><strong>{selectedPayment.sourceType}</strong></div>
                <div className="payment-receipt__row"><span>상태</span><strong>{toKoreanPaymentStatus(selectedPayment.status)}</strong></div>
                <div className="payment-receipt__row"><span>사용자</span><strong>{selectedPayment.userName || selectedPayment.userEmail || selectedPayment.userId}</strong></div>
                <div className="payment-receipt__row"><span>결제수단</span><strong>{selectedPayment.paymentMethod || '-'}</strong></div>
                {selectedPayment.sourceId ? <div className="payment-receipt__row"><span>원본 ID</span><strong>{selectedPayment.sourceId}</strong></div> : null}
                {selectedPayment.failureReason ? <div className="payment-receipt__row"><span>실패 사유</span><strong>{selectedPayment.failureReason}</strong></div> : null}
              </div>

              <div className="payment-receipt__total">
                <span>결제 금액</span>
                <strong>{Number(selectedPayment.amount || 0).toLocaleString('ko-KR')}원</strong>
              </div>

              {String(selectedPayment.sourceType ?? '').toUpperCase() === 'ORDER' ? (
                <div className="payment-receipt__order">
                  <p className="payment-receipt__order-title">주문 상세 목록</p>
                  {isPaymentOrderLoading ? <p className="payment-receipt__order-empty">주문 항목을 불러오는 중입니다.</p> : null}
                  {!isPaymentOrderLoading && paymentOrderError ? <p className="payment-receipt__order-empty">{paymentOrderError}</p> : null}
                  {!isPaymentOrderLoading && !paymentOrderError && paymentOrderItems.length === 0 ? (
                    <p className="payment-receipt__order-empty">주문 항목이 없습니다.</p>
                  ) : null}
                  {!isPaymentOrderLoading && !paymentOrderError && paymentOrderItems.length > 0 ? (
                    <div className="payment-receipt__order-list">
                      {paymentOrderItems.map((item) => (
                        <div key={item.id} className="payment-receipt__order-row">
                          <span>{item.bookTitle}</span>
                          <strong>수량 {item.quantity} · {Number(item.price || 0).toLocaleString('ko-KR')}원</strong>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
            <div className="button-row">
              <button className="button button--secondary" type="button" onClick={() => setSelectedPayment(null)}>닫기</button>
            </div>
          </article>
        </div>
      ) : null}

      {activeTab === 'subscriptions' ? (
        <div className="workspace-grid workspace-grid--single">
          <article className="panel panel--wide">
            <div className="panel__header">
              <div>
                <h3 className="panel__title">구독 종류 관리</h3>
                <p className="panel__subtitle">구독 플랜을 추가, 수정, 삭제할 수 있습니다.</p>
              </div>
              <button className="button" type="button" onClick={beginSubscriptionPlanCreate}>구독 종류 추가</button>
            </div>

            <div className="resource-list">
              {subscriptionPlans.map((plan) => (
                <div key={plan.id} className="resource-list__item resource-list__item--static">
                  <strong>{plan.name} · {plan.code}</strong>
                  <span>{Number(plan.price || 0).toLocaleString('ko-KR')}원 · {plan.durationDays}일</span>
                  <span>{plan.description}</span>
                  <div className="button-row">
                    <button className="button button--secondary" type="button" onClick={() => beginSubscriptionPlanEdit(plan)}>수정</button>
                    <button className="button button--secondary" type="button" onClick={() => handleSubscriptionPlanDelete(plan.id)}>삭제</button>
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="panel panel--wide">
            <div className="panel__header"><div><h3 className="panel__title">구독 현황</h3></div></div>
            <div className="resource-list">
              {subscriptions.map((subscription) => (
                <div key={subscription.id} className="resource-list__item resource-list__item--static">
                  <strong>{subscription.planName} · {resolveUserLabel(users, subscription.userId)}</strong>
                  <span>{toKoreanSubscriptionStatus(subscription.status)} · {subscription.startDate} ~ {subscription.endDate}</span>
                </div>
              ))}
            </div>
          </article>
        </div>
      ) : null}

      {activeTab === 'subscriptions' && isSubscriptionPlanModalOpen ? (
        <div className="admin-book-modal-backdrop" role="presentation" onClick={closeSubscriptionPlanModal}>
          <article className="panel panel--wide admin-book-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <div className="panel__header">
              <div><h3 className="panel__title">{editingSubscriptionPlanId ? '구독 종류 수정' : '구독 종류 추가'}</h3></div>
            </div>
            <form className="editor-form" onSubmit={handleSubscriptionPlanSubmit}>
              <input className="input" value={subscriptionPlanForm.code} placeholder="코드 (예: BASIC)" onChange={(event) => setSubscriptionPlanForm((current) => ({ ...current, code: event.target.value }))} required />
              <input className="input" value={subscriptionPlanForm.name} placeholder="플랜명" onChange={(event) => setSubscriptionPlanForm((current) => ({ ...current, name: event.target.value }))} required />
              <input className="input" type="number" min="0" step="100" value={subscriptionPlanForm.price} placeholder="가격" onChange={(event) => setSubscriptionPlanForm((current) => ({ ...current, price: event.target.value }))} required />
              <input className="input" type="number" min="1" step="1" value={subscriptionPlanForm.durationDays} placeholder="기간(일)" onChange={(event) => setSubscriptionPlanForm((current) => ({ ...current, durationDays: event.target.value }))} required />
              <textarea className="input input--textarea" rows="4" value={subscriptionPlanForm.description} placeholder="설명" onChange={(event) => setSubscriptionPlanForm((current) => ({ ...current, description: event.target.value }))} required />
              <div className="button-row">
                <button className="button" type="submit">{editingSubscriptionPlanId ? '구독 종류 수정' : '구독 종류 추가'}</button>
                <button className="button button--secondary" type="button" onClick={closeSubscriptionPlanModal}>취소</button>
              </div>
            </form>
          </article>
        </div>
      ) : null}
    </section>
  )
}

export default AdminPage
