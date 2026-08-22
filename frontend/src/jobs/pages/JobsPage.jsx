import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { createJob, deleteJob, getJobs, getMyJobs, updateJob } from '../services/jobsApi'

const JOB_CATEGORIES = ['ALL', 'Services', 'Retail', 'Office & Admin', 'Education & Tutoring', 'Technical', 'Hospitality', 'Other']
const JOB_TYPES = ['ALL', 'Full-time', 'Part-time', 'Contract', 'Gig / Task', 'Internship']

const defaultForm = {
  title: '',
  category: 'Services',
  location: '',
  type: 'Full-time',
  salary: '',
  companyName: '',
  contactPhone: '',
  contactEmail: '',
  description: '',
}

export default function JobsPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [activeTab, setActiveTab] = useState('available') // 'available' | 'my'
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [selectedType, setSelectedType] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [formError, setFormError] = useState('')

  // 1. Public Job Listings Query
  const { data: publicJobs = [], isLoading: isPublicLoading, isError: isPublicError } = useQuery({
    queryKey: ['jobs', selectedCategory, selectedType, searchQuery],
    queryFn: () => getJobs({
      search: searchQuery,
      category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      type: selectedType !== 'ALL' ? selectedType : undefined,
    }),
  })

  // 2. Authenticated User's Job Postings Query
  const { data: myJobs = [], isLoading: isMyLoading, isError: isMyError } = useQuery({
    queryKey: ['jobs', 'my'],
    queryFn: getMyJobs,
    enabled: Boolean(user?.id),
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateJob(editingId, payload) : createJob(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      queryClient.invalidateQueries({ queryKey: ['jobs', 'my'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      setActiveTab('my')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save job posting.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      queryClient.invalidateQueries({ queryKey: ['jobs', 'my'] })
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

    if (!form.title.trim() || !form.location.trim() || !form.description.trim()) {
      setFormError('Please fill out all required fields.')
      return
    }

    const payload = {
      title: form.title.trim(),
      category: form.category.trim(),
      location: form.location.trim(),
      type: form.type.trim(),
      description: form.description.trim(),
      companyName: form.companyName ? form.companyName.trim() : null,
      contactPhone: form.contactPhone ? form.contactPhone.trim() : null,
      contactEmail: form.contactEmail ? form.contactEmail.trim() : null,
    }

    if (form.salary && !isNaN(Number(form.salary))) {
      payload.salary = Number(form.salary)
    }

    if (editingId && form.status) {
      payload.status = form.status
    }

    saveMutation.mutate(payload)
  }

  function handleEdit(job) {
    setEditingId(job.id)
    setFormError('')
    setForm({
      title: job.title || '',
      category: job.category || 'Services',
      location: job.location || '',
      type: job.type || 'Full-time',
      salary: job.salary != null ? String(job.salary) : '',
      companyName: job.companyName || '',
      contactPhone: job.contactPhone || '',
      contactEmail: job.contactEmail || '',
      status: job.status || 'OPEN',
      description: job.description || '',
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  const isLoading = activeTab === 'available' ? isPublicLoading : isMyLoading
  const isError = activeTab === 'available' ? isPublicError : isMyError
  const visibleList = activeTab === 'available' ? publicJobs : myJobs

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading local jobs…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Jobs board is currently unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Opportunities</p>
          <h1>Community Jobs Board</h1>
          <p className="subtle-text">Discover local employment, neighborhood service gigs, and hire community talent.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {/* Main Tab Switcher */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          className={`btn ${activeTab === 'available' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('available')}
        >
          💼 Available Jobs ({publicJobs.length})
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'my' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('my')}
        >
          📝 My Postings {user?.id && myJobs ? `(${myJobs.length})` : ''}
        </button>
      </div>

      <section className="complaints-grid">
        {/* Main Jobs Listing Column */}
        <div>
          {/* Filters on Available tab */}
          {activeTab === 'available' && (
            <div className="panel" style={{ marginBottom: '1.25rem' }}>
              <div style={{ marginBottom: '0.75rem' }}>
                <input
                  type="search"
                  placeholder="Search by job title, skill, or employer…"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="form-input"
                  aria-label="Search jobs"
                  style={{ width: '100%' }}
                />
              </div>

              {/* Category Pills */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.6rem' }}>
                <strong style={{ fontSize: '0.9rem', marginRight: '0.35rem' }}>Category:</strong>
                {JOB_CATEGORIES.map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    className={`btn btn-small ${selectedCategory === cat ? 'btn-secondary' : 'btn-ghost'}`}
                    onClick={() => setSelectedCategory(cat)}
                  >
                    {cat === 'ALL' ? 'All' : cat}
                  </button>
                ))}
              </div>

              {/* Type Pills */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
                <strong style={{ fontSize: '0.9rem', marginRight: '0.35rem' }}>Job Type:</strong>
                {JOB_TYPES.map((t) => (
                  <button
                    key={t}
                    type="button"
                    className={`btn btn-small ${selectedType === t ? 'btn-secondary' : 'btn-ghost'}`}
                    onClick={() => setSelectedType(t)}
                  >
                    {t === 'ALL' ? 'All Types' : t}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Jobs Feed / List */}
          {activeTab === 'my' && !user?.id ? (
            <div className="panel empty-state">
              <h3>Sign in to manage your postings</h3>
              <p className="muted">Log in to post local job openings and manage your submissions.</p>
              <Link to="/login" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                Log in to post jobs
              </Link>
            </div>
          ) : visibleList.length === 0 ? (
            <div className="panel empty-state">
              <h3>No jobs found</h3>
              <p className="muted">
                {activeTab === 'my'
                  ? 'You haven’t posted any job openings yet. Use the form to post an opportunity.'
                  : searchQuery || selectedCategory !== 'ALL' || selectedType !== 'ALL'
                  ? 'No job listings match your active filters. Try adjusting your search criteria.'
                  : 'Be the first to post a neighborhood work opportunity.'}
              </p>
            </div>
          ) : (
            <div className="notification-list">
              {visibleList.map((job) => (
                <article key={job.id} className="panel" style={{ marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem', flexWrap: 'wrap' }}>
                        <span className="notification-badge info">{job.category}</span>
                        <span className="status-pill open">{job.type}</span>
                        <span className={`status-pill ${job.status === 'CLOSED' ? 'closed' : 'resolved'}`}>
                          {job.status === 'CLOSED' ? 'Closed' : 'Hiring'}
                        </span>
                      </div>
                      <h3 style={{ margin: '0 0 0.25rem 0', fontSize: '1.25rem' }}>{job.title}</h3>
                      <div className="small-muted">
                        {job.companyName ? `🏢 ${job.companyName} · ` : ''}📍 {job.location}
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--color-primary-deep)' }}>
                        {job.salary != null ? formatINR(job.salary) : 'Competitive'}
                      </div>
                      {job.salary != null && <div className="small-muted">Compensation</div>}
                    </div>
                  </div>

                  <p style={{ marginTop: '0.75rem', marginBottom: '0.75rem', lineHeight: '1.5' }}>
                    {job.description}
                  </p>

                  {/* Actions Bar */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid var(--color-border)' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {job.contactPhone && (
                        <a href={`tel:${job.contactPhone}`} className="btn btn-small btn-primary">
                          📞 Call {job.contactPhone}
                        </a>
                      )}
                      {job.contactEmail && (
                        <a href={`mailto:${job.contactEmail}`} className="btn btn-small btn-ghost">
                          ✉️ Email
                        </a>
                      )}
                    </div>

                    {/* Owner controls only in My Postings tab */}
                    {activeTab === 'my' && (
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <Button variant="secondary" onClick={() => handleEdit(job)}>Edit</Button>
                        <Button variant="ghost" onClick={() => deleteMutation.mutate(job.id)}>Delete</Button>
                      </div>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        {/* Sidebar Column — Post / Edit Form */}
        <aside>
          {user?.id && (
            <div className="panel">
              <div className="panel-header">
                <h2>{editingId ? 'Edit Job Posting' : 'Post a Job Opening'}</h2>
              </div>

              <form className="notification-form" onSubmit={handleSubmit}>
                <Input label="Job title" name="title" value={form.title} onChange={handleChange} required />

                <div className="form-group">
                  <label htmlFor="job-category-select">Category</label>
                  <select id="job-category-select" name="category" value={form.category} onChange={handleChange} className="form-input">
                    {JOB_CATEGORIES.filter((c) => c !== 'ALL').map((cat) => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="job-type-select">Job type</label>
                  <select id="job-type-select" name="type" value={form.type} onChange={handleChange} className="form-input">
                    {JOB_TYPES.filter((t) => t !== 'ALL').map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>

                <Input label="Location / Area" name="location" value={form.location} onChange={handleChange} required />
                <Input label="Salary / Compensation (₹)" type="number" step="0.01" min="0" name="salary" value={form.salary} onChange={handleChange} placeholder="e.g. 25000" />
                <Input label="Company / Employer name" name="companyName" value={form.companyName} onChange={handleChange} placeholder="Optional" />
                <Input label="Contact phone" name="contactPhone" value={form.contactPhone} onChange={handleChange} placeholder="Optional" />
                <Input label="Contact email" type="email" name="contactEmail" value={form.contactEmail} onChange={handleChange} placeholder="Optional" />

                {editingId && (
                  <div className="form-group">
                    <label htmlFor="job-status-select">Status</label>
                    <select id="job-status-select" name="status" value={form.status} onChange={handleChange} className="form-input">
                      <option value="OPEN">OPEN (Active & Hiring)</option>
                      <option value="CLOSED">CLOSED (Filled / Inactive)</option>
                    </select>
                  </div>
                )}

                <label className="field">
                  <span>Job description & requirements</span>
                  <textarea name="description" value={form.description} onChange={handleChange} required rows="4" className="form-input" style={{ width: '100%' }} />
                </label>

                {formError && <p className="form-error" role="alert">{formError}</p>}

                <div className="profile-actions" style={{ marginTop: '0.75rem' }}>
                  <Button type="submit" disabled={saveMutation.isPending}>
                    {saveMutation.isPending ? 'Saving…' : editingId ? 'Update Posting' : 'Publish Job'}
                  </Button>
                  {editingId && <Button variant="ghost" onClick={handleCancelEdit}>Cancel</Button>}
                </div>
              </form>
            </div>
          )}
        </aside>
      </section>
    </MainLayout>
  )
}
