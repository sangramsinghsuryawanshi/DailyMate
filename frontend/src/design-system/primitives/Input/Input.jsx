import { forwardRef } from 'react'
import './Input.css'

/**
 * DailyMate Input Primitive
 * Supports states: default, focus, filled, error, disabled, readOnly, loading
 * Supports slots: label, helperText, errorMessage, iconLeft, iconRight
 */
export const Input = forwardRef(function Input(
  {
    id,
    name,
    label,
    type = 'text',
    value,
    defaultValue,
    onChange,
    placeholder,
    disabled = false,
    readOnly = false,
    required = false,
    error,
    helperText,
    iconLeft,
    iconRight,
    className = '',
    ...props
  },
  ref
) {
  const hasError = Boolean(error)
  const inputId = id || (label ? `input-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined)

  return (
    <div className={`dm-input-group ${hasError ? 'dm-input-group--error' : ''} ${disabled ? 'dm-input-group--disabled' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={inputId} className="dm-input-label">
          {label}
        </label>
      )}

      <div className="dm-input-wrapper">
        {iconLeft && <span className="dm-input-icon dm-input-icon--left">{iconLeft}</span>}
        <input
          ref={ref}
          id={inputId}
          name={name}
          aria-label={props['aria-label']}
          type={type}
          value={value}
          defaultValue={defaultValue}
          onChange={onChange}
          placeholder={placeholder}
          disabled={disabled}
          readOnly={readOnly}
          required={required}
          aria-invalid={hasError}
          aria-describedby={
            hasError ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
          }
          className={`dm-input ${iconLeft ? 'dm-input--has-icon-left' : ''} ${
            iconRight ? 'dm-input--has-icon-right' : ''
          }`}
          {...props}
        />
        {iconRight && <span className="dm-input-icon dm-input-icon--right">{iconRight}</span>}
      </div>

      {hasError && (
        <p id={`${inputId}-error`} className="dm-input-message dm-input-message--error" role="alert">
          {error}
        </p>
      )}

      {!hasError && helperText && (
        <p id={`${inputId}-helper`} className="dm-input-message dm-input-message--helper">
          {helperText}
        </p>
      )}
    </div>
  )
})

export default Input
