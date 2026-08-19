import { useMemo, useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import MainLayout from '../../layouts/MainLayout'
import { getNotifications, updateNotification, deleteNotification } from '../services/notificationsApi'

export default function NotificationsPage() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState('all') // 'all' | 'unread'

  const PAGE_SIZE = 20
  const [page, setPage] = useState(0)
  const { data: pageData = { content: [], totalElements: 0, page: 0, size: PAGE_SIZE }, isLoading, isError, refetch } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => getNotifications(page, PAGE_SIZE),
  })

  const [accumulated, setAccumulated] = useState(() => [])

  // accumulate pages
  useEffect(() => {
    if (!pageData) return
    if (pageData.page === 0) {
      setAccumulated(pageData.content)
    } else if (page === pageData.page) {
      setAccumulated((current) => {
        const ids = new Set(current.map((i) => i.id))
        const next = pageData.content.filter((i) => !ids.has(i.id))
        return [...current, ...next]
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageData])

  const data = accumulated
  const unreadCount = useMemo(() => data.filter((n) => !n.read).length, [data])

  const markReadMutation = useMutation({
    mutationFn: ({ id }) => updateNotification(id, { read: true }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const markAllMutation = useMutation({
    mutationFn: () => {
      // Use new bulk endpoint when available
      return fetch('/api/v1/notifications/mark-all-read', { method: 'POST' }).then((res) => {
        if (!res.ok) throw new Error('Unable to mark all read')
        return res
      })
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const dismissMutation = useMutation({
    mutationFn: (id) => deleteNotification(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  function handleMarkAll() {
    if (data.filter((n) => !n.read).length === 0) return
    markAllMutation.mutate()
  }

  const visible = useMemo(() => {
    if (filter === 'unread') return data.filter((n) => !n.read)
    return data
  }, [data, filter])

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading notifications…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state">
        <h1>We couldn't load your notifications</h1>
        <p className="muted">Please try again.</p>
        <div style={{ marginTop: '1rem' }}>
          <Button onClick={() => refetch()}>Try again</Button>
        </div>
      </main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Inbox</p>
          <h1>Notifications</h1>
          <p className="subtle-text">Stay up to date with important updates and reminders.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="notifications-grid">
        <div className="panel">
          <div className="panel-header">
            <div>
              <strong>Notifications</strong>
              <div className="small-muted">{pageData.totalElements} total • {unreadCount} unread</div>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <div className="pill-row" role="tablist" aria-label="Notification filters">
                <button type="button" className={`pill ${filter === 'all' ? 'active' : ''}`} onClick={() => setFilter('all')}>All</button>
                <button type="button" className={`pill ${filter === 'unread' ? 'active' : ''}`} onClick={() => setFilter('unread')}>Unread</button>
              </div>

              {unreadCount > 0 && (
                <Button onClick={handleMarkAll} disabled={markAllMutation.isPending}>{markAllMutation.isPending ? 'Marking…' : 'Mark all as read'}</Button>
              )}
            </div>
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {visible.length === 0 ? (
              <div className="empty-state">
                <h3>You're all caught up 🎉</h3>
                <p className="muted">New DailyMate updates will appear here.</p>
                <div style={{ marginTop: '0.75rem' }}><Link to="/marketplace" className="btn btn-primary">Explore marketplace</Link></div>
              </div>
            ) : visible.map((item) => (
              <article key={item.id} className={`notification-item ${item.read ? 'is-read' : 'is-unread'}`}>
                <div className="notification-head">
                  <span className={`notification-badge ${item.type}`}>{item.type}</span>
                  <span className="small-muted">{item.read ? 'Read' : 'Unread'} • {new Date(item.createdAt).toLocaleString()}</span>
                </div>

                <h3 style={{ margin: '0.35rem 0' }}>{item.title}</h3>
                <p style={{ margin: '0.2rem 0 0.6rem' }}>{item.message}</p>

                <div className="notification-actions">
                  {!item.read && (
                    <Button variant="secondary" onClick={() => markReadMutation.mutate({ id: item.id })} disabled={markReadMutation.isPending}>Mark as read</Button>
                  )}
                  <Button variant="ghost" onClick={() => dismissMutation.mutate(item.id)} disabled={dismissMutation.isPending}>Dismiss</Button>
                  {item.targetUrl && (
                    item.targetUrl.startsWith('/') ? (
                      <Link to={item.targetUrl} className="btn btn-small btn-primary" onClick={() => { /* navigate and close */ }}>{'View'}</Link>
                    ) : (
                      <a href={item.targetUrl} className="btn btn-small btn-primary" target="_blank" rel="noreferrer">View</a>
                    )
                  )}
                </div>
              </article>
            ))}

            {accumulated.length < pageData.totalElements && (
              <div style={{ textAlign: 'center', marginTop: '0.8rem' }}>
                <Button onClick={() => setPage((p) => p + 1)}>Load more</Button>
              </div>
            )}

          </div>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Inbox summary</h2>
          </div>

          <div className="detail-list">
            <li><strong>Total:</strong> {data.length}</li>
            <li><strong>Unread:</strong> {unreadCount}</li>
          </div>

          <div style={{ marginTop: '1rem' }}>
            {unreadCount > 0 ? (
              <Button onClick={handleMarkAll} disabled={markAllMutation.isPending}>{markAllMutation.isPending ? 'Marking…' : 'Mark all as read'}</Button>
            ) : (
              <div className="small-muted">No outstanding notifications</div>
            )}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
