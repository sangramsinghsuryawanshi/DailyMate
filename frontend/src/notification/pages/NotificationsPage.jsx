import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import {
  deleteNotification,
  getNotifications,
  markAllRead,
  updateNotification,
} from '../services/notificationsApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Card,
  StatCard,
  Badge,
  ResponsiveContainer,
} from '../../design-system'
import './NotificationsPage.css'

const PAGE_SIZE = 20
const DEFAULT_PAGE_DATA = { content: [], totalElements: 0, page: 0, size: PAGE_SIZE }

export default function NotificationsPage() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState('all') // 'all' | 'unread'
  const [page, setPage] = useState(0)
  const [accumulated, setAccumulated] = useState([])

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'notifications' })
  }, [])

  const { data: pageData = DEFAULT_PAGE_DATA, isLoading, isError, refetch } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => getNotifications(page, PAGE_SIZE),
  })

  // Append new page contents to accumulated list
  useEffect(() => {
    if (pageData?.content) {
      if (page === 0) {
        setAccumulated(pageData.content)
      } else {
        setAccumulated((prev) => {
          const ids = new Set(prev.map((n) => n.id))
          const fresh = pageData.content.filter((n) => !ids.has(n.id))
          return [...prev, ...fresh]
        })
      }
    }
  }, [pageData, page])

  const totalElements = pageData.totalElements ?? 0
  const unreadCount = useMemo(() => accumulated.filter((n) => !n.read).length, [accumulated])

  const displayedNotifications = useMemo(() => {
    if (filter === 'unread') {
      return accumulated.filter((n) => !n.read)
    }
    return accumulated
  }, [accumulated, filter])

  const hasMore = (page + 1) * PAGE_SIZE < totalElements

  // Mutations
  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateNotification(id, payload),
    onSuccess: (updated) => {
      setAccumulated((prev) => prev.map((n) => (n.id === updated.id ? updated : n)))
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllMutation = useMutation({
    mutationFn: markAllRead,
    onSuccess: () => {
      setAccumulated((prev) => prev.map((n) => ({ ...n, read: true })))
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => deleteNotification(id),
    onSuccess: (_, deletedId) => {
      setAccumulated((prev) => prev.filter((n) => n.id !== deletedId))
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  function handleToggleRead(n) {
    updateMutation.mutate({
      id: n.id,
      payload: {
        title: n.title,
        message: n.message,
        type: n.type,
        read: !n.read,
        targetType: n.targetType,
        targetId: n.targetId,
        targetUrl: n.targetUrl,
      },
    })
  }

  function handleDismiss(id) {
    deleteMutation.mutate(id)
  }

  if (isLoading && accumulated.length === 0) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading notifications…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError && accumulated.length === 0) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load notifications</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-notifications-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Communication & Alerts</span>
              <h1 className="dm-page-main-title">Notifications</h1>
              <p className="dm-page-subtitle">
                {unreadCount > 0 ? `${unreadCount} unread` : 'All notifications cleared'} · Real-time medicine reminders, community alerts, and platform messages.
              </p>
            </div>
            <div className="dm-page-header-actions">
              {unreadCount > 0 && (
                <Button
                  variant="outline"
                  size="md"
                  onClick={() => markAllMutation.mutate()}
                  isLoading={markAllMutation.isPending}
                >
                  Mark all as read
                </Button>
              )}
              <Link to="/dashboard">
                <Button variant="ghost" size="md">
                  Back to dashboard
                </Button>
              </Link>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="dm-notifications-stats-grid">
            <StatCard
              domain="notifications"
              label="Unread Alerts"
              value={String(unreadCount)}
              trend={unreadCount > 0 ? 'Pending' : 'Cleared'}
              trendDirection={unreadCount > 0 ? 'down' : 'up'}
              trendLabel="Requires attention"
              icon="🔔"
            />
            <StatCard
              domain="notifications"
              label="Total Received"
              value={String(totalElements)}
              trend="All-time"
              trendDirection="neutral"
              trendLabel="Inbox history"
              icon="📬"
            />
            <StatCard
              domain="notifications"
              label="Delivery Channels"
              value="In-App + Push"
              trend="Synced"
              trendDirection="up"
              trendLabel="Multi-device active"
              icon="📱"
            />
            <StatCard
              domain="notifications"
              label="System Health"
              value="100% Online"
              trend="Active"
              trendDirection="up"
              trendLabel="Real-time webhooks"
              icon="⚡"
            />
          </div>

          {/* Filter Toolbar */}
          <div className="dm-notifications-toolbar">
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                className={`dm-category-filter-btn ${filter === 'all' ? 'active' : ''}`}
                onClick={() => setFilter('all')}
              >
                All
              </button>
              <button
                type="button"
                className={`dm-category-filter-btn ${filter === 'unread' ? 'active' : ''}`}
                onClick={() => setFilter('unread')}
              >
                Unread
              </button>
            </div>

            {unreadCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => markAllMutation.mutate()}
                isLoading={markAllMutation.isPending}
              >
                Mark all as read
              </Button>
            )}
          </div>

          {/* Feed */}
          {displayedNotifications.length === 0 ? (
            <div className="dm-empty-dashboard-box">
              <h3>You're all caught up</h3>
              <p>
                {filter === 'unread'
                  ? "You have no unread reminders or alerts."
                  : 'Your notification inbox is clean and empty.'}
              </p>
            </div>
          ) : (
            <div className="dm-notification-items-feed">
              {displayedNotifications.map((n) => (
                <article key={n.id} className={`dm-notification-card ${!n.read ? 'unread' : ''}`}>
                  <div className="dm-notification-header-row">
                    <div>
                      <h3 className="dm-notification-title">{n.title}</h3>
                      <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', margin: '0.25rem 0 0 0', lineHeight: 1.5 }}>
                        {n.message}
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                      <Badge variant={n.type === 'reminder' ? 'warning' : 'neutral'} size="sm">
                        {n.type || 'info'}
                      </Badge>
                      {!n.read && <Badge variant="primary" size="sm">Unread</Badge>}
                    </div>
                  </div>

                  <div className="dm-notification-actions-row">
                    {n.targetUrl && (
                      <Link to={n.targetUrl} style={{ marginRight: 'auto' }}>
                        <Button variant="outline" size="sm">
                          Go to target
                        </Button>
                      </Link>
                    )}
                    <Button variant="ghost" size="sm" onClick={() => handleToggleRead(n)}>
                      {n.read ? 'Mark as unread' : 'Mark as read'}
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => handleDismiss(n.id)}>
                      Dismiss
                    </Button>
                  </div>
                </article>
              ))}

              {hasMore && (
                <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                  <Button variant="outline" size="md" onClick={() => setPage((p) => p + 1)}>
                    Load more notifications
                  </Button>
                </div>
              )}
            </div>
          )}
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
