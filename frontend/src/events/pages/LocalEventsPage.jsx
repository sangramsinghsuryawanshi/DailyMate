import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createLocalEvent, deleteLocalEvent, getLocalEvents, updateLocalEvent } from '../services/localEventsApi'

const defaultForm = {
  title: '',
  category: '',
  location: '',
  eventDate: '',
  description: '',
}

export default function LocalEventsPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['local-events'],
    queryFn: getLocalEvents,
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateLocalEvent(editingId, payload) : createLocalEvent(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteLocalEvent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['local-events'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(eventItem) {
    setEditingId(eventItem.id)
    setForm({
      title: eventItem.title,
      category: eventItem.category,
      location: eventItem.location,
      eventDate: new Date(eventItem.eventDate).toISOString().slice(0, 16),
      description: eventItem.description,
    })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading local events…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Local events unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Community</p>
          <h1>Local events</h1>
          <p className="subtle-text">Discover what is happening nearby and share a new neighbourhood event.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="events-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit event' : 'Add event'}</h2>
          </div>

          <form className="expense-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Title" name="title" value={form.title} onChange={handleChange} required />
              <Input label="Category" name="category" value={form.category} onChange={handleChange} required />
            </div>

            <Input label="Location" name="location" value={form.location} onChange={handleChange} required />

            <div className="split-fields">
              <Input label="Event date" type="datetime-local" name="eventDate" value={form.eventDate} onChange={handleChange} required />
              <div />
            </div>

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="4" />
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update event' : 'Add event'}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Upcoming</h2>
          </div>

          {data.length === 0 ? (
            <div className="empty-state">
              <h3>No events yet</h3>
              <p className="muted">Add the first community event and it will appear here.</p>
            </div>
          ) : (
            <div className="transaction-list">
              {data.map((eventItem) => (
                <div key={eventItem.id} className="transaction-item">
                  <div>
                    <strong>{eventItem.title}</strong>
                    <div className="small-muted">{eventItem.category} · {eventItem.location}</div>
                    <div className="small-muted">{new Date(eventItem.eventDate).toLocaleString()}</div>
                  </div>
                  <div className="transaction-actions">
                    <div className="small-muted">{eventItem.description}</div>
                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.5rem' }}>
                      <Button variant="secondary" onClick={() => handleEdit(eventItem)}>Edit</Button>
                      <Button variant="ghost" onClick={() => deleteMutation.mutate(eventItem.id)}>Delete</Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </aside>
      </section>
    </MainLayout>
  )
}
