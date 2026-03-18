import { COMMON_TEXT } from '../../constants/messages.js'

function FileInputField({ label, fileName, onChange, accept }) {
  return (
    <label className="input-field file-input-field">
      <span className="input-field__label">{label}</span>
      <input className="input-field__control" type="file" accept={accept} onChange={onChange} />
      <span className="file-input-field__name">{fileName || COMMON_TEXT.noFile}</span>
    </label>
  )
}

export default FileInputField
