import './TrustBadge.css'

/**
 * DailyMate TrustBadge Pattern
 * Signals clear operational trust states: saved, verified, urgent, private, encrypted.
 */
export function TrustBadge({
  type = 'saved', // 'saved' | 'verified' | 'urgent' | 'private' | 'encrypted' | 'executed'
  label,
  className = '',
}) {
  const configMap = {
    saved: { icon: '✓', text: 'Saved', styleClass: 'dm-trust-badge--saved' },
    verified: { icon: '🛡️', text: 'Verified Provider', styleClass: 'dm-trust-badge--verified' },
    urgent: { icon: '🚨', text: 'Urgent Request', styleClass: 'dm-trust-badge--urgent' },
    private: { icon: '🔒', text: 'Private & Encrypted', styleClass: 'dm-trust-badge--private' },
    encrypted: { icon: '🔐', text: 'End-to-End Encrypted', styleClass: 'dm-trust-badge--encrypted' },
    executed: { icon: '✅', text: 'Executed', styleClass: 'dm-trust-badge--executed' },
  }

  const activeConfig = configMap[type] || configMap.saved

  return (
    <span className={`dm-trust-badge ${activeConfig.styleClass} ${className}`.trim()}>
      <span className="dm-trust-badge__icon" aria-hidden="true">
        {activeConfig.icon}
      </span>
      <span className="dm-trust-badge__label">{label || activeConfig.text}</span>
    </span>
  )
}

export default TrustBadge
