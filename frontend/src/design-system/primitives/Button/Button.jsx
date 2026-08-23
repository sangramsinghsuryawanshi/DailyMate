import { forwardRef } from 'react'
import './Button.css'

/**
 * DailyMate Button Primitive
 * Supports variants: primary, secondary, outline, ghost, danger, success
 * Supports sizes: sm, md, lg
 * Supports states: loading, disabled, fullWidth, iconLeft, iconRight
 */
export const Button = forwardRef(function Button(
  {
    children,
    variant = 'primary',
    size = 'md',
    isLoading = false,
    disabled = false,
    fullWidth = false,
    iconLeft,
    iconRight,
    className = '',
    type = 'button',
    onClick,
    ...props
  },
  ref
) {
  const isDisabled = disabled || isLoading

  return (
    <button
      ref={ref}
      type={type}
      disabled={isDisabled}
      onClick={onClick}
      className={`dm-button dm-button--${variant} dm-button--${size} ${
        fullWidth ? 'dm-button--full-width' : ''
      } ${isLoading ? 'dm-button--loading' : ''} ${className}`.trim()}
      aria-busy={isLoading}
      {...props}
    >
      {isLoading && (
        <span className="dm-button__spinner" aria-hidden="true" />
      )}
      {!isLoading && iconLeft && (
        <span className="dm-button__icon dm-button__icon--left">{iconLeft}</span>
      )}
      <span className="dm-button__content">{children}</span>
      {!isLoading && iconRight && (
        <span className="dm-button__icon dm-button__icon--right">{iconRight}</span>
      )}
    </button>
  )
})

export default Button
