import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import SelectField from '../components/forms/SelectField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import interviewApi from '../api/interviewApi.js'
import jobPostingApi from '../api/jobPostingApi.js'
import profileDocumentApi from '../api/profileDocumentApi.js'
import { BUTTON_LABELS, EMPTY_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'

const modeOptions = [
  { value: 'COMPREHENSIVE', label: '종합' },
  { value: 'BEHAVIORAL', label: '인성' },
  { value: 'TECHNICAL', label: '기술' },
  { value: 'RESUME_BASED', label: '자소서 기반' },
]

const defaultPositionOptions = [
  '백엔드 개발자',
  '프론트엔드 개발자',
  '풀스택 개발자',
  '안드로이드 개발자',
  'iOS 개발자',
  '데이터 엔지니어',
  'DevOps 엔지니어',
]

function InterviewSetupPage() {
  usePageTitle('면접 설정')
  const navigate = useNavigate()
  const [mode, setMode] = useState('COMPREHENSIVE')
  const [title, setTitle] = useState('지원자료 기반 모의면접')
  const [positionTitle, setPositionTitle] = useState('')
  const [documents, setDocuments] = useState([])
  const [jobPostings, setJobPostings] = useState([])
  const [selectedDocumentId, setSelectedDocumentId] = useState('')
  const [selectedJobPostingId, setSelectedJobPostingId] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isStarting, setIsStarting] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')

  const selectedDocument = useMemo(
    () => documents.find((item) => String(item.id) === String(selectedDocumentId)) ?? null,
    [documents, selectedDocumentId],
  )
  const selectedJobPosting = useMemo(
    () => jobPostings.find((item) => String(item.id) === String(selectedJobPostingId)) ?? null,
    [jobPostings, selectedJobPostingId],
  )

  const positionOptions = useMemo(() => {
    const merged = [...defaultPositionOptions, ...jobPostings.map((item) => item.positionTitle).filter(Boolean)]
    return Array.from(new Set(merged)).map((item) => ({ value: item, label: item }))
  }, [jobPostings])

  useEffect(() => {
    async function loadOptions() {
      setIsLoading(true)
      setLoadError('')
      try {
        const [documentItems, jobPostingItems] = await Promise.all([profileDocumentApi.list(), jobPostingApi.list()])
        setDocuments(documentItems)
        setJobPostings(jobPostingItems)

        if (documentItems.length > 0) setSelectedDocumentId(String(documentItems[0].id))
        if (jobPostingItems.length > 0) {
          setSelectedJobPostingId(String(jobPostingItems[0].id))
          setPositionTitle(jobPostingItems[0].positionTitle || defaultPositionOptions[0])
        } else {
          setPositionTitle(defaultPositionOptions[0])
        }
      } catch (error) {
        setLoadError(error.message || '면접 설정 정보를 불러오는 중 오류가 발생했습니다.')
      } finally {
        setIsLoading(false)
      }
    }

    loadOptions()
  }, [])

  useEffect(() => {
    if (selectedJobPosting?.positionTitle) {
      setPositionTitle(selectedJobPosting.positionTitle)
    }
  }, [selectedJobPosting])

  async function handleStart() {
    if (!selectedDocumentId) {
      setActionError('면접에 사용할 지원자료를 선택해 주세요.')
      return
    }

    if (!title.trim() || !positionTitle.trim()) {
      setActionError('면접 제목과 지원 직무를 확인해 주세요.')
      return
    }

    setIsStarting(true)
    setActionError('')

    try {
      const session = await interviewApi.startSession({
        title: title.trim(),
        positionTitle: positionTitle.trim(),
        mode,
        applicationDocumentId: Number(selectedDocumentId),
        jobPostingId: selectedJobPostingId ? Number(selectedJobPostingId) : null,
        questionCount: 5,
      })

      const sessionNotice = session.questionGenerationFallbackUsed
        ? 'AI 질문 생성에 문제가 있어 기본 질문 세트로 면접을 시작했습니다.'
        : 'AI 기반 면접 질문이 준비되었습니다.'

      navigate(`/interview/session?sessionId=${session.id}`, {
        state: {
          sessionNotice,
          sessionSource: session.questionGenerationSource || null,
          sessionFallbackUsed: Boolean(session.questionGenerationFallbackUsed),
        },
      })
    } catch (error) {
      setActionError(error.message || '면접 세션을 시작하는 중 오류가 발생했습니다.')
    } finally {
      setIsStarting(false)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">면접 설정</p>
        <h2 className="page-card__title">지원자료와 직무를 선택하고 모의면접을 시작해 보세요.</h2>
        <p className="page-card__description">면접 모드와 채용공고를 함께 선택하면 질문의 성격이 더 분명하게 달라집니다.</p>
      </div>

      {!loadError ? <StatusMessage variant="error" message={actionError} /> : null}

      <section className="panel interview-setup-card interview-setup-card--wide">
        {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingInterviewSetup} /> : null}
        {!isLoading && loadError ? <ErrorBlock message={loadError} /> : null}

        {!isLoading && !loadError && documents.length === 0 ? (
          <EmptyState
            title={EMPTY_MESSAGES.interviewDocuments.title}
            description={EMPTY_MESSAGES.interviewDocuments.description}
            action={<button className="button" type="button" onClick={() => navigate('/profile-documents')}>지원자료 등록하러 가기</button>}
          />
        ) : null}

        {!isLoading && !loadError && documents.length > 0 ? (
          <>
            <SelectField label="면접 모드" value={mode} onChange={(event) => setMode(event.target.value)} options={modeOptions} />
            <TextInput label="면접 제목" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="백엔드 개발자 모의면접" />
            <SelectField label="지원 직무" value={positionTitle} onChange={(event) => setPositionTitle(event.target.value)} options={positionOptions} />
            <SelectField
              label="지원자료"
              value={selectedDocumentId}
              onChange={(event) => setSelectedDocumentId(event.target.value)}
              options={[{ value: '', label: '지원자료를 선택해 주세요.' }, ...documents.map((item) => ({ value: String(item.id), label: item.title }))]}
            />
            <SelectField
              label="채용공고"
              value={selectedJobPostingId}
              onChange={(event) => setSelectedJobPostingId(event.target.value)}
              options={[{ value: '', label: '채용공고를 선택하지 않음' }, ...jobPostings.map((item) => ({ value: String(item.id), label: `${item.companyName} · ${item.positionTitle}` }))]}
            />

            {jobPostings.length === 0 ? <EmptyState title={EMPTY_MESSAGES.interviewJobPostings.title} description={EMPTY_MESSAGES.interviewJobPostings.description} /> : null}

            {selectedDocument ? (
              <div className="interview-note">
                <strong>선택한 지원자료</strong>
                <p>{selectedDocument.title}</p>
                <p>{selectedDocument.originalFileName ? `첨부 파일: ${selectedDocument.originalFileName}` : '텍스트 기반 지원자료입니다.'}</p>
              </div>
            ) : null}

            {selectedJobPosting ? (
              <div className="interview-note">
                <strong>선택한 채용공고</strong>
                <p>{selectedJobPosting.companyName} · {selectedJobPosting.positionTitle}</p>
                <p>{selectedJobPosting.deadline ? `마감일: ${selectedJobPosting.deadline}` : '상시 채용 또는 마감일 미정'}</p>
              </div>
            ) : null}

            <div className="button-row">
              <button className="button" type="button" onClick={handleStart} disabled={isStarting || isLoading}>
                {isStarting ? STATUS_MESSAGES.generatingInterviewQuestions : BUTTON_LABELS.startInterview}
              </button>
            </div>
          </>
        ) : null}
      </section>
    </section>
  )
}

export default InterviewSetupPage
