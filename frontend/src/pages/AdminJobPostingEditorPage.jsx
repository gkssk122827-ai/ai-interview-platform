import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import TextAreaField from '../components/forms/TextAreaField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import jobPostingApi from '../api/jobPostingApi.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { STATUS_MESSAGES } from '../constants/messages.js'

const emptyJobPosting = {
  companyName: '',
  positionTitle: '',
  description: '',
  jobUrl: '',
  deadline: '',
}

function normalizeForm(item) {
  return {
    companyName: item?.companyName ?? '',
    positionTitle: item?.positionTitle ?? '',
    description: item?.description ?? '',
    jobUrl: item?.jobUrl ?? '',
    deadline: item?.deadline ? String(item.deadline).slice(0, 10) : '',
  }
}

function notifyOpener(type) {
  if (window.opener && !window.opener.closed) {
    window.opener.postMessage({ type }, window.location.origin)
  }
}

function AdminJobPostingEditorPage() {
  const navigate = useNavigate()
  const { jobPostingId } = useParams()
  const isCreateMode = !jobPostingId || jobPostingId === 'new'

  usePageTitle(isCreateMode ? '채용공고 등록' : '채용공고 수정')

  const [form, setForm] = useState(emptyJobPosting)
  const [isLoading, setIsLoading] = useState(!isCreateMode)
  const [isSaving, setIsSaving] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const pageTitle = useMemo(() => (isCreateMode ? '채용공고 등록' : '채용공고 수정'), [isCreateMode])

  useEffect(() => {
    async function loadJobPosting() {
      if (isCreateMode) {
        setForm(emptyJobPosting)
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setError('')

      try {
        const item = await jobPostingApi.get(jobPostingId)
        setForm(normalizeForm(item))
      } catch (loadError) {
        setError(loadError.message)
      } finally {
        setIsLoading(false)
      }
    }

    loadJobPosting()
  }, [isCreateMode, jobPostingId])

  function updateField(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
  }

  function closeEditor() {
    if (window.opener && !window.opener.closed) {
      window.close()
      return
    }

    navigate('/admin')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setNotice('')

    try {
      if (isCreateMode) {
        await jobPostingApi.create(form)
      } else {
        await jobPostingApi.update(jobPostingId, form)
      }

      notifyOpener('jobPosting:saved')
      setNotice(isCreateMode ? '채용공고가 등록되었습니다. 창을 닫습니다.' : '채용공고가 수정되었습니다. 창을 닫습니다.')
      window.setTimeout(closeEditor, 500)
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete() {
    if (isCreateMode) {
      return
    }

    const confirmed = window.confirm('이 채용공고를 삭제하시겠습니까?')
    if (!confirmed) {
      return
    }

    setIsDeleting(true)
    setError('')
    setNotice('')

    try {
      await jobPostingApi.remove(jobPostingId)
      notifyOpener('jobPosting:deleted')
      setNotice('채용공고가 삭제되었습니다. 창을 닫습니다.')
      window.setTimeout(closeEditor, 500)
    } catch (deleteError) {
      setError(deleteError.message)
    } finally {
      setIsDeleting(false)
    }
  }

  if (isLoading) {
    return (
      <section className="workspace-page">
        <LoadingBlock label="채용공고 정보를 불러오는 중입니다." />
      </section>
    )
  }

  if (error && !isCreateMode && !form.positionTitle) {
    return (
      <section className="workspace-page">
        <ErrorBlock message={error} />
      </section>
    )
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">관리자</p>
        <h2 className="page-card__title">{pageTitle}</h2>
        <p className="page-card__description">
          채용공고를 저장하면 관리자 페이지 목록이 자동으로 갱신됩니다.
        </p>
      </div>

      <article className="panel">
        <div className="panel__header">
          <div>
            <h3 className="panel__title">{pageTitle}</h3>
            <p className="panel__subtitle">채용공고 정보와 상세 설명을 입력해 주세요.</p>
          </div>
        </div>

        <StatusMessage variant="success" message={notice} />
        <StatusMessage variant="error" message={form.positionTitle ? error : ''} />

        <form className="editor-form" onSubmit={handleSubmit}>
          <TextInput
            label="회사명"
            value={form.companyName}
            onChange={(event) => updateField('companyName', event.target.value)}
            placeholder="예: AIMentor"
            required
          />
          <TextInput
            label="채용공고 제목"
            value={form.positionTitle}
            onChange={(event) => updateField('positionTitle', event.target.value)}
            placeholder="예: 백엔드 개발자"
            required
          />
          <TextAreaField
            label="채용공고 설명"
            value={form.description}
            onChange={(event) => updateField('description', event.target.value)}
            placeholder="주요 업무, 자격 요건, 우대 사항을 입력해 주세요."
            rows={10}
            required
          />
          <TextInput
            label="채용공고 URL"
            value={form.jobUrl}
            onChange={(event) => updateField('jobUrl', event.target.value)}
            placeholder="https://example.com/jobs/backend"
          />
          <TextInput
            label="마감일"
            type="date"
            value={form.deadline}
            onChange={(event) => updateField('deadline', event.target.value)}
          />

          <div className="button-row">
            <button className="button" type="submit" disabled={isSaving || isDeleting}>
              {isSaving ? STATUS_MESSAGES.saving : isCreateMode ? '등록하기' : '저장하기'}
            </button>
            <button className="button button--secondary" type="button" onClick={closeEditor} disabled={isSaving || isDeleting}>
              창 닫기
            </button>
            {!isCreateMode ? (
              <button className="button button--danger" type="button" onClick={handleDelete} disabled={isSaving || isDeleting}>
                {isDeleting ? '삭제 중입니다.' : '삭제하기'}
              </button>
            ) : null}
          </div>
        </form>
      </article>
    </section>
  )
}

export default AdminJobPostingEditorPage
