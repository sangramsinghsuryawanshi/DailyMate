import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { logout as logoutRequest } from '../auth/services/authApi'
import { useAuth } from '../hooks/useAuth'
import { getProviders } from '../marketplace/services/marketplaceApi'
import { getNotifications } from '../notification/services/notificationsApi'
import { searchDailyMate } from '../services/searchService'

const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'Marketplace', to: '/marketplace' },
  { label: 'Medicines', to: '/medicines' },
  { label: 'Expenses', to: '/expenses' },
  { label: 'AI assistant', to: '/assistant' },
  { label: 'Blood Donation', to: '/blood' },
  { label: 'Lost & Found', to: '/lost-found' },
  { label: 'Emergency', to: '/emergency-contacts' },
  { label: 'Events', to: '/events' },
  { label: 'Jobs', to: '/jobs' },
  { label: 'Grocery', to: '/grocery' },
  { label: 'Community', to: '/community-complaints' },
  { label: 'Notifications', to: '/notifications' },
  { label: 'Profile', to: '/profile' },
]

const searchSuggestions = [
  { id: 'dashboard', title: 'Dashboard', category: 'Overview', description: 'Review your day, reminders, and quick actions.', to: '/dashboard', tags: ['overview', 'daily tasks'] },
  { id: 'marketplace', title: 'Marketplace', category: 'Services', description: 'Browse trusted local providers and service categories.', to: '/marketplace', tags: ['in-home help', 'plumber', 'tutor'] },
  { id: 'blood', title: 'Blood Donation', category: 'Health', description: 'Find urgent blood requests or locate community donation centers.', to: '/blood', tags: ['blood donor', 'urgent blood', 'donation'] },
  { id: 'emergency', title: 'Emergency Contacts', category: 'Support', description: 'Immediate emergency hotlines and personal ICE contacts.', to: '/emergency-contacts', tags: ['emergency', 'police', 'ambulance', 'fire', 'help'] },
  { id: 'lost-found', title: 'Lost & Found', category: 'Community', description: 'Report missing items or help reunite community belongings.', to: '/lost-found', tags: ['missing', 'found', 'lost'] },
  { id: 'notifications', title: 'Notifications', category: 'Inbox', description: 'Track updates, reminders, and community alerts.', to: '/notifications', tags: ['updates', 'alerts'] },
  { id: 'profile', title: 'Profile', category: 'Account', description: 'Update your personal details and profile preferences.', to: '/profile', tags: ['account', 'settings'] },
]

export default function MainLayout({ children }) {
  const location = useLocation()
  const { user, refreshToken, signOut } = useAuth()
  const searchRef = useRef(null)
  const notificationRef = useRef(null)
  const profileRef = useRef(null)

  const [searchTerm, setSearchTerm] = useState('')
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false)
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false)

  const { data: providers = [] } = useQuery({
    queryKey: ['marketplace-providers', 'shell-search'],
    queryFn: getProviders,
    staleTime: 60_000,
    retry: 1,
  })

  const { data: bellData = { content: [], totalElements: 0 }, isLoading: notificationsLoading, isError: notificationsError } = useQuery({
    queryKey: ['notifications', 'bell'],
    queryFn: () => getNotifications(0, 5),
    enabled: !!user,
    staleTime: 30_000,
    refetchInterval: 60_000,
    retry: 1,
  })

  const notifications = bellData.content ?? []
  const unreadCount = useMemo(() => (bellData.content ?? []).filter((item) => !item.read).length, [bellData])

  const fullName = useMemo(
    () => [user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'DailyMate member',
    [user],
  )

  const initials = useMemo(() => {
    const parts = fullName.split(/\s+/).filter(Boolean)
    const firstInitial = parts[0]?.[0] ?? 'D'
    const secondInitial = parts[1]?.[0] ?? ''
    return `${firstInitial}${secondInitial}`.toUpperCase() || 'DM'
  }, [fullName])

  const searchResults = useMemo(
    () => searchDailyMate(searchTerm, providers, searchSuggestions),
    [providers, searchTerm],
  )

  useEffect(() => {
    const handlePointerDown = (event) => {
      if (searchRef.current && !searchRef.current.contains(event.target)) {
        setIsSearchOpen(false)
      }

      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setIsNotificationsOpen(false)
      }

      if (profileRef.current && !profileRef.current.contains(event.target)) {
        setIsProfileMenuOpen(false)
      }
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsSearchOpen(false)
        setIsNotificationsOpen(false)
        setIsProfileMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [])

  useEffect(() => {
    setIsSearchOpen(false)
    setIsNotificationsOpen(false)
    setIsProfileMenuOpen(false)
  }, [location.pathname])

  async function handleSignOut() {
    if (refreshToken) {
      try {
        await logoutRequest(refreshToken)
      } catch {
        // Keep local sign-out working even if the server is unavailable.
      }
    }

    signOut()
    setIsProfileMenuOpen(false)
  }

  const sideNavItems = useMemo(() => {
    if (user?.role === 'ADMIN') {
      return [...navItems, { label: 'Admin Hub', to: '/admin' }]
    }
    return navItems
  }, [user?.role])

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark">D</span>
          <span>DailyMate</span>
        </div>

        <nav className="side-nav" aria-label="Main navigation">
          {sideNavItems.map((item) => {
            const isActive = location.pathname === item.to || (item.to !== '/dashboard' && location.pathname.startsWith(item.to))
            return (
              <Link key={item.to} to={item.to} className={`nav-item ${isActive ? 'active' : ''}`}>
                {item.label}
              </Link>
            )
          })}
        </nav>
      </aside>

      <div className="content-shell">
        <header className="topbar">
          <div className="search-shell" ref={searchRef}>
            <label className="search-box" htmlFor="global-search">
              <span>Search</span>
              <input
                id="global-search"
                type="search"
                value={searchTerm}
                onFocus={() => setIsSearchOpen(true)}
                onChange={(event) => {
                  setSearchTerm(event.target.value)
                  setIsSearchOpen(true)
                }}
                placeholder="Find a service or task"
                aria-label="Search services and tasks"
              />
            </label>

            {isSearchOpen && (
              <div className="search-popover" role="listbox" aria-label="Search suggestions">
                {!searchTerm.trim() ? (
                  <div className="search-empty-state">
                    <p>Type to search DailyMate services, tools, and pages.</p>
                    <div className="search-tag-row">
                      {searchSuggestions.map((item) => (
                        <Link key={item.id} to={item.to} className="search-tag" onClick={() => setIsSearchOpen(false)}>
                          {item.title}
                        </Link>
                      ))}
                    </div>
                  </div>
                ) : searchResults.length === 0 ? (
                  <div className="search-empty-state">
                    <p>No results found for “{searchTerm}”. Try another keyword.</p>
                  </div>
                ) : (
                  searchResults.map((item) => (
                    <Link key={`${item.to}-${item.title}`} to={item.to} className="search-result" onClick={() => setIsSearchOpen(false)}>
                      <div>
                        <strong>{item.title}</strong>
                        <span>{item.category}</span>
                      </div>
                      <small>{item.description}</small>
                    </Link>
                  ))
                )}
              </div>
            )}
          </div>

          <div className="topbar-actions">
            <div className="notification-shell" ref={notificationRef}>
              <button
                type="button"
                className="icon-button"
                aria-label="Notifications"
                aria-expanded={isNotificationsOpen}
                onClick={() => setIsNotificationsOpen((current) => !current)}
              >
                <span aria-hidden="true">🔔</span>
                {unreadCount > 0 && <span className="notification-count">{unreadCount}</span>}
              </button>

              {isNotificationsOpen && (
                <div className="notification-popover" role="menu" aria-label="Notifications panel">
                  <div className="notification-header">
                    <strong>Notifications</strong>
                    {unreadCount > 0 && <span>{unreadCount} unread</span>}
                  </div>

                  {notificationsLoading ? (
                    <div className="search-empty-state compact"><p>Loading notifications…</p></div>
                  ) : notificationsError ? (
                    <div className="search-empty-state compact"><p>Notifications are unavailable right now.</p></div>
                  ) : notifications.length === 0 ? (
                    <div className="search-empty-state compact"><p>You have no notifications yet.</p></div>
                  ) : (
                    notifications.slice(0, 4).map((item) => (
                      <div key={item.id} className={`notification-item-mini ${item.read ? 'read' : 'unread'}`}>
                        <strong>{item.title}</strong>
                        <span>{item.message}</span>
                      </div>
                    ))
                  )}

                  <Link to="/notifications" className="notification-link" onClick={() => setIsNotificationsOpen(false)}>
                    View all notifications
                  </Link>
                </div>
              )}
            </div>

            <div className="profile-shell" ref={profileRef}>
              <button
                type="button"
                className="avatar-chip"
                aria-label="Open profile menu"
                aria-expanded={isProfileMenuOpen}
                onClick={() => setIsProfileMenuOpen((current) => !current)}
              >
                {initials}
              </button>

              {isProfileMenuOpen && (
                <div className="profile-menu" role="menu" aria-label="User profile menu">
                  <div className="profile-menu-header">
                    <span className="avatar-chip avatar-mini">{initials}</span>
                    <div>
                      <strong>{fullName}</strong>
                      <span>{user?.email ?? 'member@dailymate.app'}</span>
                    </div>
                  </div>

                  <Link to="/profile" className="profile-menu-item" onClick={() => setIsProfileMenuOpen(false)}>
                    View profile
                  </Link>
                  <Link to="/notifications" className="profile-menu-item" onClick={() => setIsProfileMenuOpen(false)}>
                    Notifications
                  </Link>
                  {user?.role === 'ADMIN' && (
                    <Link to="/admin" className="profile-menu-item" onClick={() => setIsProfileMenuOpen(false)}>
                      Admin Moderation
                    </Link>
                  )}
                  <button type="button" className="profile-menu-item profile-menu-button" onClick={handleSignOut}>
                    Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <div className="page-content">{children}</div>
      </div>
    </div>
  )
}
