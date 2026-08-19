import React from 'react'

interface MessageBubbleProps {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: string
}

export default function MessageBubble({ role, content, timestamp }: MessageBubbleProps) {
  const className = `message-bubble ${role}`
  return (
    <div className={className} role="article" aria-label={`${role} message`}>
      <div className="message-content">{content}</div>
      {timestamp && <div className="message-timestamp">{new Date(timestamp).toLocaleTimeString()}</div>}
    </div>
  )
}
