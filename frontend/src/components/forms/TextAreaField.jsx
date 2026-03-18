import FieldShell from './FieldShell.jsx'

function TextAreaField({ label, hint, error, rows = 5, ...props }) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <textarea className="form-control form-control--textarea" rows={rows} {...props} />
    </FieldShell>
  )
}

export default TextAreaField
