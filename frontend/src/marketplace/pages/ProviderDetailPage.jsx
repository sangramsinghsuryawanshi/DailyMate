import { useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { formatINR } from '../../utils/formatters'
import { getProvider, updateProvider, deleteProvider } from '../services/marketplaceApi'

const categoryOptions = ['Electrician', 'Plumber', 'Mechanic', 'Tutor', 'Carpenter', 'Cleaner', 'Painter', 'Other']

export default function ProviderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const queryClient = useQueryClient()

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
    },
    onError: (err) => {
      setErrorMessage(err.response?.data?.detail || err.response?.data?.message || 'Failed to update provider profile.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteProvider(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-providers'] })
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
        <main className="page-state"><h1>Loading provider profile…</h1></main>
      </MainLayout>
    )
  }

  if (isError || !provider) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Provider not found</h1>
          <p>This service provider listing does not exist or may have been removed.</p>
          <Link to="/marketplace" className="btn btn-primary">Back to marketplace</Link>
        </main>
      </MainLayout>
    )
  }

  const isOwner = user && (user.id === provider.userId || user.role === 'ADMIN')

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Provider profile</p>
          <h1>{provider.name}</h1>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          {isOwner && !isEditing && (
            <>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleStartEdit}
              >
                Edit Listing
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                style={{ color: '#ef4444', borderColor: '#ef4444' }}
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
              </button>
            </>
          )}
          <Link to="/marketplace" className="btn btn-secondary">Back to results</Link>
        </div>
      </section>

      {errorMessage && (
        <div style={{ color: '#ef4444', margin: '1rem 0', padding: '0.75rem', background: '#fee2e2', borderRadius: '6px' }}>
          {errorMessage}
        </div>
      )}

      {isEditing ? (
        <section className="panel panel-large">
          <div className="panel-header">
            <h2>Edit Provider Profile</h2>
            <button
              type="button"
              className="btn btn-small btn-secondary"
              onClick={() => { setIsEditing(false); setErrorMessage('') }}
            >
              Cancel
            </button>
          </div>

          <form onSubmit={handleEditSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginTop: '1rem' }}>
            <label className="field">
              <span>Business / Provider Name *</span>
              <input
                type="text"
                required
                maxLength={80}
                value={editForm.name}
                onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              />
            </label>

            <label className="field">
              <span>Category *</span>
              <select
                value={editForm.category}
                onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
              >
                {categoryOptions.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Service Area *</span>
              <input
                type="text"
                required
                maxLength={120}
                value={editForm.serviceArea}
                onChange={(e) => setEditForm({ ...editForm, serviceArea: e.target.value })}
              />
            </label>

            <label className="field">
              <span>Hourly Rate ($)</span>
              <input
                type="number"
                step="0.01"
                min="0"
                value={editForm.hourlyRate}
                onChange={(e) => setEditForm({ ...editForm, hourlyRate: e.target.value })}
              />
            </label>

            <label className="field">
              <span>Phone</span>
              <input
                type="tel"
                maxLength={20}
                value={editForm.phone}
                onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
              />
            </label>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                maxLength={120}
                value={editForm.email}
                onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
              />
            </label>

            <label className="field" style={{ gridColumn: '1 / -1' }}>
              <span>Description *</span>
              <textarea
                required
                rows={4}
                maxLength={500}
                value={editForm.description}
                onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
              />
            </label>

            <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => { setIsEditing(false); setErrorMessage('') }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={updateMutation.isPending}
              >
                {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </form>
        </section>
      ) : (
        <section className="detail-layout">
          <div className="panel panel-large provider-hero">
            <div className="provider-avatar large-avatar">{provider.name.slice(0, 1)}</div>
            <div className="provider-identity">
              <div className="provider-row">
                <h2>{provider.name}</h2>
                <span className="badge">{provider.category}</span>
              </div>
              <p className="muted">Service area: {provider.serviceArea}</p>
            </div>
            <div className="provider-price detail-price">
              <strong>
                {provider.hourlyRate != null
                  ? `${formatINR(provider.hourlyRate)}/hr`
                  : 'Contact for quote'}
              </strong>
              {provider.phone ? (
                <a href={`tel:${provider.phone}`} className="btn btn-primary">
                  Call {provider.phone}
                </a>
              ) : provider.email ? (
                <a href={`mailto:${provider.email}`} className="btn btn-primary">
                  Email Provider
                </a>
              ) : null}
            </div>
          </div>

          <div className="detail-grid">
            <div className="panel">
              <div className="panel-header">
                <h2>About the Service</h2>
              </div>
              <p className="detail-copy" style={{ whiteSpace: 'pre-line' }}>{provider.description}</p>
            </div>

            <div className="panel">
              <div className="panel-header">
                <h2>Contact & Service Details</h2>
              </div>
              <ul className="detail-list">
                <li><strong>Category:</strong> {provider.category}</li>
                <li><strong>Service Area:</strong> {provider.serviceArea}</li>
                {provider.hourlyRate != null && (
                  <li><strong>Standard Rate:</strong> ${Number(provider.hourlyRate).toFixed(2)} per hour</li>
                )}
                {provider.phone && (
                  <li><strong>Phone:</strong> <a href={`tel:${provider.phone}`}>{provider.phone}</a></li>
                )}
                {provider.email && (
                  <li><strong>Email:</strong> <a href={`mailto:${provider.email}`}>{provider.email}</a></li>
                )}
              </ul>
            </div>
          </div>
        </section>
      )}
    </MainLayout>
  )
}
