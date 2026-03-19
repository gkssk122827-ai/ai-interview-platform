import TextAreaField from '../forms/TextAreaField.jsx'
import { BUTTON_LABELS } from '../../constants/messages.js'

function AnswerInput({
  answer,
  onChange,
  onSubmit,
  onRecordPlaceholder,
  isSubmitting,
  submitLabel = BUTTON_LABELS.nextQuestion,
  isSubmitDisabled = false,
  isRecording = false,
  isVoiceLoading = false,
  voiceError = '',
  isAutoPlayEnabled = true,
  onToggleAutoPlay,
}) {
  const recordButtonLabel = isRecording ? '녹음 종료' : '마이크 녹음'
  const recordButtonDisabled = isVoiceLoading || isSubmitting

  return (
    <article className="panel">
      <div className="panel__header">
        <div>
          <h3 className="panel__title">답변 작성</h3>
          <p className="panel__subtitle">텍스트로 먼저 답변을 정리하고, 필요하면 녹음 기능도 이어서 사용할 수 있습니다.</p>
        </div>
      </div>
      <div className="editor-form">
        <TextAreaField
          label="답변"
          rows={8}
          value={answer}
          onChange={(event) => onChange(event.target.value)}
          placeholder="상황, 행동, 결과를 중심으로 답변을 작성해 보세요."
        />
      </div>
      <div className="button-row">
        <button className="button button--secondary" type="button" onClick={onRecordPlaceholder} disabled={recordButtonDisabled}>
          {isVoiceLoading ? '음성 처리 중...' : recordButtonLabel}
        </button>
        <button className="button" type="button" onClick={onSubmit} disabled={isSubmitting || isSubmitDisabled}>{submitLabel}</button>
      </div>
      <div className="panel__subtitle">
        {isRecording ? '녹음 중입니다...' : '녹음 대기 중'}
      </div>
      {voiceError ? <div className="panel__subtitle">{voiceError}</div> : null}
      <label className="panel__subtitle">
        <input type="checkbox" checked={isAutoPlayEnabled} onChange={onToggleAutoPlay} /> 질문 음성 자동 재생
      </label>
    </article>
  )
}

export default AnswerInput
