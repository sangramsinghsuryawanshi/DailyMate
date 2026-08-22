import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { createGroceryItem, deleteGroceryItem, getGroceryItems, getMyGroceryItems, updateGroceryItem } from '../services/groceryApi'

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

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['grocery-items', selectedCategory, searchQuery],
    queryFn: () => getGroceryItems({ search: searchQuery, category: selectedCategory }),
  })

  const { data: myItems = [] } = useQuery({
    queryKey: ['grocery-my-items'],
    queryFn: getMyGroceryItems,
    enabled: Boolean(user?.id),
  })

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
    },
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    if (!form.name.trim() || !form.store.trim() || !form.location.trim() || !form.unit.trim()) {
      setFormError('Please fill out all required fields.')
      return
    }

    const numPrice = Number(form.price)
    if (isNaN(numPrice) || numPrice <= 0) {
      setFormError('Price must be greater than zero.')
      return
    }

    saveMutation.mutate({
      name: form.name.trim(),
      category: form.category.trim(),
      store: form.store.trim(),
      price: numPrice,
      unit: form.unit.trim(),
      location: form.location.trim(),
    })
  }

  function handleEdit(item) {
    setEditingId(item.id)
    setFormError('')
    setForm({
      name: item.name,
      category: item.category,
      store: item.store,
      price: String(item.price),
      unit: item.unit || '1 unit',
      location: item.location,
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading grocery prices…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Grocery comparison unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Shopping</p>
          <h1>Grocery Price Comparison</h1>
          <p className="subtle-text">Compare local grocery prices across stores and find the best deals in your area.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {/* Tab Switcher — matches Emergency/Blood pattern */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          className={`btn ${activeTab === 'compare' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('compare')}
        >
          🏷️ Price Comparison
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'my' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('my')}
        >
          📝 My Submissions ({myItems.length})
        </button>
      </div>

      <section className="complaints-grid">
        {/* Main content column */}
        <div>
          {/* Search + Category Filter — inside panel, matches Emergency pattern */}
          {activeTab === 'compare' && (
            <div className="panel" style={{ marginBottom: '1.25rem' }}>
              <div style={{ marginBottom: '0.75rem' }}>
                <input
                  type="search"
                  placeholder="Search products…"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="form-input"
                  aria-label="Search grocery products"
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
                <strong style={{ fontSize: '0.9rem', marginRight: '0.35rem' }}>Category:</strong>
                {CATEGORIES.map((cat) => (
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
            </div>
          )}

          {/* Price Comparison Cards or My Submissions List */}
          {activeTab === 'compare' ? (
            priceGroups.length === 0 ? (
              <div className="panel empty-state">
                <h3>No grocery prices yet</h3>
                <p className="muted">Be the first to submit local grocery prices for your community.</p>
              </div>
            ) : (
              <div className="notification-list">
                {priceGroups.map((group) => (
                  <article key={`${group.name}|${group.unit}`} className="panel" style={{ marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem' }}>
                      <span className="notification-badge info">{group.items[0]?.category}</span>
                      <span className="status-pill open">{group.unit}</span>
                      {group.items.length > 1 && (
                        <span className="status-pill resolved">{group.items.length} stores</span>
                      )}
                    </div>
                    <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.25rem' }}>{group.name}</h3>

                    {group.items.map((item, idx) => (
                      <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.4rem 0', borderBottom: idx < group.items.length - 1 ? '1px solid var(--border)' : 'none' }}>
                        <span>
                          {idx === 0 && group.items.length > 1 && <span title="Best price">🏷️ </span>}
                          <strong>{item.store}</strong>
                          <span className="small-muted"> · {item.location}</span>
                        </span>
                        <strong style={{ color: idx === 0 && group.items.length > 1 ? 'var(--success)' : 'inherit', fontSize: '1.1rem' }}>
                          {formatINR(item.price)}
                        </strong>
                      </div>
                    ))}
                  </article>
                ))}
              </div>
            )
          ) : (
            /* My Submissions tab */
            !user?.id ? (
              <div className="panel empty-state">
                <h3>Sign in to track your submissions</h3>
                <p className="muted">Log in to submit and manage your own grocery price entries.</p>
                <Link to="/login" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>Log in to submit prices</Link>
              </div>
            ) : myItems.length === 0 ? (
              <div className="panel empty-state">
                <h3>No submissions yet</h3>
                <p className="muted">Use the form to submit your first grocery price entry.</p>
              </div>
            ) : (
              <div className="notification-list">
                {myItems.map((item) => (
                  <article key={item.id} className="panel" style={{ marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div>
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem' }}>
                          <span className="notification-badge info">{item.category}</span>
                          <span className="status-pill open">{item.unit || '1 unit'}</span>
                        </div>
                        <h3 style={{ margin: 0, fontSize: '1.25rem' }}>{item.name}</h3>
                        <div className="small-muted" style={{ marginTop: '0.25rem' }}>
                          🏪 {item.store} · 📍 {item.location}
                        </div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>{formatINR(item.price)}</div>
                        <div className="small-muted">per {item.unit || '1 unit'}</div>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                      <Button variant="secondary" onClick={() => handleEdit(item)}>Edit</Button>
                      <Button variant="ghost" onClick={() => deleteMutation.mutate(item.id)}>Delete</Button>
                    </div>
                  </article>
                ))}
              </div>
            )
          )}
        </div>

        {/* Sidebar — Form + Summary (matches Emergency/Blood sidebar pattern) */}
        <aside>
          {/* Summary Stats */}
          <div className="panel" style={{ marginBottom: '1.25rem' }}>
            <div className="panel-header">
              <h2>Price Overview</h2>
            </div>
            <ul className="detail-list" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              <li><strong>Products tracked:</strong> {priceGroups.length}</li>
              <li><strong>Total price entries:</strong> {data.length}</li>
              <li><strong>Stores compared:</strong> {new Set(data.map((i) => i.store)).size}</li>
            </ul>
          </div>

          {/* Submit Price Form */}
          {user?.id && (
            <div className="panel">
              <div className="panel-header">
                <h2>{editingId ? 'Edit price entry' : 'Submit a price'}</h2>
              </div>

              <form className="notification-form" onSubmit={handleSubmit}>
                <Input label="Product name" name="name" value={form.name} onChange={handleChange} required />

                <div className="form-group">
                  <label htmlFor="category-select">Category</label>
                  <select id="category-select" name="category" value={form.category} onChange={handleChange} className="form-input">
                    {CATEGORIES.filter((c) => c !== 'ALL').map((cat) => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>

                <Input label="Store name" name="store" value={form.store} onChange={handleChange} required />
                <Input label="Price (₹)" type="number" step="0.01" min="0.01" name="price" value={form.price} onChange={handleChange} required />

                <div className="form-group">
                  <label htmlFor="unit-select">Unit / Quantity</label>
                  <select id="unit-select" name="unit" value={form.unit} onChange={handleChange} className="form-input">
                    {UNIT_OPTIONS.map((u) => (
                      <option key={u} value={u}>{u}</option>
                    ))}
                  </select>
                </div>

                <Input label="Location / Area" name="location" value={form.location} onChange={handleChange} required />

                {formError && <p className="form-error" role="alert">{formError}</p>}

                <div className="profile-actions">
                  <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update price' : 'Submit price'}</Button>
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
