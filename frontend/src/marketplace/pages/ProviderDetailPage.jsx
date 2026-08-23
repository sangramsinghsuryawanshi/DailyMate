import { useEffect, useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { formatINR } from '../../utils/formatters'
import { getProvider, updateProvider, deleteProvider } from '../services/marketplaceApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Input,
  Select,
  Card,
  Badge,
  TrustBadge,
  ResponsiveContainer,
} from '../../design-system'
import './ProviderDetailPage.css'

const categoryOptions = ['Electrician', 'Plumber', 'Mechanic', 'Tutor', 'Carpenter', 'Cleaner', 'Painter', 'Other']

export default function ProviderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const queryClient = useQueryClient()

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'provider_detail', providerId: id })
  }, [id])

  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')

  const { data: provider, isLoading, isError } = useQuery({
    queryKey: ['marketplace-provider', id],
    queryFn: () => getProvider(id),
    enabled: Boolean(id),
  })

  const updateMutation = useMutation({
    mutationFn: (payload) => updateProvider(id, payload),
    onSuccess: (updated) => {
      queryClient.setQueryData(['marketplace-provider', id], updated)
      queryClient.invalidateQueries({ queryKey: ['marketplace-providers'] })
      setIsEditing(false)
      setErrorMessage('')
      trackEvent('provider_updated')
    },
    onError: (err) => {
      setErrorMessage(err.response?.data?.detail || err.response?.data?.message || 'Failed to update provider profile.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteProvider(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-providers'] })
      trackEvent('provider_deleted')
      navigate('/marketplace', { replace: true })
    },
    onError: (err) => {
      setErrorMessage(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete provider profile.')
    },
  })

  const handleStartEdit = () => {
    setEditForm({
      name: provider.name || '',
      category: provider.category || 'Electrician',
      description: provider.description || '',
      serviceArea: provider.serviceArea || '',
      phone: provider.phone || '',
      email: provider.email || '',
      hourlyRate: provider.hourlyRate != null ? String(provider.hourlyRate) : '',
    })
    setIsEditing(true)
    setErrorMessage('')
  }

  const handleEditSubmit = (e) => {
    e.preventDefault()
    setErrorMessage('')

    const payload = {
      name: editForm.name.trim(),
      category: editForm.category.trim(),
      description: editForm.description.trim(),
      serviceArea: editForm.serviceArea.trim(),
      phone: editForm.phone.trim() || null,
      email: editForm.email.trim() || null,
      hourlyRate: editForm.hourlyRate ? parseFloat(editForm.hourlyRate) : null,
    }

    updateMutation.mutate(payload)
  }

  const handleDelete = () => {
    if (window.confirm(`Are you sure you want to delete the listing for "${provider.name}"?`)) {
      deleteMutation.mutate()
    }
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Loading provider profile…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError || !provider) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Provider not found</h1>
          <p>This service provider listing does not exist or may have been removed.</p>
          <Link to="/marketplace">
            <Button variant="primary">Back to marketplace</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  const isOwner = user && (user.id === provider.userId || user.role === 'ADMIN')

  return (
    <MainLayout>
      <div className="dm-provider-detail-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Provider profile</span>
              <h1 className="dm-page-main-title">{provider.name}</h1>
              <p className="dm-page-subtitle">Service area: {provider.serviceArea}</p>
            </div>
            <div className="dm-page-header-actions">
              {isOwner && !isEditing && (
                <>
                  <Button variant="outline" size="md" onClick={handleStartEdit}>
                    Edit Listing
                  </Button>
                  <Button
                    variant="danger"
                    size="md"
                    onClick={handleDelete}
                    disabled={deleteMutation.isPending}
                  >
                    {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
                  </Button>
                </>
              )}
              <Link to="/marketplace">
                <Button variant="ghost" size="md">
                  Back to results
                </Button>
              </Link>
            </div>
          </div>

          {errorMessage && (
            <div className="dm-form-alert dm-form-alert--error" role="alert">
              <span>⚠️ {errorMessage}</span>
            </div>
          )}

          {/* Edit Form or Profile View */}
          {isEditing ? (
            <div className="dm-provider-main-card">
              <h2>Edit Provider Profile</h2>
              <form onSubmit={handleEditSubmit} className="dm-provider-modal-form">
                <Input
                  id="edit-name"
                  name="name"
                  label="Business / Provider Name"
                  required
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                />

                <div className="dm-select-group">
                  <label htmlFor="edit-category" className="dm-input-label">
                    Category
                  </label>
                  <select
                    id="edit-category"
                    name="category"
                    className="dm-select"
                    value={editForm.category}
                    onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
                  >
                    {categoryOptions.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <Input
                  id="edit-area"
                  name="serviceArea"
                  label="Service Area"
                  required
                  value={editForm.serviceArea}
                  onChange={(e) => setEditForm({ ...editForm, serviceArea: e.target.value })}
                />

                <Input
                  id="edit-rate"
                  name="hourlyRate"
                  label="Hourly Rate (₹)"
                  type="number"
                  value={editForm.hourlyRate}
                  onChange={(e) => setEditForm({ ...editForm, hourlyRate: e.target.value })}
                />

                <Input
                  id="edit-phone"
                  name="phone"
                  label="Contact Phone"
                  type="tel"
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                />

                <Input
                  id="edit-email"
                  name="email"
                  label="Email Address"
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                />

                <Input
                  id="edit-desc"
                  name="description"
                  label="Description / Services Offered"
                  required
                  value={editForm.description}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                />

                <div className="dm-modal-actions-row">
                  <Button type="button" variant="ghost" onClick={() => setIsEditing(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" variant="primary" isLoading={updateMutation.isPending}>
                    Save Changes
                  </Button>
                </div>
              </form>
            </div>
          ) : (
            <div className="dm-provider-profile-layout">
              {/* Main Info */}
              <div className="dm-provider-main-card">
                <div className="dm-provider-badges-strip">
                  <Badge variant="primary" size="md">
                    {provider.category}
                  </Badge>
                  <Badge variant="neutral" size="md">
                    📍 {provider.serviceArea}
                  </Badge>
                </div>

                <h3 style={{ marginTop: '1rem', marginBottom: '0.5rem' }}>About this Service</h3>
                <p style={{ color: 'var(--dm-color-text-soft)', lineHeight: 1.6, fontSize: '0.9375rem' }}>
                  {provider.description}
                </p>

                <h3 style={{ marginTop: '2rem', marginBottom: '0.75rem' }}>Business Details</h3>
                <dl className="dm-provider-info-list">
                  <div className="dm-provider-info-item">
                    <dt>Service Category</dt>
                    <dd>{provider.category}</dd>
                  </div>
                  <div className="dm-provider-info-item">
                    <dt>Coverage Area</dt>
                    <dd>{provider.serviceArea}</dd>
                  </div>
                  {provider.hourlyRate != null && (
                    <div className="dm-provider-info-item">
                      <dt>Standard Rate</dt>
                      <dd>{formatINR(provider.hourlyRate)} (hourly)</dd>
                    </div>
                  )}
                  {provider.phone && (
                    <div className="dm-provider-info-item">
                      <dt>Phone</dt>
                      <dd>{provider.phone}</dd>
                    </div>
                  )}
                  {provider.email && (
                    <div className="dm-provider-info-item">
                      <dt>Email</dt>
                      <dd>{provider.email}</dd>
                    </div>
                  )}
                </dl>
              </div>

              {/* Sidebar Booking / Contact Actions */}
              <div className="dm-provider-side-card">
                <div>
                  <span className="dm-section-eyebrow">Direct Contact</span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.25rem' }}>
                    {provider.hourlyRate != null ? `${formatINR(provider.hourlyRate)}/hr` : 'Custom Pricing'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Direct resident pricing · No platform fees
                  </p>
                </div>

                <div className="dm-provider-contact-actions">
                  {provider.phone ? (
                    <a
                      href={`tel:${provider.phone}`}
                      className="dm-button dm-button--primary dm-button--lg dm-button--full-width"
                      onClick={() => trackEvent('provider_contacted', { providerId: provider.id, phone: provider.phone })}
                    >
                      Call {provider.phone}
                    </a>
                  ) : (
                    <button className="dm-button dm-button--primary dm-button--lg dm-button--full-width" disabled>
                      No Phone Provided
                    </button>
                  )}

                  {provider.email && (
                    <a
                      href={`mailto:${provider.email}`}
                      className="dm-button dm-button--outline dm-button--md dm-button--full-width"
                    >
                      Send Email Inquiry
                    </a>
                  )}
                </div>
              </div>
            </div>
          )}
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
