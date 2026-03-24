import { useEffect, useMemo, useRef, useState } from 'react'
import FieldShell from './FieldShell.jsx'

function SelectField({ label, hint, error, options, value, onChange, disabled = false, name }) {
  const [isOpen, setIsOpen] = useState(false)
  const rootRef = useRef(null)

  const selectedOption = useMemo(
    () => options.find((option) => String(option.value) === String(value)) ?? options[0] ?? null,
    [options, value],
  )

  useEffect(() => {
    function handleOutsideClick(event) {
      if (!rootRef.current?.contains(event.target)) {
        setIsOpen(false)
      }
    }

    function handleEscape(event) {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  function handleSelect(nextValue) {
    setIsOpen(false)

    if (!onChange) {
      return
    }

    onChange({
      target: {
        value: nextValue,
        name: name ?? '',
      },
    })
  }

  return (
    <FieldShell label={label} hint={hint} error={error}>
      <div className="select-field" ref={rootRef}>
        <button
          type="button"
          className="form-control select-field__trigger"
          onClick={() => setIsOpen((current) => !current)}
          disabled={disabled}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
        >
          <span>{selectedOption?.label ?? ''}</span>
          <span className="select-field__arrow">{isOpen ? '▲' : '▼'}</span>
        </button>

        {isOpen ? (
          <div className="select-field__menu" role="listbox">
            {options.map((option) => {
              const isSelected = String(option.value) === String(value)
              return (
                <button
                  key={option.value}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  className={isSelected ? 'select-field__option select-field__option--active' : 'select-field__option'}
                  onClick={() => handleSelect(option.value)}
                >
                  {option.label}
                </button>
              )
            })}
          </div>
        ) : null}
      </div>
    </FieldShell>
  )
}

export default SelectField
