import './Skeleton.css'

/**
 * DailyMate Skeleton Loading Primitive
 * Supports variants: text, circle, rectangle, card, button
 */
export function Skeleton({
  variant = 'text',
  width,
  height,
  borderRadius,
  className = '',
  style = {},
  ...props
}) {
  const customStyles = {
    ...(width && { width }),
    ...(height && { height }),
    ...(borderRadius && { borderRadius }),
    ...style,
  }

  return (
    <div
      className={`dm-skeleton dm-skeleton--${variant} ${className}`.trim()}
      style={customStyles}
      aria-hidden="true"
      {...props}
    />
  )
}

export function SkeletonCard() {
  return (
    <div className="dm-skeleton-card">
      <Skeleton variant="rectangle" height="140px" borderRadius="8px" />
      <div style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
        <Skeleton variant="text" width="60%" height="18px" />
        <Skeleton variant="text" width="90%" height="14px" />
        <Skeleton variant="text" width="40%" height="14px" />
      </div>
    </div>
  )
}

export default Skeleton
