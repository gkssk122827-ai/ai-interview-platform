import { COMMON_TEXT } from '../../constants/messages.js'

function formatUpdatedAt(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('ko-KR')
}

function DocumentList({ items, selectedId, onSelect }) {
  return (
    <div className="resource-list">
      {items.map((item) => (
        <button key={item.id} type="button" className={item.id === selectedId ? 'resource-list__item resource-list__item--active' : 'resource-list__item'} onClick={() => onSelect(item.id)}>
          <strong>{item.title}</strong>
          <span>{formatUpdatedAt(item.updatedAt)}</span>
          <span>{item.originalFileName ? COMMON_TEXT.attachedFile + ': ' + item.originalFileName : COMMON_TEXT.noFile}</span>
        </button>
      ))}
    </div>
  )
}

export default DocumentList
