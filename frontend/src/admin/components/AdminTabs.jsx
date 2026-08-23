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
    <nav className="dm-admin-tabs-nav" aria-label="Admin moderation navigation">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          className={`dm-category-filter-btn ${activeTab === tab.id ? 'active' : ''}`}
          onClick={() => onSelectTab(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  )
}
