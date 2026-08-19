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

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['community-complaints'],
    queryFn: getCommunityComplaints,
  })

  const complaintCount = useMemo(() => data.length, [data])

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateCommunityComplaint(editingId, payload) : createCommunityComplaint(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCommunityComplaint,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['community-complaints'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(complaint) {
    setEditingId(complaint.id)
    setForm({
      title: complaint.title,
      category: complaint.category,
      location: complaint.location,
      description: complaint.description,
    })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading complaints…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Complaints are unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

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

          <form className="notification-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Title" name="title" value={form.title} onChange={handleChange} required />
              <Input label="Category" name="category" value={form.category} onChange={handleChange} required />
            </div>

            <Input label="Location" name="location" value={form.location} onChange={handleChange} required />

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="4" />
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update complaint' : 'Add complaint'}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Report feed</h2>
          </div>
          <div className="detail-list">
            <li><strong>Open reports:</strong> {complaintCount}</li>
            <li><strong>Priority:</strong> {complaintCount > 0 ? 'Community response active' : 'No active issues'}</li>
            <li><strong>Local response:</strong> 24–48 hours</li>
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {data.length === 0 ? (
              <div className="empty-state">
                <h3>No complaints reported yet</h3>
                <p className="muted">The first neighborhood issue will appear here.</p>
              </div>
            ) : data.map((complaint) => (
              <article key={complaint.id} className="notification-item">
                <div className="notification-head">
                  <span className="notification-badge info">{complaint.category}</span>
                  <span className="small-muted">{complaint.location}</span>
                </div>
                <h3>{complaint.title}</h3>
                <p>{complaint.description}</p>
                <div className="notification-actions">
                  <Button variant="secondary" onClick={() => handleEdit(complaint)}>Edit</Button>
                  <Button variant="ghost" onClick={() => deleteMutation.mutate(complaint.id)}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
