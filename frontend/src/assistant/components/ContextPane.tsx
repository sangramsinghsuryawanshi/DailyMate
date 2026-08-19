import React from 'react'

interface ContextPaneProps {
  pageTitle?: string
  suggestedPrompts?: string[]
}

export default function ContextPane({ pageTitle = '', suggestedPrompts = [] }: ContextPaneProps) {
  return (
    <aside className="assistant-context" aria-label="Assistant context pane">
      <div className="context-header">
        <h3>Context</h3>
        <div className="context-meta">Page: {pageTitle || '—'}</div>
      </div>

      <div className="context-section">
        <h4>Suggested prompts</h4>
        <ul>
          {suggestedPrompts.map((p, i) => (
            <li key={i} className="suggestion">{p}</li>
          ))}
        </ul>
      </div>

      <div className="context-section">
        <h4>Files / Sources</h4>
        <div className="sources">No sources attached</div>
      </div>
    </aside>
  )
}
