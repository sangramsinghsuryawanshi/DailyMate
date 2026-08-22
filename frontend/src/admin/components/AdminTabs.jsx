export default function AdminTabs({ activeTab, onSelectTab, counts = {} }) {
  const tabs = [
    { id: 'overview', label: '📊 Overview' },
    { id: 'complaints', label: `🛡️ Complaints (${counts.complaints ?? 0})` },
    { id: 'jobs', label: `💼 Jobs (${counts.jobs ?? 0})` },
    { id: 'blood', label: `🩸 Blood Requests (${counts.blood ?? 0})` },
    { id: 'events', label: `📅 Events (${counts.events ?? 0})` },
    { id: 'lost-found', label: `🔎 Lost & Found (${counts.lostFound ?? 0})` },
    { id: 'users', label: `👥 Users (${counts.users ?? 0})` },
  ]

  return (
    <nav className="tab-nav" aria-label="Admin moderation navigation" style={{ marginBottom: '1.5rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          className={`btn ${activeTab === tab.id ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => onSelectTab(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  )
}
