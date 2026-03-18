import { STATUS_MESSAGES } from '../../constants/messages.js'

function LoadingBlock({ label = STATUS_MESSAGES.loadingData }) {
  return (
    <div className="status-block status-block--loading text-description" role="status" aria-live="polite">
      {label}
    </div>
  )
}

export default LoadingBlock
