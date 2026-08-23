import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import {
  createBloodRequest,
  createDonationCenter,
  deleteBloodRequest,
  deleteDonationCenter,
  getBloodRequests,
  getDonationCenters,
  updateBloodRequest,
  updateDonationCenter,
} from '../services/bloodApi'
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
import './BloodDonationPage.css'

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

const defaultRequestForm = {
  patientName: '',
  bloodGroup: 'O+',
  unitsNeeded: 1,
  hospitalLocation: '',
  urgency: 'STANDARD',
  contactName: '',
  contactPhone: '',
  additionalNotes: '',
}

const defaultCenterForm = {
  name: '',
  location: '',
  contact: '',
  description: '',
}

export default function BloodDonationPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'blood_donation' })
  }, [])

  const [activeTab, setActiveTab] = useState('requests') // 'requests' | 'centers'
  const [selectedGroup, setSelectedGroup] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [scopeFilter, setScopeFilter] = useState('all') // 'all' | 'my'

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  const [requestForm, setRequestForm] = useState(defaultRequestForm)
  const [editingRequestId, setEditingRequestId] = useState(null)
  const [requestFormError, setRequestFormError] = useState('')

  const [centerForm, setCenterForm] = useState(defaultCenterForm)
  const [editingCenterId, setEditingCenterId] = useState(null)
  const [centerFormError, setCenterFormError] = useState('')

  // Queries
  const { data: requestPageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading: isLoadingRequests, isError: isErrorRequests } = useQuery({
    queryKey: ['blood-requests', { page, pageSize, selectedGroup, statusFilter, scopeFilter }],
    queryFn: () =>
      getBloodRequests({
        page,
        size: pageSize,
        bloodGroup: selectedGroup !== 'ALL' ? selectedGroup : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
      }),
    placeholderData: (previousData) => previousData,
  })

  const requests = useMemo(() => {
    if (Array.isArray(requestPageData)) return requestPageData
    return requestPageData.content ?? []
  }, [requestPageData])

  const totalRequestElements = Array.isArray(requestPageData) ? requestPageData.length : (requestPageData.totalElements ?? requests.length)
  const totalRequestPages = Array.isArray(requestPageData) ? 1 : (requestPageData.totalPages ?? 1)

  const { data: centerPageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading: isLoadingCenters, isError: isErrorCenters } = useQuery({
    queryKey: ['blood-centers', { page, pageSize }],
    queryFn: () => getDonationCenters({ page, size: pageSize }),
    placeholderData: (previousData) => previousData,
  })

  const centers = useMemo(() => {
    if (Array.isArray(centerPageData)) return centerPageData
    return centerPageData.content ?? []
  }, [centerPageData])

  const totalCenterElements = Array.isArray(centerPageData) ? centerPageData.length : (centerPageData.totalElements ?? centers.length)
  const totalCenterPages = Array.isArray(centerPageData) ? 1 : (centerPageData.totalPages ?? 1)

  // Filter requests by scope
  const visibleRequests = useMemo(() => {
    if (scopeFilter === 'my') {
      if (!user?.id) return []
      return requests.filter((r) => r.userId === user.id)
    }
    return requests
  }, [requests, scopeFilter, user])

  const myRequestsCount = useMemo(() => {
    if (!user?.id) return 0
    return requests.filter((r) => r.userId === user.id).length
  }, [requests, user])

  const urgentCount = useMemo(() => requests.filter((r) => r.urgency === 'URGENT' && r.status === 'OPEN').length, [requests])
  const openCount = useMemo(() => requests.filter((r) => r.status === 'OPEN').length, [requests])

  // Mutations for Blood Requests
  const saveRequestMutation = useMutation({
    mutationFn: (payload) =>
      editingRequestId ? updateBloodRequest(editingRequestId, payload) : createBloodRequest(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      setRequestForm(defaultRequestForm)
      setEditingRequestId(null)
      setRequestFormError('')
      trackEvent('blood_request_created')
    },
    onError: (err) => {
      setRequestFormError(err.response?.data?.message || err.response?.data?.detail || 'Failed to submit blood request.')
    },
  })

  const deleteRequestMutation = useMutation({
    mutationFn: (id) => deleteBloodRequest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      trackEvent('blood_request_deleted')
    },
  })

  const statusTransitionMutation = useMutation({
    mutationFn: ({ id, payload }) => updateBloodRequest(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
    },
  })

  // Mutations for Donation Centers
  const saveCenterMutation = useMutation({
    mutationFn: (payload) =>
      editingCenterId ? updateDonationCenter(editingCenterId, payload) : createDonationCenter(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-centers'] })
      setCenterForm(defaultCenterForm)
      setEditingCenterId(null)
      setCenterFormError('')
    },
    onError: (err) => {
      setCenterFormError(err.response?.data?.message || err.response?.data?.detail || 'Failed to save donation center.')
    },
  })

  const deleteCenterMutation = useMutation({
    mutationFn: (id) => deleteDonationCenter(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-centers'] })
    },
  })

  // Handlers for Request Form
  function handleRequestFormChange(e) {
    const { name, value } = e.target
    setRequestForm((prev) => ({ ...prev, [name]: value }))
  }

  function handleRequestSubmit(e) {
    e.preventDefault()
    setRequestFormError('')

    if (!requestForm.patientName.trim() || !requestForm.hospitalLocation.trim() || !requestForm.contactName.trim() || !requestForm.contactPhone.trim()) {
      setRequestFormError('Please fill out all required fields.')
      return
    }

    const units = parseInt(requestForm.unitsNeeded, 10)
    if (isNaN(units) || units < 1) {
      setRequestFormError('Units needed must be at least 1.')
      return
    }

    saveRequestMutation.mutate({
      patientName: requestForm.patientName.trim(),
      bloodGroup: requestForm.bloodGroup,
      unitsNeeded: units,
      hospitalLocation: requestForm.hospitalLocation.trim(),
      urgency: requestForm.urgency,
      contactName: requestForm.contactName.trim(),
      contactPhone: requestForm.contactPhone.trim(),
      additionalNotes: requestForm.additionalNotes.trim(),
    })
  }

  function handleEditRequest(req) {
    setEditingRequestId(req.id)
    setRequestForm({
      patientName: req.patientName,
      bloodGroup: req.bloodGroup,
      unitsNeeded: req.unitsNeeded,
      hospitalLocation: req.hospitalLocation,
      urgency: req.urgency,
      contactName: req.contactName,
      contactPhone: req.contactPhone,
      additionalNotes: req.additionalNotes || '',
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleCancelRequestForm() {
    setEditingRequestId(null)
    setRequestForm(defaultRequestForm)
    setRequestFormError('')
  }

  // Handlers for Center Form
  function handleCenterFormChange(e) {
    const { name, value } = e.target
    setCenterForm((prev) => ({ ...prev, [name]: value }))
  }

  function handleCenterSubmit(e) {
    e.preventDefault()
    setCenterFormError('')

    if (!centerForm.name.trim() || !centerForm.location.trim() || !centerForm.contact.trim() || !centerForm.description.trim()) {
      setCenterFormError('Please fill out all required center details.')
      return
    }

    saveCenterMutation.mutate({
      name: centerForm.name.trim(),
      location: centerForm.location.trim(),
      contact: centerForm.contact.trim(),
      description: centerForm.description.trim(),
    })
  }

  function handleEditCenter(center) {
    setEditingCenterId(center.id)
    setCenterForm({
      name: center.name,
      location: center.location,
      contact: center.contact,
      description: center.description,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function handleCancelCenterForm() {
    setEditingCenterId(null)
    setCenterForm(defaultCenterForm)
    setCenterFormError('')
  }

  if (isLoadingRequests && isLoadingCenters) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading blood donation portal…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isErrorRequests || isErrorCenters) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Blood donation portal is unavailable</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-blood-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Community Health & Emergency</span>
              <h1 className="dm-page-main-title">Blood Donation</h1>
              <p className="dm-page-subtitle">
                Connect patients and donors, find urgent neighborhood blood requests, and locate verified donation centers.
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

          {/* Key Metrics Grid */}
          <div className="dm-blood-stats-grid">
            <StatCard
              domain="emergency"
              label="Urgent Appeals"
              value={String(urgentCount)}
              trend="Priority"
              trendDirection={urgentCount > 0 ? 'down' : 'up'}
              trendLabel="Critical surgery"
              icon="🔴"
            />
            <StatCard
              domain="emergency"
              label="Open Requests"
              value={String(openCount)}
              trend="Active"
              trendDirection="neutral"
              trendLabel="Community needs"
              icon="🩸"
            />
            <StatCard
              domain="emergency"
              label="Donation Centers"
              value={String(centers.length)}
              trend="Verified"
              trendDirection="up"
              trendLabel="Certified banks"
              icon="🏥"
            />
            <StatCard
              domain="emergency"
              label="My Requests"
              value={String(myRequestsCount)}
              trend="Managed"
              trendDirection="neutral"
              trendLabel="Owner controls"
              icon="👤"
            />
          </div>

          {/* Main Tab Switcher */}
          <div className="dm-blood-tabs-row">
            <div className="dm-blood-nav-tabs" role="tablist">
              <button
                type="button"
                className={`dm-blood-tab-btn ${activeTab === 'requests' ? 'active' : ''}`}
                onClick={() => setActiveTab('requests')}
              >
                🩸 Blood Requests ({requests.length})
              </button>
              <button
                type="button"
                className={`dm-blood-tab-btn ${activeTab === 'centers' ? 'active' : ''}`}
                onClick={() => setActiveTab('centers')}
              >
                🏥 Donation Centers ({centers.length})
              </button>
            </div>
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-provider-profile-layout">
            {/* Left Column: Feeds */}
            <div>
              {activeTab === 'requests' ? (
                <div>
                  {/* Filter Toolbar */}
                  <div className="dm-blood-filter-toolbar">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: '0.875rem' }}>Blood Group:</strong>
                        <div className="dm-blood-group-pills">
                          <button
                            type="button"
                            className={`dm-blood-group-pill ${selectedGroup === 'ALL' ? 'active' : ''}`}
                            onClick={() => setSelectedGroup('ALL')}
                          >
                            All
                          </button>
                          {BLOOD_GROUPS.map((bg) => (
                            <button
                              key={bg}
                              type="button"
                              className={`dm-blood-group-pill ${selectedGroup === bg ? 'active' : ''}`}
                              onClick={() => setSelectedGroup(bg)}
                            >
                              {bg}
                            </button>
                          ))}
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                          type="button"
                          className={`dm-category-filter-btn ${scopeFilter === 'all' ? 'active' : ''}`}
                          onClick={() => setScopeFilter('all')}
                        >
                          All Requests
                        </button>
                        <button
                          type="button"
                          className={`dm-category-filter-btn ${scopeFilter === 'my' ? 'active' : ''}`}
                          onClick={() => setScopeFilter('my')}
                        >
                          My Requests ({myRequestsCount})
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Requests Cards List */}
                  {visibleRequests.length === 0 ? (
                    <div className="dm-empty-dashboard-box">
                      <h3>No blood requests found</h3>
                      <p>
                        {selectedGroup !== 'ALL'
                          ? `No requests currently match blood group ${selectedGroup}.`
                          : 'No active blood requests at this time.'}
                      </p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                      {visibleRequests.map((req) => {
                        const isOwner = user?.id && req.userId === user.id
                        const isUrgent = req.urgency === 'URGENT'
                        return (
                          <div key={req.id} className={`dm-blood-card ${isUrgent ? 'urgent' : ''}`}>
                            <div>
                              <div className="dm-blood-card-header">
                                <span className="dm-blood-group-badge">{req.bloodGroup}</span>
                                <div style={{ display: 'flex', gap: '0.35rem' }}>
                                  {isUrgent && <span className="notification-badge error">🔴 URGENT</span>}
                                  <Badge variant={req.status === 'OPEN' ? 'primary' : 'neutral'} size="sm">
                                    {req.status}
                                  </Badge>
                                </div>
                              </div>

                              <div style={{ marginTop: '0.75rem' }}>
                                <h3 className="dm-blood-card-title">
                                  {req.patientName} · {req.unitsNeeded} {req.unitsNeeded === 1 ? 'unit' : 'units'}
                                </h3>
                                <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', margin: '0 0 0.5rem 0' }}>
                                  📍 {req.hospitalLocation}
                                </p>
                              </div>

                              {req.additionalNotes && (
                                <p style={{ fontSize: '0.8125rem', color: 'var(--dm-color-text-soft)', margin: '0.5rem 0' }}>
                                  <strong>Notes:</strong> {req.additionalNotes}
                                </p>
                              )}
                            </div>

                            <div>
                              <div style={{ padding: '0.5rem 0.75rem', background: 'var(--dm-color-surface-soft)', borderRadius: 'var(--dm-radius-sm, 8px)', fontSize: '0.8125rem', marginBottom: '0.75rem' }}>
                                <strong>Contact Coordinator:</strong> {req.contactName} ·{' '}
                                <a href={`tel:${req.contactPhone}`} style={{ color: 'var(--dm-color-primary-deep)', fontWeight: 700 }}>
                                  {req.contactPhone}
                                </a>
                              </div>

                              {isOwner && (
                                <div className="dm-blood-card-footer">
                                  {req.status === 'OPEN' && (
                                    <>
                                      <Button
                                        variant="secondary"
                                        size="sm"
                                        onClick={() =>
                                          statusTransitionMutation.mutate({
                                            id: req.id,
                                            payload: { ...req, status: 'FULFILLED' },
                                          })
                                        }
                                      >
                                        ✓ Mark Fulfilled
                                      </Button>
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() =>
                                          statusTransitionMutation.mutate({
                                            id: req.id,
                                            payload: { ...req, status: 'CANCELLED' },
                                          })
                                        }
                                      >
                                        Cancel Request
                                      </Button>
                                      <Button variant="ghost" size="sm" onClick={() => handleEditRequest(req)}>
                                        Edit
                                      </Button>
                                    </>
                                  )}
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => deleteRequestMutation.mutate(req.id)}
                                  >
                                    Delete
                                  </Button>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  )}

                  {/* Server-Driven Pagination for Requests */}
                  <Pagination
                    page={page}
                    totalPages={totalRequestPages}
                    totalElements={totalRequestElements}
                    pageSize={pageSize}
                    onPageChange={setPage}
                    onPageSizeChange={setPageSize}
                    disabled={isLoadingRequests}
                  />
                </div>
              ) : (
                <div>
                  {/* Donation Centers View */}
                  {centers.length === 0 ? (
                    <div className="dm-empty-dashboard-box">
                      <h3>No donation centers found</h3>
                      <p>No verified donation centers listed yet. Register one in the form to help local donors.</p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                      {centers.map((c) => (
                        <div key={c.id} className="dm-blood-card">
                          <div>
                            <h3 className="dm-blood-card-title">{c.name}</h3>
                            <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem' }}>📍 {c.location}</p>
                            <p style={{ fontSize: '0.875rem', color: 'var(--dm-color-text)' }}>{c.description}</p>
                          </div>
                          <div className="dm-blood-card-footer">
                            <span style={{ fontSize: '0.8125rem', fontWeight: 600 }}>📞 {c.contact}</span>
                            {user?.role === 'ADMIN' && (
                              <div style={{ display: 'flex', gap: '0.25rem' }}>
                                <Button variant="ghost" size="sm" onClick={() => handleEditCenter(c)}>
                                  Edit
                                </Button>
                                <Button variant="ghost" size="sm" onClick={() => deleteCenterMutation.mutate(c.id)}>
                                  Delete
                                </Button>
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Server-Driven Pagination for Centers */}
                  <Pagination
                    page={page}
                    totalPages={totalCenterPages}
                    totalElements={totalCenterElements}
                    pageSize={pageSize}
                    onPageChange={setPage}
                    onPageSizeChange={setPageSize}
                    disabled={isLoadingCenters}
                  />
                </div>
              )}
            </div>

            {/* Right Column: Sticky Quick Action Card Form */}
            <div>
              {activeTab === 'requests' ? (
                <div className="dm-provider-side-card">
                  <div>
                    <span className="dm-section-eyebrow">
                      {editingRequestId ? 'Edit Request' : 'Immediate Need'}
                    </span>
                    <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                      {editingRequestId ? 'Update Blood Request' : 'Post Blood Request'}
                    </h3>
                    <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                      Broadcast urgent patient blood requirements across the neighborhood.
                    </p>
                  </div>

                  {requestFormError && (
                    <div className="dm-form-alert dm-form-alert--error">⚠️ {requestFormError}</div>
                  )}

                  <form onSubmit={handleRequestSubmit} className="dm-provider-modal-form">
                    <Input
                      id="b-patient"
                      name="patientName"
                      label="Patient name"
                      required
                      placeholder="e.g. Kavita Patel"
                      value={requestForm.patientName}
                      onChange={handleRequestFormChange}
                    />

                    <div className="dm-select-group">
                      <label htmlFor="b-group" className="dm-input-label">
                        Blood group
                      </label>
                      <select
                        id="b-group"
                        name="bloodGroup"
                        className="dm-select"
                        value={requestForm.bloodGroup}
                        onChange={handleRequestFormChange}
                      >
                        {BLOOD_GROUPS.map((g) => (
                          <option key={g} value={g}>
                            {g}
                          </option>
                        ))}
                      </select>
                    </div>

                    <Input
                      id="b-units"
                      name="unitsNeeded"
                      label="Units needed"
                      type="number"
                      required
                      min="1"
                      value={requestForm.unitsNeeded}
                      onChange={handleRequestFormChange}
                    />

                    <Input
                      id="b-hospital"
                      name="hospitalLocation"
                      label="Hospital / Location"
                      required
                      placeholder="e.g. Sahyadri Hospital, Pune"
                      value={requestForm.hospitalLocation}
                      onChange={handleRequestFormChange}
                    />

                    <div className="dm-select-group">
                      <label htmlFor="b-urgency" className="dm-input-label">
                        Urgency level
                      </label>
                      <select
                        id="b-urgency"
                        name="urgency"
                        className="dm-select"
                        value={requestForm.urgency}
                        onChange={handleRequestFormChange}
                      >
                        <option value="STANDARD">STANDARD</option>
                        <option value="URGENT">URGENT</option>
                      </select>
                    </div>

                    <Input
                      id="b-cname"
                      name="contactName"
                      label="Contact name"
                      required
                      placeholder="e.g. Amit Patel"
                      value={requestForm.contactName}
                      onChange={handleRequestFormChange}
                    />

                    <Input
                      id="b-cphone"
                      name="contactPhone"
                      label="Contact phone"
                      type="tel"
                      required
                      placeholder="555-9999"
                      value={requestForm.contactPhone}
                      onChange={handleRequestFormChange}
                    />

                    <Input
                      id="b-notes"
                      name="additionalNotes"
                      label="Additional notes"
                      placeholder="Surgery schedule, required before date..."
                      value={requestForm.additionalNotes}
                      onChange={handleRequestFormChange}
                    />

                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                      {editingRequestId && (
                        <Button type="button" variant="ghost" fullWidth onClick={handleCancelRequestForm}>
                          Cancel
                        </Button>
                      )}
                      <Button
                        type="submit"
                        variant="danger"
                        fullWidth
                        isLoading={saveRequestMutation.isPending}
                      >
                        Submit request
                      </Button>
                    </div>
                  </form>
                </div>
              ) : (
                <div className="dm-provider-side-card">
                  <div>
                    <span className="dm-section-eyebrow">
                      {editingCenterId ? 'Edit Center' : 'Verified Directory'}
                    </span>
                    <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                      {editingCenterId ? 'Update Donation Center' : 'Register Donation Center'}
                    </h3>
                    <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                      Add accredited blood banks and hospital donation centers.
                    </p>
                  </div>

                  {centerFormError && (
                    <div className="dm-form-alert dm-form-alert--error">⚠️ {centerFormError}</div>
                  )}

                  <form onSubmit={handleCenterSubmit} className="dm-provider-modal-form">
                    <Input
                      id="c-name"
                      name="name"
                      label="Center name"
                      required
                      placeholder="e.g. Red Cross Blood Bank"
                      value={centerForm.name}
                      onChange={handleCenterFormChange}
                    />

                    <Input
                      id="c-location"
                      name="location"
                      label="Address / Location"
                      required
                      placeholder="e.g. Camp, Pune"
                      value={centerForm.location}
                      onChange={handleCenterFormChange}
                    />

                    <Input
                      id="c-contact"
                      name="contact"
                      label="Contact phone / info"
                      required
                      placeholder="e.g. +91 20 2612 0000"
                      value={centerForm.contact}
                      onChange={handleCenterFormChange}
                    />

                    <Input
                      id="c-desc"
                      name="description"
                      label="Operating details / description"
                      required
                      placeholder="Operating hours 24/7, all blood components available"
                      value={centerForm.description}
                      onChange={handleCenterFormChange}
                    />

                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                      {editingCenterId && (
                        <Button type="button" variant="ghost" fullWidth onClick={handleCancelCenterForm}>
                          Cancel
                        </Button>
                      )}
                      <Button
                        type="submit"
                        variant="primary"
                        fullWidth
                        isLoading={saveCenterMutation.isPending}
                      >
                        Register center
                      </Button>
                    </div>
                  </form>
                </div>
              )}
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
