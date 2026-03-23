import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import orderApi from '../api/orderApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import usePaymentStore from '../store/paymentStore.js'

function PaymentResultPage() {
  usePageTitle('결제 결과')
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId')
  const callback = searchParams.get('callback')
  const pgToken = searchParams.get('pg_token')
  const callbackProcessedRef = useRef(false)
  const latestOrderId = usePaymentStore((state) => state.latestOrderId)
  const latestPaymentStatus = usePaymentStore((state) => state.latestPaymentStatus)
  const paymentPhase = usePaymentStore((state) => state.paymentPhase)
  const lastMessage = usePaymentStore((state) => state.lastMessage)
  const setPaymentProgress = usePaymentStore((state) => state.setPaymentProgress)
  const [order, setOrder] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    async function loadOrder() {
      if (!orderId) {
        setError('결과를 확인할 주문이 없습니다.')
        setIsLoading(false)
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

    async function handleCallback() {
      if (!orderId || callbackProcessedRef.current) {
        return
      }

      callbackProcessedRef.current = true
      setIsLoading(true)
      setError('')

      try {
        let result = null

        if (callback === 'success') {
          if (!pgToken) {
            throw new Error('카카오페이 승인 토큰이 없습니다.')
          }
          setNotice('카카오페이 승인 처리 중입니다.')
          result = await orderApi.approveKakao(orderId, { pgToken })
          setPaymentProgress({
            orderId: result.id,
            status: result.status,
            phase: result.status === 'PAID' ? 'success' : 'fail',
            message: result.status === 'PAID' ? '카카오페이 결제가 완료되었습니다.' : '카카오페이 승인 처리에 실패했습니다.',
          })
        } else if (callback === 'cancel') {
          setNotice('카카오페이 취소 처리 중입니다.')
          result = await orderApi.cancelKakao(orderId)
          setPaymentProgress({
            orderId: result.id,
            status: result.status,
            phase: 'cancel',
            message: '카카오페이 결제가 취소되었습니다.',
          })
        } else if (callback === 'fail') {
          setNotice('카카오페이 실패 처리 중입니다.')
          result = await orderApi.failKakao(orderId)
          setPaymentProgress({
            orderId: result.id,
            status: result.status,
            phase: 'fail',
            message: '카카오페이 결제에 실패했습니다.',
          })
        } else {
          result = await orderApi.get(orderId)
        }

        setOrder(result)
        setNotice('')
      } catch (callbackError) {
        setError(callbackError.message)
      } finally {
        setIsLoading(false)
      }
    }

    if (callback) {
      handleCallback()
      return
    }

    loadOrder()
  }, [callback, orderId, pgToken, setPaymentProgress])

  const isSuccess = useMemo(() => order?.status === 'PAID', [order])
  const latestPayment = useMemo(() => order?.payments?.[0] ?? null, [order])
  const statusMessage = notice || lastMessage

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="결제 결과를 불러오는 중입니다." /></section>
  }

  if (error) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">결제 결과</p>
        <h2 className="page-card__title">{isSuccess ? '결제가 완료되었습니다.' : '결제가 완료되지 않았습니다.'}</h2>
        <p className="page-card__description">
          {isSuccess
            ? '카카오페이 승인 또는 모의 결제 성공이 기존 주문 완료 흐름에 반영되었습니다.'
            : (order?.paymentFailureReason ?? '결제 결과를 확인한 뒤 다시 시도할 수 있습니다.')}
        </p>
      </div>

      <StatusMessage variant={isSuccess ? 'success' : 'error'} message={statusMessage} />

      <div className="workspace-grid">
        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">주문 요약</h3></div></div>
          <p className="panel__subtitle">주문 번호: #{order?.id}</p>
          <p className="panel__subtitle">주문 상태: {order?.status}</p>
          <p className="panel__subtitle">결제 수단: {order?.paymentMethod ?? '미선택'}</p>
          <p className="panel__subtitle">총 결제 금액: {order?.totalPrice}원</p>
          {latestOrderId && String(latestOrderId) === String(order?.id) && latestPaymentStatus ? (
            <p className="panel__subtitle">최근 결제 상태: {latestPaymentStatus} ({paymentPhase})</p>
          ) : null}
          <div className="resource-list">
            {(order?.items ?? []).map((item) => (
              <div key={item.id} className="resource-list__item resource-list__item--static">
                <strong>{item.bookTitle}</strong>
                <span>수량 {item.quantity} · {item.price}원</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">최신 결제 시도</h3></div></div>
          {latestPayment ? (
            <>
              <p className="panel__subtitle">결제 제공자: {latestPayment.provider}</p>
              <p className="panel__subtitle">거래 키: {latestPayment.transactionKey}</p>
              {latestPayment.providerTransactionId ? <p className="panel__subtitle">PG 거래 ID: {latestPayment.providerTransactionId}</p> : null}
              <p className="panel__subtitle">상태: {latestPayment.status}</p>
              <p className="panel__subtitle">요청 시각: {latestPayment.requestedAt}</p>
              {latestPayment.failureReason ? <p className="panel__subtitle">실패 사유: {latestPayment.failureReason}</p> : null}
            </>
          ) : (
            <p className="panel__subtitle">결제 시도 내역이 없습니다.</p>
          )}
          <div className="button-row">
            {!isSuccess ? <Link className="button button--secondary" to={`/payment?orderId=${order?.id}`}>다시 결제하기</Link> : null}
            <Link className="button" to={`/orders?orderId=${order?.id}`}>주문 내역 보기</Link>
          </div>
        </article>
      </div>
    </section>
  )
}

export default PaymentResultPage
