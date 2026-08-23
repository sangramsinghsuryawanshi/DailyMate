import { useMemo, useState, useRef, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import AssistantActionCard from '../components/AssistantActionCard'
import {
  deleteAssistantConversation,
  getAssistantConversationMessages,
  getAssistantConversations,
  sendAssistantChat,
} from '../services/assistantApi'
import {
  Button,
  Badge,
  ResponsiveContainer,
} from '../../design-system'
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
  const [searchParams] = useSearchParams()
  const [isNewChat, setIsNewChat] = useState(false)
  const [selectedId, setSelectedId] = useState(null)
  const [promptText, setPromptText] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [activeMessage, setActiveMessage] = useState(null)
  const textareaRef = useRef(null)

  // Handle URL query parameter prefilling (e.g. from Dashboard / TopBar)
  useEffect(() => {
    const queryPrompt = searchParams.get('prompt')
    if (queryPrompt && queryPrompt.trim()) {
      setPromptText(queryPrompt.trim())
    }
  }, [searchParams])

  // Auto-expand textarea dynamically on promptText change
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      const newHeight = Math.min(Math.max(textareaRef.current.scrollHeight, 48), 240)
      textareaRef.current.style.height = `${newHeight}px`
    }
  }, [promptText])

  const { data = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['assistant-conversations'],
    queryFn: getAssistantConversations,
  })

  const conversations = data || []
  const selectedConversation = useMemo(() => {
    if (isNewChat) return null
    if (selectedId) {
      const found = conversations.find((c) => c.id === selectedId)
      if (found) return found
    }
    if (conversations.length > 0) {
      return conversations[0]
    }
    return activeMessage
  }, [conversations, selectedId, activeMessage, isNewChat])

  // Active conversation ID (or null for fresh chat)
  const currentConversationId = selectedId || selectedConversation?.id || null

  const { data: rawMessages = [] } = useQuery({
    queryKey: ['assistant-messages', currentConversationId],
    queryFn: () => getAssistantConversationMessages(currentConversationId),
    enabled: !!currentConversationId,
  })

  // Format messages fallback if no individual messages yet
  const messages = useMemo(() => {
    if (rawMessages && rawMessages.length > 0) {
      return rawMessages
    }
    const fallbackConvo = activeMessage || selectedConversation
    if (fallbackConvo && (fallbackConvo.prompt || fallbackConvo.response)) {
      const msgs = []
      if (fallbackConvo.prompt) {
        msgs.push({
          id: `${fallbackConvo.id || 'curr'}-u`,
          role: 'USER',
          content: fallbackConvo.prompt,
          createdAt: fallbackConvo.createdAt,
        })
      }
      if (fallbackConvo.response) {
        msgs.push({
          id: `${fallbackConvo.id || 'curr'}-a`,
          role: 'ASSISTANT',
          content: fallbackConvo.response,
          proposedAction: fallbackConvo.proposedAction,
          createdAt: fallbackConvo.createdAt,
        })
      }
      return msgs
    }
    return []
  }, [rawMessages, selectedConversation, activeMessage])

  const chatMutation = useMutation({
    mutationFn: ({ prompt, conversationId }) => sendAssistantChat(prompt, conversationId),
    onSuccess: (res) => {
      setIsNewChat(false)
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
      if (res?.id) {
        setSelectedId(res.id)
        queryClient.invalidateQueries({ queryKey: ['assistant-messages', res.id] })
      }
      setActiveMessage(res)
      setPromptText('')
      setErrorMsg('')
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
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete conversation.')
    },
  })

  function handleSend(textToSend) {
    const text = (typeof textToSend === 'string' ? textToSend : promptText).trim()
    if (!text) return

    setErrorMsg('')
    chatMutation.mutate({ prompt: text, conversationId: currentConversationId })
  }

  function handleStartNewChat() {
    setIsNewChat(true)
    setSelectedId(null)
    setPromptText('')
    setErrorMsg('')
    setActiveMessage(null)
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
      <div style={{ padding: 'var(--dm-space-6, 1.5rem) 0 var(--dm-space-12, 3rem) 0' }}>
        <ResponsiveContainer size="wide">
          {/* Unified Page Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Contextual Intelligence</span>
              <h1 className="dm-page-main-title">DailyMate Assistant</h1>
              <p className="dm-page-subtitle">
                Ask questions, organize your day, inspect reminders and expenses, or propose actions with confirmed execution.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/dashboard">
                <Button variant="ghost" size="md">
                  Back to dashboard
                </Button>
              </Link>
            </div>
          </div>

          {errorMsg && (
            <div className="dm-form-alert dm-form-alert--error" style={{ marginBottom: '1rem' }}>
              <strong>Error:</strong> {errorMsg}
            </div>
          )}

          <div className="assistant-container">
            {/* Left Sidebar: Conversations */}
            <aside className="assistant-sidebar">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                <h2 style={{ fontSize: '1rem', fontWeight: 800, margin: 0 }}>History</h2>
                <Button variant="secondary" size="sm" onClick={handleStartNewChat}>
                  + New chat
                </Button>
              </div>

              <div className="assistant-convo-list">
                {conversations.length === 0 && !activeMessage ? (
                  <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--dm-color-text-soft)', fontSize: '0.875rem' }}>
                    <p style={{ margin: 0 }}>No conversations yet.</p>
                  </div>
                ) : (
                  (conversations.length > 0 ? conversations : activeMessage ? [activeMessage] : []).map((c) => (
                    <div
                      key={c.id}
                      className={`assistant-convo-card ${selectedConversation?.id === c.id ? 'active' : ''}`}
                      onClick={() => {
                        setIsNewChat(false)
                        setSelectedId(c.id)
                      }}
                      role="button"
                      tabIndex={0}
                    >
                      <div className="assistant-convo-info">
                        <div className="assistant-convo-title">{c.title || 'Conversation'}</div>
                        <div className="assistant-convo-date">
                          {c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }) : 'Recent'}
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
                        style={{ background: 'none', border: 'none', color: 'var(--dm-color-text-soft)', cursor: 'pointer', padding: '0.2rem' }}
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
                  <strong style={{ fontSize: '1rem', color: 'var(--dm-color-text)' }}>
                    {selectedConversation ? selectedConversation.title : 'New conversation'}
                  </strong>
                  <div style={{ fontSize: '0.75rem', color: 'var(--dm-color-text-soft)', marginTop: '0.1rem' }}>
                    DailyMate Action Engine · Controlled tool execution with confirmation
                  </div>
                </div>
                <Badge variant="primary" size="sm">Active</Badge>
              </header>

              <div className="assistant-chat-messages">
                {messages.length === 0 && !chatMutation.isPending ? (
                  <div style={{ margin: 'auto', textAlign: 'center', maxWidth: '480px', padding: '2rem 1rem' }}>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>
                      Welcome to DailyMate Assistant 👋
                    </h3>
                    <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', lineHeight: 1.5, margin: '0 0 1.25rem 0' }}>
                      How can I help you today? Choose a prompt below or type your question.
                    </p>

                    <div className="suggested-prompt-pills" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                      {SUGGESTED_PROMPTS.map((prompt) => (
                        <button
                          key={prompt}
                          type="button"
                          className="suggested-pill"
                          onClick={() => handleSend(prompt)}
                        >
                          💡 {prompt}
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  <>
                    {/* Render Chronological Message Thread */}
                    {messages.map((m) => {
                      const isUser = (m.role || '').toUpperCase() === 'USER'
                      return (
                        <div key={m.id || Math.random()} className={`chat-bubble-row ${isUser ? 'user' : 'assistant'}`}>
                          {!isUser && <div className="chat-avatar">AI</div>}
                          <div className={`chat-bubble ${isUser ? 'user' : 'assistant'}`}>
                            <div style={{ whiteSpace: 'pre-line' }}>{m.content}</div>

                            {/* Render Proposal Card strictly for the owning message */}
                            {m.proposedAction && (
                              <AssistantActionCard
                                key={m.proposedAction.actionId || m.id}
                                proposal={m.proposedAction}
                              />
                            )}

                            {m.createdAt && (
                              <div className="chat-bubble-meta">
                                {new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </div>
                            )}
                          </div>
                        </div>
                      )
                    })}

                    {/* Thinking / Streaming Indicator */}
                    {chatMutation.isPending && (
                      <div className="chat-bubble-row assistant">
                        <div className="chat-avatar">AI</div>
                        <div className="chat-bubble assistant" style={{ fontStyle: 'italic', color: 'var(--dm-color-text-soft)' }}>
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
                    ref={textareaRef}
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
                  <Button
                    type="submit"
                    variant="primary"
                    disabled={!promptText.trim() || chatMutation.isPending}
                    isLoading={chatMutation.isPending}
                  >
                    {chatMutation.isPending ? 'Thinking…' : 'Send'}
                  </Button>
                </form>
              </div>
            </main>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
