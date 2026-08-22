import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../hooks/useAuth'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { getProviders } from '../../marketplace/services/marketplaceApi'
import { getReminders } from '../../medicine/services/medicineApi'
import { getLocalEvents } from '../../events/services/localEventsApi'
import { getNotifications } from '../../notification/services/notificationsApi'

const quickActions = [
  { label: 'Find a service', to: '/marketplace', tone: 'primary' },
  { label: 'Medicine reminders', to: '/medicines', tone: 'secondary' },
  { label: 'AI assistant', to: '/assistant', tone: 'secondary' },
]

export default function DashboardPage() {
  const { user } = useAuth()

  // 1. Recommended Services from Marketplace API
  const {
    data: providers = [],
    isLoading: isProvidersLoading,
    isError: isProvidersError,
    refetch: refetchProviders,
  } = useQuery({
    queryKey: ['marketplace-providers'],
    queryFn: getProviders,
    staleTime: 60_000,
  })

  // 2. Active Medicine Reminders
  const {
    data: reminders = [],
    isLoading: isRemindersLoading,
    isError: isRemindersError,
    refetch: refetchReminders,
  } = useQuery({
    queryKey: ['medicine-reminders'],
    queryFn: getReminders,
    enabled: Boolean(user?.id),
    staleTime: 30_000,
  })

  // 3. Upcoming Local Events
  const {
    data: events = [],
    isLoading: isEventsLoading,
    isError: isEventsError,
    refetch: refetchEvents,
  } = useQuery({
    queryKey: ['local-events'],
    queryFn: () => getLocalEvents(),
    staleTime: 60_000,
  })

  // 4. Recent Notifications
  const {
    data: notificationData = { content: [], totalElements: 0 },
    isLoading: isNotificationsLoading,
    isError: isNotificationsError,
    refetch: refetchNotifications,
  } = useQuery({
    queryKey: ['notifications', 'dashboard'],
    queryFn: () => getNotifications(0, 3),
    enabled: Boolean(user?.id),
    staleTime: 30_000,
  })

  const topProviders = useMemo(() => providers.slice(0, 3), [providers])

  const activeReminders = useMemo(
    () => reminders.filter((r) => r.active !== false).slice(0, 3),
    [reminders],
  )

  const upcomingEvents = useMemo(() => {
    const now = new Date().getTime()
    return events
      .filter((e) => e.status === 'PUBLISHED' && new Date(e.eventDate).getTime() >= now)
      .sort((a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime())
      .slice(0, 2)
  }, [events])

  const unreadNotifications = useMemo(
    () => (notificationData.content ?? []).filter((n) => !n.read).slice(0, 2),
    [notificationData],
  )

  const hasTodayData = activeReminders.length > 0 || upcomingEvents.length > 0 || unreadNotifications.length > 0
  const isTodayLoading = isRemindersLoading || isEventsLoading || isNotificationsLoading

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Hello, {user?.firstName ?? 'there'} 👋</h1>
          <p className="subtle-text">Here is a quick view of your day, active reminders, and local services.</p>
        </div>
      </section>

      <section className="quick-actions" aria-label="Quick actions">
        {quickActions.map((action) => (
          <Link key={action.label} to={action.to} className={`action-card action-${action.tone}`}>
            {action.label}
          </Link>
        ))}
      </section>

      <section className="dashboard-grid">
        {/* Recommended Services Section */}
        <div className="panel panel-large">
          <div className="panel-header">
            <h2>Recommended services</h2>
            <Link to="/marketplace">View all</Link>
          </div>

          {isProvidersLoading ? (
            <div className="search-empty-state compact">
              <p>Loading local services…</p>
            </div>
          ) : isProvidersError ? (
            <div className="search-empty-state compact" role="alert">
              <p>Unable to load recommended services.</p>
              <button type="button" className="btn btn-small btn-secondary" onClick={() => refetchProviders()} style={{ marginTop: '0.5rem' }}>
                Retry
              </button>
            </div>
          ) : topProviders.length === 0 ? (
            <div className="search-empty-state compact">
              <p>No service providers listed yet.</p>
              <Link to="/marketplace" className="btn btn-small btn-primary" style={{ marginTop: '0.5rem' }}>
                Explore Marketplace
              </Link>
            </div>
          ) : (
            <div className="provider-list">
              {topProviders.map((provider) => (
                <article key={provider.id} className="provider-card">
                  <div className="provider-avatar">{provider.name ? provider.name.slice(0, 1) : 'P'}</div>
                  <div className="provider-copy">
                    <div className="provider-row">
                      <h3>{provider.name}</h3>
                      <span className="badge">{provider.category}</span>
                    </div>
                    <p className="muted">Area: {provider.serviceArea}</p>
                    {provider.description && <p className="provider-description">{provider.description}</p>}
                  </div>
                  <div className="provider-price">
                    <strong>
                      {provider.hourlyRate != null ? `${formatINR(provider.hourlyRate)}/hr` : 'Contact for quote'}
                    </strong>
                    <Link to={`/marketplace/${provider.id}`} className="btn btn-small btn-primary">
                      View profile
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        {/* Aggregated Today & Shortcuts Stack */}
        <aside className="side-stack">
          {/* Today Aggregated Schedule */}
          <div className="panel">
            <div className="panel-header">
              <h2>Today’s Schedule</h2>
              <Link to="/medicines">Manage</Link>
            </div>

            {isTodayLoading ? (
              <div className="search-empty-state compact">
                <p>Loading your schedule…</p>
              </div>
            ) : isRemindersError && isEventsError ? (
              <div className="search-empty-state compact" role="alert">
                <p>Unable to load schedule items.</p>
                <button type="button" className="btn btn-small btn-secondary" onClick={() => { refetchReminders(); refetchEvents() }} style={{ marginTop: '0.5rem' }}>
                  Retry
                </button>
              </div>
            ) : !hasTodayData ? (
              <div className="search-empty-state compact">
                <p>No pending reminders or events scheduled for today.</p>
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
                  <Link to="/medicines" className="btn btn-small btn-secondary">
                    + Add reminder
                  </Link>
                  <Link to="/events" className="btn btn-small btn-ghost">
                    Browse events
                  </Link>
                </div>
              </div>
            ) : (
              <ul className="list-stack" style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: '0.5rem' }}>
                {activeReminders.map((reminder) => (
                  <li key={reminder.id} className="notification-item-mini" style={{ borderLeft: '3px solid var(--color-primary)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>💊 {reminder.name}</strong>
                      <small className="small-muted">{reminder.remindAt}</small>
                    </div>
                    <span style={{ fontSize: '0.85rem' }}>
                      {reminder.dosage ? `${reminder.dosage} · ` : ''}{reminder.frequency || 'Daily'}
                    </span>
                  </li>
                ))}

                {upcomingEvents.map((event) => (
                  <li key={event.id} className="notification-item-mini" style={{ borderLeft: '3px solid #3b82f6' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>📅 {event.title}</strong>
                      <span className="badge" style={{ fontSize: '0.7rem' }}>{event.category}</span>
                    </div>
                    <span style={{ fontSize: '0.85rem' }}>
                      📍 {event.location}
                    </span>
                  </li>
                ))}

                {unreadNotifications.map((notif) => (
                  <li key={notif.id} className="notification-item-mini unread" style={{ borderLeft: '3px solid #f59e0b' }}>
                    <strong>🔔 {notif.title}</strong>
                    <span style={{ fontSize: '0.85rem' }}>{notif.message}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Quick Hub Shortcuts */}
          <div className="panel">
            <div className="panel-header">
              <h2>Helpful links</h2>
            </div>
            <nav className="mini-links">
              <Link to="/expenses">Track expenses</Link>
              <Link to="/events">Local events</Link>
              <Link to="/blood">Blood donation</Link>
              <Link to="/grocery">Grocery prices</Link>
              <Link to="/emergency-contacts">Emergency directory</Link>
              <Link to="/community-complaints">Community reports</Link>
            </nav>
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
