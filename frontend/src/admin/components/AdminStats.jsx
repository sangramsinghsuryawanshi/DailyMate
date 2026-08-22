export default function AdminStats({ stats, isLoading, isError }) {
  if (isLoading) {
    return <div className="card"><p className="muted">Loading administrative statistics…</p></div>
  }

  if (isError || !stats) {
    return null
  }

  const statCards = [
    {
      title: 'Registered Users',
      value: stats.totalUsers,
      sub: `${stats.activeUsers} active · ${stats.suspendedUsers} suspended`,
      badge: 'info',
    },
    {
      title: 'Community Complaints',
      value: stats.totalComplaints,
      sub: `${stats.openComplaints} open · ${stats.inReviewComplaints} review · ${stats.resolvedComplaints} resolved`,
      badge: 'warning',
    },
    {
      title: 'Job Postings',
      value: stats.totalJobs,
      sub: `${stats.openJobs} active · ${stats.closedJobs} closed`,
      badge: 'success',
    },
    {
      title: 'Blood Requests',
      value: stats.totalBloodRequests,
      sub: `${stats.openBloodRequests} open · ${stats.fulfilledBloodRequests} fulfilled`,
      badge: 'danger',
    },
    {
      title: 'Local Events',
      value: stats.totalEvents,
      sub: `${stats.publishedEvents} published · ${stats.cancelledEvents} cancelled`,
      badge: 'info',
    },
    {
      title: 'Lost & Found',
      value: stats.totalLostFound,
      sub: 'Community listings',
      badge: 'neutral',
    },
  ]

  return (
    <section className="stats-grid" aria-label="Admin overview statistics">
      {statCards.map((item) => (
        <article key={item.title} className="card metric-card">
          <p className="small-muted">{item.title}</p>
          <strong className="metric-value">{item.value}</strong>
          <span className="small-muted">{item.sub}</span>
        </article>
      ))}
    </section>
  )
}
