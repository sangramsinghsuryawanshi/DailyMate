import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { createLocalEvent, deleteLocalEvent, getLocalEvents, updateLocalEvent } from '../services/localEventsApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Input,
  Select,
  Card,
  StatCard,
  Badge,
  Pagination,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import './LocalEventsPage.css'

const EVENT_CATEGORIES = ['Volunteer', 'Sports', 'Workshop', 'Cultural', 'Music', 'Meetup', 'Social', 'Other']

const defaultForm = {
  title: '',
  category: 'Meetup',
  location: '',
  eventDate: '',
  description: '',
}

export default function LocalEventsPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [activeTab, setActiveTab] = useState('upcoming') // 'upcoming' | 'past' | 'my'
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [formError, setFormError] = useState('')

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'local_events' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['local-events', { page, pageSize, selectedCategory, activeTab }],
    queryFn: () =>
      getLocalEvents({
        page,
        size: pageSize,
        category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      }),
    placeholderData: (previousData) => previousData,
  })

  const events = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? events.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  // Filter events by tab (upcoming, past, my)
  const filteredEvents = useMemo(() => {
    const now = new Date()
    return events.filter((evt) => {
      const evtDate = new Date(evt.eventDate)
      if (activeTab === 'upcoming') {
        return evtDate >= now && evt.status !== 'CANCELLED'
      }
      if (activeTab === 'past') {
        return evtDate < now || evt.status === 'COMPLETED'
      }
      if (activeTab === 'my') {
        return user?.id && evt.userId === user.id
      }
      return true
    })
  }, [events, activeTab, user])

  const myEventsCount = useMemo(() => {
    if (!user?.id) return 0
    return events.filter((e) => e.userId === user.id).length
  }, [events, user])

  const upcomingCount = useMemo(() => {
    const now = new Date()
    return events.filter((e) => new Date(e.eventDate) >= now && e.status !== 'CANCELLED').length
  }, [events])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateLocalEvent(editingId, payload) : createLocalEvent(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      trackEvent('event_saved')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save event. Please ensure you are logged in.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteLocalEvent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      if (editingId) {
        setEditingId(null)
        setForm(defaultForm)
      }
      trackEvent('event_deleted')
    },
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    if (!form.title.trim() || !form.category.trim() || !form.location.trim() || !form.eventDate || !form.description.trim()) {
      setFormError('Please fill out all required fields.')
      return
    }

    saveMutation.mutate({
      title: form.title.trim(),
      category: form.category.trim(),
      location: form.location.trim(),
      eventDate: form.eventDate,
      description: form.description.trim(),
    })
  }

  function handleEdit(evt) {
    setEditingId(evt.id)
    setFormError('')
    setForm({
      title: evt.title,
      category: evt.category,
      location: evt.location,
      eventDate: evt.eventDate ? evt.eventDate.slice(0, 16) : '',
      description: evt.description,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  const cancelEventMutation = useMutation({
    mutationFn: (evt) => updateLocalEvent(evt.id, { ...evt, status: 'CANCELLED' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      trackEvent('event_cancelled')
    },
  })

  function handleCancelEvent(evt) {
    cancelEventMutation.mutate(evt)
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading community events…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load local events</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-events-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Neighborhood & Society</span>
              <h1 className="dm-page-main-title">Local Events</h1>
              <p className="dm-page-subtitle">
                Discover nearby cultural gatherings, sports tournaments, volunteer drives, workshops, and society meetups.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/dashboard">
                <Button variant="ghost" size="md">
                  Back to dashboard
                </Button>
              </Link>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="dm-events-stats-grid">
            <StatCard
              domain="community"
              label="Upcoming Events"
              value={String(upcomingCount)}
              trend="Scheduled"
              trendDirection="up"
              trendLabel="Neighborhood activities"
              icon="📅"
            />
            <StatCard
              domain="community"
              label="My Hosted Events"
              value={String(myEventsCount)}
              trend="Organizer"
              trendDirection="neutral"
              trendLabel="Created by you"
              icon="👑"
            />
            <StatCard
              domain="community"
              label="Categories"
              value={String(EVENT_CATEGORIES.length)}
              trend="Diverse"
              trendDirection="up"
              trendLabel="Sports to Arts"
              icon="🎨"
            />
            <StatCard
              domain="community"
              label="Participation"
              value="Free / Public"
              trend="Community"
              trendDirection="up"
              trendLabel="Open neighborhood"
              icon="🤝"
            />
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-provider-profile-layout">
            {/* Left Column: Events Feed */}
            <div>
              {/* Tab Bar & Category Filters */}
              <div className="dm-events-toolbar">
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'upcoming' ? 'active' : ''}`}
                    onClick={() => setActiveTab('upcoming')}
                  >
                    Upcoming
                  </button>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'past' ? 'active' : ''}`}
                    onClick={() => setActiveTab('past')}
                  >
                    Past Events
                  </button>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'my' ? 'active' : ''}`}
                    onClick={() => setActiveTab('my')}
                  >
                    My Events ({myEventsCount})
                  </button>
                </div>

                <div style={{ display: 'flex', gap: '0.35rem', overflowX: 'auto', paddingBottom: '0.2rem' }}>
                  <button
                    type="button"
                    className={`dm-blood-group-pill ${selectedCategory === 'ALL' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('ALL')}
                  >
                    All
                  </button>
                  {EVENT_CATEGORIES.map((cat) => (
                    <button
                      key={cat}
                      type="button"
                      className={`dm-blood-group-pill ${selectedCategory === cat ? 'active' : ''}`}
                      onClick={() => setSelectedCategory(cat)}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
              </div>

              {filteredEvents.length === 0 ? (
                <div className="dm-empty-dashboard-box">
                  <h3>No events found</h3>
                  <p>
                    {activeTab === 'my'
                      ? 'You have not hosted or created any local events yet.'
                      : 'No events matching your selected filters.'}
                  </p>
                </div>
              ) : (
                <div className="dm-events-card-feed">
                  {filteredEvents.map((evt) => {
                    const isOwner = user?.id && evt.userId === user.id
                    const eventTimeStr = evt.eventDate ? new Date(evt.eventDate).toLocaleDateString('en-IN', {
                      weekday: 'short',
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    }) : ''

                    return (
                      <article key={evt.id} className="dm-event-card">
                        <div className="dm-event-card-header">
                          <div>
                            <h3 className="dm-event-card-title">{evt.title}</h3>
                            <div className="dm-event-card-meta" style={{ marginTop: '0.25rem' }}>
                              <span>📁 {evt.category}</span>
                              <span>📍 {evt.location}</span>
                              {eventTimeStr && <span>⏰ {eventTimeStr}</span>}
                            </div>
                          </div>
                          <Badge
                            variant={evt.status === 'PUBLISHED' ? 'primary' : evt.status === 'CANCELLED' ? 'danger' : 'neutral'}
                            size="md"
                          >
                            {evt.status}
                          </Badge>
                        </div>

                        <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', lineHeight: 1.5, margin: 0 }}>
                          {evt.description}
                        </p>

                        {isOwner && (
                          <div className="dm-complaint-actions-row">
                            <Button variant="ghost" size="sm" onClick={() => handleEdit(evt)}>
                              Edit
                            </Button>
                            {evt.status === 'PUBLISHED' && (
                              <Button variant="ghost" size="sm" onClick={() => handleCancelEvent(evt)}>
                                Cancel Event
                              </Button>
                            )}
                            <Button variant="danger" size="sm" onClick={() => deleteMutation.mutate(evt.id)}>
                              Delete
                            </Button>
                          </div>
                        )}
                      </article>
                    )
                  })}
                </div>
              )}

              {/* Server-Driven Pagination */}
              <Pagination
                page={page}
                totalPages={totalPages}
                totalElements={totalElements}
                pageSize={pageSize}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                disabled={isLoading}
              />
            </div>

            {/* Right Column: Sticky Form Card */}
            <div>
              <div className="dm-provider-side-card">
                <div>
                  <span className="dm-section-eyebrow">
                    {editingId ? 'Edit Event' : 'Host an Event'}
                  </span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                    {editingId ? 'Edit community event' : 'Publish local event'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Organize cleanup drives, festival celebrations, cricket matches, yoga classes, or book clubs.
                  </p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="dm-provider-modal-form">
                  <Input
                    id="evt-title"
                    name="title"
                    label="Event title"
                    placeholder="e.g. Community Cleanup Drive"
                    value={form.title}
                    onChange={handleChange}
                    required
                  />

                  <div className="dm-select-group">
                    <label htmlFor="evt-category" className="dm-input-label">
                      Category
                    </label>
                    <select
                      id="evt-category"
                      name="category"
                      className="dm-select"
                      value={form.category}
                      onChange={handleChange}
                    >
                      {EVENT_CATEGORIES.map((cat) => (
                        <option key={cat} value={cat}>
                          {cat}
                        </option>
                      ))}
                    </select>
                  </div>

                  <Input
                    id="evt-location"
                    name="location"
                    label="Location / Venue"
                    placeholder="e.g. Riverside Park / Clubhouse"
                    value={form.location}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="evt-date"
                    name="eventDate"
                    label="Date & Time"
                    type="datetime-local"
                    value={form.eventDate}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="evt-description"
                    name="description"
                    label="Description"
                    placeholder="Provide event schedule, rules, and what attendees should bring"
                    value={form.description}
                    onChange={handleChange}
                    required
                  />

                  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                    {editingId && (
                      <Button type="button" variant="ghost" fullWidth onClick={handleCancelEdit}>
                        Cancel Edit
                      </Button>
                    )}
                    <Button
                      type="submit"
                      variant="primary"
                      fullWidth
                      isLoading={saveMutation.isPending}
                    >
                      {editingId ? 'Save Changes' : 'Publish event'}
                    </Button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
