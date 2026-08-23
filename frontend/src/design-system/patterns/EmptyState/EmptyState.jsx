import './EmptyState.css'
import Button from '../../primitives/Button/Button'

/**
 * DailyMate EmptyState Pattern
 * Explains: What is this? Why is it empty? What can I do next?
 */
export function EmptyState({
  icon = '📂',
  title = 'No items yet',
  description = 'Get started by creating your first entry.',
  primaryAction,
  secondaryAction,
  className = '',
}) {
  return (
    <div className={`dm-empty-state ${className}`.trim()}>
      <div className="dm-empty-state__icon" aria-hidden="true">
        {icon}
      </div>
      <h3 className="dm-empty-state__title">{title}</h3>
      <p className="dm-empty-state__description">{description}</p>

      {(primaryAction || secondaryAction) && (
        <div className="dm-empty-state__actions">
          {primaryAction && (
            <Button
              variant="primary"
              onClick={primaryAction.onClick}
              iconLeft={primaryAction.icon}
            >
              {primaryAction.label}
            </Button>
          )}
          {secondaryAction && (
            <Button
              variant="outline"
              onClick={secondaryAction.onClick}
              iconLeft={secondaryAction.icon}
            >
              {secondaryAction.label}
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

export default EmptyState
