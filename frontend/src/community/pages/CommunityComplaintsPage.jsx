import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { createCommunityComplaint, deleteCommunityComplaint, getCommunityComplaints, updateCommunityComplaint } from '../services/communityComplaintsApi'
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
import './CommunityComplaintsPage.css'

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

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'complaints' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['community-complaints', { page, pageSize, selectedStatus }],
    queryFn: () =>
      getCommunityComplaints({
        page,
        size: pageSize,
        status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
      }),
    placeholderData: (previousData) => previousData,
  })

  const rawComplaints = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? rawComplaints.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  const filteredComplaints = useMemo(() => {
    if (selectedStatus === 'ALL') return rawComplaints
    return rawComplaints.filter((item) => item.status === selectedStatus)
  }, [rawComplaints, selectedStatus])

  const openCount = useMemo(() => rawComplaints.filter((item) => item.status === 'OPEN').length, [rawComplaints])
  const inReviewCount = useMemo(() => rawComplaints.filter((item) => item.status === 'IN_REVIEW').length, [rawComplaints])
  const resolvedCount = useMemo(() => rawComplaints.filter((item) => item.status === 'RESOLVED').length, [rawComplaints])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateCommunityComplaint(editingId, payload) : createCommunityComplaint(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      trackEvent('complaint_saved')
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
      trackEvent('complaint_deleted')
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
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading community complaints…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load community complaints</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-complaints-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Civic Action & Transparency</span>
              <h1 className="dm-page-main-title">Community Complaints</h1>
              <p className="dm-page-subtitle">
                Report neighborhood civic issues, track municipal resolution progress, and escalate infrastructure maintenance.
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
          <div className="dm-complaints-stats-grid">
            <StatCard
              domain="community"
              label="Total Complaints"
              value={String(totalElements)}
              trend="Reported"
              trendDirection="neutral"
              trendLabel="Civic tracking"
              icon="📢"
            />
            <StatCard
              domain="community"
              label="Open Issues"
              value={String(openCount)}
              trend="Pending"
              trendDirection={openCount > 0 ? 'down' : 'up'}
              trendLabel="Awaiting inspection"
              icon="⚠️"
            />
            <StatCard
              domain="community"
              label="In Progress"
              value={String(inReviewCount)}
              trend="Under Review"
              trendDirection="neutral"
              trendLabel="Assigned field team"
              icon="🔄"
            />
            <StatCard
              domain="community"
              label="Resolved"
              value={String(resolvedCount)}
              trend="Completed"
              trendDirection="up"
              trendLabel="Verified by residents"
              icon="✅"
            />
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-complaints-layout">
            {/* Left Column: Complaints Feed */}
            <div>
              {/* Status Filter Bar */}
              <div className="dm-complaints-filter-bar" role="tablist" aria-label="Complaint Status Filter">
                <button
                  type="button"
                  className={`dm-category-filter-btn ${selectedStatus === 'ALL' ? 'active' : ''}`}
                  onClick={() => setSelectedStatus('ALL')}
                >
                  ALL
                </button>
                <button
                  type="button"
                  className={`dm-category-filter-btn ${selectedStatus === 'OPEN' ? 'active' : ''}`}
                  onClick={() => setSelectedStatus('OPEN')}
                >
                  OPEN
                </button>
                <button
                  type="button"
                  className={`dm-category-filter-btn ${selectedStatus === 'IN_REVIEW' ? 'active' : ''}`}
                  onClick={() => setSelectedStatus('IN_REVIEW')}
                >
                  IN_REVIEW
                </button>
                <button
                  type="button"
                  className={`dm-category-filter-btn ${selectedStatus === 'RESOLVED' ? 'active' : ''}`}
                  onClick={() => setSelectedStatus('RESOLVED')}
                >
                  RESOLVED
                </button>
              </div>

              {filteredComplaints.length === 0 ? (
                <div className="dm-empty-dashboard-box">
                  <h3>No community complaints found</h3>
                  <p>There are no complaints under this filter status.</p>
                </div>
              ) : (
                <div className="dm-complaints-card-feed">
                  {filteredComplaints.map((item) => (
                    <article key={item.id} className="dm-complaint-item-card">
                      <div className="dm-complaint-item-header">
                        <div>
                          <h3 className="dm-complaint-item-title">{item.title}</h3>
                          <div className="dm-complaint-meta" style={{ marginTop: '0.25rem' }}>
                            <span>📁 {item.category}</span>
                            <span>📍 {item.location}</span>
                          </div>
                        </div>
                        <Badge
                          variant={
                            item.status === 'RESOLVED'
                              ? 'success'
                              : item.status === 'IN_REVIEW'
                              ? 'warning'
                              : 'primary'
                          }
                          size="md"
                        >
                          {item.status}
                        </Badge>
                      </div>

                      <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', lineHeight: 1.5, margin: 0 }}>
                        {item.description}
                      </p>

                      <div className="dm-complaint-actions-row">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(item)}>
                          Edit
                        </Button>
                        <Button variant="danger" size="sm" onClick={() => deleteMutation.mutate(item.id)}>
                          Delete
                        </Button>
                      </div>
                    </article>
                  ))}
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
                    {editingId ? 'Edit Record' : 'Report an Issue'}
                  </span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                    {editingId ? 'Edit complaint' : 'Submit community report'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Report potholes, street light outages, garbage accumulation, or sanitation issues.
                  </p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="dm-provider-modal-form">
                  <Input
                    id="complaint-title"
                    name="title"
                    label="Title"
                    placeholder="e.g. Broken Street Light"
                    value={form.title}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="complaint-category"
                    name="category"
                    label="Category"
                    placeholder="e.g. Infrastructure, Roads, Sanitation"
                    value={form.category}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="complaint-location"
                    name="location"
                    label="Location"
                    placeholder="e.g. Oak Avenue near Main Cross"
                    value={form.location}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="complaint-description"
                    name="description"
                    label="Description"
                    placeholder="Provide details on the severity and safety risk"
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
                      {editingId ? 'Save Changes' : 'Submit report'}
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
