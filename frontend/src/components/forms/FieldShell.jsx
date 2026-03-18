function FieldShell({ label, hint, error, children }) {
  return (
    <label className="form-field">
      <span className="form-field__label">{label}</span>
      {children}
      {hint ? <span className="form-field__hint">{hint}</span> : null}
      {error ? <span className="form-field__error">{error}</span> : null}
    </label>
  )
}

export default FieldShell
