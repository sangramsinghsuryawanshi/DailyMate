import { StatCard } from '../../design-system'

export default function AdminStats({ stats, isLoading, isError }) {
  if (isLoading) {
    return (
      <div className="dm-empty-dashboard-box">
        <p>Loading administrative statistics…</p>
      </div>
    )
  }

  if (isError || !stats) {
    return null
  }

  const statCards = [
    {
      title: 'Registered Users',
      value: String(stats.totalUsers),
      sub: `${stats.activeUsers} active · ${stats.suspendedUsers} suspended`,
      domain: 'community',
      icon: '👥',
      trend: 'Users',
    },
    {
      title: 'Community Complaints',
      value: String(stats.totalComplaints),
      sub: `${stats.openComplaints} open · ${stats.inReviewComplaints} review · ${stats.resolvedComplaints} resolved`,
      domain: 'emergency',
      icon: '🛡️',
      trend: 'Complaints',
    },
    {
      title: 'Job Postings',
      value: String(stats.totalJobs),
      sub: `${stats.openJobs} active · ${stats.closedJobs} closed`,
      domain: 'expense',
      icon: '💼',
      trend: 'Jobs',
    },
    {
      title: 'Blood Requests',
      value: String(stats.totalBloodRequests),
      sub: `${stats.openBloodRequests} open · ${stats.fulfilledBloodRequests} fulfilled`,
      domain: 'health',
      icon: '🩸',
      trend: 'Blood',
    },
    {
      title: 'Local Events',
      value: String(stats.totalEvents),
      sub: `${stats.publishedEvents} published · ${stats.cancelledEvents} cancelled`,
      domain: 'community',
      icon: '📅',
      trend: 'Events',
    },
    {
      title: 'Lost & Found',
      value: String(stats.totalLostFound),
      sub: 'Community listings',
      domain: 'community',
      icon: '🔎',
      trend: 'Lost/Found',
    },
  ]

  return (
    <section className="stats-grid dm-admin-stats-grid" aria-label="Admin overview statistics">
      {statCards.map((item) => (
        <article key={item.title} className="card metric-card">
          <StatCard
            domain={item.domain}
            label={item.title}
            value={item.value}
            trend={item.trend}
            trendDirection="neutral"
            trendLabel={item.sub}
            icon={item.icon}
          />
        </article>
      ))}
    </section>
  )
}
