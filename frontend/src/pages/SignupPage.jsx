import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import authApi from '../api/authApi.js'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import { BUTTON_LABELS, ERROR_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import useAuthStore from '../store/authStore.js'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_REGEX = /^[0-9-]+$/
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).+$/

function SignupPage() {
  usePageTitle('회원가입')
  const navigate = useNavigate()
  const setUser = useAuthStore((state) => state.setUser)
  const [formData, setFormData] = useState({ name: '', email: '', phone: '', password: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(name, value) {
    setFormData((current) => ({ ...current, [name]: value }))
  }

  function validateForm() {
    if (!formData.name.trim()) return '이름을 입력해 주세요.'
    if (!EMAIL_REGEX.test(formData.email)) return '올바른 이메일 형식을 입력해 주세요.'
    if (!formData.phone.trim()) return '연락처를 입력해 주세요.'
    if (formData.phone.length > 20 || !PHONE_REGEX.test(formData.phone)) return '연락처는 20자 이하의 숫자와 하이픈만 사용할 수 있습니다.'
    if (formData.password.length < 8 || formData.password.length > 20) return '비밀번호는 8자 이상 20자 이하로 입력해 주세요.'
    if (!PASSWORD_REGEX.test(formData.password)) return '비밀번호에는 영문과 숫자가 모두 포함되어야 합니다.'
    if (formData.password !== formData.confirmPassword) return '비밀번호 확인이 일치하지 않습니다.'
    return ''
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    const validationMessage = validateForm()
    if (validationMessage) {
      setError(validationMessage)
      return
    }

    const payload = {
      name: formData.name.trim(),
      email: formData.email.trim(),
      phone: formData.phone.trim(),
      password: formData.password,
    }

    console.log('signup payload', payload)
    setIsSubmitting(true)

    try {
      const result = await authApi.signup(payload)
      setUser(result.user)
      setSuccess('회원가입이 완료되었습니다. 대시보드로 이동합니다.')
      setTimeout(() => navigate('/dashboard'), 800)
    } catch (signupError) {
      setError(signupError.message ?? ERROR_MESSAGES.signup)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-panel auth-panel--hero">
        <p className="page-card__eyebrow">회원가입</p>
        <h2 className="page-card__title">지원자료와 모의면접 준비를 한 곳에서 시작해 보세요.</h2>
        <p className="page-card__description">계정을 만들면 지원자료, 모의면접, 학습 기록을 계속 관리할 수 있습니다.</p>
      </div>

      <form className="auth-panel auth-panel--form" onSubmit={handleSubmit}>
        <div className="panel__header">
          <div>
            <h3 className="panel__title">계정 정보 입력</h3>
            <p className="panel__subtitle">백엔드가 요구하는 필드만 전송합니다.</p>
          </div>
        </div>

        <StatusMessage variant="error" message={error} />
        <StatusMessage variant="success" message={success} />

        <div className="editor-form">
          <TextInput label="이름" name="name" value={formData.name} onChange={(event) => updateField('name', event.target.value)} placeholder="홍길동" />
          <TextInput label="이메일" name="email" value={formData.email} onChange={(event) => updateField('email', event.target.value)} placeholder="name@example.com" />
          <TextInput label="연락처" name="phone" value={formData.phone} onChange={(event) => updateField('phone', event.target.value)} placeholder="010-1234-5678" />
          <TextInput label="비밀번호" type="password" name="password" value={formData.password} onChange={(event) => updateField('password', event.target.value)} placeholder="영문, 숫자 포함 8~20자" />
          <TextInput label="비밀번호 확인" type="password" name="confirmPassword" value={formData.confirmPassword} onChange={(event) => updateField('confirmPassword', event.target.value)} placeholder="비밀번호를 다시 입력해 주세요" />
        </div>

        <div className="button-row">
          <button className="button" type="submit" disabled={isSubmitting}>{isSubmitting ? STATUS_MESSAGES.creatingAccount : BUTTON_LABELS.register}</button>
        </div>

        <p className="auth-panel__footnote">이미 계정이 있으신가요? <Link to="/auth/login">{BUTTON_LABELS.login}</Link></p>
      </form>
    </section>
  )
}

export default SignupPage
