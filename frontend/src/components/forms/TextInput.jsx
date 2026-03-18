import FieldShell from './FieldShell.jsx'

function TextInput({ label, hint, error, ...props }) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <input className="form-control" {...props} />
    </FieldShell>
  )
}

export default TextInput
