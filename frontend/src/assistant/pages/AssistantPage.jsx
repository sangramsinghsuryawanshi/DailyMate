import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import ConversationList from '../components/ConversationList'
import ContextPane from '../components/ContextPane'
import MessageBubble from '../components/MessageBubble'
import { createAssistantConversation, deleteAssistantConversation, getAssistantConversations, updateAssistantConversation } from '../services/assistantApi'

export default function AssistantPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState(null)
  const [composerText, setComposerText] = useState('')

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['assistant-conversations'],
    queryFn: getAssistantConversations,
  })

  const conversations = data || []
  const selectedConversation = conversations.find((c) => c.id === selectedId) || conversations[0] || null

  const createMutation = useMutation({
    mutationFn: (payload) => createAssistantConversation(payload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] })
      setComposerText('')
      setSelectedId(res.id)
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateAssistantConversation(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => deleteAssistantConversation(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assistant-conversations'] }),
  })

  function handleSend() {
    if (!composerText.trim()) return
    const payload = {
      title: composerText.trim().slice(0, 60),
      prompt: composerText.trim(),
      response: '', // backend will populate once assistant processing is implemented
    }
    createMutation.mutate(payload)
  }

  function handleUpdateResponse(newResponse) {
    if (!selectedConversation) return
    updateMutation.mutate({ id: selectedConversation.id, payload: { ...selectedConversation, response: newResponse } })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading assistant…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Assistant unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Smart support</p>
          <h1>DailyMate AI assistant</h1>
          <p className="subtle-text">Capture quick prompts and interact with the assistant. Use the composer to start a new conversation.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <div className="assistant-page">
        <ConversationList items={conversations.map((c) => ({ id: c.id, title: c.title, updatedAt: c.updatedAt }))} selectedId={selectedConversation?.id} onSelect={setSelectedId} />

        <main className="assistant-thread" aria-live="polite">
          <header className="assistant-thread-header">
            <h2>{selectedConversation ? selectedConversation.title : 'New conversation'}</h2>
            <div className="thread-actions">Model: <select><option>default</option></select></div>
          </header>

          <div className="messages-list">
            {selectedConversation ? (
              <>
                <MessageBubble role="user" content={selectedConversation.prompt || ''} timestamp={selectedConversation.createdAt} />
                <MessageBubble role="assistant" content={selectedConversation.response || 'Assistant response pending...'} timestamp={selectedConversation.updatedAt} />
              </>
            ) : (
              <div className="empty-state"><p>No conversation selected.</p></div>
            )}
          </div>

          <div className="thread-composer">
            <textarea className="composer-input" placeholder="Type your prompt" value={composerText} onChange={(e) => setComposerText(e.target.value)} />
            <div style={{ display: 'flex', gap: '8px' }}>
              <button className="composer-send" onClick={handleSend} disabled={createMutation.isPending}>{createMutation.isPending ? 'Sending…' : 'Send'}</button>
              {selectedConversation && <button className="composer-send" onClick={() => handleUpdateResponse(prompt('Enter assistant response to save:') || '')}>Save response</button>}
              {selectedConversation && <button className="composer-send" onClick={() => deleteMutation.mutate(selectedConversation.id)}>Delete</button>}
            </div>
          </div>
        </main>

        <ContextPane pageTitle="/dashboard" suggestedPrompts={["Show today's schedule", 'Add reminder']} />
      </div>
    </MainLayout>
  )
}
