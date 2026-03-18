import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import authApi from '../api/authApi.js'
import { consumeAuthNotice } from '../api/client.js'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import { BUTTON_LABELS, ERROR_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useAuthStore from '../store/authStore.js'

function LoginPage() {
  usePageTitle('로그인')
  const location = useLocation()
  const navigate = useNavigate()
  const setUser = useAuthStore((state) => state.setUser)
  const [formData, setFormData] = useState({ email: 'demo@aimentor.dev', password: 'password123' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [notice, setNotice] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const message = consumeAuthNotice()
    if (message) setNotice(message)
  }, [])

  function updateField(name, value) {
    setFormData((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setNotice('')
    setIsSubmitting(true)

    try {
      const result = await authApi.login(formData)
      setUser(result.user)
      setSuccess(`${result.user.name ?? result.user.email}님, 로그인되었습니다.`)
      const nextPath = location.state?.from?.pathname ?? '/dashboard'
      setTimeout(() => navigate(nextPath, { replace: true }), 500)
    } catch (loginError) {
      setError(loginError.message ?? ERROR_MESSAGES.login)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-panel auth-panel--hero">
        <p className="page-card__eyebrow">다시 시작해 보세요</p>
        <h2 className="page-card__title">지원자료 관리와 모의면접을 이어서 진행하려면 로그인해 주세요.</h2>
        <p className="page-card__description">스텁 모드에서는 demo@aimentor.dev / password123 계정으로도 확인할 수 있습니다.</p>
      </div>

      <form className="auth-panel auth-panel--form" onSubmit={handleSubmit}>
        <div className="panel__header">
          <div>
            <h3 className="panel__title">로그인</h3>
            <p className="panel__subtitle">JWT 기반 인증 흐름을 사용합니다.</p>
          </div>
        </div>

        <StatusMessage variant="error" message={notice || error} />
        <StatusMessage variant="success" message={success} />

        <div className="editor-form">
          <TextInput label="이메일" name="email" value={formData.email} onChange={(event) => updateField('email', event.target.value)} placeholder="name@example.com" />
          <TextInput label="비밀번호" type="password" name="password" value={formData.password} onChange={(event) => updateField('password', event.target.value)} placeholder="비밀번호를 입력해 주세요" />
        </div>

        <div className="button-row">
          <button className="button" type="submit" disabled={isSubmitting}>{isSubmitting ? STATUS_MESSAGES.signingIn : BUTTON_LABELS.login}</button>
        </div>

        <p className="auth-panel__footnote">계정이 없으신가요? <Link to="/auth/register">{BUTTON_LABELS.register}</Link></p>
      </form>
    </section>
  )
}

export default LoginPage
