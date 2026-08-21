import React from 'react'

export default function MessageBubble({ role, content, timestamp }) {
  const className = `message-bubble ${role}`
  return (
    <div className={className} role="article" aria-label={`${role} message`}>
      <div className="message-content">{content}</div>
      {timestamp && <div className="message-timestamp">{new Date(timestamp).toLocaleTimeString()}</div>}
    </div>
  )
}
