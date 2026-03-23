import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import { EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import orderApi from '../api/orderApi.js'
import usePageTitle from '../hooks/usePageTitle.js'

function OrderPage() {
  usePageTitle('주문')
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedOrderId = searchParams.get('orderId')
  const [orders, setOrders] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [isDetailLoading, setIsDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  useEffect(() => {
    async function loadOrders() {
      setIsLoading(true)
      setError('')
      try {
        const result = await orderApi.list()
        setOrders(result)
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadOrders()
  }, [])

  useEffect(() => {
    if (!orders.length || selectedOrderId) return
    setSearchParams({ orderId: String(orders[0].id) }, { replace: true })
  }, [orders, selectedOrderId, setSearchParams])

  useEffect(() => {
    async function loadOrderDetail() {
      if (!selectedOrderId) {
        setSelectedOrder(null)
        setDetailError('')
        return
      }

      setIsDetailLoading(true)
      setDetailError('')

      try {
        const result = await orderApi.get(selectedOrderId)
        setSelectedOrder(result)
      } catch (loadError) {
        setSelectedOrder(null)
        setDetailError(loadError.message)
      } finally {
        setIsDetailLoading(false)
      }
    }

    loadOrderDetail()
  }, [selectedOrderId])

  function handleSelectOrder(orderId) {
    setSearchParams({ orderId: String(orderId) })
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label={STATUS_MESSAGES.loadingOrders} /></section>
  }

  if (error) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">주문</p>
        <h2 className="page-card__title">주문과 결제 내역을 한 번에 확인해 보세요.</h2>
        <p className="page-card__description">카카오페이와 모의 결제 이력이 모두 같은 주문 상세 화면에 반영됩니다.</p>
      </div>

      {orders.length === 0 ? <EmptyState title={EMPTY_MESSAGES.orders.title} description={EMPTY_MESSAGES.orders.description} /> : null}

      {orders.length > 0 ? (
        <div className="workspace-grid">
          <article className="panel">
            <div className="panel__header"><div><h3 className="panel__title">주문 목록</h3></div></div>
            <div className="resource-list">
              {orders.map((order) => (
                <button
                  key={order.id}
                  type="button"
                  className={String(order.id) === String(selectedOrder?.id) ? 'resource-list__item resource-list__item--active' : 'resource-list__item'}
                  onClick={() => handleSelectOrder(order.id)}
                >
                  <strong>주문 #{order.id}</strong>
                  <span>{order.status} · {order.totalPrice}원</span>
                  <span>{order.orderedAt}</span>
                </button>
              ))}
            </div>
          </article>

          <article className="panel panel--wide">
            <div className="panel__header"><div><h3 className="panel__title">주문 상세</h3></div></div>
            {isDetailLoading ? <LoadingBlock label="주문 상세를 불러오는 중입니다." /> : null}
            {!isDetailLoading && detailError ? <ErrorBlock message={detailError} /> : null}
            {!isDetailLoading && !detailError && selectedOrder ? (
              <>
                <p className="panel__subtitle">상태: {selectedOrder.status}</p>
                <p className="panel__subtitle">배송지: {selectedOrder.address}</p>
                <p className="panel__subtitle">총 금액: {selectedOrder.totalPrice}원</p>
                <p className="panel__subtitle">결제 수단: {selectedOrder.paymentMethod ?? '미선택'}</p>
                {selectedOrder.paymentFailureReason ? <p className="panel__subtitle">실패 사유: {selectedOrder.paymentFailureReason}</p> : null}

                <div className="resource-list">
                  {selectedOrder.items.map((item) => (
                    <div key={`${selectedOrder.id}-${item.id}`} className="resource-list__item resource-list__item--static">
                      <strong>{item.bookTitle}</strong>
                      <span>수량 {item.quantity} · {item.price}원</span>
                    </div>
                  ))}
                </div>

                <div className="panel__header"><div><h3 className="panel__title">결제 시도 이력</h3></div></div>
                <div className="resource-list">
                  {(selectedOrder.payments ?? []).length ? selectedOrder.payments.map((payment) => (
                    <div key={payment.id} className="resource-list__item resource-list__item--static">
                      <strong>{payment.provider} · {payment.status}</strong>
                      <span>결제 수단: {payment.paymentMethod}</span>
                      <span>거래 키: {payment.transactionKey}</span>
                      {payment.providerTransactionId ? <span>PG 거래 ID: {payment.providerTransactionId}</span> : null}
                      <span>{payment.requestedAt}</span>
                      {payment.failureReason ? <span>{payment.failureReason}</span> : null}
                    </div>
                  )) : <div className="resource-list__item resource-list__item--static"><strong>결제 시도 내역이 없습니다.</strong></div>}
                </div>
              </>
            ) : null}
            {!isDetailLoading && !detailError && !selectedOrder ? (
              <EmptyState title={EMPTY_MESSAGES.orders.title} description={EMPTY_MESSAGES.orders.description} />
            ) : null}
          </article>
        </div>
      ) : null}
    </section>
  )
}

export default OrderPage
