import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createGroceryItem, deleteGroceryItem, getGroceryItems, updateGroceryItem } from '../services/groceryApi'

const defaultForm = {
  name: '',
  category: '',
  store: '',
  price: '',
  location: '',
}

export default function GroceryPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['grocery-items'],
    queryFn: getGroceryItems,
  })

  const totalPrice = useMemo(
    () => data.reduce((sum, item) => sum + Number(item.price || 0), 0),
    [data],
  )

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateGroceryItem(editingId, payload) : createGroceryItem(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grocery-items'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteGroceryItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['grocery-items'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate({ ...form, price: Number(form.price) })
  }

  function handleEdit(item) {
    setEditingId(item.id)
    setForm({
      name: item.name,
      category: item.category,
      store: item.store,
      price: String(item.price),
      location: item.location,
    })
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
          <h1>Grocery comparison</h1>
          <p className="subtle-text">Track local product prices and compare where your essentials cost less.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="grocery-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit item' : 'Add grocery item'}</h2>
          </div>

          <form className="notification-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Item name" name="name" value={form.name} onChange={handleChange} required />
              <Input label="Category" name="category" value={form.category} onChange={handleChange} required />
            </div>

            <div className="split-fields">
              <Input label="Store" name="store" value={form.store} onChange={handleChange} required />
              <Input label="Price" type="number" step="0.01" min="0.01" name="price" value={form.price} onChange={handleChange} required />
            </div>

            <Input label="Location" name="location" value={form.location} onChange={handleChange} required />

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : editingId ? 'Update item' : 'Add item'}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Price overview</h2>
          </div>
          <div className="detail-list">
            <li><strong>Total listed:</strong> ${totalPrice.toFixed(2)}</li>
            <li><strong>Products:</strong> {data.length}</li>
            <li><strong>Best savings:</strong> {data.length > 0 ? 'Compare stores' : 'No items yet'}</li>
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {data.length === 0 ? (
              <div className="empty-state">
                <h3>No grocery prices yet</h3>
                <p className="muted">Add local shopping prices to compare options.</p>
              </div>
            ) : data.map((item) => (
              <article key={item.id} className="notification-item">
                <div className="notification-head">
                  <span className="notification-badge reminder">{item.category}</span>
                  <span className="small-muted">{item.store}</span>
                </div>
                <h3>{item.name}</h3>
                <p><strong>${Number(item.price).toFixed(2)}</strong> · {item.location}</p>
                <div className="notification-actions">
                  <Button variant="secondary" onClick={() => handleEdit(item)}>Edit</Button>
                  <Button variant="ghost" onClick={() => deleteMutation.mutate(item.id)}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
