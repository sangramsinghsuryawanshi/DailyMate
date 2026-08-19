import React from 'react'

export default function ConversationList({ items = [], selectedId, onSelect }) {
  return (
    <aside className="assistant-conversations" aria-label="Conversations">
      <div className="conversations-header">
        <button className="new-convo">New</button>
      </div>
      <ul className="conversations-list">
        {items.map((c) => (
          <li key={c.id} className={`convo-item ${selectedId === c.id ? 'selected' : ''}`}>
            <button onClick={() => onSelect?.(c.id)} className="convo-button">
              <div className="convo-title">{c.title ?? 'Conversation'}</div>
              <div className="convo-meta">{c.updatedAt ? new Date(c.updatedAt).toLocaleString() : ''}</div>
            </button>
          </li>
        ))}
      </ul>
    </aside>
  )
}
