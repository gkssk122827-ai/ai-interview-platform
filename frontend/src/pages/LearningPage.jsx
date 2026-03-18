import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import StatusMessage from '../components/common/StatusMessage.jsx'
import SelectField from '../components/forms/SelectField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import learningApi from '../api/learningApi.js'
import { BUTTON_LABELS, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

const subjectOptions = [
  { value: '영어', label: '영어' },
  { value: '국사', label: '국사' },
]

const difficultyOptions = [
  { value: 'EASY', label: '쉬움' },
  { value: 'MEDIUM', label: '보통' },
  { value: 'HARD', label: '어려움' },
]

const typeOptions = [
  { value: 'MIX', label: '혼합' },
  { value: 'MULTIPLE', label: '객관식' },
  { value: 'SHORT', label: '주관식' },
]

function LearningPage() {
  usePageTitle('학습')
  const navigate = useNavigate()
  const [subject, setSubject] = useState('영어')
  const [difficulty, setDifficulty] = useState('MEDIUM')
  const [count, setCount] = useState('4')
  const [type, setType] = useState('MIX')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  async function handleStart() {
    setError('')
    setIsLoading(true)

    try {
      const response = await learningApi.generate({ subject, difficulty, count: Number(count), type })
      navigate('/learning/session', { state: { config: { subject, difficulty, count: Number(count), type }, problems: response.problems ?? [] } })
    } catch (generateError) {
      setError(generateError.message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">학습</p>
        <h2 className="page-card__title">과목과 난이도를 선택하고 학습을 시작해 보세요.</h2>
        <p className="page-card__description">backend learning API를 통해 문제를 생성하고 채점까지 이어집니다.</p>
      </div>
      <StatusMessage variant="error" message={error} />
      <section className="panel interview-setup-card interview-setup-card--wide">
        <SelectField label="과목" value={subject} onChange={(event) => setSubject(event.target.value)} options={subjectOptions} />
        <SelectField label="난이도" value={difficulty} onChange={(event) => setDifficulty(event.target.value)} options={difficultyOptions} />
        <SelectField label="문제 유형" value={type} onChange={(event) => setType(event.target.value)} options={typeOptions} />
        <TextInput label="문제 수" value={count} onChange={(event) => setCount(event.target.value)} placeholder="4" />
        <div className="button-row"><button className="button" type="button" onClick={handleStart} disabled={isLoading}>{isLoading ? STATUS_MESSAGES.preparing : BUTTON_LABELS.startLearning}</button></div>
      </section>
    </section>
  )
}

export default LearningPage