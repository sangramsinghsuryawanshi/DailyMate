import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createJob, deleteJob, getJobs, updateJob } from '../services/jobsApi'

const defaultForm = {
  title: '',
  category: '',
  location: '',
  type: '',
  description: '',
}

export default function JobsPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['jobs'],
    queryFn: getJobs,
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateJob(editingId, payload) : createJob(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteJob,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['jobs'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate(form)
  }

  function handleEdit(job) {
    setEditingId(job.id)
    setForm({
      title: job.title,
      category: job.category,
      location: job.location,
      type: job.type,
      description: job.description,
    })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading local jobs…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Jobs unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Work</p>
          <h1>Local jobs</h1>
          <p className="subtle-text">Discover neighborhood work opportunities and service gigs that match your area.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="jobs-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit job' : 'Post a job'}</h2>
          </div>

          <form className="expense-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Title" name="title" value={form.title} onChange={handleChange} required />
              <Input label="Category" name="category" value={form.category} onChange={handleChange} required />
            </div>

            <div className="split-fields">
              <Input label="Location" name="location" value={form.location} onChange={handleChange} required />
              <Input label="Type" name="type" value={form.type} onChange={handleChange} required />
            </div>

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="4" />
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update job' : 'Add job'}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Open opportunities</h2>
          </div>

          {data.length === 0 ? (
            <div className="empty-state">
              <h3>No jobs posted yet</h3>
              <p className="muted">Add the first local opportunity and it will appear here.</p>
            </div>
          ) : (
            <div className="transaction-list">
              {data.map((job) => (
                <div key={job.id} className="transaction-item">
                  <div>
                    <strong>{job.title}</strong>
                    <div className="small-muted">{job.category} · {job.type}</div>
                    <div className="small-muted">{job.location}</div>
                  </div>
                  <div className="transaction-actions">
                    <div className="small-muted">{job.description}</div>
                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.5rem' }}>
                      <Button variant="secondary" onClick={() => handleEdit(job)}>Edit</Button>
                      <Button variant="ghost" onClick={() => deleteMutation.mutate(job.id)}>Delete</Button>
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
