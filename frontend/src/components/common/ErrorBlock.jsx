import { COMMON_TEXT, ERROR_MESSAGES } from '../../constants/messages.js'

function ErrorBlock({ message, title = COMMON_TEXT.errorTitle }) {
  if (!message) return null

  return (
    <div className="status-block status-block--error" role="alert">
      <strong className="text-title">{title}</strong>
      <span className="text-description">{message || ERROR_MESSAGES.generic}</span>
    </div>
  )
}

export default ErrorBlock
