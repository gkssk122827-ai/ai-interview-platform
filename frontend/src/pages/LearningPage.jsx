import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import StatusMessage from '../components/common/StatusMessage.jsx'
import SelectField from '../components/forms/SelectField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import learningApi from '../api/learningApi.js'
import { STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

const subjectOptions = [
  { value: 'frontend', label: '프론트엔드' },
  { value: 'backend', label: '백엔드' },
  { value: 'fullstack', label: '풀스택' },
  { value: 'database', label: '데이터베이스' },
]

const difficultyOptions = [
  { value: 'EASY', label: '쉬움' },
  { value: 'MEDIUM', label: '보통' },
  { value: 'HARD', label: '어려움' },
]

function LearningPage() {
  usePageTitle('학습')
  const navigate = useNavigate()
  const [subject, setSubject] = useState('frontend')
  const [difficulty, setDifficulty] = useState('MEDIUM')
  const [count, setCount] = useState('4')
  const [type] = useState('MULTIPLE')
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
        <p className="page-card__eyebrow">{'학습'}</p>
        <h2 className="page-card__title">{'IT 분야 과목을 선택하고 5지선다 학습을 시작해 보세요.'}</h2>
        <p className="page-card__description">{'백엔드 learning API로 문제를 생성하고 채점까지 이어집니다.'}</p>
      </div>
      <StatusMessage variant="error" message={error} />
      <section className="panel interview-setup-card interview-setup-card--wide">
        <SelectField
          label={'과목'}
          value={subject}
          onChange={(event) => setSubject(event.target.value)}
          options={subjectOptions}
        />
        <SelectField
          label={'난이도'}
          value={difficulty}
          onChange={(event) => setDifficulty(event.target.value)}
          options={difficultyOptions}
        />
        <TextInput
          label={'문제 수'}
          value={count}
          onChange={(event) => setCount(event.target.value)}
          placeholder="4"
        />
        <div className="button-row">
          <button className="button" type="button" onClick={handleStart} disabled={isLoading}>
            {isLoading ? STATUS_MESSAGES.preparing : '학습 시작'}
          </button>
        </div>
      </section>
    </section>
  )
}

export default LearningPage
