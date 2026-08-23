import { useState } from 'react'
import './ActionProposal.css'
import Badge from '../../primitives/Badge/Badge'
import Button from '../../primitives/Button/Button'

/**
 * DailyMate ActionProposal Pattern
 * Visualizes the strict state machine: PENDING -> EXECUTED / CANCELLED / FAILED / SUPERSEDED
 * Strictly message-bound and controlled.
 */
export function ActionProposal({
  proposal,
  onConfirm,
  onCancel,
  isExecuting = false,
  className = '',
}) {
  const [localStatus, setLocalStatus] = useState(proposal?.status || 'PENDING')
  const [resultMessage, setResultMessage] = useState(proposal?.resultMessage || '')

  if (!proposal) return null

  const isPending = localStatus === 'PENDING'
  const isExecuted = localStatus === 'EXECUTED'
  const isCancelled = localStatus === 'CANCELLED'
  const isSuperseded = localStatus === 'SUPERSEDED'
  const isFailed = localStatus === 'FAILED'

  const handleConfirm = async () => {
    try {
      const res = await onConfirm?.(proposal.actionId)
      setLocalStatus('EXECUTED')
      if (res?.resultMessage) setResultMessage(res.resultMessage)
    } catch (_) {
      setLocalStatus('FAILED')
    }
  }

  const handleCancel = async () => {
    try {
      await onCancel?.(proposal.actionId)
      setLocalStatus('CANCELLED')
    } catch (_) {}
  }

  const getStatusBadge = () => {
    if (isPending) return <Badge variant="warning" dot>PENDING</Badge>
    if (isExecuted) return <Badge variant="success">EXECUTED</Badge>
    if (isCancelled) return <Badge variant="neutral">CANCELLED</Badge>
    if (isSuperseded) return <Badge variant="neutral">SUPERSEDED</Badge>
    if (isFailed) return <Badge variant="danger">FAILED</Badge>
    return <Badge variant="neutral">{localStatus}</Badge>
  }

  return (
    <div
      className={`dm-action-proposal dm-action-proposal--${localStatus.toLowerCase()} ${className}`.trim()}
      role="region"
      aria-label="Action Proposal"
    >
      <div className="dm-action-proposal__header">
        <div className="dm-action-proposal__type-row">
          <span className="dm-action-proposal__bolt" aria-hidden="true">
            ⚡
          </span>
          <strong className="dm-action-proposal__type">
            {isPending && 'Action Proposal'}
            {isExecuted && 'Action Executed'}
            {isCancelled && 'Action Cancelled'}
            {isSuperseded && 'Action Superseded'}
            {isFailed && 'Action Failed'}
          </strong>
        </div>
        {getStatusBadge()}
      </div>

      <p className="dm-action-proposal__summary">{proposal.summary}</p>

      {/* Result feedback */}
      {resultMessage && (
        <div className="dm-action-proposal__feedback dm-action-proposal__feedback--success">
          ✅ {resultMessage}
        </div>
      )}

      {/* Execution controls (Active ONLY in PENDING state) */}
      {isPending && (
        <div className="dm-action-proposal__actions">
          <Button
            variant="success"
            size="sm"
            onClick={handleConfirm}
            isLoading={isExecuting}
          >
            Confirm Action
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleCancel}
            disabled={isExecuting}
          >
            Cancel
          </Button>
        </div>
      )}
    </div>
  )
}

export default ActionProposal
