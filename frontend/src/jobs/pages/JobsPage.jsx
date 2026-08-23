import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { createJob, deleteJob, getJobs, getMyJobs, updateJob } from '../services/jobsApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Input,
  StatCard,
  Badge,
  Pagination,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import './JobsPage.css'

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
  status: 'OPEN',
  description: '',
}

export default function JobsPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'jobs' })
  }, [])

  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [activeTab, setActiveTab] = useState('available') // 'available' | 'my'
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [selectedType, setSelectedType] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [formError, setFormError] = useState('')

  // 1. Public Job Listings Query
  const { data: publicPageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading: isPublicLoading, isError: isPublicError } = useQuery({
    queryKey: ['jobs', { page, pageSize, selectedCategory, selectedType, searchQuery }],
    queryFn: () => getJobs({
      page,
      size: pageSize,
      search: searchQuery,
      category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      type: selectedType !== 'ALL' ? selectedType : undefined,
    }),
    placeholderData: (previousData) => previousData,
  })

  const publicJobs = useMemo(() => {
    if (Array.isArray(publicPageData)) return publicPageData
    return publicPageData.content ?? []
  }, [publicPageData])

  const totalPublicElements = Array.isArray(publicPageData) ? publicPageData.length : (publicPageData.totalElements ?? publicJobs.length)
  const totalPublicPages = Array.isArray(publicPageData) ? 1 : (publicPageData.totalPages ?? 1)

  // 2. Authenticated User's Job Postings Query
  const { data: myPageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading: isMyLoading, isError: isMyError } = useQuery({
    queryKey: ['jobs', 'my', { page, pageSize }],
    queryFn: () => getMyJobs({ page, size: pageSize }),
    enabled: Boolean(user?.id),
    placeholderData: (previousData) => previousData,
  })

  const myJobs = useMemo(() => {
    if (Array.isArray(myPageData)) return myPageData
    return myPageData.content ?? []
  }, [myPageData])

  const totalMyElements = Array.isArray(myPageData) ? myPageData.length : (myPageData.totalElements ?? myJobs.length)
  const totalMyPages = Array.isArray(myPageData) ? 1 : (myPageData.totalPages ?? 1)

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateJob(editingId, payload) : createJob(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      queryClient.invalidateQueries({ queryKey: ['jobs', 'my'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      setActiveTab('my')
      trackEvent('job_saved')
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
      trackEvent('job_deleted')
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
      category: form.category,
      location: form.location.trim(),
      type: form.type,
      description: form.description.trim(),
      companyName: form.companyName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      contactEmail: form.contactEmail.trim() || undefined,
      salary: form.salary ? parseFloat(form.salary) : undefined,
      status: form.status || 'OPEN',
    }

    saveMutation.mutate(payload)
  }

  function handleEdit(job) {
    setEditingId(job.id)
    setForm({
      title: job.title,
      category: job.category,
      location: job.location,
      type: job.type,
      salary: job.salary != null ? String(job.salary) : '',
      companyName: job.companyName || '',
      contactPhone: job.contactPhone || '',
      contactEmail: job.contactEmail || '',
      status: job.status || 'OPEN',
      description: job.description,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  function handleToggleStatus(job) {
    const newStatus = job.status === 'OPEN' ? 'CLOSED' : 'OPEN'
    saveMutation.mutate({
      ...job,
      status: newStatus,
    })
  }

  const displayedJobs = activeTab === 'available' ? publicJobs : myJobs
  const isLoading = activeTab === 'available' ? isPublicLoading : isMyLoading
  const isError = activeTab === 'available' ? isPublicError : isMyError

  if (isLoading && publicJobs.length === 0 && myJobs.length === 0) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading community jobs…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError && publicJobs.length === 0 && myJobs.length === 0) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load job listings</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-jobs-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Neighborhood Employment & Hiring</span>
              <h1 className="dm-page-main-title">Community Jobs Board</h1>
              <p className="dm-page-subtitle">
                Discover verified local job openings, hire residential staff, caretakers, and skilled trade professionals.
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
          <div className="dm-jobs-stats-grid">
            <StatCard
              domain="community"
              label="Active Openings"
              value={String(publicJobs.length)}
              trend="Verified"
              trendDirection="up"
              trendLabel="Neighborhood vacancies"
              icon="💼"
            />
            <StatCard
              domain="community"
              label="My Postings"
              value={String(myJobs.length)}
              trend="Employer"
              trendDirection="neutral"
              trendLabel="Created by you"
              icon="📌"
            />
            <StatCard
              domain="community"
              label="Categories"
              value={String(JOB_CATEGORIES.length - 1)}
              trend="Diverse"
              trendDirection="up"
              trendLabel="Trade to Professional"
              icon="🏢"
            />
            <StatCard
              domain="community"
              label="Direct Contact"
              value="100% Free"
              trend="Zero Fee"
              trendDirection="up"
              trendLabel="No intermediary commissions"
              icon="🤝"
            />
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-provider-profile-layout">
            {/* Left Column: Jobs Feed */}
            <div>
              {/* Tab Navigation */}
              <div className="dm-events-toolbar">
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'available' ? 'active' : ''}`}
                    onClick={() => setActiveTab('available')}
                  >
                    Available Jobs ({publicJobs.length})
                  </button>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'my' ? 'active' : ''}`}
                    onClick={() => setActiveTab('my')}
                  >
                    My Postings ({myJobs.length})
                  </button>
                </div>
              </div>

              {/* Search and Filters */}
              {activeTab === 'available' && (
                <div className="dm-jobs-toolbar">
                  <input
                    type="text"
                    className="dm-search-input"
                    style={{ padding: '0.625rem 1rem', width: '100%' }}
                    placeholder="Search jobs by title, company, or skills..."
                    aria-label="Search jobs"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />

                  <div className="dm-grocery-categories-bar">
                    {JOB_CATEGORIES.map((cat) => (
                      <button
                        key={cat}
                        type="button"
                        className={`dm-blood-group-pill ${selectedCategory === cat ? 'active' : ''}`}
                        onClick={() => setSelectedCategory(cat)}
                      >
                        {cat === 'ALL' ? 'All Categories' : cat}
                      </button>
                    ))}
                  </div>

                  <div className="dm-grocery-categories-bar" style={{ marginTop: '0.25rem' }}>
                    {JOB_TYPES.map((t) => (
                      <button
                        key={t}
                        type="button"
                        className={`dm-blood-group-pill ${selectedType === t ? 'active' : ''}`}
                        onClick={() => setSelectedType(t)}
                      >
                        {t === 'ALL' ? 'All Types' : t}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {displayedJobs.length === 0 ? (
                <div className="dm-empty-dashboard-box">
                  <h3>
                    {activeTab === 'available' ? 'No jobs found' : 'No personal postings yet'}
                  </h3>
                  <p>
                    {activeTab === 'available'
                      ? 'No jobs match your current search filters.'
                      : 'Post a local opening using the form on the right.'}
                  </p>
                </div>
              ) : (
                <div className="dm-jobs-card-feed">
                  {displayedJobs.map((job) => {
                    const isOwner = user?.id && job.userId === user.id
                    return (
                      <article key={job.id} className="dm-job-card">
                        <div className="dm-job-card-header">
                          <div>
                            <h3 className="dm-job-card-title">{job.title}</h3>
                            <div className="dm-job-card-meta" style={{ marginTop: '0.25rem' }}>
                              {job.companyName && <span>🏢 {job.companyName}</span>}
                              <span>📍 {job.location}</span>
                              <span>🏷️ {job.category}</span>
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                            <Badge variant="primary" size="sm">{job.type}</Badge>
                            {job.status === 'CLOSED' && <Badge variant="danger" size="sm">Closed</Badge>}
                          </div>
                        </div>

                        <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', lineHeight: 1.5, margin: 0 }}>
                          {job.description}
                        </p>

                        <div className="dm-job-contact-box">
                          <div>
                            {job.salary != null && (
                              <strong style={{ fontSize: '1.125rem', color: 'var(--dm-color-primary-deep)' }}>
                                {formatINR(job.salary)}
                              </strong>
                            )}
                          </div>

                          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                            {job.contactPhone && (
                              <a href={`tel:${job.contactPhone}`} className="dm-button dm-button--secondary dm-button--sm">
                                Call {job.contactPhone}
                              </a>
                            )}
                            {job.contactEmail && (
                              <a href={`mailto:${job.contactEmail}`} className="dm-button dm-button--ghost dm-button--sm">
                                Email
                              </a>
                            )}

                            {isOwner && (
                              <div style={{ display: 'flex', gap: '0.25rem' }}>
                                <Button variant="ghost" size="sm" onClick={() => handleEdit(job)}>
                                  Edit
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleToggleStatus(job)}
                                >
                                  {job.status === 'OPEN' ? 'Close Job' : 'Reopen'}
                                </Button>
                                <Button variant="danger" size="sm" onClick={() => deleteMutation.mutate(job.id)}>
                                  Delete
                                </Button>
                              </div>
                            )}
                          </div>
                        </div>
                      </article>
                    )
                  })}
                </div>
              )}

              {/* Server-Driven Pagination */}
              <Pagination
                page={page}
                totalPages={activeTab === 'available' ? totalPublicPages : totalMyPages}
                totalElements={activeTab === 'available' ? totalPublicElements : totalMyElements}
                pageSize={pageSize}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                disabled={activeTab === 'available' ? isPublicLoading : isMyLoading}
              />
            </div>

            {/* Right Column: Sticky Job Posting Form */}
            <div>
              <div className="dm-provider-side-card">
                <div>
                  <span className="dm-section-eyebrow">
                    {editingId ? 'Edit Opportunity' : 'Hire in Neighborhood'}
                  </span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                    {editingId ? 'Update Job Vacancy' : 'Post a Job Opening'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Post full-time, part-time, or contract positions for local residents.
                  </p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="dm-provider-modal-form">
                  <Input
                    id="j-title"
                    name="title"
                    label="Job title"
                    placeholder="e.g. Community Electrician, Tutor"
                    value={form.title}
                    onChange={handleChange}
                    required
                  />

                  <div className="dm-select-group">
                    <label htmlFor="j-cat" className="dm-input-label">
                      Category
                    </label>
                    <select
                      id="j-cat"
                      name="category"
                      className="dm-select"
                      value={form.category}
                      onChange={handleChange}
                    >
                      {JOB_CATEGORIES.filter((c) => c !== 'ALL').map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="dm-select-group">
                    <label htmlFor="j-type" className="dm-input-label">
                      Job type
                    </label>
                    <select
                      id="j-type"
                      name="type"
                      className="dm-select"
                      value={form.type}
                      onChange={handleChange}
                    >
                      {JOB_TYPES.filter((t) => t !== 'ALL').map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                  </div>

                  <Input
                    id="j-company"
                    name="companyName"
                    label="Company / Employer name"
                    placeholder="e.g. Kothrud Resident Welfare (Optional)"
                    value={form.companyName}
                    onChange={handleChange}
                  />

                  <Input
                    id="j-location"
                    name="location"
                    label="Location / Area"
                    placeholder="e.g. Kothrud, Pune"
                    value={form.location}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="j-salary"
                    name="salary"
                    label="Salary / Compensation (₹)"
                    type="number"
                    step="1"
                    placeholder="e.g. 28000"
                    value={form.salary}
                    onChange={handleChange}
                  />

                  <Input
                    id="j-phone"
                    name="contactPhone"
                    label="Contact phone"
                    type="tel"
                    placeholder="+91-9876543210"
                    value={form.contactPhone}
                    onChange={handleChange}
                  />

                  <Input
                    id="j-email"
                    name="contactEmail"
                    label="Contact email"
                    type="email"
                    placeholder="jobs@example.com"
                    value={form.contactEmail}
                    onChange={handleChange}
                  />

                  {editingId && (
                    <div className="dm-select-group">
                      <label htmlFor="j-status" className="dm-input-label">
                        Status
                      </label>
                      <select
                        id="j-status"
                        name="status"
                        className="dm-select"
                        value={form.status}
                        onChange={handleChange}
                      >
                        <option value="OPEN">OPEN</option>
                        <option value="CLOSED">CLOSED</option>
                      </select>
                    </div>
                  )}

                  <Input
                    id="j-desc"
                    name="description"
                    label="Job description"
                    placeholder="Describe daily duties, qualifications, and working hours"
                    value={form.description}
                    onChange={handleChange}
                    required
                  />

                  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                    {editingId && (
                      <Button type="button" variant="ghost" fullWidth onClick={handleCancelEdit}>
                        Cancel
                      </Button>
                    )}
                    <Button
                      type="submit"
                      variant="primary"
                      fullWidth
                      isLoading={saveMutation.isPending}
                    >
                      {editingId ? 'Update Posting' : 'Publish Job'}
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
