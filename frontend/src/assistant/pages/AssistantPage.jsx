import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import MainLayout from '../../layouts/MainLayout'
import AssistantActionCard from '../components/AssistantActionCard'
import {
  deleteAssistantConversation,
  getAssistantConversations,
  sendAssistantChat,
} from '../services/assistantApi'
import '../assistantStyles.css'

const SUGGESTED_PROMPTS = [
  'What medicines do I have scheduled today?',
  'Generate my monthly DailyMate life report',
  'Add expense for my lunch, Khichadi and amount is 50',
  'Add electrician Rahul with phone 9876543210 in Pune',
  'Mark all notifications as read',
  'Who can I call in an emergency?',
]

export default function AssistantPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState(null)
  const [promptText, setPromptText] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [activeMessage, setActiveMessage] = useState(null)
  const [activeProposal, setActiveProposal] = useState(null)

  const { data = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['assistant-conversations'],
    queryFn: getAssistantConversations,
  })

  const conversations = data || []
  const selectedConversation = useMemo(() => {
    if (selectedId) {
      const found = conversations.find((c) => c.id === selectedId)
      if (found) return found
    }
    if (conversations.length > 0) {
      return conversations[0]
    }
    return activeMessage
  }, [conversations, selectedId, activeMessage])

  const chatMutation = useMutation({
    mutationFn: ({ prompt, conversationId }) => sendAssistantChat(prompt, conversationId),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
      setPromptText('')
      setErrorMsg('')
      if (res?.id) {
        setSelectedId(res.id)
        setActiveMessage(res)
      }
      if (res?.proposedAction) {
        setActiveProposal(res.proposedAction)
      } else {
        setActiveProposal(null)
      }
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to send prompt to assistant.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => deleteAssistantConversation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
      setSelectedId(null)
      setActiveMessage(null)
      setActiveProposal(null)
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete conversation.')
    },
  })

  function handleSend(textToSend) {
    const text = (typeof textToSend === 'string' ? textToSend : promptText).trim()
    if (!text) return

    setErrorMsg('')
    setActiveProposal(null)
    chatMutation.mutate({ prompt: text, conversationId: selectedConversation?.id || null })
  }

  function handleStartNewChat() {
    setSelectedId(null)
    setPromptText('')
    setErrorMsg('')
    setActiveMessage(null)
    setActiveProposal(null)
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading assistant…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Assistant is currently unavailable</h1>
          <p className="muted">Unable to reach the assistant service. Please check your connection.</p>
          <div style={{ marginTop: '1rem' }}>
            <Button onClick={() => refetch()}>Try again</Button>
          </div>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Contextual Intelligence</p>
          <h1>DailyMate Assistant</h1>
          <p className="subtle-text">
            Ask questions, organize your day, inspect reminders and expenses, or propose actions with confirmed execution.
          </p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {errorMsg && (
        <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.75rem 1rem', background: '#fee2e2', borderRadius: '6px', border: '1px solid #fca5a5' }}>
          <strong>Error:</strong> {errorMsg}
        </div>
      )}

      <div className="assistant-container">
        {/* Left Sidebar: Conversations */}
        <aside className="assistant-sidebar panel">
          <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2>History</h2>
            <Button variant="secondary" onClick={handleStartNewChat}>
              + New chat
            </Button>
          </div>

          <div className="assistant-convo-list">
            {conversations.length === 0 && !activeMessage ? (
              <div className="empty-state" style={{ padding: '1rem', textAlign: 'center' }}>
                <p className="muted" style={{ fontSize: '0.9rem' }}>No conversations yet.</p>
              </div>
            ) : (
              (conversations.length > 0 ? conversations : activeMessage ? [activeMessage] : []).map((c) => (
                <div
                  key={c.id}
                  className={`assistant-convo-card ${selectedConversation?.id === c.id ? 'active' : ''}`}
                  onClick={() => {
                    setSelectedId(c.id)
                    setActiveProposal(null)
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="assistant-convo-info">
                    <div className="assistant-convo-title">{c.title || 'Conversation'}</div>
                    <div className="assistant-convo-date">
                      {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'Recent'}
                    </div>
                  </div>
                  <button
                    type="button"
                    title="Delete conversation"
                    aria-label={`Delete ${c.title || 'conversation'}`}
                    onClick={(e) => {
                      e.stopPropagation()
                      deleteMutation.mutate(c.id)
                    }}
                    style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '0.2rem' }}
                  >
                    ✕
                  </button>
                </div>
              ))
            )}
          </div>
        </aside>

        {/* Right Panel: Chat Thread */}
        <main className="assistant-chat-panel" aria-live="polite">
          <header className="assistant-chat-header">
            <div>
              <strong style={{ fontSize: '1.05rem' }}>
                {selectedConversation ? selectedConversation.title : 'New conversation'}
              </strong>
              <div className="small-muted">DailyMate Action Engine · Controlled tool execution with confirmation</div>
            </div>
            <span className="status-pill open">Active</span>
          </header>

          <div className="assistant-chat-messages">
            {!selectedConversation && !chatMutation.isPending ? (
              <div className="empty-state" style={{ margin: 'auto', textAlign: 'center', maxWidth: '480px' }}>
                <h3>Welcome to DailyMate Assistant 👋</h3>
                <p className="muted">
                  How can I help you today? Choose a prompt below or type your question.
                </p>

                <div className="suggested-prompt-pills" style={{ marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {SUGGESTED_PROMPTS.map((prompt) => (
                    <button
                      key={prompt}
                      type="button"
                      className="suggested-pill"
                      onClick={() => handleSend(prompt)}
                      style={{ textAlign: 'left', padding: '0.6rem 1rem', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', cursor: 'pointer' }}
                    >
                      💡 {prompt}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <>
                {/* User Prompt */}
                {selectedConversation && (
                  <>
                    <div className="chat-bubble-row user">
                      <div className="chat-bubble user">
                        <div>{selectedConversation.prompt}</div>
                        {selectedConversation.createdAt && (
                          <div className="chat-bubble-meta">
                            {new Date(selectedConversation.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Assistant Response */}
                    <div className="chat-bubble-row assistant">
                      <div className="chat-avatar">AI</div>
                      <div className="chat-bubble assistant">
                        <div style={{ whiteSpace: 'pre-line' }}>{selectedConversation.response || 'Generating response…'}</div>

                        {/* Render Active Proposal Card if available */}
                        {activeProposal && <AssistantActionCard proposal={activeProposal} />}

                        {selectedConversation.createdAt && (
                          <div className="chat-bubble-meta" style={{ color: 'var(--color-text-soft)', marginTop: '0.5rem' }}>
                            {new Date(selectedConversation.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        )}
                      </div>
                    </div>
                  </>
                )}

                {/* Thinking / Streaming Indicator */}
                {chatMutation.isPending && (
                  <div className="chat-bubble-row assistant">
                    <div className="chat-avatar">AI</div>
                    <div className="chat-bubble assistant" style={{ fontStyle: 'italic', color: '#64748b' }}>
                      Processing request and checking action proposals…
                    </div>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Chat Composer */}
          <div className="assistant-composer-area">
            <form
              className="assistant-composer-form"
              onSubmit={(e) => {
                e.preventDefault()
                handleSend()
              }}
            >
              <textarea
                placeholder="Ask DailyMate or type 'Add expense 500 for Groceries'..."
                value={promptText}
                onChange={(e) => setPromptText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    handleSend()
                  }
                }}
                rows={1}
                aria-label="Ask DailyMate a question"
              />
              <Button type="submit" disabled={!promptText.trim() || chatMutation.isPending}>
                {chatMutation.isPending ? 'Thinking…' : 'Send'}
              </Button>
            </form>
          </div>
        </main>
      </div>
    </MainLayout>
  )
}
