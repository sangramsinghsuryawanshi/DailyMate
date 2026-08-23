import { forwardRef } from 'react'
import './Card.css'

/**
 * DailyMate Card Primitive
 * Supports variants: default, elevated, flat, interactive, selected
 * Supports states: loading, disabled, error
 */
export const Card = forwardRef(function Card(
  {
    children,
    variant = 'default',
    interactive = false,
    selected = false,
    disabled = false,
    isLoading = false,
    hasError = false,
    className = '',
    onClick,
    ...props
  },
  ref
) {
  const isClickable = interactive || Boolean(onClick)

  return (
    <div
      ref={ref}
      onClick={disabled || isLoading ? undefined : onClick}
      role={isClickable ? 'button' : undefined}
      tabIndex={isClickable && !disabled ? 0 : undefined}
      aria-selected={selected}
      aria-disabled={disabled}
      aria-busy={isLoading}
      className={`dm-card dm-card--${variant} ${isClickable ? 'dm-card--interactive' : ''} ${
        selected ? 'dm-card--selected' : ''
      } ${disabled ? 'dm-card--disabled' : ''} ${isLoading ? 'dm-card--loading' : ''} ${
        hasError ? 'dm-card--error' : ''
      } ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  )
})

export function CardHeader({ children, className = '', ...props }) {
  return (
    <div className={`dm-card__header ${className}`.trim()} {...props}>
      {children}
    </div>
  )
}

export function CardTitle({ children, className = '', as = 'h3', ...props }) {
  const Component = as
  return (
    <Component className={`dm-card__title ${className}`.trim()} {...props}>
      {children}
    </Component>
  )
}

export function CardDescription({ children, className = '', ...props }) {
  return (
    <p className={`dm-card__description ${className}`.trim()} {...props}>
      {children}
    </p>
  )
}

export function CardContent({ children, className = '', ...props }) {
  return (
    <div className={`dm-card__content ${className}`.trim()} {...props}>
      {children}
    </div>
  )
}

export function CardFooter({ children, className = '', ...props }) {
  return (
    <div className={`dm-card__footer ${className}`.trim()} {...props}>
      {children}
    </div>
  )
}

export default Card
