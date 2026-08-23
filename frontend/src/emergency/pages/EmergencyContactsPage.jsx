import { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import {
  Button,
  Input,
  Card,
  Badge,
  TrustBadge,
  Skeleton,
  Pagination,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import {
  createEmergencyContact,
  deleteEmergencyContact,
  getEmergencyContacts,
  getMyEmergencyContacts,
  updateEmergencyContact,
} from '../services/emergencyContactsApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './EmergencyContactsPage.css'

const EMERGENCY_CATEGORIES = ['Personal', 'Police', 'Ambulance', 'Fire', 'Hospital', 'Helpline', 'Other']

const NATIONAL_HOTLINES = [
  { name: 'National Emergency', number: '112', icon: '🚨', type: 'All-in-one' },
  { name: 'Police Helpline', number: '100', icon: '👮', type: 'Immediate response' },
  { name: 'Ambulance Service', number: '108', icon: '🚑', type: 'Medical transport' },
  { name: 'Fire Control', number: '101', icon: '🚒', type: 'Fire rescue' },
]

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

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'emergency_contacts' })
  }, [])

  // Public contacts query
  const {
    data: publicPageData = { content: [], totalElements: 0, totalPages: 0 },
    isLoading: isPublicLoading,
    isError: isPublicError,
    refetch: refetchPublic,
  } = useQuery({
    queryKey: ['emergency-contacts-public', { page, pageSize, selectedCategory }],
    queryFn: () =>
      getEmergencyContacts({
        page,
        size: pageSize,
        category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      }),
    enabled: activeTab === 'public',
    placeholderData: (previousData) => previousData,
  })

  const publicContacts = Array.isArray(publicPageData) ? publicPageData : (publicPageData.content ?? [])
  const totalPublicElements = Array.isArray(publicPageData) ? publicPageData.length : (publicPageData.totalElements ?? publicContacts.length)
  const totalPublicPages = Array.isArray(publicPageData) ? 1 : (publicPageData.totalPages ?? 1)

  // Personal contacts query
  const {
    data: personalPageData = { content: [], totalElements: 0, totalPages: 0 },
    isLoading: isPersonalLoading,
    isError: isPersonalError,
    refetch: refetchPersonal,
  } = useQuery({
    queryKey: ['emergency-contacts-personal', { page, pageSize, selectedCategory }],
    queryFn: () =>
      getMyEmergencyContacts({
        page,
        size: pageSize,
        category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      }),
    enabled: activeTab === 'personal' && Boolean(user?.id),
    placeholderData: (previousData) => previousData,
  })

  const personalContacts = Array.isArray(personalPageData) ? personalPageData : (personalPageData.content ?? [])
  const totalPersonalElements = Array.isArray(personalPageData) ? personalPageData.length : (personalPageData.totalElements ?? personalContacts.length)
  const totalPersonalPages = Array.isArray(personalPageData) ? 1 : (personalPageData.totalPages ?? 1)

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateEmergencyContact(editingId, payload) : createEmergencyContact(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-personal'] })
      queryClient.invalidateQueries({ queryKey: ['emergency-contacts-public'] })
      setActiveTab('personal')
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      trackEvent('emergency_contact_created', { isEdit: !!editingId })
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
      trackEvent('emergency_contact_deleted')
    },
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    if (!form.name?.trim() || !form.category?.trim() || !form.phone?.trim() || !form.location?.trim() || !form.description?.trim()) {
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
    setForm({
      name: contact.name || '',
      category: contact.category || 'Personal',
      phone: contact.phone || '',
      location: contact.location || '',
      description: contact.description || '',
    })
    setFormError('')
    if (typeof window !== 'undefined' && typeof window.scrollTo === 'function') {
      try {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      } catch (_) {}
    }
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  const primaryContact = personalContacts[0]

  return (
    <MainLayout>
      <div className="dm-emergency-page">
        <ResponsiveContainer size="wide">
          {/* ===================================================================
              1. PAGE HEADER & NATIONAL HOTLINES
             =================================================================== */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow" style={{ color: 'var(--dm-color-danger)' }}>
                Urgent Assistance & ICE
              </span>
              <h1 className="dm-page-main-title">Emergency Contacts</h1>
              <p className="dm-page-subtitle">
                Instant one-tap emergency calling for national hotlines and personal ICE contacts.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/assistant?prompt=Who%20are%20my%20registered%20emergency%20contacts%3F">
                <Button variant="outline" size="sm" iconLeft="✨">
                  Ask AI Contacts
                </Button>
              </Link>
            </div>
          </div>

          {/* National Hotlines Direct Calling Strip */}
          <div className="dm-hotlines-grid" aria-label="National Emergency Hotlines">
            {NATIONAL_HOTLINES.map((hotline) => (
              <a
                key={hotline.number}
                href={`tel:${hotline.number}`}
                className="dm-hotline-card"
                onClick={() => trackEvent('emergency_call_initiated', { name: hotline.name, phone: hotline.number })}
              >
                <span className="dm-hotline-icon">{hotline.icon}</span>
                <div className="dm-hotline-info">
                  <strong>{hotline.name}</strong>
                  <span className="dm-hotline-desc">{hotline.type}</span>
                </div>
                <span className="dm-hotline-number">{hotline.number}</span>
              </a>
            ))}
          </div>

          {/* ===================================================================
              2. TWO-COLUMN WORKFLOW: FORM & DIRECTORY LIST
             =================================================================== */}
          <div className="dm-emergency-layout-grid">
            {/* Left Column: Directory / Personal Contacts */}
            <div className="dm-emergency-list-col">
              <Card className="dm-emergency-panel">
                {/* Tab Switcher */}
                <div className="dm-emergency-tabs">
                  <button
                    type="button"
                    className={`dm-emergency-tab ${activeTab === 'public' ? 'active' : ''}`}
                    onClick={() => {
                      setActiveTab('public')
                      trackEvent('emergency_tab_switched', { tab: 'public' })
                    }}
                  >
                    🏛️ Public Emergency Services
                  </button>
                  <button
                    type="button"
                    className={`dm-emergency-tab ${activeTab === 'personal' ? 'active' : ''}`}
                    onClick={() => {
                      setActiveTab('personal')
                      trackEvent('emergency_tab_switched', { tab: 'personal' })
                    }}
                  >
                    👤 My Personal Contacts
                  </button>
                </div>

                {/* Content based on Active Tab */}
                {activeTab === 'public' ? (
                  isPublicLoading ? (
                    <div className="dm-skeleton-stack">
                      <Skeleton variant="card" height="90px" />
                      <Skeleton variant="card" height="90px" />
                    </div>
                  ) : isPublicError ? (
                    <div className="dm-error-box">
                      <p>Unable to load public emergency services.</p>
                      <Button variant="outline" size="sm" onClick={() => refetchPublic()}>
                        Retry
                      </Button>
                    </div>
                  ) : publicContacts.length > 0 ? (
                    <>
                    <div className="dm-contacts-list">
                      {publicContacts.map((contact) => (
                        <div key={contact.id} className="dm-contact-card dm-contact-card--public">
                          <div className="dm-contact-top">
                            <div className="dm-contact-info">
                              <div className="dm-contact-title-row">
                                <h3>{contact.name}</h3>
                                <span className="dm-verified-tag">Verified Public Service</span>
                              </div>
                              <span className="dm-contact-cat">{contact.category} · {contact.location}</span>
                              <p className="dm-contact-desc">{contact.description}</p>
                            </div>
                            <div className="dm-contact-action">
                              <a
                                href={`tel:${contact.phone}`}
                                className="dm-call-button"
                                aria-label={`Call ${contact.phone}`}
                                onClick={() => trackEvent('emergency_call_initiated', { name: contact.name, phone: contact.phone })}
                              >
                                📞 Call {contact.phone}
                              </a>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Server-Driven Pagination for Public Contacts */}
                    <Pagination
                      page={page}
                      totalPages={totalPublicPages}
                      totalElements={totalPublicElements}
                      pageSize={pageSize}
                      onPageChange={setPage}
                      onPageSizeChange={setPageSize}
                      disabled={isPublicLoading}
                    />
                  </>
                  ) : (
                    <div className="dm-empty-dashboard-box">
                      <p>No emergency contacts found.</p>
                    </div>
                  )
                ) : (
                  /* Personal ICE Contacts Tab */
                  !user?.id ? (
                    <div className="dm-auth-prompt-card">
                      <p>Log in to save and manage your family doctors, personal ICE contacts, and trusted neighbors.</p>
                      <Link to="/login" className="dm-button dm-button--primary">
                        Log in to view personal contacts
                      </Link>
                    </div>
                  ) : isPersonalLoading ? (
                    <div className="dm-skeleton-stack">
                      <Skeleton variant="card" height="90px" />
                      <Skeleton variant="card" height="90px" />
                    </div>
                  ) : isPersonalError ? (
                    <div className="dm-error-box">
                      <p>Unable to load personal emergency contacts.</p>
                      <Button variant="outline" size="sm" onClick={() => refetchPersonal()}>
                        Retry
                      </Button>
                    </div>
                  ) : personalContacts.length > 0 ? (
                    <>
                    <div className="dm-contacts-list">
                      {personalContacts.map((contact, idx) => {
                        const isPrimary = idx === 0
                        return (
                          <div
                            key={contact.id}
                            className={`dm-contact-card dm-contact-card--personal ${
                              isPrimary ? 'dm-contact-card--primary' : ''
                            }`}
                          >
                            {isPrimary && (
                              <div className="dm-primary-badge-strip">
                                <span>⭐ PRIMARY ICE CONTACT</span>
                              </div>
                            )}
                            <div className="dm-contact-top">
                              <div className="dm-contact-info">
                                <div className="dm-contact-title-row">
                                  <h3>{contact.name}</h3>
                                  <span className="dm-personal-tag">Personal Contact</span>
                                </div>
                                <span className="dm-contact-cat">{contact.category} · {contact.location}</span>
                                <p className="dm-contact-desc">{contact.description}</p>
                              </div>
                              <div className="dm-contact-action">
                                <a
                                  href={`tel:${contact.phone}`}
                                  className="dm-call-button dm-call-button--primary"
                                  aria-label={`Call ${contact.phone}`}
                                  onClick={() => trackEvent('emergency_call_initiated', { name: contact.name, phone: contact.phone })}
                                >
                                  📞 Call {contact.phone}
                                </a>
                              </div>
                            </div>

                            <div className="dm-contact-bottom">
                              <span className="dm-contact-phone-display">Phone: {contact.phone}</span>
                              <div className="dm-contact-util-actions">
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => handleEdit(contact)}
                                >
                                  Edit
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="dm-btn-danger-hover"
                                  onClick={() => deleteMutation.mutate(contact.id)}
                                  isLoading={
                                    deleteMutation.isPending && deleteMutation.variables === contact.id
                                  }
                                >
                                  Delete
                                </Button>
                              </div>
                            </div>
                          </div>
                        )
                      })}
                    </div>

                    {/* Server-Driven Pagination for Personal Contacts */}
                    <Pagination
                      page={page}
                      totalPages={totalPersonalPages}
                      totalElements={totalPersonalElements}
                      pageSize={pageSize}
                      onPageChange={setPage}
                      onPageSizeChange={setPageSize}
                      disabled={isPersonalLoading}
                    />
                  </>
                  ) : (
                    <div className="dm-empty-dashboard-box">
                      <p>No emergency contacts found.</p>
                    </div>
                  )
                )}
              </Card>
            </div>

            {/* Right Column: Contact Form Card */}
            <div className="dm-emergency-form-col">
              <Card className="dm-emergency-form-card">
                <div className="dm-form-card-header">
                  <h2>{editingId ? 'Edit Emergency Contact' : 'Add Emergency Contact'}</h2>
                  <p>Register a personal ICE doctor, family member, or neighbor.</p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form className="dm-emergency-form" onSubmit={handleSubmit}>
                  <Input
                    id="emergency-name"
                    name="name"
                    label="Contact name"
                    required
                    placeholder="e.g. Dr. Ramesh Kulkarni or Father"
                    value={form.name}
                    onChange={handleChange}
                  />

                  <div className="dm-select-group">
                    <label htmlFor="emergency-category" className="dm-input-label">
                      Category
                    </label>
                    <select
                      id="emergency-category"
                      name="category"
                      aria-label="Category"
                      className="dm-select"
                      value={form.category}
                      onChange={handleChange}
                    >
                      {EMERGENCY_CATEGORIES.map((cat) => (
                        <option key={cat} value={cat}>
                          {cat}
                        </option>
                      ))}
                    </select>
                  </div>

                  <Input
                    id="emergency-phone"
                    name="phone"
                    label="Phone number"
                    type="tel"
                    required
                    placeholder="+91 98220 12345"
                    value={form.phone}
                    onChange={handleChange}
                  />

                  <Input
                    id="emergency-location"
                    name="location"
                    label="Location / Clinic"
                    required
                    placeholder="e.g. Kothrud Clinic or Gate 1"
                    value={form.location}
                    onChange={handleChange}
                  />

                  <Input
                    id="emergency-desc"
                    name="description"
                    label="Description / Notes"
                    required
                    placeholder="e.g. Family physician, available 24x7"
                    value={form.description}
                    onChange={handleChange}
                  />

                  <div className="dm-form-submit-row">
                    {editingId && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={handleCancelEdit}
                      >
                        Cancel
                      </Button>
                    )}
                    <Button
                      type="submit"
                      variant="primary"
                      size="lg"
                      fullWidth={!editingId}
                      isLoading={saveMutation.isPending}
                    >
                      {editingId ? 'Update contact' : 'Save contact'}
                    </Button>
                  </div>
                </form>
              </Card>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
