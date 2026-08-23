import './ErrorState.css'
import Button from '../../primitives/Button/Button'

/**
 * DailyMate ErrorState Pattern
 * Actionable error state with helpful message and retry action.
 */
export function ErrorState({
  title = 'Something went wrong',
  message = "We couldn't load this information. Please try again.",
  onRetry,
  retryLabel = 'Try Again',
  className = '',
}) {
  return (
    <div className={`dm-error-state ${className}`.trim()} role="alert">
      <div className="dm-error-state__icon" aria-hidden="true">
        ⚠️
      </div>
      <h3 className="dm-error-state__title">{title}</h3>
      <p className="dm-error-state__message">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          {retryLabel}
        </Button>
      )}
    </div>
  )
}

export default ErrorState
