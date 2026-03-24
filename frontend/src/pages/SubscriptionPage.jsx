import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import subscriptionApi from '../api/subscriptionApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { toKoreanPaymentStatus, toKoreanSubscriptionStatus } from '../utils/paymentStatus.js'

function SubscriptionPage() {
  usePageTitle('구독관리')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const callbackProcessedRef = useRef(false)

  const [plans, setPlans] = useState([])
  const [currentSubscription, setCurrentSubscription] = useState(null)
  const [history, setHistory] = useState([])
  const [payments, setPayments] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const hasActiveSubscription = Boolean(currentSubscription?.active)
  const currentPlanId = currentSubscription?.planId ?? null

  async function loadData() {
    setIsLoading(true)
    setError('')
    try {
      const [planItems, current, subscriptionHistory, paymentHistory] = await Promise.all([
        subscriptionApi.getPlans(),
        subscriptionApi.getCurrent(),
        subscriptionApi.getHistory(),
        subscriptionApi.getPayments(),
      ])
      setPlans(planItems)
      setCurrentSubscription(current)
      setHistory(subscriptionHistory)
      setPayments(paymentHistory)
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  useEffect(() => {
    const callback = searchParams.get('callback')
    const planIdParam = searchParams.get('planId')
    const pgToken = searchParams.get('pg_token')

    if (!callback || callbackProcessedRef.current) {
      return
    }

    if (!planIdParam) {
      setError('결제 처리에 필요한 planId가 없습니다.')
      navigate('/subscriptions', { replace: true })
      return
    }

    callbackProcessedRef.current = true

    async function processCallback() {
      setIsSubmitting(true)
      setError('')
      setNotice('')
      try {
        const planId = Number(planIdParam)
        if (callback === 'success') {
          if (!pgToken) {
            throw new Error('카카오페이 승인 토큰이 없습니다.')
          }
          await subscriptionApi.approveKakao({ planId, pgToken })
          setNotice('카카오페이 결제가 완료되었습니다.')
        } else if (callback === 'cancel') {
          await subscriptionApi.cancelKakao({ planId })
          setNotice('카카오페이 결제가 취소되었습니다.')
        } else if (callback === 'fail') {
          await subscriptionApi.failKakao({ planId })
          setNotice('카카오페이 결제에 실패했습니다.')
        }

        await loadData()
      } catch (callbackError) {
        setError(callbackError.message)
      } finally {
        setIsSubmitting(false)
        navigate('/subscriptions', { replace: true })
      }
    }

    processCallback()
  }, [navigate, searchParams])

  async function handleSubscribe(plan) {
    setIsSubmitting(true)
    setError('')
    setNotice('')

    try {
      if (hasActiveSubscription && String(currentPlanId) === String(plan.id)) {
        setNotice('현재 이용 중인 구독 상품입니다.')
        return
      }

      // 무료 플랜은 즉시 반영, 유료 플랜은 카카오 결제 플로우로 진행
      if (Number(plan.price) <= 0) {
        if (hasActiveSubscription) {
          await subscriptionApi.cancel()
        }
        await subscriptionApi.subscribe({ planId: plan.id, paymentMethod: 'FREE' })
        setNotice(hasActiveSubscription ? '구독 상품이 변경되었습니다.' : '구독이 시작되었습니다.')
        await loadData()
        return
      }

      const readyResult = await subscriptionApi.readyKakao({ planId: plan.id, paymentMethod: 'KAKAO_PAY' })
      window.location.assign(readyResult.redirectUrl)
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleCancel() {
    setIsSubmitting(true)
    setError('')
    setNotice('')
    try {
      await subscriptionApi.cancel()
      setNotice('구독이 해지되었습니다.')
      await loadData()
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <section className="workspace-page"><LoadingBlock label="구독 정보를 불러오는 중입니다." /></section>
  }

  if (error && !plans.length && !currentSubscription) {
    return <section className="workspace-page"><ErrorBlock message={error} /></section>
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">구독관리</p>
        <h2 className="page-card__title">현재 구독 상태와 결제 이력을 확인하세요.</h2>
        <p className="page-card__description">유료 플랜은 카카오페이 결제를 통해 구독/변경됩니다.</p>
      </div>

      <StatusMessage variant="success" message={notice} />
      <StatusMessage variant="error" message={plans.length || currentSubscription ? error : ''} />

      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">현재 구독</h3>
              <p className="panel__subtitle">시작일, 종료일, 활성 상태를 확인할 수 있습니다.</p>
            </div>
          </div>
          <p className="panel__subtitle">플랜: {currentSubscription?.planName ?? '없음'}</p>
          <p className="panel__subtitle">상태: {toKoreanSubscriptionStatus(currentSubscription?.status ?? 'EXPIRED')}</p>
          <p className="panel__subtitle">시작일: {currentSubscription?.startDate ?? '-'}</p>
          <p className="panel__subtitle">종료일: {currentSubscription?.endDate ?? '-'}</p>
          <p className="panel__subtitle">결제수단: {currentSubscription?.paymentMethod ?? '-'}</p>
          {currentSubscription?.active ? (
            <div className="button-row">
              <button className="button button--secondary" type="button" onClick={handleCancel} disabled={isSubmitting}>
                {isSubmitting ? '처리 중...' : '구독 해지'}
              </button>
            </div>
          ) : null}
        </article>

        <article className="panel panel--wide">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">구독 상품</h3>
              <p className="panel__subtitle">현재 구독이 있으면 변경하기로 처리됩니다.</p>
            </div>
          </div>
          <div className="resource-list">
            {plans.map((plan) => (
              <div key={plan.id} className="resource-list__item resource-list__item--static">
                <strong>{plan.name}</strong>
                <span>{plan.description}</span>
                <span>{Number(plan.price).toLocaleString('ko-KR')}원 / {plan.durationDays}일</span>
                <div className="button-row">
                  <button className="button" type="button" onClick={() => handleSubscribe(plan)} disabled={isSubmitting}>
                    {hasActiveSubscription ? '변경하기' : '구독하기'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </article>
      </div>

      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header"><div><h3 className="panel__title">구독 이력</h3></div></div>
          <div className="resource-list">
            {history.map((item) => (
              <div key={item.id} className="resource-list__item resource-list__item--static">
                <strong>{item.planName}</strong>
                <span>{toKoreanSubscriptionStatus(item.status)}</span>
                <span>{item.startDate} ~ {item.endDate}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel panel--wide">
          <div className="panel__header"><div><h3 className="panel__title">결제 이력</h3></div></div>
          <div className="resource-list">
            {payments.map((payment) => (
              <div key={payment.id} className="resource-list__item resource-list__item--static">
                <strong>{payment.planName}</strong>
                <span>{toKoreanPaymentStatus(payment.status)} · {payment.paymentMethod}</span>
                <span>{Number(payment.amount).toLocaleString('ko-KR')}원</span>
                <span>{payment.paidAt}</span>
              </div>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}

export default SubscriptionPage
