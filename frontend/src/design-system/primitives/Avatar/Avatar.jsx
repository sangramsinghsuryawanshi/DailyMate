import { useState } from 'react'
import './Avatar.css'

/**
 * DailyMate Avatar Primitive
 * Supports sizes: xs, sm, md, lg, xl
 * Supports status indicators: online, busy, away, offline
 */
export function Avatar({
  src,
  alt = '',
  name,
  size = 'md',
  status,
  className = '',
  ...props
}) {
  const [hasImageError, setHasImageError] = useState(false)

  const getInitials = (str) => {
    if (!str) return 'DM'
    const parts = str.trim().split(/\s+/)
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase()
    }
    return str.substring(0, 2).toUpperCase()
  }

  const showFallback = !src || hasImageError

  return (
    <div
      className={`dm-avatar dm-avatar--${size} ${className}`.trim()}
      aria-label={alt || name || 'User avatar'}
      role="img"
      {...props}
    >
      {!showFallback ? (
        <img
          src={src}
          alt={alt || name || ''}
          onError={() => setHasImageError(true)}
          className="dm-avatar__image"
        />
      ) : (
        <span className="dm-avatar__fallback">{getInitials(name)}</span>
      )}

      {status && (
        <span
          className={`dm-avatar__status dm-avatar__status--${status}`}
          aria-label={`Status: ${status}`}
        />
      )}
    </div>
  )
}

export default Avatar
