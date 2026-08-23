import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { createLostFoundPost, deleteLostFoundPost, getLostFoundPosts, updateLostFoundPost } from '../services/lostFoundApi'
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
import './LostFoundPage.css'

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

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'lost_found' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['lost-found-posts', { page, pageSize, tab }],
    queryFn: () => getLostFoundPosts({ page, size: pageSize }),
    placeholderData: (previousData) => previousData,
  })

  const rawPosts = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? rawPosts.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  const myPostsCount = useMemo(() => {
    if (!user?.id) return 0
    return rawPosts.filter((item) => item.userId === user.id).length
  }, [rawPosts, user])

  const visiblePosts = useMemo(() => {
    if (tab === 'my') {
      if (!user?.id) return []
      return rawPosts.filter((item) => item.userId === user.id)
    }
    return rawPosts
  }, [rawPosts, tab, user])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateLostFoundPost(editingId, payload) : createLostFoundPost(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      trackEvent('lost_found_saved')
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
      trackEvent('lost_found_deleted')
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
          <h1>Loading lost & found notices…</h1>
        </main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Unable to load notices</h1>
          <Link to="/dashboard">
            <Button variant="primary">Back to dashboard</Button>
          </Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <div className="dm-lostfound-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Community Belongings & Trust</span>
              <h1 className="dm-page-main-title">Lost & Found</h1>
              <p className="dm-page-subtitle">
                Help reunite lost items with their owners across your residential society, parks, and local transit.
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
          <div className="dm-lostfound-stats-grid">
            <StatCard
              domain="community"
              label="Total Notices"
              value={String(totalElements)}
              trend="Community"
              trendDirection="neutral"
              trendLabel="Active notices"
              icon="🔍"
            />
            <StatCard
              domain="community"
              label="My Notices"
              value={String(myPostsCount)}
              trend="Owned"
              trendDirection="neutral"
              trendLabel="Created by you"
              icon="📌"
            />
            <StatCard
              domain="community"
              label="Direct Contact"
              value="100% Private"
              trend="Secure"
              trendDirection="up"
              trendLabel="Direct caller line"
              icon="📞"
            />
            <StatCard
              domain="community"
              label="Local Recovery"
              value="High"
              trend="Neighborhood"
              trendDirection="up"
              trendLabel="Community verified"
              icon="🤝"
            />
          </div>

          {/* 2-Column Responsive Layout */}
          <div className="dm-provider-profile-layout">
            {/* Left Column: Notices Feed */}
            <div>
              {/* Tab Bar */}
              <div className="dm-lostfound-tab-bar" role="tablist">
                <button
                  type="button"
                  className={`dm-category-filter-btn ${tab === 'all' ? 'active' : ''}`}
                  onClick={() => setTab('all')}
                >
                  All notices
                </button>
                <button
                  type="button"
                  className={`dm-category-filter-btn ${tab === 'my' ? 'active' : ''}`}
                  onClick={() => setTab('my')}
                >
                  My notices ({myPostsCount})
                </button>
              </div>

              {visiblePosts.length === 0 ? (
                <div className="dm-empty-dashboard-box">
                  <h3>No lost & found notices yet</h3>
                  <p>
                    {tab === 'my'
                      ? 'You have not published any lost or found notices yet.'
                      : 'No lost or found items reported yet in this area.'}
                  </p>
                </div>
              ) : (
                <div className="dm-lostfound-cards-feed">
                  {visiblePosts.map((post) => {
                    const isOwner = user?.id && post.userId === user.id
                    return (
                      <article key={post.id} className="dm-lostfound-card">
                        <div className="dm-lostfound-card-header">
                          <div>
                            <h3 className="dm-lostfound-card-title">{post.title}</h3>
                            <div className="dm-lostfound-card-meta" style={{ marginTop: '0.25rem' }}>
                              <span>🏷️ {post.itemType}</span>
                              <span>📍 {post.location}</span>
                            </div>
                          </div>
                          <Badge variant="primary" size="md">
                            {post.itemType}
                          </Badge>
                        </div>

                        <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', lineHeight: 1.5, margin: 0 }}>
                          {post.description}
                        </p>

                        <div className="dm-lostfound-contact-box">
                          <div>
                            <strong>Contact:</strong> {post.contactName} ·{' '}
                            <a href={`tel:${post.contactPhone}`} style={{ color: 'var(--dm-color-primary-deep)', fontWeight: 700 }}>
                              {post.contactPhone}
                            </a>
                          </div>

                          {isOwner && (
                            <div style={{ display: 'flex', gap: '0.35rem' }}>
                              <Button variant="ghost" size="sm" onClick={() => handleEdit(post)}>
                                Edit
                              </Button>
                              <Button variant="danger" size="sm" onClick={() => deleteMutation.mutate(post.id)}>
                                Delete
                              </Button>
                            </div>
                          )}
                        </div>
                      </article>
                    )
                  })}
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
            </div>

            {/* Right Column: Sticky Form Card */}
            <div>
              <div className="dm-provider-side-card">
                <div>
                  <span className="dm-section-eyebrow">
                    {editingId ? 'Edit Notice' : 'Post a Notice'}
                  </span>
                  <h3 style={{ margin: '0.25rem 0', fontSize: '1.125rem' }}>
                    {editingId ? 'Edit notice' : 'Publish lost or found item'}
                  </h3>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.8125rem', margin: 0 }}>
                    Report misplaced keys, wallets, pets, backpacks, electronics, or items found nearby.
                  </p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="dm-provider-modal-form">
                  <Input
                    id="lf-title"
                    name="title"
                    label="Title"
                    placeholder="e.g. Lost Blue Backpack"
                    value={form.title}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="lf-type"
                    name="itemType"
                    label="Item type"
                    placeholder="e.g. Backpack, Keys, Wallet, Watch"
                    value={form.itemType}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="lf-location"
                    name="location"
                    label="Location"
                    placeholder="e.g. Central Station / North Plaza"
                    value={form.location}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="lf-description"
                    name="description"
                    label="Description"
                    placeholder="Describe color, identifying marks, and specific place found"
                    value={form.description}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="lf-cname"
                    name="contactName"
                    label="Contact name"
                    placeholder="e.g. Ava"
                    value={form.contactName}
                    onChange={handleChange}
                    required
                  />

                  <Input
                    id="lf-cphone"
                    name="contactPhone"
                    label="Contact phone"
                    type="tel"
                    placeholder="555-1111"
                    value={form.contactPhone}
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
                      {editingId ? 'Save Changes' : 'Post report'}
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
