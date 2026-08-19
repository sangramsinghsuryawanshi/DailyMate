import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { logout as logoutRequest } from '../../auth/services/authApi'
import MainLayout from '../../layouts/MainLayout'

const quickActions = [
  { label: 'Find a service', to: '/marketplace', tone: 'primary' },
  { label: 'My profile', to: '/profile', tone: 'secondary' },
  { label: 'AI assistant', to: '/assistant', tone: 'secondary' },
]

const serviceHighlights = [
  { name: 'HomeCare Pro', category: 'Plumber', rating: '4.9', price: '$48', distance: '1.2 km', status: 'Available today' },
  { name: 'BrightNest', category: 'Cleaner', rating: '4.8', price: '$36', distance: '2.1 km', status: 'Booked for 4 PM' },
  { name: 'City Tutors', category: 'Tutor', rating: '5.0', price: '$22', distance: '3.5 km', status: 'New slots' },
]

const agenda = [
  'Take your evening medicine at 8:00 PM',
  'Review grocery deals for this week',
  'Check local events this weekend',
]

export default function DashboardPage() {
  const { user, refreshToken, signOut } = useAuth()

  async function handleSignOut() {
    if (refreshToken) {
      try {
        await logoutRequest(refreshToken)
      } catch {
        // Local sign-out still proceeds if the server is unavailable.
      }
    }
    signOut()
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Good morning</p>
          <h1>Hello, {user?.firstName ?? 'there'} 👋</h1>
          <p className="subtle-text">Here is a quick view of your day, local help, and important reminders.</p>
        </div>
        <button type="button" className="btn btn-ghost" onClick={handleSignOut}>Sign out</button>
      </section>

      <section className="quick-actions" aria-label="Quick actions">
        {quickActions.map((action) => (
          <Link key={action.label} to={action.to} className={`action-card action-${action.tone}`}>
            {action.label}
          </Link>
        ))}
      </section>

      <section className="dashboard-grid">
        <div className="panel panel-large">
          <div className="panel-header">
            <h2>Recommended services</h2>
            <Link to="/marketplace">View all</Link>
          </div>

          <div className="provider-list">
            {serviceHighlights.map((provider) => (
              <article key={provider.name} className="provider-card">
                <div className="provider-avatar">{provider.name.slice(0, 1)}</div>
                <div className="provider-copy">
                  <div className="provider-row">
                    <h3>{provider.name}</h3>
                    <span className="rating-badge">★ {provider.rating}</span>
                  </div>
                  <p className="muted">{provider.category}</p>
                  <div className="meta-row">
                    <span>{provider.distance}</span>
                    <span>{provider.status}</span>
                  </div>
                </div>
                <div className="provider-price">
                  <strong>{provider.price}</strong>
                  <button type="button" className="btn btn-small">Request</button>
                </div>
              </article>
            ))}
          </div>
        </div>

        <aside className="side-stack">
          <div className="panel">
            <div className="panel-header">
              <h2>Today</h2>
              <Link to="/medicines">Manage</Link>
            </div>
            <ul className="list-stack">
              {agenda.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Helpful links</h2>
            </div>
            <nav className="mini-links">
              <Link to="/expenses">Track expenses</Link>
              <Link to="/events">Local events</Link>
              <Link to="/community-complaints">Community reports</Link>
              <Link to="/grocery">Grocery prices</Link>
            </nav>
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
