import FieldShell from './FieldShell.jsx'

function SelectField({ label, hint, error, options, ...props }) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <select className="form-control" {...props}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </FieldShell>
  )
}

export default SelectField
