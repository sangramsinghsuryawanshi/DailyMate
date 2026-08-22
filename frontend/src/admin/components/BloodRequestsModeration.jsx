export default function BloodRequestsModeration({ requests, onUpdateStatus, onDeleteRequest, isMutating }) {
  if (requests.length === 0) {
    return (
      <div className="empty-state">
        <h3>No blood donation requests</h3>
        <p className="muted">No emergency blood requests are currently active.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {requests.map((req) => (
        <article key={req.id} className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span className="status-pill" style={{ background: '#fee2e2', color: '#991b1b', fontWeight: 'bold' }}>
                🩸 {req.bloodGroup}
              </span>
              <span className="small-muted">{req.unitsNeeded} unit(s)</span>
              <span className="small-muted">{req.hospitalLocation}</span>
            </div>
            <span className={`status-pill ${req.status === 'OPEN' ? 'open' : ''}`}>{req.status}</span>
          </div>

          <h3 style={{ margin: '0.25rem 0' }}>Patient: {req.patientName}</h3>
          <p className="small-muted">Contact: {req.contactName} ({req.contactPhone})</p>
          {req.additionalNotes && <p className="subtle-text">{req.additionalNotes}</p>}

          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            {req.status === 'OPEN' ? (
              <button
                type="button"
                className="btn btn-small btn-primary"
                disabled={isMutating}
                onClick={() => onUpdateStatus(req.id, 'FULFILLED')}
              >
                Mark Fulfilled
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-small btn-ghost"
                disabled={isMutating}
                onClick={() => onUpdateStatus(req.id, 'OPEN')}
              >
                Reopen Request
              </button>
            )}
            <button
              type="button"
              className="btn btn-small btn-ghost"
              style={{ color: '#ef4444' }}
              disabled={isMutating}
              onClick={() => onDeleteRequest(req.id)}
            >
              Delete
            </button>
          </div>
        </article>
      ))}
    </div>
  )
}
