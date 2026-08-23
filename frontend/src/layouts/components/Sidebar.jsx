import { Link, useLocation } from 'react-router-dom'
import { Button } from '../../design-system'
import './Sidebar.css'

export const PRIMARY_NAV_ITEMS = [
  { label: 'Dashboard', to: '/dashboard', icon: '📊', domain: 'default' },
  { label: 'AI Assistant', to: '/assistant', icon: '✨', domain: 'ai' },
  { label: 'Expenses', to: '/expenses', icon: '💰', domain: 'expense' },
  { label: 'Medicines', to: '/medicines', icon: '💊', domain: 'health' },
  { label: 'Grocery', to: '/grocery', icon: '🛒', domain: 'expense' },
  { label: 'Jobs', to: '/jobs', icon: '💼', domain: 'community' },
  { label: 'Marketplace', to: '/marketplace', icon: '🛠️', domain: 'marketplace' },
  { label: 'Blood Donation', to: '/blood', icon: '🩸', domain: 'emergency' },
  { label: 'Emergency ICE', to: '/emergency-contacts', icon: '🚨', domain: 'emergency' },
  { label: 'Complaints', to: '/community-complaints', icon: '📢', domain: 'community' },
  { label: 'Lost & Found', to: '/lost-found', icon: '🔍', domain: 'community' },
  { label: 'Events', to: '/events', icon: '📅', domain: 'community' },
  { label: 'Notifications', to: '/notifications', icon: '🔔', domain: 'default' },
  { label: 'Profile', to: '/profile', icon: '👤', domain: 'default' },
  { label: 'About Us', to: '/about', icon: 'ℹ️', domain: 'default' },
]

export function Sidebar({
  isCollapsed = false,
  onToggleCollapse,
  onOpenQuickAdd,
  user,
}) {
  const location = useLocation()

  const allNavItems = user?.role === 'ADMIN'
    ? [...PRIMARY_NAV_ITEMS, { label: 'Admin Hub', to: '/admin', icon: '🛡️', domain: 'default' }]
    : PRIMARY_NAV_ITEMS

  return (
    <aside className={`dm-sidebar ${isCollapsed ? 'dm-sidebar--collapsed' : ''}`} aria-label="Main Sidebar">
      {/* Brand Header */}
      <div className="dm-sidebar__brand">
        <Link to="/dashboard" className="dm-sidebar__brand-link">
          <img src="/images/DailyMate.png" alt="DailyMate" className="dm-sidebar__logo" />
          {!isCollapsed && <span className="dm-sidebar__brand-title">DailyMate</span>}
        </Link>
        <button
          type="button"
          className="dm-sidebar__toggle-btn"
          onClick={onToggleCollapse}
          aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? '›' : '‹'}
        </button>
      </div>

      {/* Quick Action Trigger Button */}
      <div className="dm-sidebar__quick-action">
        <Button
          variant="primary"
          size={isCollapsed ? 'sm' : 'md'}
          fullWidth
          onClick={onOpenQuickAdd}
          iconLeft="+"
          title="Universal Quick Actions"
          aria-label="Universal Quick Actions"
        >
          {!isCollapsed && 'Quick Action'}
        </Button>
      </div>

      {/* Navigation List */}
      <nav className="dm-sidebar__nav" aria-label="Primary Navigation">
        {allNavItems.map((item) => {
          const isActive =
            location.pathname === item.to ||
            (item.to !== '/dashboard' && location.pathname.startsWith(item.to))

          return (
            <Link
              key={item.to}
              to={item.to}
              className={`dm-sidebar__item ${isActive ? 'dm-sidebar__item--active' : ''} dm-sidebar__item--${item.domain}`}
              title={isCollapsed ? item.label : undefined}
              aria-current={isActive ? 'page' : undefined}
            >
              <span className="dm-sidebar__item-icon" aria-hidden="true">
                {item.icon}
              </span>
              {!isCollapsed && (
                <span className="dm-sidebar__item-label">{item.label}</span>
              )}
            </Link>
          )
        })}
      </nav>

      {/* Sidebar Footer User Info */}
      <div className="dm-sidebar__footer">
        <Link to="/profile" className="dm-sidebar__user-link" title={isCollapsed ? 'View Profile' : undefined}>
          <div className="dm-sidebar__user-avatar">
            {[user?.firstName, user?.lastName].filter(Boolean).map((n) => n[0]).join('').toUpperCase() || 'DM'}
          </div>
          {!isCollapsed && (
            <div className="dm-sidebar__user-info">
              <strong className="dm-sidebar__user-name">
                {[user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'DailyMate User'}
              </strong>
              <span className="dm-sidebar__user-role">{user?.role || 'Member'}</span>
            </div>
          )}
        </Link>
      </div>
    </aside>
  )
}

export default Sidebar
