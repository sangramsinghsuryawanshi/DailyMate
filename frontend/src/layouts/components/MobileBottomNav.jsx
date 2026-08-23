import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Drawer } from '../../design-system'
import './MobileBottomNav.css'

export const MOBILE_MORE_ITEMS = [
  { label: 'Medicines', to: '/medicines', icon: '💊', desc: 'Schedules and reminders' },
  { label: 'Marketplace', to: '/marketplace', icon: '🛠️', desc: 'Find trusted local services' },
  { label: 'Blood Donation', to: '/blood', icon: '🩸', desc: 'Urgent donor requests' },
  { label: 'Emergency ICE', to: '/emergency-contacts', icon: '🚨', desc: 'Hotlines & trusted contacts' },
  { label: 'Complaints', to: '/community-complaints', icon: '📢', desc: 'Report civic issues' },
  { label: 'Lost & Found', to: '/lost-found', icon: '🔍', desc: 'Community missing items' },
  { label: 'Events', to: '/events', icon: '📅', desc: 'Local tournaments & meetups' },
  { label: 'Jobs', to: '/jobs', icon: '💼', desc: 'Community employment listings' },
  { label: 'Grocery', to: '/grocery', icon: '🛒', desc: 'Weekly supplies checklist' },
  { label: 'Notifications', to: '/notifications', icon: '🔔', desc: 'Alerts & updates' },
  { label: 'Profile', to: '/profile', icon: '👤', desc: 'Account & preferences' },
]

export function MobileBottomNav({ onOpenQuickAdd, user }) {
  const location = useLocation()
  const [isMoreOpen, setIsMoreOpen] = useState(false)

  const isMoreActive = MOBILE_MORE_ITEMS.some((item) => location.pathname.startsWith(item.to))

  return (
    <>
      <nav className="dm-mobile-bottom-nav" aria-label="Mobile Navigation">
        <Link
          to="/dashboard"
          className={`dm-mobile-nav-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
        >
          <span className="dm-mobile-nav-item__icon">📊</span>
          <span className="dm-mobile-nav-item__label">Home</span>
        </Link>

        <Link
          to="/assistant"
          className={`dm-mobile-nav-item ${location.pathname === '/assistant' ? 'active' : ''}`}
        >
          <span className="dm-mobile-nav-item__icon">✨</span>
          <span className="dm-mobile-nav-item__label">Assistant</span>
        </Link>

        {/* Center Prominent Quick Add Trigger */}
        <button
          type="button"
          className="dm-mobile-nav-quick-add"
          onClick={onOpenQuickAdd}
          aria-label="Universal Quick Action"
          title="Universal Quick Action"
        >
          <span className="dm-mobile-nav-quick-add__plus">+</span>
        </button>

        <Link
          to="/expenses"
          className={`dm-mobile-nav-item ${location.pathname === '/expenses' ? 'active' : ''}`}
        >
          <span className="dm-mobile-nav-item__icon">💰</span>
          <span className="dm-mobile-nav-item__label">Expenses</span>
        </Link>

        <button
          type="button"
          className={`dm-mobile-nav-item ${isMoreActive || isMoreOpen ? 'active' : ''}`}
          onClick={() => setIsMoreOpen(true)}
          aria-label="More options"
        >
          <span className="dm-mobile-nav-item__icon">☰</span>
          <span className="dm-mobile-nav-item__label">More</span>
        </button>
      </nav>

      {/* Mobile More Drawer */}
      <Drawer
        isOpen={isMoreOpen}
        onClose={() => setIsMoreOpen(false)}
        position="bottom"
        title="More DailyMate Tools"
        description="Explore community, healthcare, and service modules"
      >
        <div className="dm-mobile-more-grid">
          {MOBILE_MORE_ITEMS.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              onClick={() => setIsMoreOpen(false)}
              className="dm-mobile-more-card"
            >
              <span className="dm-mobile-more-card__icon">{item.icon}</span>
              <div className="dm-mobile-more-card__info">
                <strong>{item.label}</strong>
                <span>{item.desc}</span>
              </div>
            </Link>
          ))}
          {user?.role === 'ADMIN' && (
            <Link
              to="/admin"
              onClick={() => setIsMoreOpen(false)}
              className="dm-mobile-more-card dm-mobile-more-card--admin"
            >
              <span className="dm-mobile-more-card__icon">🛡️</span>
              <div className="dm-mobile-more-card__info">
                <strong>Admin Hub</strong>
                <span>System moderation & users</span>
              </div>
            </Link>
          )}
        </div>
      </Drawer>
    </>
  )
}

export default MobileBottomNav
