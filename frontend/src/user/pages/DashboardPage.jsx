import { useState, useMemo, useRef, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../hooks/useAuth'
import MainLayout from '../../layouts/MainLayout'
import {
  Button,
  Card,
  StatCard,
  Badge,
  TrustBadge,
  Skeleton,
  ResponsiveContainer,
} from '../../design-system'
import { formatINR } from '../../utils/formatters'
import { getProviders } from '../../marketplace/services/marketplaceApi'
import { getReminders } from '../../medicine/services/medicineApi'
import { getLocalEvents } from '../../events/services/localEventsApi'
import { getNotifications } from '../../notification/services/notificationsApi'
import { getExpenses } from '../../expense/services/expenseApi'
import { getBloodRequests } from '../../blood/services/bloodApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './DashboardPage.css'

const SUGGESTED_AI_PROMPTS = [
  { label: '💰 Spending breakdown', query: 'How much have I spent this month and what are my top expense categories?' },
  { label: '💊 Due today', query: 'Show me my active medicine schedule for today' },
  { label: '📅 Nearby events', query: 'What community events are happening this week?' },
  { label: '🛠️ Find plumber', query: 'Find a reliable plumber nearby' },
]

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [aiPromptInput, setAiPromptInput] = useState('')
  const aiTextareaRef = useRef(null)

  // Auto-expand AI textarea dynamically
  useEffect(() => {
    if (aiTextareaRef.current) {
      aiTextareaRef.current.style.height = 'auto'
      const newHeight = Math.min(Math.max(aiTextareaRef.current.scrollHeight, 44), 160)
      aiTextareaRef.current.style.height = `${newHeight}px`
    }
  }, [aiPromptInput])

  // 1. Recommended Services from Marketplace API (Real data only)
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
  } = useQuery({
    queryKey: ['local-events'],
    queryFn: () => getLocalEvents(),
    staleTime: 60_000,
  })

  // 4. Recent Notifications
  const {
    data: notificationData = { content: [], totalElements: 0 },
    isLoading: isNotificationsLoading,
  } = useQuery({
    queryKey: ['notifications', 'dashboard'],
    queryFn: () => getNotifications(0, 3),
    enabled: Boolean(user?.id),
    staleTime: 30_000,
  })

  // 5. Monthly Expenses
  const { data: expenses = [] } = useQuery({
    queryKey: ['expenses'],
    queryFn: getExpenses,
    enabled: Boolean(user?.id),
    staleTime: 60_000,
    retry: false,
  })

  // 6. Blood Requests
  const { data: bloodRequests = [] } = useQuery({
    queryKey: ['blood-requests', 'dashboard'],
    queryFn: () => getBloodRequests(),
    staleTime: 60_000,
    retry: false,
  })

  // Aggregated data slices with defensive array extraction
  const extractList = (val) => {
    if (!val) return []
    if (Array.isArray(val)) return val
    if (Array.isArray(val.content)) return val.content
    return []
  }

  const providersList = useMemo(() => extractList(providers), [providers])
  const remindersList = useMemo(() => extractList(reminders), [reminders])
  const eventsList = useMemo(() => extractList(events), [events])
  const notificationsList = useMemo(() => extractList(notificationData), [notificationData])
  const expensesList = useMemo(() => extractList(expenses), [expenses])
  const bloodRequestsList = useMemo(() => extractList(bloodRequests), [bloodRequests])

  const topProviders = useMemo(() => providersList.slice(0, 3), [providersList])

  const activeReminders = useMemo(
    () => remindersList.filter((r) => r.active !== false).slice(0, 3),
    [remindersList]
  )

  const upcomingEvents = useMemo(() => {
    const now = new Date().getTime()
    return eventsList
      .filter((e) => e.status === 'PUBLISHED' && new Date(e.eventDate).getTime() >= now)
      .sort((a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime())
      .slice(0, 2)
  }, [eventsList])

  const unreadNotifications = useMemo(
    () => notificationsList.filter((n) => !n.read).slice(0, 2),
    [notificationsList]
  )

  const totalExpenseAmount = useMemo(() => {
    return expensesList.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
  }, [expensesList])

  const urgentBloodCount = useMemo(() => {
    return bloodRequestsList.filter((r) => r.urgency === 'URGENT' || r.urgency === 'CRITICAL').length
  }, [bloodRequestsList])

  // Contextual time-of-day greeting & client browser date
  const greetingTime = useMemo(() => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good morning'
    if (hour < 18) return 'Good afternoon'
    return 'Good evening'
  }, [])

  const todayFormattedDate = useMemo(() => {
    return new Date().toLocaleDateString(undefined, {
      weekday: 'long',
      month: 'short',
      day: 'numeric',
    })
  }, [])

  // Dynamic status summary sentence
  const greetingSummary = useMemo(() => {
    const parts = []
    if (activeReminders.length > 0) {
      parts.push(`${activeReminders.length} medicine reminder${activeReminders.length > 1 ? 's' : ''} today`)
    }
    if (upcomingEvents.length > 0) {
      parts.push(`${upcomingEvents.length} upcoming event${upcomingEvents.length > 1 ? 's' : ''}`)
    }
    if (totalExpenseAmount > 0) {
      parts.push(`${formatINR(totalExpenseAmount)} recorded spending`)
    }
    if (parts.length === 0) {
      return 'Everything is calm and up to date.'
    }
    return `You have ${parts.join(', ')}.`
  }, [activeReminders, upcomingEvents, totalExpenseAmount])

  const hasTodayData =
    activeReminders.length > 0 || upcomingEvents.length > 0 || unreadNotifications.length > 0

  const isTodayLoading = isRemindersLoading || isEventsLoading || isNotificationsLoading

  const handleAiSubmit = (e) => {
    e.preventDefault()
    if (!aiPromptInput.trim()) return
    trackEvent(AnalyticsEvents.AI_QUERY_SENT, { source: 'dashboard_command_bar' })
    navigate(`/assistant?prompt=${encodeURIComponent(aiPromptInput.trim())}`)
  }

  const handlePromptChipClick = (prompt) => {
    trackEvent(AnalyticsEvents.AI_QUERY_SENT, { source: 'dashboard_prompt_chip', prompt: prompt.label })
    navigate(`/assistant?prompt=${encodeURIComponent(prompt.query)}`)
  }

  return (
    <MainLayout>
      <div className="dm-dashboard">
        <ResponsiveContainer size="wide">
          <div className="dm-bento-container">
            {/* ===================================================================
                ROW 1: BENTO HERO (7 cols) + AI COMPANION (5 cols)
               =================================================================== */}
            <div className="dm-bento-hero-row">
              {/* Level 1 — Daily Overview Hero Bento Card */}
              <div className="dm-bento-hero-card" aria-label="Daily Overview">
                <div className="dm-bento-hero__top">
                  <span className="dm-bento-hero__eyebrow-badge">Daily Command</span>
                  <span className="dm-bento-hero__date">{todayFormattedDate}</span>
                </div>

                <div>
                  <h1 className="dm-bento-hero__greeting-title">
                    {greetingTime}, {user?.firstName ?? 'there'} 👋
                  </h1>
                  <p className="dm-bento-hero__subtitle">{greetingSummary}</p>
                </div>

                {/* Priority Attention Chips (if urgent blood or medications exist) */}
                {(urgentBloodCount > 0 || activeReminders.length > 0) && (
                  <div className="dm-bento-hero__alerts" aria-label="Priority Attention">
                    {urgentBloodCount > 0 && (
                      <Link to="/blood" className="dm-bento-alert-chip dm-bento-alert-chip--urgent">
                        <span className="dm-bento-alert-chip__icon" aria-hidden="true">🩸</span>
                        <div className="dm-bento-alert-chip__text">
                          <strong>Urgent Blood Request Nearby</strong>
                          <span>{urgentBloodCount} community appeal{urgentBloodCount > 1 ? 's' : ''} require immediate response</span>
                        </div>
                        <span className="dm-bento-alert-chip__action">View Appeal →</span>
                      </Link>
                    )}

                    {activeReminders.length > 0 && (
                      <Link to="/medicines" className="dm-bento-alert-chip dm-bento-alert-chip--health">
                        <span className="dm-bento-alert-chip__icon" aria-hidden="true">💊</span>
                        <div className="dm-bento-alert-chip__text">
                          <strong>Medicine Reminder: {activeReminders[0].name}</strong>
                          <span>Scheduled for {activeReminders[0].remindAt || 'Today'} · {activeReminders[0].dosage}</span>
                        </div>
                        <span className="dm-bento-alert-chip__action">Schedule →</span>
                      </Link>
                    )}
                  </div>
                )}

                {/* Level 2 — Fast Action Bar */}
                <div className="dm-bento-hero__quick-actions">
                  <Link to="/expenses?action=add">
                    <Button variant="outline" size="sm" iconLeft="+">
                      Add Expense
                    </Button>
                  </Link>
                  <Link to="/medicines?action=add">
                    <Button variant="outline" size="sm" iconLeft="+">
                      Add Reminder
                    </Button>
                  </Link>
                  <Link to="/assistant">
                    <Button variant="primary" size="sm" iconLeft="✨">
                      Ask AI
                    </Button>
                  </Link>
                </div>
              </div>

              {/* Level 2 — Integrated AI Companion Bento Widget (5 cols) */}
              <div className="dm-bento-ai-card" aria-label="DailyMate AI Companion">
                <div className="dm-bento-ai__header">
                  <div className="dm-bento-ai__badge">
                    <span className="dm-bento-ai__bot-sparkle">✨</span>
                    <span>DailyMate AI</span>
                  </div>
                  <Badge variant="success" dot size="sm">Active</Badge>
                </div>

                <p className="dm-bento-ai__prompt-preview">
                  Ask anything about your schedule, expenses, or everyday tasks.
                </p>

                <form className="dm-bento-ai__form" onSubmit={handleAiSubmit}>
                  <textarea
                    ref={aiTextareaRef}
                    className="dm-bento-ai__input dm-bento-ai__textarea"
                    placeholder="Ask DailyMate AI anything..."
                    value={aiPromptInput}
                    onChange={(e) => setAiPromptInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault()
                        handleAiSubmit(e)
                      }
                    }}
                    rows={1}
                    aria-label="Ask DailyMate AI anything"
                  />
                  <Button type="submit" variant="primary" size="sm">
                    Ask
                  </Button>
                </form>

                <div className="dm-bento-ai__chips">
                  {SUGGESTED_AI_PROMPTS.map((prompt, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className="dm-bento-ai-chip"
                      onClick={() => handlePromptChipClick(prompt)}
                    >
                      {prompt.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* ===================================================================
                ROW 2: METRIC BENTO CARDS (Level 3 — 4 modular cards)
               =================================================================== */}
            <section className="dm-bento-stats-grid" aria-label="Key Life Metrics">
              <StatCard
                domain="expense"
                label="Monthly Spending"
                value={formatINR(totalExpenseAmount)}
                trend="Tracked"
                trendDirection="neutral"
                trendLabel="This Month"
                icon="💰"
              />
              <StatCard
                domain="health"
                label="Today's Reminders"
                value={String(activeReminders.length)}
                trend="Active"
                trendDirection="up"
                trendLabel="Dosages Scheduled"
                icon="💊"
              />
              <StatCard
                domain="community"
                label="Community Items"
                value={String(upcomingEvents.length + unreadNotifications.length)}
                trend="Alerts"
                trendDirection="neutral"
                trendLabel="Events & Updates"
                icon="🤝"
              />
              <StatCard
                domain="marketplace"
                label="Verified Providers"
                value={String(providersList.length)}
                trend="Available"
                trendDirection="up"
                trendLabel="Local Services"
                icon="🛠️"
              />
            </section>

            {/* ===================================================================
                ROW 3: MAIN ASYMMETRIC BENTO PANELS (Schedule 7 cols / Services 5 cols)
               =================================================================== */}
            <div className="dm-bento-main-row">
              {/* Today's Schedule & Reminders (7 cols) */}
              <div className="dm-bento-panel">
                <div className="dm-panel-header">
                  <div>
                    <h2 className="dm-panel-title">Today's Schedule & Reminders</h2>
                    <p className="dm-panel-desc">Your daily medications, events, and community alerts.</p>
                  </div>
                  <Link to="/medicines">
                    <Button variant="ghost" size="sm">
                      View all
                    </Button>
                  </Link>
                </div>

                {isTodayLoading ? (
                  <div className="dm-skeleton-stack">
                    <Skeleton variant="text" width="100%" height="48px" />
                    <Skeleton variant="text" width="100%" height="48px" />
                  </div>
                ) : hasTodayData ? (
                  <div className="dm-timeline-list">
                    {/* Active Medicine Reminders */}
                    {activeReminders.map((reminder) => (
                      <div key={reminder.id} className="dm-timeline-item">
                        <span className="dm-timeline-item__badge" aria-hidden="true">💊</span>
                        <div className="dm-timeline-item__info">
                          <strong>💊 {reminder.name}</strong>
                          <span>{reminder.dosage} · {reminder.frequency || 'Daily'}</span>
                        </div>
                        <span className="dm-timeline-item__time">{reminder.remindAt || '08:00'}</span>
                      </div>
                    ))}

                    {/* Upcoming Events */}
                    {upcomingEvents.map((event) => (
                      <div key={event.id} className="dm-timeline-item">
                        <span className="dm-timeline-item__badge" aria-hidden="true">📅</span>
                        <div className="dm-timeline-item__info">
                          <strong>📅 {event.title}</strong>
                          <span>📍 {event.location || 'Local Community'}</span>
                        </div>
                        <span className="dm-timeline-item__time">
                          {new Date(event.eventDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                        </span>
                      </div>
                    ))}

                    {/* Unread Notifications */}
                    {unreadNotifications.map((notification) => (
                      <div key={notification.id} className="dm-timeline-item">
                        <span className="dm-timeline-item__badge" aria-hidden="true">🔔</span>
                        <div className="dm-timeline-item__info">
                          <strong>🔔 {notification.title}</strong>
                          <span>{notification.message}</span>
                        </div>
                        <span className="dm-timeline-item__time">Alert</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="dm-empty-dashboard-box">
                    <p>No pending reminders or events scheduled for today.</p>
                    <div className="dm-empty-actions">
                      <Link to="/medicines">
                        <Button variant="primary" size="sm">+ Add reminder</Button>
                      </Link>
                      <Link to="/events">
                        <Button variant="outline" size="sm">Browse events</Button>
                      </Link>
                    </div>
                  </div>
                )}
              </div>

              {/* Recommended Local Services (5 cols — Real Backend Data Only) */}
              <div className="dm-bento-panel">
                <div className="dm-panel-header">
                  <div>
                    <h2 className="dm-panel-title">Recommended Local Services</h2>
                    <p className="dm-panel-desc">Verified neighborhood helpers & professionals.</p>
                  </div>
                  <Link to="/marketplace">
                    <Button variant="ghost" size="sm">
                      Explore All
                    </Button>
                  </Link>
                </div>

                {isProvidersLoading ? (
                  <div className="dm-skeleton-stack">
                    <Skeleton variant="card" height="80px" />
                    <Skeleton variant="card" height="80px" />
                  </div>
                ) : isProvidersError ? (
                  <div className="dm-error-box">
                    <p>Unable to load recommended services.</p>
                    <Button variant="outline" size="sm" onClick={() => refetchProviders()}>
                      Retry
                    </Button>
                  </div>
                ) : topProviders.length > 0 ? (
                  <div className="dm-providers-list">
                    {topProviders.map((provider) => (
                      <div key={provider.id} className="dm-provider-item">
                        <div className="dm-provider-item__info">
                          <h3>{provider.name}</h3>
                          <span className="dm-provider-category">{provider.category}</span>
                          {provider.serviceArea && (
                            <span className="dm-provider-area">Area: {provider.serviceArea}</span>
                          )}
                        </div>
                        <div className="dm-provider-item__action">
                          {provider.hourlyRate != null && (
                            <strong className="dm-provider-rate">
                              {formatINR(provider.hourlyRate)}/hr
                            </strong>
                          )}
                          <Link to={`/marketplace/${provider.id}`}>
                            <Button variant="primary" size="sm">
                              View
                            </Button>
                          </Link>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="dm-empty-dashboard-box">
                    <p>No service providers listed yet.</p>
                    <Link to="/marketplace">
                      <Button variant="outline" size="sm">
                        Browse Marketplace
                      </Button>
                    </Link>
                  </div>
                )}
              </div>
            </div>

            {/* ===================================================================
                ROW 4: BOTTOM BENTO (Emergency ICE 6 cols / Trust & Security 6 cols)
               =================================================================== */}
            <div className="dm-bento-bottom-row">
              {/* Emergency ICE & Community Safety Quick Access */}
              <div className="dm-bento-panel">
                <div className="dm-panel-header">
                  <div>
                    <h2 className="dm-panel-title">🚨 Emergency & ICE Directory</h2>
                    <p className="dm-panel-desc">Immediate access to personal ICE contacts and civic hotlines.</p>
                  </div>
                  <Link to="/emergency-contacts">
                    <Button variant="ghost" size="sm">
                      View All
                    </Button>
                  </Link>
                </div>

                <div className="dm-ice-hotlines">
                  <a href="tel:112" className="dm-ice-hotline-btn">
                    <span className="dm-ice-hotline-num">112</span>
                    <span>National Help</span>
                  </a>
                  <a href="tel:102" className="dm-ice-hotline-btn">
                    <span className="dm-ice-hotline-num">102</span>
                    <span>Ambulance</span>
                  </a>
                  <a href="tel:101" className="dm-ice-hotline-btn">
                    <span className="dm-ice-hotline-num">101</span>
                    <span>Fire Brigade</span>
                  </a>
                </div>
              </div>

              {/* Data Invariants & Trust Container */}
              <div className="dm-bento-panel">
                <div className="dm-panel-header">
                  <div>
                    <h2 className="dm-panel-title">System Status & Data Invariants</h2>
                    <p className="dm-panel-desc">All user data is stored safely with zero unintended mutations.</p>
                  </div>
                  <TrustBadge type="saved" label="State Protected" />
                </div>

                <div className="dm-trust-points">
                  <div className="dm-trust-point">
                    <span className="dm-trust-icon" aria-hidden="true">🔒</span>
                    <div>
                      <strong>Private Personal Container</strong>
                      <span>Data isolated to your authenticated account ID.</span>
                    </div>
                  </div>
                  <div className="dm-trust-point">
                    <span className="dm-trust-icon" aria-hidden="true">⚡</span>
                    <div>
                      <strong>Explicit AI Proposals</strong>
                      <span>AI creates structured proposals; changes occur only after confirmation.</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
