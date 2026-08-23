import './StatCard.css'

/**
 * DailyMate StatCard Pattern
 * Shows prominent numeric metrics, trend badges (+12%, -5%), domain accents, and descriptions.
 */
export function StatCard({
  label,
  value,
  trend,
  trendDirection = 'neutral', // 'up' | 'down' | 'neutral'
  trendLabel,
  icon,
  domain,
  description,
  onClick,
  className = '',
}) {
  const isClickable = Boolean(onClick)

  return (
    <div
      onClick={onClick}
      role={isClickable ? 'button' : undefined}
      tabIndex={isClickable ? 0 : undefined}
      className={`dm-stat-card ${domain ? `dm-stat-card--domain-${domain}` : ''} ${
        isClickable ? 'dm-stat-card--clickable' : ''
      } ${className}`.trim()}
    >
      <div className="dm-stat-card__top">
        <span className="dm-stat-card__label">{label}</span>
        {icon && <span className="dm-stat-card__icon" aria-hidden="true">{icon}</span>}
      </div>

      <div className="dm-stat-card__value-row">
        <span className="dm-stat-card__value">{value}</span>
        {trend && (
          <span
            className={`dm-stat-card__trend dm-stat-card__trend--${trendDirection}`}
            aria-label={`Trend: ${trend}`}
          >
            {trendDirection === 'up' && '↑ '}
            {trendDirection === 'down' && '↓ '}
            {trend}
          </span>
        )}
      </div>

      {(trendLabel || description) && (
        <div className="dm-stat-card__footer">
          {trendLabel && <span className="dm-stat-card__trend-label">{trendLabel}</span>}
          {description && <p className="dm-stat-card__description">{description}</p>}
        </div>
      )}
    </div>
  )
}

export default StatCard
