export default function EventsModeration({ events, onUpdateStatus, onDeleteEvent, isMutating }) {
  if (events.length === 0) {
    return (
      <div className="empty-state">
        <h3>No community events</h3>
        <p className="muted">No local events are scheduled.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {events.map((event) => (
        <article key={event.id} className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span className="status-pill open">{event.category}</span>
              <span className="small-muted">{event.location}</span>
            </div>
            <span className={`status-pill ${event.status === 'PUBLISHED' ? 'open' : ''}`}>{event.status}</span>
          </div>

          <h3 style={{ margin: '0.25rem 0' }}>{event.title}</h3>
          <p className="small-muted">
            Date: {new Date(event.eventDate).toLocaleDateString('en-IN', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
          </p>
          <p className="subtle-text">{event.description}</p>

          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <button
              type="button"
              className={`btn btn-small ${event.status === 'PUBLISHED' ? 'btn-ghost' : 'btn-primary'}`}
              disabled={isMutating}
              onClick={() => onUpdateStatus(event.id, event.status === 'PUBLISHED' ? 'CANCELLED' : 'PUBLISHED')}
            >
              {event.status === 'PUBLISHED' ? 'Cancel Event' : 'Publish Event'}
            </button>
            <button
              type="button"
              className="btn btn-small btn-ghost"
              style={{ color: '#ef4444' }}
              disabled={isMutating}
              onClick={() => onDeleteEvent(event.id)}
            >
              Delete
            </button>
          </div>
        </article>
      ))}
    </div>
  )
}
