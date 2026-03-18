function StatusMessage({ variant = 'info', message }) {
  if (!message) {
    return null
  }

  return <div className={`status-message status-message--${variant} text-description`}>{message}</div>
}

export default StatusMessage
