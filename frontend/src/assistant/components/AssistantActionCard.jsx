import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import Button from '../../components/Button'
import { confirmAssistantAction, cancelAssistantAction } from '../services/assistantApi'

export default function AssistantActionCard({ proposal }) {
  const queryClient = useQueryClient()
  const isExpired = proposal?.expiresAt && new Date(proposal.expiresAt) < new Date()
  const initialStatus = isExpired ? 'EXPIRED' : (proposal?.status || 'PENDING')

  const [actionState, setActionState] = useState(initialStatus)
  const [resultMessage, setResultMessage] = useState('')
  const [errorMsg, setErrorMsg] = useState('')

  // Stable idempotency key tied to the proposal instance
  const getIdempotencyKey = () => {
    if (!proposal.__idempotencyKey) {
      proposal.__idempotencyKey = `idemp-${proposal.actionId}`
    }
    return proposal.__idempotencyKey
  }

  const confirmMutation = useMutation({
    mutationFn: () => confirmAssistantAction(proposal.actionId, getIdempotencyKey()),
    onSuccess: (res) => {
      setActionState('EXECUTED')
      setResultMessage(res.resultMessage || 'Action executed successfully.')
      setErrorMsg('')

      // Invalidate relevant domain query caches across all 8 modules
      if (proposal.actionType === 'RECORD_EXPENSE' || proposal.actionType === 'DELETE_EXPENSE') {
        queryClient.invalidateQueries({ queryKey: ['expenses'] })
      } else if (proposal.actionType === 'CREATE_REMINDER' || proposal.actionType === 'DELETE_REMINDER') {
        queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      } else if (proposal.actionType === 'REGISTER_PROVIDER') {
        queryClient.invalidateQueries({ queryKey: ['marketplace-providers'] })
        queryClient.invalidateQueries({ queryKey: ['providers'] })
      } else if (proposal.actionType === 'MARK_NOTIFICATIONS_READ' || proposal.actionType === 'CREATE_NOTIFICATION') {
        queryClient.invalidateQueries({ queryKey: ['notifications'] })
      } else if (proposal.actionType === 'CREATE_BLOOD_REQUEST' || proposal.actionType === 'DELETE_BLOOD_REQUEST') {
        queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      } else if (proposal.actionType === 'CREATE_ICE_CONTACT' || proposal.actionType === 'DELETE_ICE_CONTACT') {
        queryClient.invalidateQueries({ queryKey: ['emergency-contacts'] })
        queryClient.invalidateQueries({ queryKey: ['emergency-my-contacts'] })
      } else if (proposal.actionType === 'CREATE_EVENT') {
        queryClient.invalidateQueries({ queryKey: ['local-events'] })
      } else if (proposal.actionType === 'CREATE_JOB') {
        queryClient.invalidateQueries({ queryKey: ['job-posts'] })
      }
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to execute action.')
    },
  })

  const cancelMutation = useMutation({
    mutationFn: () => cancelAssistantAction(proposal.actionId),
    onSuccess: () => {
      setActionState('CANCELLED')
      setErrorMsg('')
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to cancel action.')
    },
  })

  if (!proposal) return null

  const getHeaderTitle = () => {
    switch (actionState) {
      case 'EXECUTED':
        return '⚡ Action Executed'
      case 'CANCELLED':
        return '⚡ Action Cancelled'
      case 'EXPIRED':
        return '⚡ Action Expired'
      case 'FAILED':
        return '⚡ Action Failed'
      case 'PROCESSING':
        return '⚡ Executing Action…'
      case 'PENDING':
      default:
        return '⚡ Action Proposal'
    }
  }

  return (
    <div
      className="assistant-action-card"
      style={{
        marginTop: '0.85rem',
        padding: '1rem',
        background: '#ffffff',
        border: '1px solid #cbd5e1',
        borderRadius: '10px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
        <strong style={{ fontSize: '0.95rem', color: '#1e293b' }}>{getHeaderTitle()}</strong>
        <span
          className={`status-pill ${
            actionState === 'EXECUTED'
              ? 'closed'
              : actionState === 'CANCELLED' || actionState === 'EXPIRED' || actionState === 'FAILED'
              ? 'rejected'
              : 'open'
          }`}
          style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
        >
          {actionState}
        </span>
      </div>

      <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.9rem', color: '#334155', fontWeight: 500 }}>
        {proposal.summary}
      </p>

      {errorMsg && (
        <div style={{ color: '#ef4444', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
          <strong>Error:</strong> {errorMsg}
        </div>
      )}

      {resultMessage && (
        <div style={{ color: '#16a34a', fontSize: '0.85rem', marginBottom: '0.5rem', fontWeight: 500 }}>
          ✅ {resultMessage}
        </div>
      )}

      {actionState === 'PENDING' && !isExpired && (
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
          <Button
            type="button"
            onClick={() => confirmMutation.mutate()}
            disabled={confirmMutation.isPending || cancelMutation.isPending}
            style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}
          >
            {confirmMutation.isPending ? 'Executing…' : 'Confirm Action'}
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => cancelMutation.mutate()}
            disabled={confirmMutation.isPending || cancelMutation.isPending}
            style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}
          >
            Cancel
          </Button>
        </div>
      )}
    </div>
  )
}
