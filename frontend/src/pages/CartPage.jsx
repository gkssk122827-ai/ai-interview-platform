import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import cartApi from '../api/cartApi.js'
import orderApi from '../api/orderApi.js'
import { BUTTON_LABELS, EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

function CartPage() {
  usePageTitle('장바구니')
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [address, setAddress] = useState('서울시 강남구 테헤란로 123')
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

  async function handleOrder() {
    try {
      const order = await orderApi.create(address)
      setSuccessMessage('주문이 완료되었습니다.')
      navigate(`/orders?orderId=${order.id}`)
    } catch (orderError) {
      setError(orderError.message)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingCart} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">장바구니</p>
        <h2 className="page-card__title">담아 둔 도서를 확인하고 주문해 보세요.</h2>
        <p className="page-card__description">수량 변경과 삭제, 주문까지 실제 API와 연결됩니다.</p>
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
            <div className="panel__header"><div><h3 className="panel__title">주문 정보</h3></div></div>
            <p className="panel__subtitle">총 수량: {cart?.totalQuantity ?? 0}</p>
            <p className="panel__subtitle">총 금액: {cart?.totalPrice ?? 0}원</p>
            <TextInput label="배송지" value={address} onChange={(event) => setAddress(event.target.value)} placeholder="배송지를 입력해 주세요." />
            <div className="button-row"><button className="button" type="button" onClick={handleOrder} disabled={!cart?.items?.length}>{BUTTON_LABELS.orderNow}</button></div>
          </article>
        </section>
      ) : null}
    </section>
  )
}

export default CartPage
