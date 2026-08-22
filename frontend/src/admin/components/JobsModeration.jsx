import { formatINR } from '../../utils/formatters'

export default function JobsModeration({ jobs, onUpdateStatus, onDeleteJob, isMutating }) {
  if (jobs.length === 0) {
    return (
      <div className="empty-state">
        <h3>No job postings</h3>
        <p className="muted">No community opportunities have been submitted.</p>
      </div>
    )
  }

  return (
    <div className="notification-list">
      {jobs.map((job) => (
        <article key={job.id} className="card" style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span className="status-pill open">{job.category}</span>
              <span className="status-pill">{job.type}</span>
              <span className="small-muted">{job.location}</span>
            </div>
            <span className={`status-pill ${job.status === 'OPEN' ? 'open' : ''}`}>{job.status}</span>
          </div>

          <h3 style={{ margin: '0.25rem 0' }}>{job.title}</h3>
          {job.companyName && <p className="small-muted" style={{ margin: '0 0 0.5rem 0' }}>{job.companyName}</p>}
          <p className="subtle-text">{job.description}</p>

          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              {job.salary != null && (
                <span style={{ fontWeight: '600', color: 'var(--color-primary, #0284c7)' }}>
                  {formatINR(job.salary)}
                </span>
              )}
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <button
                type="button"
                className={`btn btn-small ${job.status === 'OPEN' ? 'btn-ghost' : 'btn-primary'}`}
                disabled={isMutating}
                onClick={() => onUpdateStatus(job.id, job.status === 'OPEN' ? 'CLOSED' : 'OPEN')}
              >
                {job.status === 'OPEN' ? 'Close Job' : 'Reopen Job'}
              </button>
              <button
                type="button"
                className="btn btn-small btn-ghost"
                style={{ color: '#ef4444' }}
                disabled={isMutating}
                onClick={() => onDeleteJob(job.id)}
              >
                Delete
              </button>
            </div>
          </div>
        </article>
      ))}
    </div>
  )
}
