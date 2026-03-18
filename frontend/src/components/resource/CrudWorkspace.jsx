import { useEffect, useMemo, useState } from 'react'
import ErrorBlock from '../common/ErrorBlock.jsx'
import LoadingBlock from '../common/LoadingBlock.jsx'
import StatusMessage from '../common/StatusMessage.jsx'
import TextInput from '../forms/TextInput.jsx'
import TextAreaField from '../forms/TextAreaField.jsx'
import { BUTTON_LABELS, COMMON_TEXT, STATUS_MESSAGES } from '../../constants/messages.js'
import useCrudResource from '../../hooks/useCrudResource.js'

function buildInitialState(fields, value) {
  return fields.reduce((accumulator, field) => {
    accumulator[field.name] = value?.[field.name] ?? ''
    return accumulator
  }, {})
}

function CrudWorkspace({ resourceName, eyebrow, title, description, api, createEmptyItem, fields, emptyTitle, emptyDescription, itemTitle, listItemMeta }) {
  const { items, selectedId, setSelectedId, isLoading, isSaving, error, setError, successMessage, setSuccessMessage, saveItem, deleteItem } = useCrudResource(api)
  const selectedItem = useMemo(() => items.find((item) => item.id === selectedId) ?? null, [items, selectedId])
  const [formState, setFormState] = useState(buildInitialState(fields, createEmptyItem()))

  useEffect(() => {
    setFormState(buildInitialState(fields, selectedItem ?? createEmptyItem()))
    setSuccessMessage('')
    setError('')
  }, [createEmptyItem, fields, selectedItem, setError, setSuccessMessage])

  function updateField(name, value) {
    setFormState((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    await saveItem(formState)
  }

  async function handleDelete() {
    if (!selectedId) return
    await deleteItem(selectedId)
    setFormState(buildInitialState(fields, createEmptyItem()))
  }

  function handleNewItem() {
    setSelectedId(null)
    setFormState(buildInitialState(fields, createEmptyItem()))
    setSuccessMessage('')
    setError('')
  }

  return (
    <section className="workspace-page">
      <div className="workspace-page__hero">
        <p className="page-card__eyebrow">{eyebrow}</p>
        <h2 className="page-card__title">{title}</h2>
        <p className="page-card__description">{description}</p>
      </div>
      <div className="workspace-grid">
        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{COMMON_TEXT.list}</h3>
              <p className="panel__subtitle">등록된 {resourceName}를 선택할 수 있습니다.</p>
            </div>
            <button className="button button--secondary" type="button" onClick={handleNewItem}>{BUTTON_LABELS.newItem}</button>
          </div>
          {isLoading ? <LoadingBlock label={STATUS_MESSAGES.loadingList} /> : null}
          {!isLoading && error ? <ErrorBlock message={error} /> : null}
          {!isLoading && !error ? (items.length > 0 ? <div className="resource-list">{items.map((item) => <button key={item.id} type="button" className={item.id === selectedId ? 'resource-list__item resource-list__item--active' : 'resource-list__item'} onClick={() => setSelectedId(item.id)}><strong>{itemTitle(item)}</strong><span>{listItemMeta(item)}</span></button>)}</div> : <div className="panel panel--empty"><h3 className="panel__title">{emptyTitle}</h3><p className="panel__subtitle">{emptyDescription}</p></div>) : null}
        </article>

        <article className="panel">
          <div className="panel__header">
            <div>
              <h3 className="panel__title">{selectedId ? `${resourceName} 수정` : `${resourceName} 등록`}</h3>
              <p className="panel__subtitle">필요한 정보를 입력하고 저장해 주세요.</p>
            </div>
          </div>
          <StatusMessage variant="error" message={error} />
          <StatusMessage variant="success" message={successMessage} />
          <form className="editor-form" onSubmit={handleSubmit}>
            {fields.map((field) => field.type === 'textarea' ? <TextAreaField key={field.name} label={field.label} name={field.name} value={formState[field.name]} placeholder={field.placeholder} onChange={(event) => updateField(field.name, event.target.value)} /> : <TextInput key={field.name} label={field.label} name={field.name} value={formState[field.name]} placeholder={field.placeholder} onChange={(event) => updateField(field.name, event.target.value)} />)}
            <div className="button-row">
              <button className="button" type="submit" disabled={isSaving}>{isSaving ? STATUS_MESSAGES.saving : BUTTON_LABELS.save}</button>
              <button className="button button--secondary" type="button" onClick={handleNewItem}>{BUTTON_LABELS.reset}</button>
              <button className="button button--ghost" type="button" onClick={handleDelete} disabled={!selectedId}>{BUTTON_LABELS.delete}</button>
            </div>
          </form>
        </article>
      </div>
    </section>
  )
}

export default CrudWorkspace
