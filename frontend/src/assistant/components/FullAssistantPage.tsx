import React, { useState } from 'react'
import ConversationList from './ConversationList'
import MessageBubble from './MessageBubble'
import ContextPane from './ContextPane'
import '../../assistant/assistantStyles.css'

export interface Message {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt?: string
}

export default function FullAssistantPage() {
  const [conversations] = useState([{ id: 'c1', title: 'Appointments', updatedAt: new Date().toISOString() }])
  const [selectedId, setSelectedId] = useState<string | undefined>('c1')
  const [messages] = useState<Message[]>([
    { id: 'm1', role: 'assistant', content: 'You have 3 appointments tomorrow.' },
    { id: 'm2', role: 'user', content: 'Remind me at 9 AM' },
  ])

  return (
    <div className="assistant-page">
      <ConversationList items={conversations} selectedId={selectedId} onSelect={setSelectedId} />

      <main className="assistant-thread" aria-live="polite">
        <header className="assistant-thread-header">
          <h2>Assistant</h2>
          <div className="thread-actions">Model: <select><option>default</option></select></div>
        </header>

        <div className="messages-list">
          {messages.map((m) => (
            <MessageBubble key={m.id} role={m.role} content={m.content} timestamp={m.createdAt} />
          ))}
        </div>

        <div className="thread-composer">
          <textarea className="composer-input" placeholder="Type your message" />
          <button className="composer-send">Send</button>
        </div>
      </main>

      <ContextPane pageTitle="/dashboard" suggestedPrompts={["Show today's schedule", 'Add reminder']} />
    </div>
  )
}
