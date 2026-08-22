export default function UsersModeration({ users, currentUser, onUpdateStatus, isMutating }) {
  if (users.length === 0) {
    return (
      <div className="empty-state">
        <h3>No users found</h3>
        <p className="muted">No user accounts are registered.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {users.map((usr) => {
        const isSelf = currentUser?.id === usr.id
        const isAdmin = usr.role === 'ADMIN'
        const canModify = !isSelf && !isAdmin

        return (
          <article key={usr.id} className="card" style={{ marginBottom: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <span className="status-pill open">{usr.role}</span>
                <span className={`status-pill ${usr.status === 'ACTIVE' ? 'open' : ''}`} style={usr.status === 'SUSPENDED' ? { background: '#fee2e2', color: '#991b1b' } : {}}>
                  {usr.status}
                </span>
                {isSelf && <span className="status-pill" style={{ background: '#fef3c7', color: '#92400e' }}>You</span>}
              </div>
              <span className="small-muted">
                Joined {new Date(usr.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })}
              </span>
            </div>

            <h3 style={{ margin: '0.25rem 0' }}>{usr.firstName} {usr.lastName}</h3>
            <p className="small-muted">{usr.email}</p>

            <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', alignItems: 'center' }}>
              {canModify ? (
                <button
                  type="button"
                  className={`btn btn-small ${usr.status === 'ACTIVE' ? 'btn-ghost' : 'btn-primary'}`}
                  style={usr.status === 'ACTIVE' ? { color: '#ef4444' } : {}}
                  disabled={isMutating}
                  onClick={() => onUpdateStatus(usr.id, usr.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE')}
                >
                  {usr.status === 'ACTIVE' ? 'Suspend Account' : 'Reactivate Account'}
                </button>
              ) : (
                <span className="small-muted" style={{ fontStyle: 'italic' }}>
                  {isSelf ? 'Cannot modify self' : 'Admin accounts protected'}
                </span>
              )}
            </div>
          </article>
        )
      })}
    </div>
  )
}
