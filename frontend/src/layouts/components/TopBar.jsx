import { useState, useRef, useEffect, useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { SearchBar, Avatar, Badge, Button } from '../../design-system'
import './TopBar.css'

export function TopBar({
  user,
  onSignOut,
  onOpenSearch,
  onOpenQuickAdd,
  unreadCount = 0,
  notifications = [],
  notificationsLoading = false,
}) {
  const location = useLocation()
  const navigate = useNavigate()
  const [isNotifOpen, setIsNotifOpen] = useState(false)
  const [isProfileOpen, setIsProfileOpen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(false)

  const notifRef = useRef(null)
  const profileRef = useRef(null)

  // Context-aware AI prompt based on current route
  const contextualAiPrompt = useMemo(() => {
    const p = location.pathname
    if (p.startsWith('/expenses')) return { label: 'Ask AI about spending', prompt: 'Summarize my spending this month' }
    if (p.startsWith('/medicines')) return { label: 'Ask AI about reminders', prompt: 'What medicines are due today?' }
    if (p.startsWith('/blood')) return { label: 'Ask AI about blood requests', prompt: 'Find urgent blood appeals' }
    if (p.startsWith('/marketplace')) return { label: 'Ask AI to find a provider', prompt: 'Find top rated providers' }
    if (p.startsWith('/events')) return { label: 'Ask AI about events', prompt: 'What events are happening this week?' }
    return { label: 'Ask DailyMate AI', prompt: '' }
  }, [location.pathname])

  const toggleDarkMode = () => {
    const next = !isDarkMode
    setIsDarkMode(next)
    document.documentElement.setAttribute('data-theme', next ? 'dark' : 'light')
  }

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) setIsNotifOpen(false)
      if (profileRef.current && !profileRef.current.contains(e.target)) setIsProfileOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => {
    setIsNotifOpen(false)
    setIsProfileOpen(false)
  }, [location.pathname])

  return (
    <header className="dm-topbar" aria-label="App TopBar">
      {/* Search Input Trigger */}
      <div className="dm-topbar__search">
        <SearchBar
          placeholder="Search services, tasks, tools... (⌘K)"
          onClick={onOpenSearch}
          readOnly
        />
      </div>

      {/* Right Utility Actions */}
      <div className="dm-topbar__actions">
        {/* Contextual AI Quick Entry Pill */}
        <button
          type="button"
          className="dm-topbar__ai-pill"
          onClick={() => navigate(`/assistant${contextualAiPrompt.prompt ? `?prompt=${encodeURIComponent(contextualAiPrompt.prompt)}` : ''}`)}
          title="Open AI Assistant"
        >
          <span className="dm-topbar__ai-icon">✨</span>
          <span className="dm-topbar__ai-label">{contextualAiPrompt.label}</span>
        </button>

        {/* Theme Mode Toggle */}
        <button
          type="button"
          className="dm-topbar__icon-btn"
          onClick={toggleDarkMode}
          aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
          title={isDarkMode ? 'Light mode' : 'Dark mode'}
        >
          {isDarkMode ? '☀️' : '🌙'}
        </button>

        {/* Notification Bell */}
        <div className="dm-topbar__popover-anchor" ref={notifRef}>
          <button
            type="button"
            className="dm-topbar__icon-btn"
            onClick={() => setIsNotifOpen((prev) => !prev)}
            aria-label="Notifications"
            aria-expanded={isNotifOpen}
          >
            🔔
            {unreadCount > 0 && (
              <span className="dm-topbar__notif-badge">{unreadCount}</span>
            )}
          </button>

          {isNotifOpen && (
            <div className="dm-topbar__dropdown dm-topbar__dropdown--notif" role="menu">
              <div className="dm-topbar__dropdown-header">
                <strong>Notifications</strong>
                {unreadCount > 0 && <Badge variant="primary" size="sm">{unreadCount} unread</Badge>}
              </div>

              <div className="dm-topbar__notif-list">
                {notificationsLoading ? (
                  <p className="dm-topbar__dropdown-empty">Loading notifications...</p>
                ) : notifications.length === 0 ? (
                  <p className="dm-topbar__dropdown-empty">No notifications yet.</p>
                ) : (
                  notifications.slice(0, 4).map((item) => (
                    <Link
                      key={item.id}
                      to="/notifications"
                      className={`dm-topbar__notif-item ${!item.read ? 'unread' : ''}`}
                      onClick={() => setIsNotifOpen(false)}
                    >
                      <strong className="dm-topbar__notif-item-title">{item.title}</strong>
                      <span className="dm-topbar__notif-item-desc">{item.message}</span>
                    </Link>
                  ))
                )}
              </div>

              <Link
                to="/notifications"
                className="dm-topbar__dropdown-footer"
                onClick={() => setIsNotifOpen(false)}
              >
                View all notifications →
              </Link>
            </div>
          )}
        </div>

        {/* Profile Avatar & Menu */}
        <div className="dm-topbar__popover-anchor" ref={profileRef}>
          <button
            type="button"
            className="dm-topbar__avatar-btn"
            onClick={() => setIsProfileOpen((prev) => !prev)}
            aria-label="Open user menu"
            aria-expanded={isProfileOpen}
          >
            <Avatar
              name={[user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'DailyMate User'}
              size="sm"
            />
          </button>

          {isProfileOpen && (
            <div className="dm-topbar__dropdown dm-topbar__dropdown--profile" role="menu">
              <div className="dm-topbar__profile-header">
                <strong>{[user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'DailyMate User'}</strong>
                <span>{user?.email || 'member@dailymate.app'}</span>
              </div>

              <div className="dm-topbar__menu-list">
                <Link to="/profile" className="dm-topbar__menu-item" onClick={() => setIsProfileOpen(false)}>
                  👤 Profile & Account
                </Link>
                <Link to="/notifications" className="dm-topbar__menu-item" onClick={() => setIsProfileOpen(false)}>
                  🔔 Notification Preferences
                </Link>
                {user?.role === 'ADMIN' && (
                  <Link to="/admin" className="dm-topbar__menu-item" onClick={() => setIsProfileOpen(false)}>
                    🛡️ Admin Moderation Hub
                  </Link>
                )}
                <button
                  type="button"
                  className="dm-topbar__menu-item dm-topbar__menu-item--danger"
                  onClick={() => {
                    setIsProfileOpen(false)
                    onSignOut?.()
                  }}
                >
                  🚪 Sign Out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

export default TopBar
