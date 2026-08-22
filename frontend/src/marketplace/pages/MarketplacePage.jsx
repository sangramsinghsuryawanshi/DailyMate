import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { useAuth } from '../../hooks/useAuth'
import { getProviders, createProvider } from '../services/marketplaceApi'

const categoryOptions = ['All', 'Electrician', 'Plumber', 'Mechanic', 'Tutor', 'Carpenter', 'Cleaner', 'Painter']
const sortOptions = ['Name (A-Z)', 'Category', 'Price (Low to High)', 'Price (High to Low)']

export default function MarketplacePage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const { data: providers = [], isLoading, isError } = useQuery({
    queryKey: ['marketplace-providers'],
    queryFn: getProviders,
  })

  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('All')
  const [sortBy, setSortBy] = useState('Name (A-Z)')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [formError, setFormError] = useState('')

  const [formData, setFormData] = useState({
    name: '',
    category: 'Electrician',
    description: '',
    serviceArea: '',
    phone: '',
    email: '',
    hourlyRate: '',
  })

  const createMutation = useMutation({
    mutationFn: (payload) => createProvider(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['marketplace-providers'] })
      setShowCreateModal(false)
      setFormError('')
      setFormData({
        name: '',
        category: 'Electrician',
        description: '',
        serviceArea: '',
        phone: '',
        email: '',
        hourlyRate: '',
      })
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to create provider listing.')
    },
  })

  const handleCreateSubmit = (e) => {
    e.preventDefault()
    setFormError('')

    const payload = {
      name: formData.name.trim(),
      category: formData.category.trim(),
      description: formData.description.trim(),
      serviceArea: formData.serviceArea.trim(),
      phone: formData.phone.trim() || null,
      email: formData.email.trim() || null,
      hourlyRate: formData.hourlyRate ? parseFloat(formData.hourlyRate) : null,
    }

    if (!payload.name || !payload.category || !payload.description || !payload.serviceArea) {
      setFormError('Please fill out all required fields.')
      return
    }

    createMutation.mutate(payload)
  }

  const visibleProviders = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    const filtered = providers.filter((provider) => {
      const matchesQuery =
        !normalizedQuery ||
        [provider.name, provider.category, provider.description, provider.serviceArea].some((value) =>
          value?.toLowerCase().includes(normalizedQuery),
        )

      const matchesCategory = category === 'All' || provider.category === category
      return matchesQuery && matchesCategory
    })

    return [...filtered].sort((a, b) => {
      if (sortBy === 'Name (A-Z)') return (a.name || '').localeCompare(b.name || '')
      if (sortBy === 'Category') return (a.category || '').localeCompare(b.category || '')
      if (sortBy === 'Price (Low to High)') return (Number(a.hourlyRate) || 0) - (Number(b.hourlyRate) || 0)
      if (sortBy === 'Price (High to Low)') return (Number(b.hourlyRate) || 0) - (Number(a.hourlyRate) || 0)
      return 0
    })
  }, [category, providers, query, sortBy])

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading marketplace…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Marketplace unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Local services</p>
          <h1>Find trusted help nearby</h1>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          {user ? (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setShowCreateModal(true)}
            >
              + List your service
            </button>
          ) : (
            <Link to="/login" className="btn btn-secondary">
              Sign in to list service
            </Link>
          )}
          <Link to="/dashboard" className="btn btn-secondary">Back to dashboard</Link>
        </div>
      </section>

      {showCreateModal && (
        <div className="panel" style={{ marginBottom: '1.5rem', border: '2px solid var(--color-primary, #3b82f6)' }}>
          <div className="panel-header">
            <h2>Create service provider profile</h2>
            <button
              type="button"
              className="btn btn-small btn-secondary"
              onClick={() => { setShowCreateModal(false); setFormError('') }}
            >
              Cancel
            </button>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form onSubmit={handleCreateSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
            <label className="field">
              <span>Business / Provider Name *</span>
              <input
                type="text"
                required
                maxLength={80}
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. Apex Electrical Services"
              />
            </label>

            <label className="field">
              <span>Category *</span>
              <select
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                {categoryOptions.filter((c) => c !== 'All').map((cat) => (
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
                value={formData.serviceArea}
                onChange={(e) => setFormData({ ...formData, serviceArea: e.target.value })}
                placeholder="e.g. Downtown & Metro Area"
              />
            </label>

            <label className="field">
              <span>Hourly Rate ($)</span>
              <input
                type="number"
                step="0.01"
                min="0"
                value={formData.hourlyRate}
                onChange={(e) => setFormData({ ...formData, hourlyRate: e.target.value })}
                placeholder="e.g. 65.00"
              />
            </label>

            <label className="field">
              <span>Phone</span>
              <input
                type="tel"
                maxLength={20}
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="+1-555-0199"
              />
            </label>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                maxLength={120}
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="contact@business.example"
              />
            </label>

            <label className="field" style={{ gridColumn: '1 / -1' }}>
              <span>Description *</span>
              <textarea
                required
                rows={3}
                maxLength={500}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Describe your services, skills, and specialties..."
              />
            </label>

            <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => { setShowCreateModal(false); setFormError('') }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createMutation.isPending}
              >
                {createMutation.isPending ? 'Publishing…' : 'Publish Listing'}
              </button>
            </div>
          </form>
        </div>
      )}

      <section className="marketplace-toolbar panel">
        <label className="search-box search-wide">
          <span>What do you need help with?</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by provider name, category, area, or description..."
          />
        </label>

        <div className="marketplace-controls">
          <div className="pill-row" aria-label="Service categories">
            {categoryOptions.map((item) => (
              <button
                key={item}
                type="button"
                className={`pill ${category === item ? 'active' : ''}`}
                onClick={() => setCategory(item)}
              >
                {item}
              </button>
            ))}
          </div>

          <label className="field compact-field">
            <span>Sort by</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              {sortOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <section className="marketplace-layout">
        <div className="panel panel-large">
          <div className="panel-header">
            <h2>Available near you</h2>
            <span>{visibleProviders.length} providers</span>
          </div>

          {visibleProviders.length === 0 ? (
            <div className="empty-state">
              <h3>No providers match your search.</h3>
              <p>Try a different keyword or reset the category filter to browse nearby services.</p>
              <button type="button" className="btn btn-primary" onClick={() => { setQuery(''); setCategory('All') }}>
                Reset filters
              </button>
            </div>
          ) : (
            <div className="provider-list">
              {visibleProviders.map((provider) => (
                <article key={provider.id} className="provider-card">
                  <div className="provider-avatar">{provider.name.slice(0, 1)}</div>

                  <div className="provider-copy">
                    <div className="provider-row">
                      <h3>{provider.name}</h3>
                      <span className="badge">{provider.category}</span>
                    </div>
                    <p className="muted">Area: {provider.serviceArea}</p>
                    <p className="provider-description">{provider.description}</p>
                  </div>

                  <div className="provider-price">
                    <strong>
                      {provider.hourlyRate != null
                        ? `${formatINR(provider.hourlyRate)}/hr`
                        : 'Contact for quote'}
                    </strong>
                    <Link to={`/marketplace/${provider.id}`} className="btn btn-small btn-primary">
                      View profile
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        <aside className="side-stack">
          <div className="panel">
            <div className="panel-header">
              <h2>Service Categories</h2>
            </div>
            <div className="tag-cloud">
              {categoryOptions.filter((c) => c !== 'All').map((cat) => (
                <button
                  key={cat}
                  type="button"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                  onClick={() => setCategory(cat)}
                >
                  <span className={category === cat ? 'badge' : ''}>{cat}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>About DailyMate Services</h2>
            </div>
            <ul className="list-stack">
              <li>Direct local provider contact</li>
              <li>Clear upfront service areas</li>
              <li>Transparent hourly pricing</li>
            </ul>
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
