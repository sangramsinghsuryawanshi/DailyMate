import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { createLostFoundPost, deleteLostFoundPost, getLostFoundPosts, updateLostFoundPost } from '../services/lostFoundApi'

const defaultForm = {
  title: '',
  itemType: '',
  location: '',
  description: '',
  contactName: '',
  contactPhone: '',
}

export default function LostFoundPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [tab, setTab] = useState('all') // 'all' | 'my'
  const [formError, setFormError] = useState('')

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['lost-found-posts'],
    queryFn: getLostFoundPosts,
  })

  const myPostsCount = useMemo(() => {
    if (!user?.id) return 0
    return data.filter((item) => item.userId === user.id).length
  }, [data, user])

  const visiblePosts = useMemo(() => {
    if (tab === 'my') {
      if (!user?.id) return []
      return data.filter((item) => item.userId === user.id)
    }
    return data
  }, [data, tab, user])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateLostFoundPost(editingId, payload) : createLostFoundPost(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save post. Please ensure you are signed in.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteLostFoundPost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] })
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

    if (
      !form.title.trim() ||
      !form.itemType.trim() ||
      !form.location.trim() ||
      !form.description.trim() ||
      !form.contactName.trim() ||
      !form.contactPhone.trim()
    ) {
      setFormError('Please fill out all required fields.')
      return
    }

    saveMutation.mutate({
      title: form.title.trim(),
      itemType: form.itemType.trim(),
      location: form.location.trim(),
      description: form.description.trim(),
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone.trim(),
    })
  }

  function handleEdit(post) {
    setEditingId(post.id)
    setFormError('')
    setForm({
      title: post.title,
      itemType: post.itemType,
      location: post.location,
      description: post.description,
      contactName: post.contactName,
      contactPhone: post.contactPhone,
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading lost and found posts…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Lost &amp; found is unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Community notices</p>
          <h1>Lost &amp; Found</h1>
          <p className="subtle-text">Report missing neighborhood possessions or help return found items to their owners.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="complaints-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit report' : 'Report an item'}</h2>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form className="notification-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Title" name="title" value={form.title} onChange={handleChange} maxLength={120} required />
              <Input label="Item type / Category" name="itemType" value={form.itemType} onChange={handleChange} maxLength={80} required />
            </div>

            <Input label="Location" name="location" value={form.location} onChange={handleChange} maxLength={160} required />

            <label className="field">
              <span>Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} required rows="3" maxLength={1000} />
            </label>

            <div className="split-fields">
              <Input label="Contact name" name="contactName" value={form.contactName} onChange={handleChange} maxLength={80} required />
              <Input label="Contact phone" name="contactPhone" value={form.contactPhone} onChange={handleChange} maxLength={80} required />
            </div>

            <div className="profile-actions" style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : editingId ? 'Update post' : 'Post report'}
              </Button>
              {editingId && (
                <Button type="button" variant="ghost" onClick={handleCancelEdit}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Notices feed</h2>
          </div>

          <ul className="detail-list">
            <li><strong>Total notices:</strong> {data.length}</li>
            <li><strong>My notices:</strong> {myPostsCount}</li>
          </ul>

          <div style={{ marginTop: '1.25rem', display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              className={`btn btn-sm ${tab === 'all' ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setTab('all')}
            >
              All notices
            </button>
            <button
              type="button"
              className={`btn btn-sm ${tab === 'my' ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setTab('my')}
            >
              My notices ({myPostsCount})
            </button>
          </div>

          <div className="notification-list" style={{ marginTop: '1rem' }}>
            {visiblePosts.length === 0 ? (
              <div className="empty-state">
                <h3>{tab === 'my' ? 'No reports posted by you' : 'No lost & found notices yet'}</h3>
                <p className="muted">
                  {tab === 'my'
                    ? 'Items you report will appear here for easy editing and deletion.'
                    : 'The first missing or found item report will appear here.'}
                </p>
              </div>
            ) : (
              visiblePosts.map((post) => {
                const isOwner = user?.id && post.userId === user.id
                return (
                  <article key={post.id} className="notification-item">
                    <div className="notification-head" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <span className="notification-badge info">{post.itemType}</span>
                        <span className="small-muted">{post.location}</span>
                      </div>
                      <span className="small-muted">{new Date(post.createdAt).toLocaleDateString()}</span>
                    </div>

                    <h3 style={{ marginTop: '0.35rem' }}>{post.title}</h3>
                    <p>{post.description}</p>

                    <div style={{ margin: '0.5rem 0', fontSize: '0.9rem', color: '#475569' }}>
                      <strong>Contact:</strong> {post.contactName} · {post.contactPhone}
                    </div>

                    {isOwner && (
                      <div className="notification-actions" style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                        <Button variant="secondary" onClick={() => handleEdit(post)}>Edit</Button>
                        <Button variant="ghost" onClick={() => deleteMutation.mutate(post.id)}>Delete</Button>
                      </div>
                    )}
                  </article>
                )
              })
            )}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
