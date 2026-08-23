import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { useAuth } from '../../hooks/useAuth'
import { getProviders, createProvider } from '../services/marketplaceApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Input,
  Select,
  Card,
  StatCard,
  Badge,
  TrustBadge,
  Skeleton,
  Pagination,
  EmptyState,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import './MarketplacePage.css'

const categoryOptions = ['All', 'Electrician', 'Plumber', 'Mechanic', 'Tutor', 'Carpenter', 'Cleaner', 'Painter']
const sortOptions = ['Name (A-Z)', 'Category', 'Price (Low to High)', 'Price (High to Low)']

export default function MarketplacePage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('All')
  const [sortBy, setSortBy] = useState('Name (A-Z)')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [formError, setFormError] = useState('')

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'marketplace' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['marketplace-providers', { page, pageSize, query, category }],
    queryFn: () => getProviders({ page, size: pageSize, search: query || undefined, category: category !== 'All' ? category : undefined }),
    placeholderData: (previousData) => previousData,
  })

  const providers = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? providers.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

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
      trackEvent('provider_created')
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

  // Statistics calculation
  const totalVerified = useMemo(() => providers.length, [providers])
  const categoriesCount = useMemo(() => new Set(providers.map((p) => p.category)).size, [providers])

  return (
    <MainLayout>
      <div className="dm-marketplace-page">
        <ResponsiveContainer size="wide">
          {/* ===================================================================
              1. PAGE HEADER
             =================================================================== */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Local Services & Trade</span>
              <h1 className="dm-page-main-title">Marketplace</h1>
              <p className="dm-page-subtitle">
                Discover trusted local electricians, plumbers, tutors, and verified home service providers.
              </p>
            </div>
            <div className="dm-page-header-actions">
              {user ? (
                <Button
                  variant="primary"
                  size="md"
                  iconLeft="+"
                  onClick={() => setShowCreateModal(true)}
                >
                  + List your service
                </Button>
              ) : (
                <Link to="/login">
                  <Button variant="primary" size="md">
                    Sign in to list service
                  </Button>
                </Link>
              )}
            </div>
          </div>

          {/* Key Metrics Grid */}
          <div className="dm-marketplace-stats-grid">
            <StatCard
              domain="marketplace"
              label="Active Providers"
              value={String(totalVerified)}
              trend="Verified"
              trendDirection="up"
              trendLabel="Direct contact"
              icon="🛠️"
            />
            <StatCard
              domain="marketplace"
              label="Trade Categories"
              value={String(categoriesCount || 8)}
              trend="Available"
              trendDirection="neutral"
              trendLabel="Home & Technical"
              icon="⚡"
            />
            <StatCard
              domain="marketplace"
              label="Average Response"
              value="< 15 Mins"
              trend="High speed"
              trendDirection="up"
              trendLabel="Direct phone/email"
              icon="📞"
            />
            <StatCard
              domain="marketplace"
              label="Zero Commission"
              value="100% Direct"
              trend="No middleman"
              trendDirection="up"
              trendLabel="Direct resident pricing"
              icon="🤝"
            />
          </div>

          {/* ===================================================================
              2. SEARCH & FILTER TOOLBAR
             =================================================================== */}
          <div className="dm-marketplace-toolbar">
            <div className="dm-marketplace-search-row">
              <input
                type="search"
                className="dm-marketplace-search-input"
                placeholder="Search for a plumber, electrician, tutor, mechanic, or location…"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                  trackEvent('search_started', { query: e.target.value })
                }}
              />
              <select
                className="dm-marketplace-sort-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                aria-label="Sort providers"
              >
                {sortOptions.map((opt) => (
                  <option key={opt} value={opt}>
                    {opt}
                  </option>
                ))}
              </select>
            </div>

            {/* Category Pills */}
            <div className="dm-marketplace-category-pills" role="tablist" aria-label="Provider Categories">
              {categoryOptions.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`dm-category-filter-btn ${category === cat ? 'active' : ''}`}
                  onClick={() => setCategory(cat)}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* ===================================================================
              3. PROVIDERS DIRECTORY GRID
             =================================================================== */}
          {isLoading ? (
            <div className="dm-providers-grid">
              <Skeleton variant="card" height="220px" />
              <Skeleton variant="card" height="220px" />
              <Skeleton variant="card" height="220px" />
            </div>
          ) : isError ? (
            <div className="dm-error-box">
              <p>Unable to load marketplace providers.</p>
              <Button variant="outline" size="sm" onClick={() => refetch()}>
                Retry
              </Button>
            </div>
          ) : visibleProviders.length > 0 ? (
            <div className="dm-providers-grid">
              {visibleProviders.map((provider) => (
                <div key={provider.id} className="dm-provider-card">
                  <div>
                    <div className="dm-provider-card-top">
                      <div className="dm-provider-title-wrap">
                        <h3>{provider.name}</h3>
                        <span className="dm-provider-category-tag">{provider.category}</span>
                      </div>
                      {provider.hourlyRate != null && (
                        <span className="dm-provider-rate">
                          {formatINR(provider.hourlyRate)}/hr
                        </span>
                      )}
                    </div>

                    <p className="dm-provider-description">{provider.description}</p>
                  </div>

                  <div>
                    <div className="dm-provider-meta-row">
                      <span>Area: {provider.serviceArea}</span>
                    </div>

                    <div className="dm-provider-card-actions">
                      <Link to={`/marketplace/${provider.id}`} className="dm-btn-provider-profile">
                        <Button variant="outline" size="sm" fullWidth>
                          View Profile
                        </Button>
                      </Link>
                      {provider.phone && (
                        <a
                          href={`tel:${provider.phone}`}
                          onClick={() => trackEvent('provider_contacted', { providerId: provider.id, phone: provider.phone })}
                        >
                          <Button variant="secondary" size="sm" iconLeft="📞">
                            Call
                          </Button>
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="dm-empty-dashboard-box">
              <p>No service providers found matching "{query || category}".</p>
              <small>Try selecting a different category or clearing search filters.</small>
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
        </ResponsiveContainer>
      </div>

      {/* ===================================================================
          LISTING MODAL
         =================================================================== */}
      {showCreateModal && (
        <div className="dm-modal-backdrop" role="dialog" aria-modal="true">
          <div className="dm-modal dm-modal--md">
            <div className="dm-modal__header">
              <h2 className="dm-modal__title">Create service provider profile</h2>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="dm-modal__close"
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <div className="dm-modal__body">
              {formError && (
                <div className="dm-form-alert dm-form-alert--error" role="alert">
                  <span>⚠️ {formError}</span>
                </div>
              )}

              <form onSubmit={handleCreateSubmit} className="dm-provider-modal-form">
                <Input
                  id="p-name"
                  name="name"
                  label="Business / Provider Name"
                  required
                  placeholder="e.g. Apex Electrical Solutions"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />

                <div className="dm-select-group">
                  <label htmlFor="p-category" className="dm-input-label">
                    Category
                  </label>
                  <select
                    id="p-category"
                    name="category"
                    className="dm-select"
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  >
                    {categoryOptions.filter((c) => c !== 'All').map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <Input
                  id="p-area"
                  name="serviceArea"
                  label="Service Area"
                  required
                  placeholder="e.g. Kothrud, Baner, Pune West"
                  value={formData.serviceArea}
                  onChange={(e) => setFormData({ ...formData, serviceArea: e.target.value })}
                />

                <Input
                  id="p-rate"
                  name="hourlyRate"
                  label="Hourly Rate (₹)"
                  type="number"
                  placeholder="e.g. 150"
                  value={formData.hourlyRate}
                  onChange={(e) => setFormData({ ...formData, hourlyRate: e.target.value })}
                />

                <Input
                  id="p-phone"
                  name="phone"
                  label="Contact Phone"
                  type="tel"
                  placeholder="+91 98220 12345"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />

                <Input
                  id="p-desc"
                  name="description"
                  label="Description / Services Offered"
                  required
                  placeholder="Describe your expertise, experience, and service guarantees"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />

                <div className="dm-modal-actions-row">
                  <Button type="button" variant="ghost" onClick={() => setShowCreateModal(false)}>
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    isLoading={createMutation.isPending}
                  >
                    Publish Listing
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </MainLayout>
  )
}
