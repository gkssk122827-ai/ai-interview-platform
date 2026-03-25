import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import SelectField from '../components/forms/SelectField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import cartApi from '../api/cartApi.js'
import orderApi from '../api/orderApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import usePaymentStore from '../store/paymentStore.js'

const paymentMethodOptions = [
  { value: 'KAKAO_PAY', label: '카카오페이' },
  { value: 'CARD', label: '모의 카드 결제' },
  { value: 'BANK_TRANSFER', label: '모의 계좌 이체' },
  { value: 'MOBILE', label: '모의 모바일 결제' },
]

function CheckoutPage() {
  usePageTitle('주문서 작성')
  const navigate = useNavigate()
  const checkoutForm = usePaymentStore((state) => state.checkoutForm)
  const updateCheckoutForm = usePaymentStore((state) => state.updateCheckoutForm)
  const clearPaymentProgress = usePaymentStore((state) => state.clearPaymentProgress)
  const [cart, setCart] = useState(null)
  const [buyerName, setBuyerName] = useState(checkoutForm.buyerName)
  const [buyerPhone, setBuyerPhone] = useState(checkoutForm.buyerPhone)
  const [address, setAddress] = useState(checkoutForm.address)
  const [paymentMethod, setPaymentMethod] = useState(checkoutForm.paymentMethod)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  const orderSummary = useMemo(() => ({
    totalQuantity: cart?.totalQuantity ?? 0,
    totalPrice: cart?.totalPrice ?? 0,
  }), [cart])

  useEffect(() => {
    clearPaymentProgress()

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

    loadCart()
  }, [clearPaymentProgress])

  async function handleProceedPayment() {
    if (!address.trim()) {
      setError('배송지를 입력해 주세요.')
      return
    }

    setIsSubmitting(true)
    setError('')

    try {
      updateCheckoutForm({
        buyerName: buyerName.trim(),
        buyerPhone: buyerPhone.trim(),
        address: address.trim(),
        paymentMethod,
      })

      const order = await orderApi.create({ address: address.trim() })
      navigate(`/payment?orderId=${order.id}`, {
        state: {
          buyerName: buyerName.trim(),
          buyerPhone: buyerPhone.trim(),
          paymentMethod,
        },
      })
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="주문서를 준비하는 중입니다." /></section>
  }

  if (error && !cart) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">주문서 작성</p>
        <h2 className="page-card__title">주문 정보를 확인하고 결제를 진행해 보세요.</h2>
        <p className="page-card__description">기존 주문 흐름은 유지하면서 카카오페이와 모의 결제를 함께 사용할 수 있습니다.</p>
      </div>

      <StatusMessage variant="error" message={error} />

      {!error && !(cart?.items?.length) ? (
        <EmptyState
          title="장바구니가 비어 있습니다."
          description="상품을 먼저 담은 뒤 주문서를 작성해 주세요."
          action={<button className="button" type="button" onClick={() => navigate('/books')}>도서 보러 가기</button>}
        />
      ) : null}

      {cart?.items?.length ? (
        <div className="workspace-grid">
          <article className="panel panel--wide">
            <div className="panel__header"><div><h3 className="panel__title">주문 상품</h3></div></div>
            <div className="resource-list">
              {cart.items.map((item) => (
                <div key={item.bookId} className="resource-list__item resource-list__item--static">
                  <strong>{item.bookTitle}</strong>
                  <span>{item.bookAuthor} · 수량 {item.quantity}</span>
                  <span>{item.bookPrice}원</span>
                </div>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel__header"><div><h3 className="panel__title">주문자 정보</h3></div></div>
            <TextInput label="주문자명" value={buyerName} onChange={(event) => setBuyerName(event.target.value)} placeholder="주문자명을 입력해 주세요." />
            <TextInput label="연락처" value={buyerPhone} onChange={(event) => setBuyerPhone(event.target.value)} placeholder="연락처를 입력해 주세요." />
            <TextInput label="배송지" value={address} onChange={(event) => setAddress(event.target.value)} placeholder="배송지를 입력해 주세요." />
            <SelectField label="결제 수단" value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)} options={paymentMethodOptions} />
            <p className="panel__subtitle">총 수량: {orderSummary.totalQuantity}</p>
            <p className="panel__subtitle">총 금액: {orderSummary.totalPrice}원</p>
            <div className="button-row">
              <button className="button button--secondary" type="button" onClick={() => navigate('/cart')}>장바구니로 돌아가기</button>
              <button className="button" type="button" onClick={handleProceedPayment} disabled={isSubmitting}>
                {isSubmitting ? '주문 생성 중...' : '결제 진행'}
              </button>
            </div>
          </article>
        </div>
      ) : null}
    </section>
  )
}

export default CheckoutPage
