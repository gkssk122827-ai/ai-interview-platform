import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import cartApi from '../api/cartApi.js'
import { EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function CartPage() {
  usePageTitle('장바구니')
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  async function loadCart() {
    setIsLoading(true)
    setError('')
    try {
      const result = await cartApi.getCart()
      setCart(result)
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadCart()
  }, [])

  async function handleQuantityChange(bookId, quantity) {
    try {
      await cartApi.updateItem(bookId, quantity)
      await loadCart()
    } catch (updateError) {
      setError(updateError.message)
    }
  }

  async function handleRemove(bookId) {
    try {
      await cartApi.removeItem(bookId)
      await loadCart()
    } catch (removeError) {
      setError(removeError.message)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingCart} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">장바구니</p>
        <h2 className="page-card__title">담아 둔 도서를 확인하고 결제 단계로 넘어가 보세요.</h2>
        <p className="page-card__description">수량 변경과 삭제 후 주문서 작성, 결제 진행, 완료 화면까지 이어집니다.</p>
      </div>

      <StatusMessage variant="error" message={error} />
      <StatusMessage variant="success" message={successMessage} />

      {!error && !(cart?.items?.length) ? <EmptyState title={EMPTY_MESSAGES.cart.title} description={EMPTY_MESSAGES.cart.description} action={<button className="button" type="button" onClick={() => navigate('/books')}>도서 보러 가기</button>} /> : null}
      {error ? <ErrorBlock message={error} /> : null}

      {cart?.items?.length ? (
        <section className="workspace-grid">
          <article className="panel panel--wide">
            <div className="panel__header"><div><h3 className="panel__title">장바구니 목록</h3></div></div>
            <div className="resource-list">
              {(cart?.items ?? []).map((item) => (
                <div key={item.bookId} className="resource-list__item resource-list__item--static">
                  <strong>{item.bookTitle}</strong>
                  <span>{item.bookAuthor} · {item.bookPrice}원</span>
                  <div className="button-row">
                    <button className="button button--secondary" type="button" onClick={() => handleQuantityChange(item.bookId, Math.max(1, item.quantity - 1))}>-</button>
                    <span className="panel__subtitle">수량 {item.quantity}</span>
                    <button className="button button--secondary" type="button" onClick={() => handleQuantityChange(item.bookId, item.quantity + 1)}>+</button>
                    <button className="button button--danger" type="button" onClick={() => handleRemove(item.bookId)}>삭제</button>
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel__header"><div><h3 className="panel__title">결제 준비</h3></div></div>
            <p className="panel__subtitle">총 수량: {cart?.totalQuantity ?? 0}</p>
            <p className="panel__subtitle">총 금액: {cart?.totalPrice ?? 0}원</p>
            <p className="panel__subtitle">다음 단계에서 주문자 정보와 결제 수단을 입력합니다.</p>
            <div className="button-row">
              <button className="button" type="button" onClick={() => navigate('/checkout')} disabled={!cart?.items?.length}>
                주문서 작성
              </button>
            </div>
          </article>
        </section>
      ) : null}
    </section>
  )
}

export default CartPage
