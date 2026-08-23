import { useState, useMemo, useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { logout as logoutRequest } from '../auth/services/authApi'
import { useAuth } from '../hooks/useAuth'
import { getProviders } from '../marketplace/services/marketplaceApi'
import { getNotifications } from '../notification/services/notificationsApi'
import Sidebar from './components/Sidebar'
import TopBar from './components/TopBar'
import MobileBottomNav from './components/MobileBottomNav'
import QuickActionMenu from './components/QuickActionMenu'
import GlobalSearchDialog from './components/GlobalSearchDialog'
import Footer from '../components/Footer'
import './components/AppShell.css'

const searchSuggestions = [
  { id: 'dashboard', title: 'Dashboard', category: 'Overview', description: 'Review your day, reminders, and quick actions.', to: '/dashboard', tags: ['overview', 'daily tasks'] },
  { id: 'assistant', title: 'AI Assistant', category: 'AI', description: 'Chat with AI for life organization and bulk actions.', to: '/assistant', tags: ['chat', 'bot', 'actions'] },
  { id: 'expenses', title: 'Expenses', category: 'Finance', description: 'Log transactions, inspect monthly totals and budgets.', to: '/expenses', tags: ['spending', 'budget', 'money'] },
  { id: 'medicines', title: 'Medicines', category: 'Health', description: 'Track medication reminders and daily schedules.', to: '/medicines', tags: ['medicine', 'reminders', 'pills'] },
  { id: 'grocery', title: 'Grocery', category: 'Supplies', description: 'Manage shopping checklists and price comparisons.', to: '/grocery', tags: ['grocery', 'shopping', 'supplies', 'food'] },
  { id: 'jobs', title: 'Jobs', category: 'Careers', description: 'Explore neighborhood job vacancies or hire local talent.', to: '/jobs', tags: ['jobs', 'careers', 'hiring', 'work'] },
  { id: 'marketplace', title: 'Marketplace', category: 'Services', description: 'Browse trusted local providers and service categories.', to: '/marketplace', tags: ['in-home help', 'plumber', 'tutor'] },
  { id: 'blood', title: 'Blood Donation', category: 'Health', description: 'Find urgent blood requests or locate community donation centers.', to: '/blood', tags: ['blood donor', 'urgent blood', 'donation'] },
  { id: 'emergency', title: 'Emergency Contacts', category: 'Support', description: 'Immediate emergency hotlines and personal ICE contacts.', to: '/emergency-contacts', tags: ['emergency', 'police', 'ambulance', 'fire', 'help'] },
  { id: 'complaints', title: 'Complaints', category: 'Community', description: 'Report civic issues and infrastructure maintenance.', to: '/community-complaints', tags: ['complaints', 'civic', 'pothole'] },
  { id: 'events', title: 'Local Events', category: 'Community', description: 'Discover neighborhood meetups and community activities.', to: '/events', tags: ['events', 'meetup', 'sports'] },
  { id: 'lost-found', title: 'Lost & Found', category: 'Community', description: 'Report missing items or help reunite community belongings.', to: '/lost-found', tags: ['missing', 'found', 'lost'] },
  { id: 'notifications', title: 'Notifications', category: 'Inbox', description: 'Track updates, reminders, and community alerts.', to: '/notifications', tags: ['updates', 'alerts'] },
  { id: 'profile', title: 'Profile', category: 'Account', description: 'Update your personal details and profile preferences.', to: '/profile', tags: ['account', 'settings'] },
  { id: 'about', title: 'About Us', category: 'Company', description: 'Explore DailyMate mission, core values, and features.', to: '/about', tags: ['about', 'company', 'mission', 'vision'] },
]

export default function MainLayout({ children }) {
  const location = useLocation()
  const { user, refreshToken, signOut } = useAuth()

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    try {
      return localStorage.getItem('dailymate.sidebar_collapsed') === 'true'
    } catch (_) {
      return false
    }
  })

  const [isQuickAddOpen, setIsQuickAddOpen] = useState(false)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const toggleSidebarCollapse = () => {
    setIsSidebarCollapsed((prev) => {
      const next = !prev
      try {
        localStorage.setItem('dailymate.sidebar_collapsed', String(next))
      } catch (_) {}
      return next
    })
  }

  // TanStack Queries for Providers & Notifications
  const { data: providers = [] } = useQuery({
    queryKey: ['marketplace-providers', 'shell-search'],
    queryFn: getProviders,
    staleTime: 60_000,
    retry: 1,
  })

  const { data: bellData = { content: [], totalElements: 0 }, isLoading: notificationsLoading } = useQuery({
    queryKey: ['notifications', 'bell'],
    queryFn: () => getNotifications(0, 5),
    enabled: !!user,
    staleTime: 30_000,
    refetchInterval: 60_000,
    retry: 1,
  })

  const notifications = bellData.content ?? []
  const unreadCount = useMemo(
    () => (bellData.content ?? []).filter((item) => !item.read).length,
    [bellData]
  )

  async function handleSignOut() {
    if (refreshToken) {
      try {
        await logoutRequest(refreshToken)
      } catch {
        // Keep local sign-out working
      }
    }
    signOut()
  }

  return (
    <div className="dm-app-shell">
      {/* Desktop & Tablet Sidebar */}
      <Sidebar
        isCollapsed={isSidebarCollapsed}
        onToggleCollapse={toggleSidebarCollapse}
        onOpenQuickAdd={() => setIsQuickAddOpen(true)}
        user={user}
      />

      <div className="dm-app-shell__main-area">
        {/* Universal TopBar */}
        <TopBar
          user={user}
          onSignOut={handleSignOut}
          onOpenSearch={() => setIsSearchOpen(true)}
          onOpenQuickAdd={() => setIsQuickAddOpen(true)}
          unreadCount={unreadCount}
          notifications={notifications}
          notificationsLoading={notificationsLoading}
        />

        {/* Content Viewport */}
        <main className="dm-app-shell__content" id="main-content">
          <div style={{ flex: 1, minHeight: 'calc(100vh - 220px)' }}>
            {children}
          </div>

          {/* Seamless, Aligned Responsive Footer */}
          <Footer />
        </main>
      </div>

      {/* Mobile Bottom Navigation with Floating Quick Add */}
      <MobileBottomNav
        onOpenQuickAdd={() => setIsQuickAddOpen(true)}
        user={user}
      />

      {/* Universal Quick Action Menu */}
      <QuickActionMenu
        isOpen={isQuickAddOpen}
        onClose={() => setIsQuickAddOpen(false)}
        isMobile={isMobile}
      />

      {/* Global ⌘K Search Dialog */}
      <GlobalSearchDialog
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        providers={providers}
        searchSuggestions={searchSuggestions}
      />
    </div>
  )
}
