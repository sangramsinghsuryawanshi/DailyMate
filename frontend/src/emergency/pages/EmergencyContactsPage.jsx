import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import {
  createEmergencyContact,
  deleteEmergencyContact,
  getEmergencyContacts,
  getMyEmergencyContacts,
  updateEmergencyContact,
} from '../services/emergencyContactsApi'

const EMERGENCY_CATEGORIES = ['Police', 'Ambulance', 'Fire', 'Hospital', 'Helpline', 'Personal', 'Other']

const defaultForm = {
  name: '',
  category: 'Personal',
  phone: '',
  location: '',
  description: '',
}

export default function EmergencyContactsPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()

  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [activeTab, setActiveTab] = useState('public') // 'public' | 'personal'
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [formError, setFormError] = useState('')

  // Public contacts query
  const {
    data: publicContacts = [],
    isLoading: isPublicLoading,
    isError: isPublicError,
  } = useQuery({
    queryKey: ['emergency-contacts-public', selectedCategory],
    queryFn: () => getEmergencyContacts({ category: selectedCategory !== 'ALL' ? selectedCategory : undefined }),
    enabled: activeTab === 'public',
  })

  // Personal contacts query
  const {
    data: personalContacts = [],
    isLoading: isPersonalLoading,
    isError: isPersonalError,
  } = useQuery({
    queryKey: ['emergency-contacts-personal', selectedCategory],
    queryFn: () => getMyEmergencyContacts({ category: selectedCategory !== 'ALL' ? selectedCategory : undefined }),
    enabled: activeTab === 'personal' && Boolean(user?.id),
  })

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateEmergencyContact(editingId, payload) : createEmergencyContact(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-personal'] })
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-public'] })
      setActiveTab('personal')
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save contact. Please check all fields.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteEmergencyContact,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-personal'] })
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-public'] })
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

    if (!form.name.trim() || !form.category.trim() || !form.phone.trim() || !form.location.trim() || !form.description.trim()) {
      setFormError('Please fill out all required fields.')
      return
    }

    saveMutation.mutate({
      name: form.name.trim(),
      category: form.category.trim(),
      phone: form.phone.trim(),
      location: form.location.trim(),
      description: form.description.trim(),
    })
  }

  function handleEdit(contact) {
    setEditingId(contact.id)
    setFormError('')
    setForm({
      name: contact.name,
      category: contact.category,
      phone: contact.phone,
      location: contact.location,
      description: contact.description,
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  const isLoading = activeTab === 'public' ? isPublicLoading : isPersonalLoading
  const isError = activeTab === 'public' ? isPublicError : isPersonalError
  const contactsList = activeTab === 'public' ? publicContacts : personalContacts

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading emergency contacts…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Emergency contacts are currently unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Safety &amp; Support</p>
          <h1>Emergency Directory</h1>
          <p className="subtle-text">Immediate emergency support, verified municipal hotlines, and quick-dial personal contacts.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          className={`btn ${activeTab === 'public' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('public')}
        >
          🚨 Verified Emergency Services
        </button>
        <button
          type="button"
          className={`btn ${activeTab === 'personal' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setActiveTab('personal')}
        >
          👤 My Personal Contacts {user?.id && personalContacts ? `(${personalContacts.length})` : ''}
        </button>
      </div>

      <section className="complaints-grid">
        {/* Main Contacts List */}
        <div>
          {/* Category Filter Pills */}
          <div className="panel" style={{ marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
              <strong style={{ fontSize: '0.9rem', marginRight: '0.35rem' }}>Category:</strong>
              <button
                type="button"
                className={`btn btn-small ${selectedCategory === 'ALL' ? 'btn-secondary' : 'btn-ghost'}`}
                onClick={() => setSelectedCategory('ALL')}
              >
                All
              </button>
              {EMERGENCY_CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`btn btn-small ${selectedCategory === cat ? 'btn-secondary' : 'btn-ghost'}`}
                  onClick={() => setSelectedCategory(cat)}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {activeTab === 'personal' && !user?.id ? (
            <div className="panel empty-state">
              <h3>Personal Emergency Contacts</h3>
              <p className="muted">Log in to save and manage your family doctors, emergency contacts, and ICE numbers.</p>
              <Link to="/login" className="btn btn-primary" style={{ marginTop: '0.75rem' }}>
                Log in to view personal contacts
              </Link>
            </div>
          ) : contactsList.length === 0 ? (
            <div className="panel empty-state">
              <h3>No emergency contacts found</h3>
              <p className="muted">
                {activeTab === 'personal'
                  ? 'Add your personal emergency contacts, doctor, or neighbor using the form.'
                  : selectedCategory !== 'ALL'
                  ? `No verified emergency services found for category "${selectedCategory}".`
                  : 'No emergency contacts registered.'}
              </p>
            </div>
          ) : (
            <div className="notification-list">
              {contactsList.map((contact) => {
                const isOwner = user?.id && contact.userId === user.id
                return (
                  <article key={contact.id} className="panel" style={{ marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div>
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.35rem' }}>
                          <span className="notification-badge info">{contact.category}</span>
                          {contact.userId ? (
                            <span className="status-pill open">Personal Contact</span>
                          ) : (
                            <span className="status-pill resolved">Verified Public Service</span>
                          )}
                        </div>
                        <h3 style={{ margin: 0, fontSize: '1.25rem' }}>{contact.name}</h3>
                        <div className="small-muted" style={{ marginTop: '0.25rem' }}>
                          📍 {contact.location}
                        </div>
                      </div>

                      {/* Primary One-Tap Call Action */}
                      <div>
                        <a
                          href={`tel:${contact.phone}`}
                          className="btn btn-primary"
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', textDecoration: 'none', fontWeight: 600 }}
                        >
                          📞 Call {contact.phone}
                        </a>
                      </div>
                    </div>

                    <p style={{ marginTop: '0.75rem', color: 'var(--color-text)', fontSize: '0.95rem', lineHeight: '1.5' }}>
                      {contact.description}
                    </p>

                    {/* Owner controls: rendered ONLY for personal contacts owned by logged-in user */}
                    {isOwner && (
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
                        <Button variant="secondary" onClick={() => handleEdit(contact)}>
                          Edit
                        </Button>
                        <Button variant="ghost" onClick={() => deleteMutation.mutate(contact.id)}>
                          Delete
                        </Button>
                      </div>
                    )}
                  </article>
                )
              })}
            </div>
          )}
        </div>

        {/* Side Panel Form (Available for authenticated users) */}
        {user?.id && (
          <aside className="panel summary-panel">
            <div className="panel-header">
              <h2>{editingId ? 'Edit contact' : 'Add personal contact'}</h2>
            </div>

            {formError && (
              <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
                {formError}
              </div>
            )}

            <form className="notification-form" onSubmit={handleSubmit}>
              <Input label="Contact name / Service" name="name" value={form.name} onChange={handleChange} maxLength={120} required />

              <label className="field">
                <span>Category</span>
                <select name="category" value={form.category} onChange={handleChange}>
                  {EMERGENCY_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </label>

              <Input label="Phone number" name="phone" value={form.phone} onChange={handleChange} maxLength={40} placeholder="e.g. +91 98765 43210 or 108" required />

              <Input label="Location / Clinic" name="location" value={form.location} onChange={handleChange} maxLength={160} required />

              <label className="field">
                <span>Description / Notes</span>
                <textarea name="description" value={form.description} onChange={handleChange} required rows="3" maxLength={500} placeholder="e.g. 24x7 family doctor, blood group specialist, or building watchman" />
              </label>

              <div className="profile-actions" style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <Button type="submit" disabled={saveMutation.isPending}>
                  {saveMutation.isPending ? 'Saving…' : editingId ? 'Update contact' : 'Save contact'}
                </Button>
                {editingId && (
                  <Button type="button" variant="ghost" onClick={handleCancelEdit}>
                    Cancel
                  </Button>
                )}
              </div>
            </form>
          </aside>
        )}
      </section>
    </MainLayout>
  )
}
