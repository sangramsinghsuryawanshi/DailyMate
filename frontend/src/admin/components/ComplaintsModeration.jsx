const statusOptions = ['OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED']

export default function ComplaintsModeration({ complaints, onUpdateStatus, isUpdating }) {
  if (complaints.length === 0) {
    return (
      <div className="empty-state">
        <h3>No complaints to review</h3>
        <p className="muted">All resident reports have been addressed or none have been submitted.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {complaints.map((complaint) => (
        <article key={complaint.id} className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span className="status-pill open">{complaint.category}</span>
              <span className="small-muted">{complaint.location}</span>
            </div>
            <span className="status-pill">{complaint.status}</span>
          </div>

          <h3 style={{ margin: '0.25rem 0' }}>{complaint.title}</h3>
          <p className="subtle-text">{complaint.description}</p>

          <div style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.9rem' }}>
              <span>Moderate status:</span>
              <select
                value={complaint.status}
                disabled={isUpdating}
                onChange={(e) => onUpdateStatus(complaint.id, e.target.value)}
                style={{ padding: '0.3rem 0.5rem', borderRadius: '4px', border: '1px solid #cbd5e1' }}
              >
                {statusOptions.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
          </div>
        </article>
      ))}
    </div>
  )
}
