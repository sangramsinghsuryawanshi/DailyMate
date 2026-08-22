import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
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

  const [activeTab, setActiveTab] = useState('requests') // 'requests' | 'centers'
  const [selectedGroup, setSelectedGroup] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [scopeFilter, setScopeFilter] = useState('all') // 'all' | 'my'

  const [requestForm, setRequestForm] = useState(defaultRequestForm)
  const [editingRequestId, setEditingRequestId] = useState(null)
  const [isRequestFormOpen, setIsRequestFormOpen] = useState(false)
  const [requestFormError, setRequestFormError] = useState('')

  const [centerForm, setCenterForm] = useState(defaultCenterForm)
  const [editingCenterId, setEditingCenterId] = useState(null)
  const [isCenterFormOpen, setIsCenterFormOpen] = useState(false)
  const [centerFormError, setCenterFormError] = useState('')

  // Queries
  const { data: requests = [], isLoading: isLoadingRequests, isError: isErrorRequests } = useQuery({
    queryKey: ['blood-requests', selectedGroup, statusFilter],
    queryFn: () =>
      getBloodRequests({
        bloodGroup: selectedGroup !== 'ALL' ? selectedGroup : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
      }),
  })

  const { data: centers = [], isLoading: isLoadingCenters, isError: isErrorCenters } = useQuery({
    queryKey: ['blood-centers'],
    queryFn: getDonationCenters,
  })

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

  // Mutations for Blood Requests
  const saveRequestMutation = useMutation({
    mutationFn: (payload) =>
      editingRequestId ? updateBloodRequest(editingRequestId, payload) : createBloodRequest(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      setRequestForm(defaultRequestForm)
      setEditingRequestId(null)
      setIsRequestFormOpen(false)
      setRequestFormError('')
    },
    onError: (err) => {
      setRequestFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save blood request. Please check required fields.')
    },
  })

  const statusTransitionMutation = useMutation({
    mutationFn: ({ id, payload }) => updateBloodRequest(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
    },
    onError: (err) => {
      alert(err.response?.data?.detail || err.response?.data?.message || 'Failed to update status.')
    },
  })

  const deleteRequestMutation = useMutation({
    mutationFn: deleteBloodRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
    },
  })

  // Mutations for Centers
  const saveCenterMutation = useMutation({
    mutationFn: (payload) =>
      editingCenterId ? updateDonationCenter(editingCenterId, payload) : createDonationCenter(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blood-centers'] })
      setCenterForm(defaultCenterForm)
      setEditingCenterId(null)
      setIsCenterFormOpen(false)
      setCenterFormError('')
    },
    onError: (err) => {
      setCenterFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save donation center.')
    },
  })

  const deleteCenterMutation = useMutation({
    mutationFn: deleteDonationCenter,
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

    const units = parseInt(requestForm.unitsNeeded, 10)

    if (
      !requestForm.patientName.trim() ||
      !requestForm.hospitalLocation.trim() ||
      !requestForm.contactName.trim() ||
      !requestForm.contactPhone.trim() ||
      !units ||
      units < 1
    ) {
      setRequestFormError('Please fill out all required fields with valid units (minimum 1).')
      return
    }

    const payload = {
      patientName: requestForm.patientName.trim(),
      bloodGroup: requestForm.bloodGroup,
      unitsNeeded: units,
      hospitalLocation: requestForm.hospitalLocation.trim(),
      urgency: requestForm.urgency,
      contactName: requestForm.contactName.trim(),
      contactPhone: requestForm.contactPhone.trim(),
      additionalNotes: requestForm.additionalNotes ? requestForm.additionalNotes.trim() : '',
    }

    if (editingRequestId) {
      payload.status = requestForm.status || 'OPEN'
    }

    saveRequestMutation.mutate(payload)
  }

  function handleEditRequest(req) {
    setEditingRequestId(req.id)
    setRequestForm({
      patientName: req.patientName,
      bloodGroup: req.bloodGroup,
      unitsNeeded: req.unitsNeeded,
      hospitalLocation: req.hospitalLocation,
      urgency: req.urgency,
      status: req.status,
      contactName: req.contactName,
      contactPhone: req.contactPhone,
      additionalNotes: req.additionalNotes || '',
    })
    setIsRequestFormOpen(true)
  }

  function handleCancelRequestForm() {
    setEditingRequestId(null)
    setRequestForm(defaultRequestForm)
    setIsRequestFormOpen(false)
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
    setIsCenterFormOpen(true)
  }

  function handleCancelCenterForm() {
    setEditingCenterId(null)
    setCenterForm(defaultCenterForm)
    setIsCenterFormOpen(false)
    setCenterFormError('')
  }

  if (isLoadingRequests && isLoadingCenters) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading blood donation portal…</h1></main>
      </MainLayout>
    )
  }

  if (isErrorRequests || isErrorCenters) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Blood donation portal is unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Community health</p>
          <h1>Blood Donation</h1>
          <p className="subtle-text">
            Connect patients and donors, find urgent neighborhood blood requests, and locate verified donation centers.
          </p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {/* Main Tab Switcher */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <button
          type="button"
          className={`btn ${activeTab === 'requests' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('requests')}
        >
          🩸 Blood Requests ({requests.length})
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'centers' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('centers')}
        >
          🏥 Donation Centers ({centers.length})
        </button>
      </div>

      {activeTab === 'requests' ? (
        <section className="complaints-grid">
          {/* Main Requests Content */}
          <div>
            {/* Filter Bar */}
            <div className="panel" style={{ marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <strong style={{ fontSize: '0.9rem', marginRight: '0.5rem' }}>Blood Group:</strong>
                  <div style={{ display: 'inline-flex', gap: '0.35rem', flexWrap: 'wrap', marginTop: '0.35rem' }}>
                    <button
                      type="button"
                      className={`btn btn-small ${selectedGroup === 'ALL' ? 'btn-primary' : 'btn-ghost'}`}
                      onClick={() => setSelectedGroup('ALL')}
                    >
                      All
                    </button>
                    {BLOOD_GROUPS.map((bg) => (
                      <button
                        key={bg}
                        type="button"
                        className={`btn btn-small ${selectedGroup === bg ? 'btn-primary' : 'btn-ghost'}`}
                        onClick={() => setSelectedGroup(bg)}
                      >
                        {bg}
                      </button>
                    ))}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <button
                    type="button"
                    className={`btn btn-small ${scopeFilter === 'all' ? 'btn-secondary' : 'btn-ghost'}`}
                    onClick={() => setScopeFilter('all')}
                  >
                    All Requests
                  </button>
                  <button
                    type="button"
                    className={`btn btn-small ${scopeFilter === 'my' ? 'btn-secondary' : 'btn-ghost'}`}
                    onClick={() => setScopeFilter('my')}
                  >
                    My Requests ({myRequestsCount})
                  </button>
                </div>
              </div>
            </div>

            {/* Requests List */}
            <div className="notification-list">
              {visibleRequests.length === 0 ? (
                <div className="panel empty-state">
                  <h3>No blood requests found</h3>
                  <p className="muted">
                    {selectedGroup !== 'ALL'
                      ? `No requests currently match blood group ${selectedGroup}.`
                      : 'No active blood requests at this time.'}
                  </p>
                </div>
              ) : (
                visibleRequests.map((req) => {
                  const isOwner = user?.id && req.userId === user.id
                  const isUrgent = req.urgency === 'URGENT'
                  return (
                    <article
                      key={req.id}
                      className="panel"
                      style={{
                        borderLeft: isUrgent ? '4px solid #ef4444' : '4px solid var(--color-primary)',
                        marginBottom: '1rem',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                          <span
                            style={{
                              background: isUrgent ? '#fee2e2' : 'var(--color-secondary)',
                              color: isUrgent ? '#b91c1c' : 'var(--color-primary-deep)',
                              padding: '0.35rem 0.75rem',
                              borderRadius: 'var(--radius-sm)',
                              fontWeight: '800',
                              fontSize: '1.1rem',
                            }}
                          >
                            {req.bloodGroup}
                          </span>
                          <div>
                            <h3 style={{ margin: 0, fontSize: '1.15rem' }}>
                              {req.patientName} · {req.unitsNeeded} {req.unitsNeeded === 1 ? 'unit' : 'units'}
                            </h3>
                            <div className="small-muted" style={{ marginTop: '0.2rem' }}>
                              📍 {req.hospitalLocation}
                            </div>
                          </div>
                        </div>

                        <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                          {isUrgent && <span className="notification-badge error">🔴 URGENT</span>}
                          <span className={`status-pill ${req.status.toLowerCase()}`}>{req.status}</span>
                        </div>
                      </div>

                      {req.additionalNotes && (
                        <p style={{ marginTop: '0.75rem', color: 'var(--color-text-soft)', fontSize: '0.92rem' }}>
                          <strong>Notes:</strong> {req.additionalNotes}
                        </p>
                      )}

                      <div style={{ marginTop: '0.85rem', padding: '0.65rem 0.85rem', background: 'var(--color-surface-soft)', borderRadius: 'var(--radius-sm)', fontSize: '0.88rem' }}>
                        <strong>Contact Coordinator:</strong> {req.contactName} ·{' '}
                        <a href={`tel:${req.contactPhone}`} style={{ color: 'var(--color-primary-deep)', fontWeight: 600 }}>
                          {req.contactPhone}
                        </a>
                      </div>

                      {isOwner && (
                        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
                          {req.status === 'OPEN' && (
                            <>
                              <Button
                                variant="secondary"
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
                                onClick={() =>
                                  statusTransitionMutation.mutate({
                                    id: req.id,
                                    payload: { ...req, status: 'CANCELLED' },
                                  })
                                }
                              >
                                Cancel Request
                              </Button>
                              <Button variant="ghost" onClick={() => handleEditRequest(req)}>
                                Edit
                              </Button>
                            </>
                          )}
                          <Button variant="ghost" onClick={() => deleteRequestMutation.mutate(req.id)}>
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

          {/* Aside: Request Form & Stats */}
          <aside className="panel summary-panel">
            <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2>{editingRequestId ? 'Edit request' : 'Request blood'}</h2>
            </div>

            {requestFormError && (
              <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
                {requestFormError}
              </div>
            )}

            <form onSubmit={handleRequestSubmit} className="notification-form">
              <Input
                label="Patient name"
                name="patientName"
                value={requestForm.patientName}
                onChange={handleRequestFormChange}
                maxLength={120}
                required
              />

              <div className="split-fields">
                <label className="field">
                  <span>Blood group</span>
                  <select name="bloodGroup" value={requestForm.bloodGroup} onChange={handleRequestFormChange}>
                    {BLOOD_GROUPS.map((bg) => (
                      <option key={bg} value={bg}>
                        {bg}
                      </option>
                    ))}
                  </select>
                </label>

                <Input
                  label="Units needed"
                  name="unitsNeeded"
                  type="number"
                  min="1"
                  max="50"
                  value={requestForm.unitsNeeded}
                  onChange={handleRequestFormChange}
                  required
                />
              </div>

              <Input
                label="Hospital / Location"
                name="hospitalLocation"
                value={requestForm.hospitalLocation}
                onChange={handleRequestFormChange}
                maxLength={160}
                required
              />

              <label className="field">
                <span>Urgency level</span>
                <select name="urgency" value={requestForm.urgency} onChange={handleRequestFormChange}>
                  <option value="STANDARD">Standard</option>
                  <option value="URGENT">Urgent (Immediate need)</option>
                </select>
              </label>

              <div className="split-fields">
                <Input
                  label="Contact name"
                  name="contactName"
                  value={requestForm.contactName}
                  onChange={handleRequestFormChange}
                  maxLength={80}
                  required
                />
                <Input
                  label="Contact phone"
                  name="contactPhone"
                  value={requestForm.contactPhone}
                  onChange={handleRequestFormChange}
                  maxLength={80}
                  required
                />
              </div>

              <label className="field">
                <span>Additional notes</span>
                <textarea
                  name="additionalNotes"
                  value={requestForm.additionalNotes}
                  onChange={handleRequestFormChange}
                  rows={2}
                  maxLength={1000}
                />
              </label>

              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <Button type="submit" disabled={saveRequestMutation.isPending}>
                  {saveRequestMutation.isPending ? 'Saving…' : editingRequestId ? 'Update request' : 'Submit request'}
                </Button>
                {editingRequestId && (
                  <Button type="button" variant="ghost" onClick={handleCancelRequestForm}>
                    Cancel
                  </Button>
                )}
              </div>
            </form>
          </aside>
        </section>
      ) : (
        /* Donation Centers Tab */
        <section className="complaints-grid">
          <div>
            <div className="notification-list">
              {centers.length === 0 ? (
                <div className="panel empty-state">
                  <h3>No donation centers listed</h3>
                  <p className="muted">Community blood banks and donation centers will appear here.</p>
                </div>
              ) : (
                centers.map((center) => (
                  <article key={center.id} className="panel" style={{ marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <h3 style={{ margin: 0, fontSize: '1.2rem' }}>🏥 {center.name}</h3>
                        <div className="small-muted" style={{ marginTop: '0.25rem' }}>
                          📍 {center.location} · 📞 {center.contact}
                        </div>
                      </div>
                    </div>
                    <p style={{ marginTop: '0.75rem', color: 'var(--color-text)' }}>{center.description}</p>
                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                      <Button variant="secondary" onClick={() => handleEditCenter(center)}>
                        Edit
                      </Button>
                      <Button variant="ghost" onClick={() => deleteCenterMutation.mutate(center.id)}>
                        Delete
                      </Button>
                    </div>
                  </article>
                ))
              )}
            </div>
          </div>

          <aside className="panel summary-panel">
            <div className="panel-header">
              <h2>{editingCenterId ? 'Edit donation center' : 'Add donation center'}</h2>
            </div>

            {centerFormError && (
              <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
                {centerFormError}
              </div>
            )}

            <form onSubmit={handleCenterSubmit} className="notification-form">
              <Input
                label="Center name"
                name="name"
                value={centerForm.name}
                onChange={handleCenterFormChange}
                maxLength={120}
                required
              />
              <Input
                label="Location"
                name="location"
                value={centerForm.location}
                onChange={handleCenterFormChange}
                maxLength={160}
                required
              />
              <Input
                label="Contact phone / info"
                name="contact"
                value={centerForm.contact}
                onChange={handleCenterFormChange}
                maxLength={40}
                required
              />
              <label className="field">
                <span>Description / Hours</span>
                <textarea
                  name="description"
                  value={centerForm.description}
                  onChange={handleCenterFormChange}
                  rows={3}
                  maxLength={500}
                  required
                />
              </label>

              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <Button type="submit" disabled={saveCenterMutation.isPending}>
                  {saveCenterMutation.isPending ? 'Saving…' : editingCenterId ? 'Update center' : 'Add center'}
                </Button>
                {editingCenterId && (
                  <Button type="button" variant="ghost" onClick={handleCancelCenterForm}>
                    Cancel
                  </Button>
                )}
              </div>
            </form>
          </aside>
        </section>
      )}
    </MainLayout>
  )
}
