import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import orderApi from '../api/orderApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import usePaymentStore from '../store/paymentStore.js'

function PaymentPage() {
  usePageTitle('결제 진행')
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId')
  const checkoutForm = usePaymentStore((state) => state.checkoutForm)
  const setPaymentProgress = usePaymentStore((state) => state.setPaymentProgress)
  const [order, setOrder] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isPaying, setIsPaying] = useState(false)
  const [error, setError] = useState('')

  const paymentMethod = useMemo(
    () => location.state?.paymentMethod ?? checkoutForm.paymentMethod ?? order?.paymentMethod ?? 'KAKAO_PAY',
    [checkoutForm.paymentMethod, location.state, order],
  )

  const isKakaoPay = paymentMethod === 'KAKAO_PAY'

  useEffect(() => {
    async function loadOrder() {
      if (!orderId) {
        setIsLoading(false)
        setError('결제할 주문 정보를 찾을 수 없습니다.')
        return
      }

      setIsLoading(true)
      setError('')
      try {
        const result = await orderApi.get(orderId)
        setOrder(result)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadOrder()
  }, [orderId])

  async function handleStartKakaoPay() {
    if (!orderId) return

    setIsPaying(true)
    setError('')

    try {
      const readyResult = await orderApi.readyKakao(orderId, { paymentMethod: 'KAKAO_PAY' })
      setPaymentProgress({
        orderId: readyResult.orderId,
        status: 'READY',
        phase: 'redirect',
        redirectUrl: readyResult.redirectUrl,
        message: '카카오페이 결제창으로 이동합니다.',
      })
      window.location.assign(readyResult.redirectUrl)
    } catch (paymentError) {
      setError(paymentError.message)
      setPaymentProgress({
        orderId: Number(orderId),
        status: 'PAYMENT_FAILED',
        phase: 'error',
        message: paymentError.message,
      })
    } finally {
      setIsPaying(false)
    }
  }

  async function handleMockPayment(success) {
    if (!orderId) return

    setIsPaying(true)
    setError('')
    try {
      const paidOrder = await orderApi.pay(orderId, {
        paymentMethod,
        success,
        failureReason: success ? null : '사용자가 실패 시나리오를 선택했습니다.',
      })

      setPaymentProgress({
        orderId: paidOrder.id,
        status: paidOrder.status,
        phase: success ? 'success' : 'fail',
        message: success ? '모의 결제가 완료되었습니다.' : '모의 결제가 실패했습니다.',
      })

      navigate(`/payment/result?orderId=${paidOrder.id}`, { replace: true })
    } catch (paymentError) {
      setError(paymentError.message)
    } finally {
      setIsPaying(false)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="결제 정보를 불러오는 중입니다." /></section>
  }

  if (error && !order) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">결제 진행</p>
        <h2 className="page-card__title">{isKakaoPay ? '카카오페이 결제를 시작합니다.' : '모의 결제를 진행합니다.'}</h2>
        <p className="page-card__description">
          {isKakaoPay
            ? '결제 시작 시 백엔드가 카카오페이 ready API를 호출하고, 승인 후 기존 주문 완료 흐름과 연결됩니다.'
            : '기존 모의 결제 흐름도 테스트용으로 계속 사용할 수 있습니다.'}
        </p>
      </div>

      <StatusMessage variant="error" message={error} />

      {!order ? (
        <EmptyState title="주문 정보를 찾을 수 없습니다." description="주문서를 다시 작성한 뒤 결제를 진행해 주세요." />
      ) : (
        <div className="workspace-grid">
          <article className="panel panel--wide">
            <div className="panel__header"><div><h3 className="panel__title">결제 대상 주문</h3></div></div>
            <p className="panel__subtitle">주문 번호: #{order.id}</p>
            <p className="panel__subtitle">결제 수단: {paymentMethod}</p>
            <p className="panel__subtitle">총 결제 금액: {order.totalPrice}원</p>
            <div className="resource-list">
              {order.items.map((item) => (
                <div key={item.id} className="resource-list__item resource-list__item--static">
                  <strong>{item.bookTitle}</strong>
                  <span>수량 {item.quantity} · {item.price}원</span>
                </div>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel__header"><div><h3 className="panel__title">{isKakaoPay ? '카카오페이' : '모의 결제'}</h3></div></div>
            {isKakaoPay ? (
              <>
                <p className="panel__subtitle">결제 시작 버튼을 누르면 카카오페이 결제창으로 이동합니다.</p>
                <div className="button-row">
                  <button className="button button--secondary" type="button" onClick={() => navigate('/checkout', { replace: true })}>
                    주문서로 돌아가기
                  </button>
                  <button className="button" type="button" onClick={handleStartKakaoPay} disabled={isPaying}>
                    {isPaying ? '결제창 준비 중...' : '카카오페이로 이동'}
                  </button>
                </div>
              </>
            ) : (
              <>
                <p className="panel__subtitle">기존 테스트용 모의 결제도 계속 사용할 수 있습니다.</p>
                <div className="button-row">
                  <button className="button button--secondary" type="button" onClick={() => navigate('/checkout', { replace: true })}>
                    주문서로 돌아가기
                  </button>
                  <button className="button button--danger" type="button" onClick={() => handleMockPayment(false)} disabled={isPaying}>
                    {isPaying ? '처리 중...' : '실패로 결제'}
                  </button>
                  <button className="button" type="button" onClick={() => handleMockPayment(true)} disabled={isPaying}>
                    {isPaying ? '처리 중...' : '성공으로 결제'}
                  </button>
                </div>
              </>
            )}
          </article>
        </div>
      )}
    </section>
  )
}

export default PaymentPage
