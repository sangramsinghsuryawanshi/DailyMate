import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { updateProfile, getProfile } from '../services/userApi'
import { useAuth } from '../../hooks/useAuth'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import {
  Button,
  Input,
  Badge,
  ResponsiveContainer,
} from '../../design-system'
import './ProfilePage.css'

export default function ProfilePage() {
  const { user, signIn } = useAuth()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '' })
  const [saveSuccess, setSaveSuccess] = useState(false)

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'profile' })
  }, [])

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !!user,
    staleTime: 60_000,
  })

  useEffect(() => {
    if (profile) {
      setForm({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
      })
    }
  }, [profile])

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile)
      signIn((current) => (current ? { ...current, user: updatedProfile } : current))
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 4000)
    },
  })

  const currentProfile = profile || user

  const fullName = useMemo(
    () => [form.firstName || currentProfile?.firstName, form.lastName || currentProfile?.lastName].filter(Boolean).join(' ') || 'DailyMate member',
    [form.firstName, form.lastName, currentProfile?.firstName, currentProfile?.lastName],
  )

  const initials = useMemo(
    () => fullName.split(' ').map((part) => part[0]).filter(Boolean).slice(0, 2).join('').toUpperCase() || 'DM',
    [fullName],
  )

  const isSubmitDisabled = useMemo(
    () => mutation.isPending || !form.firstName.trim() || !form.lastName.trim(),
    [form.firstName, form.lastName, mutation.isPending],
  )

  function handleChange(field, value) {
    setSaveSuccess(false)
    setForm((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setSaveSuccess(false)
    mutation.mutate({
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
    })
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading profile…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <div>
            <h1>Unable to load profile</h1>
            <Link to="/dashboard">
              <Button variant="primary">Back to dashboard</Button>
            </Link>
          </div>
        </main>
      </MainLayout>
    )
  }

  const memberSince = currentProfile?.createdAt
    ? new Date(currentProfile.createdAt).toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : 'Active'

  return (
    <MainLayout>
      <div className="dm-profile-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Your account</span>
              <h1 className="dm-page-main-title">Profile settings</h1>
              <p className="dm-page-subtitle">
                Manage your personal information, display name, and view your verified DailyMate account status.
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

          <div className="dm-profile-layout">
            {/* Left Column: Identity Card & Account Overview */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div className="dm-profile-identity-card">
                <div className="dm-profile-avatar-circle">{initials}</div>
                <div>
                  <span className="dm-section-eyebrow">Role: {currentProfile?.role ?? 'USER'}</span>
                  <h2 style={{ fontSize: '1.35rem', fontWeight: 800, margin: '0.25rem 0' }}>{fullName}</h2>
                  <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', margin: 0 }}>
                    {currentProfile?.email ?? 'member@dailymate.app'}
                  </p>
                </div>
                <Badge variant={currentProfile?.status === 'ACTIVE' ? 'success' : 'neutral'} size="md">
                  {currentProfile?.status ?? 'ACTIVE'}
                </Badge>
              </div>

              <div className="dm-profile-details-card">
                <h3 style={{ fontSize: '1.05rem', fontWeight: 800, margin: 0 }}>Account Overview</h3>
                <ul className="dm-profile-info-list">
                  <li className="dm-profile-info-item">
                    <span className="dm-profile-info-label">Email:</span>
                    <span className="dm-profile-info-value">{currentProfile?.email ?? '—'}</span>
                  </li>
                  <li className="dm-profile-info-item">
                    <span className="dm-profile-info-label">Role:</span>
                    <span className="dm-profile-info-value">{currentProfile?.role ?? 'USER'}</span>
                  </li>
                  <li className="dm-profile-info-item">
                    <span className="dm-profile-info-label">Account Status:</span>
                    <span className="dm-profile-info-value">{currentProfile?.status ?? 'ACTIVE'}</span>
                  </li>
                  <li className="dm-profile-info-item">
                    <span className="dm-profile-info-label">Member Since:</span>
                    <span className="dm-profile-info-value">{memberSince}</span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Right Column: Edit Personal Details Form */}
            <div className="dm-profile-details-card">
              <div>
                <span className="dm-section-eyebrow">Identity Details</span>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0.25rem 0' }}>Personal Details</h3>
                <p style={{ color: 'var(--dm-color-text-soft)', fontSize: '0.875rem', margin: 0 }}>
                  Update your legal name displayed on neighborhood complaints, job listings, and marketplace inquiries.
                </p>
              </div>

              <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                  <Input
                    id="profile-firstname"
                    label="First name"
                    value={form.firstName}
                    onChange={(event) => handleChange('firstName', event.target.value)}
                    maxLength={100}
                    required
                  />

                  <Input
                    id="profile-lastname"
                    label="Last name"
                    value={form.lastName}
                    onChange={(event) => handleChange('lastName', event.target.value)}
                    maxLength={100}
                    required
                  />
                </div>

                {saveSuccess && (
                  <div className="dm-form-alert dm-form-alert--success">
                    Profile updated successfully!
                  </div>
                )}

                {mutation.isError && (
                  <div className="dm-form-alert dm-form-alert--error">
                    ⚠️ {mutation.error?.response?.data?.detail || mutation.error?.message || 'Unable to save profile changes.'}
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'flex-start', marginTop: '0.5rem' }}>
                  <Button
                    type="submit"
                    variant="primary"
                    disabled={isSubmitDisabled}
                    isLoading={mutation.isPending}
                  >
                    {mutation.isPending ? 'Saving…' : 'Save changes'}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
