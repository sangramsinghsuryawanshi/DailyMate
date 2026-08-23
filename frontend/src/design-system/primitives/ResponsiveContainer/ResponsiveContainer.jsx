/**
 * DailyMate ResponsiveContainer Primitive
 * Enforces standardized maximum layout widths across the application.
 * Variants: form (540px), content (780px), dashboard (1200px), wide (1400px), full (100%)
 */
export function ResponsiveContainer({
  children,
  size = 'dashboard',
  className = '',
  style = {},
  ...props
}) {
  const maxWidthMap = {
    form: '540px',
    content: '780px',
    dashboard: '1200px',
    wide: '1400px',
    full: '100%',
  }

  const containerStyles = {
    width: '100%',
    maxWidth: maxWidthMap[size] || maxWidthMap.dashboard,
    marginLeft: 'auto',
    marginRight: 'auto',
    paddingLeft: 'var(--dm-space-4, 1rem)',
    paddingRight: 'var(--dm-space-4, 1rem)',
    boxSizing: 'border-box',
    ...style,
  }

  return (
    <div
      className={`dm-responsive-container dm-responsive-container--${size} ${className}`.trim()}
      style={containerStyles}
      {...props}
    >
      {children}
    </div>
  )
}

export default ResponsiveContainer
