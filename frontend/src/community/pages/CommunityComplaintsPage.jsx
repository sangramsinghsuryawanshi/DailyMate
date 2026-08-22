import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createCommunityComplaint, deleteCommunityComplaint, getCommunityComplaints, updateCommunityComplaint } from '../services/communityComplaintsApi'

const defaultForm = {
  title: '',
  category: '',
  location: '',
  description: '',
}

export default function CommunityComplaintsPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [selectedStatus, setSelectedStatus] = useState('ALL')
  const [formError, setFormError] = useState('')

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['community-complaints'],
    queryFn: getCommunityComplaints,
  })

  const filteredComplaints = useMemo(() => {
    if (selectedStatus === 'ALL') return data
    return data.filter((item) => item.status === selectedStatus)
  }, [data, selectedStatus])

  const openCount = useMemo(() => data.filter((item) => item.status === 'OPEN').length, [data])
  const inReviewCount = useMemo(() => data.filter((item) => item.status === 'IN_REVIEW').length, [data])
  const resolvedCount = useMemo(() => data.filter((item) => item.status === 'RESOLVED').length, [data])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateCommunityComplaint(editingId, payload) : createCommunityComplaint(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to submit report. Please ensure you are logged in.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCommunityComplaint,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
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

    if (!form.title.trim() || !form.category.trim() || !form.location.trim() || !form.description.trim()) {
      setFormError('Please fill out all required fields.')
      return
    }

    saveMutation.mutate({
      title: form.title.trim(),
      category: form.category.trim(),
      location: form.location.trim(),
      description: form.description.trim(),
    })
  }

  function handleEdit(complaint) {
    setEditingId(complaint.id)
    setFormError('')
    setForm({
      title: complaint.title,
      category: complaint.category,
      location: complaint.location,
      description: complaint.description,
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
        <main className="page-state"><h1>Loading complaints…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Complaints are unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Community care</p>
          <h1>Community complaints</h1>
          <p className="subtle-text">Report neighborhood issues so local teams can respond quickly and keep everyone informed.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="complaints-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit report' : 'Report an issue'}</h2>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form className="notification-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Title" name="title" value={form.title} onChange={handleChange} maxLength={120} required />
              <Input label="Category" name="category" value={form.category} onChange={handleChange} maxLength={80} required />
            </div>

            <Input label="Location" name="location" value={form.location} onChange={handleChange} maxLength={160} required />

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="4" maxLength={1000} />
            </label>

            <div className="profile-actions" style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : editingId ? 'Update complaint' : 'Submit report'}
              </Button>
              {editingId && (
                <Button type="button" variant="ghost" onClick={handleCancelEdit}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Report feed</h2>
          </div>
          <ul className="detail-list">
            <li><strong>Total reports:</strong> {data.length}</li>
            <li><strong>Open:</strong> {openCount}</li>
            <li><strong>In Review:</strong> {inReviewCount}</li>
            <li><strong>Resolved:</strong> {resolvedCount}</li>
          </ul>

          <div style={{ marginTop: '1.25rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {['ALL', 'OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED'].map((st) => (
              <button
                key={st}
                type="button"
                className={`btn btn-sm ${selectedStatus === st ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setSelectedStatus(st)}
              >
                {st}
              </button>
            ))}
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {filteredComplaints.length === 0 ? (
              <div className="empty-state">
                <h3>No complaints reported</h3>
                <p className="muted">No issues found matching the selected status filter.</p>
              </div>
            ) : (
              filteredComplaints.map((complaint) => (
                <article key={complaint.id} className="notification-item">
                  <div className="notification-head" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <span className="notification-badge info">{complaint.category}</span>
                      <span className="small-muted">{complaint.location}</span>
                    </div>
                    <span className="status-pill">{complaint.status || 'OPEN'}</span>
                  </div>
                  <h3>{complaint.title}</h3>
                  <p>{complaint.description}</p>
                  <div className="notification-actions">
                    <Button variant="secondary" onClick={() => handleEdit(complaint)}>Edit</Button>
                    <Button variant="ghost" onClick={() => deleteMutation.mutate(complaint.id)}>Delete</Button>
                  </div>
                </article>
              ))
            )}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
