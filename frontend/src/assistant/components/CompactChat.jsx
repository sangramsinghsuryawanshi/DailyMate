import React, { useEffect, useRef, useState } from 'react'
import '../../assistant/assistantStyles.css'
import MessageBubble from './MessageBubble'

export default function CompactChat({ conversationId, initialMessages = [], onOpenFull }) {
  const [messages, setMessages] = useState(initialMessages)
  const [input, setInput] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const containerRef = useRef(null)

  useEffect(() => {
    // Placeholder: wire to React Query / SSE in implementation
  }, [conversationId])

  function send() {
    if (!input.trim()) return
    const msg = { id: Date.now().toString(), role: 'user', content: input.trim(), createdAt: new Date().toISOString() }
    setMessages((m) => [...m, msg])
    setInput('')
    setIsStreaming(true)
    // TODO: call sendMessage mutation + subscribe to SSE for streaming tokens
    setTimeout(() => setIsStreaming(false), 600) // placeholder streaming simulation
  }

  return (
    <div className="compact-chat" role="dialog" aria-label="AI assistant compact chat" ref={containerRef}>
      <div className="compact-header">
        <div className="assistant-avatar" aria-hidden>AI</div>
        <div className="assistant-title">Assistant</div>
        <button className="open-full" onClick={onOpenFull} aria-label="Open full assistant">⇱</button>
      </div>

      <div className="compact-body" aria-live="polite">
        {messages.slice(-6).map((m) => (
          <MessageBubble key={m.id} role={m.role} content={m.content} />
        ))}

        {isStreaming && <div className="streaming-indicator">Streaming…</div>}
      </div>

      <div className="compact-composer">
        <textarea
          className="composer-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask me anything — press Enter to send"
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              send()
            }
          }}
          aria-label="Assistant message input"
        />
        <button className="composer-send" onClick={send} aria-label="Send message">Send</button>
      </div>
    </div>
  )
}
