import { forwardRef } from 'react'
import './Select.css'

/**
 * DailyMate Select Primitive
 * Supports states: default, focus, error, disabled
 * Supports slots: label, helperText, errorMessage, options, placeholder
 */
export const Select = forwardRef(function Select(
  {
    id,
    label,
    value,
    defaultValue,
    onChange,
    disabled = false,
    required = false,
    error,
    helperText,
    placeholder,
    options = [],
    children,
    className = '',
    ...props
  },
  ref
) {
  const hasError = Boolean(error)
  const selectId = id || (label ? `select-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)

  return (
    <div className={`dm-select-group ${hasError ? 'dm-select-group--error' : ''} ${disabled ? 'dm-select-group--disabled' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={selectId} className="dm-select-label">
          {label} {required && <span className="dm-select-required" aria-hidden="true">*</span>}
        </label>
      )}

      <div className="dm-select-wrapper">
        <select
          ref={ref}
          id={selectId}
          value={value}
          defaultValue={defaultValue}
          onChange={onChange}
          disabled={disabled}
          required={required}
          aria-invalid={hasError}
          aria-describedby={
            hasError ? `${selectId}-error` : helperText ? `${selectId}-helper` : undefined
          }
          className="dm-select"
          {...props}
        >
          {placeholder && (
            <option value="" disabled hidden>
              {placeholder}
            </option>
          )}
          {options.map((opt) => {
            const val = typeof opt === 'object' ? opt.value : opt
            const lab = typeof opt === 'object' ? opt.label : opt
            return (
              <option key={val} value={val}>
                {lab}
              </option>
            )
          })}
          {children}
        </select>
        <span className="dm-select-arrow" aria-hidden="true">
          ▼
        </span>
      </div>

      {hasError && (
        <p id={`${selectId}-error`} className="dm-select-message dm-select-message--error" role="alert">
          {error}
        </p>
      )}

      {!hasError && helperText && (
        <p id={`${selectId}-helper`} className="dm-select-message dm-select-message--helper">
          {helperText}
        </p>
      )}
    </div>
  )
})

export default Select
