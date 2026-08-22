import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { createLocalEvent, deleteLocalEvent, getLocalEvents, updateLocalEvent } from '../services/localEventsApi'

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

  const { data: events = [], isLoading, isError } = useQuery({
    queryKey: ['local-events', selectedCategory],
    queryFn: () => getLocalEvents({ category: selectedCategory !== 'ALL' ? selectedCategory : undefined }),
  })

  const myEventsCount = useMemo(() => {
    if (!user?.id) return 0
    return events.filter((e) => e.userId === user.id).length
  }, [events, user])

  const visibleEvents = useMemo(() => {
    const now = new Date().getTime()
    if (activeTab === 'my') {
      if (!user?.id) return []
      return events.filter((e) => e.userId === user.id)
    }
    if (activeTab === 'past') {
      return events.filter((e) => new Date(e.eventDate).getTime() < now)
    }
    // upcoming: future dates or today
    return events.filter((e) => new Date(e.eventDate).getTime() >= now)
  }, [events, activeTab, user])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateLocalEvent(editingId, payload) : createLocalEvent(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save event. Please verify required fields.')
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, payload }) => updateLocalEvent(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
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

    const payload = {
      title: form.title.trim(),
      category: form.category.trim(),
      location: form.location.trim(),
      eventDate: new Date(form.eventDate).toISOString(),
      description: form.description.trim(),
    }

    if (editingId) {
      payload.status = form.status || 'PUBLISHED'
    }

    saveMutation.mutate(payload)
  }

  function handleEdit(eventItem) {
    setEditingId(eventItem.id)
    setFormError('')
    setForm({
      title: eventItem.title,
      category: eventItem.category,
      location: eventItem.location,
      eventDate: new Date(eventItem.eventDate).toISOString().slice(0, 16),
      description: eventItem.description,
      status: eventItem.status,
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading local events…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Local events are currently unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Community</p>
          <h1>Local Events</h1>
          <p className="subtle-text">Discover what is happening nearby, join neighborhood gatherings, and organize community events.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {/* Tabs and Categories */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          className={`btn ${activeTab === 'upcoming' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('upcoming')}
        >
          📅 Upcoming Events
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'past' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('past')}
        >
          📜 Past Events
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'my' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('my')}
        >
          👤 My Events ({myEventsCount})
        </button>
      </div>

      <section className="complaints-grid">
        {/* Main List */}
        <div>
          {/* Category Filter Pills */}
          <div className="panel" style={{ marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
              <strong style={{ fontSize: '0.9rem', marginRight: '0.35rem' }}>Category:</strong>
              <button
                type="button"
                className={`btn btn-small ${selectedCategory === 'ALL' ? 'btn-secondary' : 'btn-ghost'}`}
                onClick={() => setSelectedCategory('ALL')}
              >
                All
              </button>
              {EVENT_CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`btn btn-small ${selectedCategory === cat ? 'btn-secondary' : 'btn-ghost'}`}
                  onClick={() => setSelectedCategory(cat)}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          <div className="notification-list">
            {visibleEvents.length === 0 ? (
              <div className="panel empty-state">
                <h3>No events found</h3>
                <p className="muted">
                  {activeTab === 'my'
                    ? 'Events you organize will appear here.'
                    : selectedCategory !== 'ALL'
                    ? `No events currently match category "${selectedCategory}".`
                    : 'No community events scheduled in this view.'}
                </p>
              </div>
            ) : (
              visibleEvents.map((eventItem) => {
                const isOwner = user?.id && eventItem.userId === user.id
                const isCancelled = eventItem.status === 'CANCELLED'
                return (
                  <article key={eventItem.id} className="panel" style={{ marginBottom: '1rem', opacity: isCancelled ? 0.75 : 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div>
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem' }}>
                          <span className="notification-badge info">{eventItem.category}</span>
                          <span className={`status-pill ${eventItem.status.toLowerCase()}`}>{eventItem.status}</span>
                        </div>
                        <h3 style={{ margin: 0, fontSize: '1.25rem' }}>{eventItem.title}</h3>
                        <div className="small-muted" style={{ marginTop: '0.25rem' }}>
                          📍 {eventItem.location} · ⏰ {new Date(eventItem.eventDate).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
                        </div>
                      </div>
                    </div>

                    <p style={{ marginTop: '0.75rem', color: 'var(--color-text)', fontSize: '0.95rem', lineHeight: '1.55' }}>
                      {eventItem.description}
                    </p>

                    {isOwner && (
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
                        <Button variant="secondary" onClick={() => handleEdit(eventItem)}>
                          Edit
                        </Button>
                        {eventItem.status === 'PUBLISHED' ? (
                          <Button
                            variant="ghost"
                            onClick={() =>
                              statusMutation.mutate({
                                id: eventItem.id,
                                payload: { ...eventItem, status: 'CANCELLED' },
                              })
                            }
                          >
                            Cancel Event
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            onClick={() =>
                              statusMutation.mutate({
                                id: eventItem.id,
                                payload: { ...eventItem, status: 'PUBLISHED' },
                              })
                            }
                          >
                            Reopen Event
                          </Button>
                        )}
                        <Button variant="ghost" onClick={() => deleteMutation.mutate(eventItem.id)}>
                          Delete
                        </Button>
                      </div>
                    )}
                  </article>
                )
              })
            )}
          </div>
        </div>

        {/* Aside Form */}
        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit event' : 'Organize event'}</h2>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form className="notification-form" onSubmit={handleSubmit}>
            <Input label="Event title" name="title" value={form.title} onChange={handleChange} maxLength={120} required />

            <label className="field">
              <span>Category</span>
              <select name="category" value={form.category} onChange={handleChange}>
                {EVENT_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </label>

            <Input label="Location / Venue" name="location" value={form.location} onChange={handleChange} maxLength={160} required />

            <Input
              label="Date &amp; Time"
              type="datetime-local"
              name="eventDate"
              value={form.eventDate}
              onChange={handleChange}
              required
            />

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="3" maxLength={1000} />
            </label>

            <div className="profile-actions" style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : editingId ? 'Update event' : 'Publish event'}
              </Button>
              {editingId && (
                <Button type="button" variant="ghost" onClick={handleCancelEdit}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </aside>
      </section>
    </MainLayout>
  )
}
