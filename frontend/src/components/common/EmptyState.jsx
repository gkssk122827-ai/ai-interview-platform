import { COMMON_TEXT } from '../../constants/messages.js'

function EmptyState({ title = COMMON_TEXT.emptyTitle, description, action }) {
  return (
    <div className="empty-card" role="status">
      <h4 className="text-title">{title}</h4>
      {description ? <p className="text-description">{description}</p> : null}
      {action ? <div className="button-row">{action}</div> : null}
    </div>
  )
}

export default EmptyState
