export default function LostFoundModeration({ items, onDeleteItem, isMutating }) {
  if (items.length === 0) {
    return (
      <div className="empty-state">
        <h3>No lost & found posts</h3>
        <p className="muted">No neighborhood listings have been reported.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {items.map((item) => (
        <article key={item.id} className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span className="status-pill open">{item.itemType}</span>
              <span className="small-muted">{item.location}</span>
            </div>
          </div>

          <h3 style={{ margin: '0.25rem 0' }}>{item.title}</h3>
          <p className="small-muted">Contact: {item.contactName} ({item.contactPhone})</p>
          <p className="subtle-text">{item.description}</p>

          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'flex-end' }}>
            <button
              type="button"
              className="btn btn-small btn-ghost"
              style={{ color: '#ef4444' }}
              disabled={isMutating}
              onClick={() => onDeleteItem(item.id)}
            >
              Delete / Remove Listing
            </button>
          </div>
        </article>
      ))}
    </div>
  )
}
