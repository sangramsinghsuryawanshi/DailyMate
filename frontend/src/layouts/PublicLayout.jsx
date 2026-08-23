import { useState, useEffect, useRef } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Button } from '../design-system'
import Footer from '../components/Footer'
import { useAuth } from '../hooks/useAuth'
import './PublicLayout.css'

export const PUBLIC_NAV_ITEMS = [
  { label: 'Home', path: '/' },
  { label: 'Features', path: '/#features' },
  { label: 'About', path: '/about' },
  { label: 'Pricing', path: '/pricing' },
]

export const AUTHENTICATED_NAV_ITEMS = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Assistant', path: '/assistant' },
  { label: 'Expenses', path: '/expenses' },
  { label: 'Medicine', path: '/medicines' },
  { label: 'Community', path: '/community-complaints' },
  { label: 'Marketplace', path: '/marketplace' },
  { label: 'Notifications', path: '/notifications' },
]

export const SECONDARY_NAV_ITEMS = [
  { label: 'About', path: '/about' },
  { label: 'How It Works', path: '/how-it-works' },
  { label: 'Pricing', path: '/pricing' },
  { label: 'FAQ', path: '/faq' },
  { label: 'Contact', path: '/contact' },
  { label: 'Privacy Policy', path: '/privacy' },
  { label: 'Terms of Service', path: '/terms' },
]

function useSafeAuth() {
  try {
    return useAuth()
  } catch {
    return { user: null, isAuthenticated: false }
  }
}

export function PublicLayout({ children }) {
  const location = useLocation()
  const { user } = useSafeAuth()
  const isAuthenticated = Boolean(user)

  const [isMoreOpen, setIsMoreOpen] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const moreRef = useRef(null)

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (moreRef.current && !moreRef.current.contains(event.target)) {
        setIsMoreOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Close menus on route change
  useEffect(() => {
    setIsMoreOpen(false)
    setIsMobileMenuOpen(false)
  }, [location.pathname])

  const primaryNav = isAuthenticated ? AUTHENTICATED_NAV_ITEMS : PUBLIC_NAV_ITEMS

  const isLinkActive = (path) => {
    if (path === '/') return location.pathname === '/'
    if (path.startsWith('/#')) return false
    return location.pathname.startsWith(path)
  }

  return (
    <div className="dm-public-shell">
      <header className="dm-public-header" role="banner">
        <div className="dm-public-header__container">
          {/* Brand Logo */}
          <Link to={isAuthenticated ? "/dashboard" : "/"} className="dm-public-brand" aria-label="DailyMate Home">
            <img src="/images/DailyMateIcon.png" alt="DailyMate" className="dm-public-logo" />
            <span>DailyMate</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="dm-public-nav" aria-label={isAuthenticated ? 'Application Navigation' : 'Public Navigation'}>
            {primaryNav.map((item) => {
              const active = isLinkActive(item.path)
              const isHash = item.path.startsWith('/#')

              if (isHash) {
                return (
                  <a
                    key={item.label}
                    href={item.path}
                    className="dm-public-nav__link"
                  >
                    {item.label}
                  </a>
                )
              }

              return (
                <Link
                  key={item.label}
                  to={item.path}
                  className={`dm-public-nav__link ${active ? 'dm-public-nav__link--active' : ''}`}
                  aria-current={active ? 'page' : undefined}
                >
                  {item.label}
                </Link>
              )
            })}

            {/* Secondary "More ▾" Dropdown */}
            <div className="dm-public-more-dropdown" ref={moreRef}>
              <button
                type="button"
                className={`dm-public-more-btn ${isMoreOpen ? 'dm-public-more-btn--open' : ''}`}
                onClick={() => setIsMoreOpen((prev) => !prev)}
                aria-expanded={isMoreOpen}
                aria-haspopup="true"
                aria-label="More options"
              >
                <span>More</span>
                <svg
                  className={`dm-public-chevron ${isMoreOpen ? 'dm-public-chevron--rotated' : ''}`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                  aria-hidden="true"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {isMoreOpen && (
                <div className="dm-public-dropdown-menu" role="menu">
                  {SECONDARY_NAV_ITEMS.map((item) => (
                    <Link
                      key={item.label}
                      to={item.path}
                      className={`dm-public-dropdown-item ${isLinkActive(item.path) ? 'dm-public-dropdown-item--active' : ''}`}
                      role="menuitem"
                      onClick={() => setIsMoreOpen(false)}
                    >
                      {item.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </nav>

          {/* Desktop Right Actions */}
          <div className="dm-public-actions">
            {isAuthenticated ? (
              <div className="dm-public-auth-pill">
                <Link to="/profile" className="dm-public-user-link" aria-label="Go to User Profile">
                  <div className="dm-public-user-avatar">
                    {(user?.name || user?.email || 'U').charAt(0).toUpperCase()}
                  </div>
                  <span className="dm-public-user-name">
                    {user?.name || user?.email?.split('@')[0] || 'My Account'}
                  </span>
                </Link>
              </div>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" size="sm">
                    Sign In
                  </Button>
                </Link>
                <Link to="/register">
                  <Button variant="primary" size="sm">
                    Get Started
                  </Button>
                </Link>
              </>
            )}

            {/* Mobile Menu Toggle Button */}
            <button
              type="button"
              className="dm-public-mobile-toggle"
              onClick={() => setIsMobileMenuOpen((prev) => !prev)}
              aria-label={isMobileMenuOpen ? 'Close Menu' : 'Open Menu'}
              aria-expanded={isMobileMenuOpen}
            >
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                {isMobileMenuOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {isMobileMenuOpen && (
          <div className="dm-public-mobile-drawer" role="dialog" aria-label="Mobile Navigation Menu">
            <div className="dm-public-mobile-links">
              {primaryNav.map((item) => (
                <Link
                  key={item.label}
                  to={item.path}
                  className={`dm-public-mobile-item ${isLinkActive(item.path) ? 'dm-public-mobile-item--active' : ''}`}
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  {item.label}
                </Link>
              ))}

              <div className="dm-public-mobile-divider" />
              <span className="dm-public-mobile-group-title">More Pages</span>

              {SECONDARY_NAV_ITEMS.map((item) => (
                <Link
                  key={item.label}
                  to={item.path}
                  className={`dm-public-mobile-item ${isLinkActive(item.path) ? 'dm-public-mobile-item--active' : ''}`}
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  {item.label}
                </Link>
              ))}

              <div className="dm-public-mobile-divider" />

              {isAuthenticated ? (
                <Link
                  to="/profile"
                  className="dm-public-mobile-item dm-public-mobile-item--highlight"
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  👤 Profile & Settings
                </Link>
              ) : (
                <div className="dm-public-mobile-auth-btns">
                  <Link to="/login" onClick={() => setIsMobileMenuOpen(false)}>
                    <Button variant="outline" size="md" style={{ width: '100%' }}>
                      Sign In
                    </Button>
                  </Link>
                  <Link to="/register" onClick={() => setIsMobileMenuOpen(false)}>
                    <Button variant="primary" size="md" style={{ width: '100%' }}>
                      Get Started
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="dm-public-main" id="public-main-content">
        {children}
      </main>

      {/* Shared Modern Footer */}
      <Footer />
    </div>
  )
}

export default PublicLayout
