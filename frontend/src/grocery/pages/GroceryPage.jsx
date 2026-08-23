import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { createGroceryItem, deleteGroceryItem, getGroceryItems, getMyGroceryItems, updateGroceryItem } from '../services/groceryApi'
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
import './GroceryPage.css'

const defaultForm = {
  name: '',
  category: 'Grains & Pulses',
  store: '',
  price: '',
  unit: '1 kg',
  location: '',
}

const CATEGORIES = ['ALL', 'Dairy & Eggs', 'Grains & Pulses', 'Produce & Fruits', 'Snacks & Beverages', 'Personal Care', 'Household', 'Other']

const UNIT_OPTIONS = ['1 kg', '500 g', '250 g', '100 g', '1 L', '500 mL', '1 dozen', '1 unit', '1 pack']

export default function GroceryPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [formError, setFormError] = useState('')
  const [activeTab, setActiveTab] = useState('compare') // 'compare' | 'my'
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'grocery' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError } = useQuery({
    queryKey: ['grocery-items', { page, pageSize, selectedCategory, searchQuery }],
    queryFn: () =>
      getGroceryItems({
        page,
        size: pageSize,
        search: searchQuery,
        category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      }),
    placeholderData: (previousData) => previousData,
  })

  const { data: myPageData = { content: [], totalElements: 0, totalPages: 0 } } = useQuery({
    queryKey: ['grocery-my-items', { page, pageSize }],
    queryFn: () => getMyGroceryItems({ page, size: pageSize }),
    enabled: Boolean(user?.id),
    placeholderData: (previousData) => previousData,
  })

  const data = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? data.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  const myItems = useMemo(() => {
    if (Array.isArray(myPageData)) return myPageData
    return myPageData.content ?? []
  }, [myPageData])

  const totalMyElements = Array.isArray(myPageData) ? myPageData.length : (myPageData.totalElements ?? myItems.length)
  const totalMyPages = Array.isArray(myPageData) ? 1 : (myPageData.totalPages ?? 1)

  // Group items by name+unit for price comparison
  const priceGroups = useMemo(() => {
    const groups = {}
    data.forEach((item) => {
      const key = `${item.name.toLowerCase().trim()}|${(item.unit || '1 unit').toLowerCase().trim()}`
      if (!groups[key]) {
        groups[key] = { name: item.name, unit: item.unit || '1 unit', items: [] }
      }
      groups[key].items.push(item)
    })
    Object.values(groups).forEach((group) => {
      group.items.sort((a, b) => Number(a.price) - Number(b.price))
      group.lowestPrice = Number(group.items[0].price)
    })
    return Object.values(groups).sort((a, b) => a.name.localeCompare(b.name))
  }, [data])

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateGroceryItem(editingId, payload) : createGroceryItem(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grocery-items'] })
      queryClient.invalidateQueries({ queryKey: ['grocery-my-items'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      trackEvent('grocery_price_saved')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save grocery item.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteGroceryItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grocery-items'] })
      queryClient.invalidateQueries({ queryKey: ['grocery-my-items'] })
      if (editingId) {
        setEditingId(null)
        setForm(defaultForm)
      }
      trackEvent('grocery_price_deleted')
    },
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    if (!form.name.trim() || !form.store.trim() || !form.location.trim() || !form.price) {
      setFormError('Please fill out all required fields.')
      return
    }

    const numericPrice = parseFloat(form.price)
    if (isNaN(numericPrice) || numericPrice <= 0) {
      setFormError('Price must be greater than zero.')
      return
    }

    saveMutation.mutate({
      name: form.name.trim(),
      category: form.category,
      store: form.store.trim(),
      price: numericPrice,
      unit: form.unit,
      location: form.location.trim(),
    })
  }

  function handleEdit(item) {
    setEditingId(item.id)
    setForm({
      name: item.name,
      category: item.category || 'Grains & Pulses',
      store: item.store,
      price: String(item.price),
      unit: item.unit || '1 kg',
      location: item.location || '',
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
          <h1>Loading grocery price comparison…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load grocery prices</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-grocery-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Household Supplies & Economy</span>
              <h1 className="dm-page-main-title">Grocery Price Comparison</h1>
              <p className="dm-page-subtitle">
                Crowdsourced neighborhood supermarket prices, local kirana deals, and smart grocery expense optimization.
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
          <div className="dm-grocery-stats-grid">
            <StatCard
              domain="expense"
              label="Products Tracked"
              value={String(priceGroups.length)}
              trend="Catalog"
              trendDirection="up"
              trendLabel="Unique items"
              icon="🛒"
            />
            <StatCard
              domain="expense"
              label="Price Submissions"
              value={String(data.length)}
              trend="Crowdsourced"
              trendDirection="neutral"
              trendLabel="Verified local rates"
              icon="🏷️"
            />
            <StatCard
              domain="expense"
              label="My Contributions"
              value={String(myItems.length)}
              trend="Owner"
              trendDirection="up"
              trendLabel="Created by you"
              icon="📌"
            />
            <StatCard
              domain="expense"
              label="Best Value Deals"
              value="Live"
              trend="Real-time"
              trendDirection="up"
              trendLabel="Lowest store rates"
              icon="⚡"
            />
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-provider-profile-layout">
            {/* Left Column: Compare Feed / My Submissions */}
            <div>
              {/* Tab Navigation */}
              <div className="dm-events-toolbar">
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'compare' ? 'active' : ''}`}
                    onClick={() => setActiveTab('compare')}
                  >
                    Price Comparison
                  </button>
                  <button
                    type="button"
                    className={`dm-category-filter-btn ${activeTab === 'my' ? 'active' : ''}`}
                    onClick={() => setActiveTab('my')}
                  >
                    My Submissions ({myItems.length})
                  </button>
                </div>
              </div>

              {activeTab === 'compare' ? (
                <div>
                  {/* Search and Category Filters */}
                  <div className="dm-grocery-toolbar">
                    <input
                      type="text"
                      className="dm-search-input"
                      style={{ padding: '0.625rem 1rem', width: '100%' }}
                      placeholder="Search grocery products (e.g. Milk, Rice, Oil)..."
                      aria-label="Search grocery products"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />

                    <div className="dm-grocery-categories-bar">
                      {CATEGORIES.map((cat) => (
                        <button
                          key={cat}
                          type="button"
                          className={`dm-blood-group-pill ${selectedCategory === cat ? 'active' : ''}`}
                          onClick={() => setSelectedCategory(cat)}
                        >
                          {cat === 'ALL' ? 'All' : cat}
                        </button>
                      ))}
                    </div>
                  </div>

                  {priceGroups.length === 0 ? (
                    <div className="dm-empty-dashboard-box">
                      <h3>No grocery prices yet</h3>
                      <p>Be the first to submit a local store price using the submission form.</p>
                    </div>
                  ) : (
                    <>
                    <div className="dm-grocery-card-feed">
                      {priceGroups.map((group) => (
                        <article key={`${group.name}-${group.unit}`} className="dm-grocery-group-card">
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div>
                              <h3 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 800 }}>{group.name}</h3>
                              <span style={{ fontSize: '0.8125rem', color: 'var(--dm-color-text-soft)' }}>
                                Unit: {group.unit} · {group.items.length} store {group.items.length === 1 ? 'quote' : 'quotes'}
                              </span>
                            </div>
                            <Badge variant="primary" size="md">
                              From {formatINR(group.lowestPrice)}
                            </Badge>
                          </div>

                          <table className="dm-grocery-store-table">
                            <thead>
                              <tr>
                                <th>Store</th>
                                <th>Location</th>
                                <th>Price</th>
                                <th>Value</th>
                              </tr>
                            </thead>
                            <tbody>
                              {group.items.map((item) => {
                                const isCheapest = Number(item.price) === group.lowestPrice
                                return (
                                  <tr key={item.id} style={isCheapest ? { backgroundColor: 'var(--dm-color-primary-soft, rgba(31, 143, 116, 0.08))' } : {}}>
                                    <td>
                                      <strong>{item.store}</strong>
                                    </td>
                                    <td>{item.location || '—'}</td>
                                    <td>
                                      <strong style={{ color: isCheapest ? 'var(--dm-color-primary-deep)' : 'inherit' }}>
                                        {formatINR(item.price)}
                                      </strong>
                                    </td>
                                    <td>
                                      {isCheapest && (
                                        <span title="Best price" style={{ fontWeight: 700, color: 'var(--dm-color-primary-deep)' }}>
                                          🏷️ Best price
                                        </span>
                                      )}
                                    </td>
                                  </tr>
                                )
                              })}
                            </tbody>
                          </table>
                        </article>
                      ))}
                    </div>

                    {/* Server-Driven Pagination for Grocery Search */}
                    <Pagination
                      page={page}
                      totalPages={totalPages}
                      totalElements={totalElements}
                      pageSize={pageSize}
                      onPageChange={setPage}
                      onPageSizeChange={setPageSize}
                      disabled={isLoading}
                    />
                  </>
                  )}
                </div>
              ) : (
                <div>
                  {myItems.length === 0 ? (
                    <div className="dm-empty-dashboard-box">
                      <h3>No personal submissions yet</h3>
                      <p>Submit grocery prices to track prices and help neighbors discover deals.</p>
                    </div>
                  ) : (
                    <>
                    <div className="dm-grocery-card-feed">
                      {myItems.map((item) => (
                        <article key={item.id} className="dm-grocery-group-card">
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <div>
                              <h3 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 800 }}>{item.name}</h3>
                              <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem', color: 'var(--dm-color-text-soft)' }}>
                                🏪 {item.store} · 📍 {item.location}
                              </p>
                              <span style={{ fontSize: '0.8125rem', color: 'var(--dm-color-text-soft)' }}>
                                per {item.unit}
                              </span>
                            </div>
                            <strong style={{ fontSize: '1.25rem', color: 'var(--dm-color-primary-deep)' }}>
                              {formatINR(item.price)}
                            </strong>
                          </div>

                          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem', borderTop: '1px solid var(--dm-color-border)', paddingTop: '0.5rem' }}>
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

                    {/* Server-Driven Pagination for My Submissions */}
                    <Pagination
                      page={page}
                      totalPages={totalMyPages}
                      totalElements={totalMyElements}
                      pageSize={pageSize}
                      onPageChange={setPage}
                      onPageSizeChange={setPageSize}
                    />
                  </>
                  )}
                </div>
              )}
            </div>

            {/* Right Column: Sticky Submission Form */}
            <div>
              <div className="dm-provider-side-card">
                <div>
                  <span className="dm-section-eyebrow">
                    {editingId ? 'Edit Price' : 'Submit Grocery Price'}
                  </span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                    {editingId ? 'Update Price Entry' : 'Add Store Price'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Log local rates for rice, oil, milk, lentils, vegetables, or household essentials.
                  </p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="dm-provider-modal-form">
                  <Input
                    id="g-name"
                    name="name"
                    label="Product name"
                    placeholder="e.g. Amul Taaza Milk, Toor Dal"
                    value={form.name}
                    onChange={handleChange}
                    required
                  />

                  <div className="dm-select-group">
                    <label htmlFor="g-cat" className="dm-input-label">
                      Category
                    </label>
                    <select
                      id="g-cat"
                      name="category"
                      className="dm-select"
                      value={form.category}
                      onChange={handleChange}
                    >
                      {CATEGORIES.filter((c) => c !== 'ALL').map((cat) => (
                        <option key={cat} value={cat}>
                          {cat}
                        </option>
                      ))}
                    </select>
                  </div>

                  <Input
                    id="g-store"
                    name="store"
                    label="Store name"
                    placeholder="e.g. D-Mart, Local Kirana, Fresh Mart"
                    value={form.store}
                    onChange={handleChange}
                    required
                  />

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                    <Input
                      id="g-price"
                      name="price"
                      label="Price (₹)"
                      type="number"
                      step="0.01"
                      placeholder="e.g. 66.00"
                      value={form.price}
                      onChange={handleChange}
                      required
                    />

                    <div className="dm-select-group">
                      <label htmlFor="g-unit" className="dm-input-label">
                        Unit
                      </label>
                      <select
                        id="g-unit"
                        name="unit"
                        className="dm-select"
                        value={form.unit}
                        onChange={handleChange}
                      >
                        {UNIT_OPTIONS.map((u) => (
                          <option key={u} value={u}>
                            {u}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <Input
                    id="g-loc"
                    name="location"
                    label="Location / Area"
                    placeholder="e.g. Kothrud, Pune / MG Road"
                    value={form.location}
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
                      {editingId ? 'Update price' : 'Submit price'}
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
