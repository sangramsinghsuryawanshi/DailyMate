import './Badge.css'

/**
 * DailyMate Badge Primitive
 * Supports variants: default, success, warning, danger, info, neutral, outline
 * Supports domain variants: expense, health, emergency, community, marketplace, ai
 * Supports sizes: sm, md
 */
export function Badge({
  children,
  variant = 'default',
  domain,
  size = 'md',
  dot = false,
  className = '',
  ...props
}) {
  const variantClass = domain ? `dm-badge--domain-${domain}` : `dm-badge--${variant}`

  return (
    <span
      className={`dm-badge ${variantClass} dm-badge--${size} ${className}`.trim()}
      {...props}
    >
      {dot && <span className="dm-badge__dot" aria-hidden="true" />}
      <span className="dm-badge__text">{children}</span>
    </span>
  )
}

export default Badge
