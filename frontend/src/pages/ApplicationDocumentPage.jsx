import { useEffect, useMemo, useState } from 'react'
import EmptyState from '../components/common/EmptyState.jsx'
import ErrorBlock from '../components/common/ErrorBlock.jsx'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import StatusMessage from '../components/common/StatusMessage.jsx'
import FileInputField from '../components/forms/FileInputField.jsx'
import TextAreaField from '../components/forms/TextAreaField.jsx'
import TextInput from '../components/forms/TextInput.jsx'
import DocumentList from '../components/profile/DocumentList.jsx'
import { BUTTON_LABELS, COMMON_TEXT, EMPTY_MESSAGES, ERROR_MESSAGES, STATUS_MESSAGES } from '../constants/messages.js'
import usePageTitle from '../hooks/usePageTitle.js'
import profileDocumentApi from '../api/profileDocumentApi.js'

const emptyForm = { title: '', resumeText: '', coverLetterText: '', file: null }

function toFormData(document) {
  return {
    title: document?.title ?? '',
    resumeText: document?.resumeText ?? '',
    coverLetterText: document?.coverLetterText ?? '',
    file: null,
  }
}

function ApplicationDocumentPage() {
  usePageTitle('지원자료 관리')
  const [documents, setDocuments] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [formData, setFormData] = useState(emptyForm)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const selectedDocument = useMemo(() => documents.find((item) => item.id === selectedId) ?? null, [documents, selectedId])

  async function loadDocuments() {
    setIsLoading(true)
    setError('')

    try {
      const items = await profileDocumentApi.list()
      setDocuments(items)
      if (items.length > 0) {
        setSelectedId(items[0].id)
        setFormData(toFormData(items[0]))
      } else {
        setSelectedId(null)
        setFormData(emptyForm)
      }
    } catch (loadError) {
      setError(loadError.message || ERROR_MESSAGES.loadList)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadDocuments()
  }, [])

  function handleSelect(id) {
    const item = documents.find((candidate) => candidate.id === id)
    if (!item) return
    setSelectedId(id)
    setFormData(toFormData(item))
    setSuccessMessage('')
    setError('')
  }

  function handleStartCreate() {
    setSelectedId(null)
    setFormData(emptyForm)
    setSuccessMessage('')
    setError('')
  }

  function handleFieldChange(name, value) {
    setFormData((current) => ({ ...current, [name]: value }))
  }

  function handleFileChange(event) {
    const nextFile = event.target.files?.[0] ?? null
    setFormData((current) => ({ ...current, file: nextFile }))
  }

  async function handleSave() {
    if (!formData.title.trim()) {
      setError('제목을 입력해 주세요.')
      return
    }

    if (!formData.resumeText.trim() && !formData.coverLetterText.trim() && !formData.file && !selectedDocument?.originalFileName) {
      setError('이력서, 자기소개서, 첨부 파일 중 하나 이상을 입력해 주세요.')
      return
    }

    setIsSaving(true)
    setError('')
    setSuccessMessage('')

    try {
      const payload = {
        title: formData.title.trim(),
        resumeText: formData.resumeText.trim(),
        coverLetterText: formData.coverLetterText.trim(),
        file: formData.file,
      }

      const saved = selectedId ? await profileDocumentApi.update(selectedId, payload) : await profileDocumentApi.create(payload)

      if (selectedId) {
        setDocuments((current) => current.map((item) => (item.id === selectedId ? saved : item)))
        setSuccessMessage('지원자료를 수정했습니다.')
      } else {
        setDocuments((current) => [saved, ...current])
        setSuccessMessage('지원자료를 저장했습니다.')
      }

      setSelectedId(saved.id)
      setFormData(toFormData(saved))
    } catch (saveError) {
      setError(saveError.message || ERROR_MESSAGES.saveItem)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete() {
    if (!selectedId) {
      handleStartCreate()
      return
    }

    setIsSaving(true)
    setError('')
    setSuccessMessage('')

    try {
      await profileDocumentApi.remove(selectedId)
      const nextItems = documents.filter((item) => item.id !== selectedId)
      setDocuments(nextItems)
      if (nextItems.length > 0) {
        setSelectedId(nextItems[0].id)
        setFormData(toFormData(nextItems[0]))
      } else {
        setSelectedId(null)
        setFormData(emptyForm)
      }
      setSuccessMessage('지원자료를 삭제했습니다.')
    } catch (deleteError) {
      setError(deleteError.message || ERROR_MESSAGES.deleteItem)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">지원자료</p>
        <h2 className="page-card__title">이력서와 자기소개서를 한 곳에서 관리해 보세요.</h2>
        <p className="page-card__description">제목, 이력서 텍스트, 자기소개서 텍스트, 첨부 파일을 하나의 지원자료로 저장할 수 있습니다.</p>
      </div>

      <StatusMessage variant="error" message={error} />
      <StatusMessage variant="success" message={successMessage} />

      <div className="workspace-grid">
        <aside className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{COMMON_TEXT.list}</h3>
              <p className="panel__subtitle">{COMMON_TEXT.total} {documents.length}건</p>
            </div>
            <button className="button button--ghost" type="button" onClick={handleStartCreate}>{BUTTON_LABELS.newDocument}</button>
          </div>

          {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingDocuments} /> : null}
          {!isLoading && error ? <ErrorBlock message={error} /> : null}
          {!isLoading && !error && documents.length === 0 ? <EmptyState title={EMPTY_MESSAGES.documents.title} description={EMPTY_MESSAGES.documents.description} /> : null}
          {!isLoading && !error && documents.length > 0 ? <DocumentList items={documents} selectedId={selectedId} onSelect={handleSelect} /> : null}
        </aside>

        <section className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{selectedId ? '지원자료 수정' : '지원자료 등록'}</h3>
              <p className="panel__subtitle">저장된 지원자료는 모의면접 설정에서 바로 선택할 수 있습니다.</p>
            </div>
          </div>

          <div className="editor-form">
            <TextInput label="제목" value={formData.title} onChange={(event) => handleFieldChange('title', event.target.value)} placeholder="백엔드 개발자 지원자료" />
            <TextAreaField label="이력서 텍스트" rows={8} value={formData.resumeText} onChange={(event) => handleFieldChange('resumeText', event.target.value)} placeholder="경력, 프로젝트, 기술 스택, 성과를 정리해 주세요." />
            <TextAreaField label="자기소개서 텍스트" rows={8} value={formData.coverLetterText} onChange={(event) => handleFieldChange('coverLetterText', event.target.value)} placeholder="지원 동기, 강점, 문제 해결 경험을 작성해 주세요." />
            <FileInputField label="첨부 파일" fileName={formData.file?.name || selectedDocument?.originalFileName} onChange={handleFileChange} />
            {selectedDocument?.fileUrl ? <div className="document-file-meta"><strong>{COMMON_TEXT.savedFile}</strong><span>{selectedDocument.originalFileName}</span></div> : null}
          </div>

          <div className="button-row">
            <button className="button" type="button" onClick={handleSave} disabled={isSaving || isLoading}>{isSaving ? STATUS_MESSAGES.saving : BUTTON_LABELS.save}</button>
            <button className="button button--secondary" type="button" onClick={handleStartCreate} disabled={isSaving}>{BUTTON_LABELS.reset}</button>
            <button className="button button--danger" type="button" onClick={handleDelete} disabled={isSaving}>{BUTTON_LABELS.delete}</button>
          </div>
        </section>
      </div>
    </section>
  )
}

export default ApplicationDocumentPage
