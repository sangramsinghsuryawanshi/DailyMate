import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { updateProfile, getProfile } from '../services/userApi'
import { useAuth } from '../../hooks/useAuth'

export default function ProfilePage() {
  const { user, signIn } = useAuth()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '' })
  const [saveSuccess, setSaveSuccess] = useState(false)

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
            <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
          </div>
        </main>
      </MainLayout>
    )
  }

  const memberSince = currentProfile?.createdAt
    ? new Date(currentProfile.createdAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : 'Active'

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Your account</p>
          <h1>Profile settings</h1>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back to dashboard</Link>
      </section>

      <section className="profile-grid">
        <div className="panel profile-summary">
          <div className="profile-avatar">{initials}</div>
          <div className="profile-summary-copy">
            <p className="eyebrow">Role: {currentProfile?.role ?? 'USER'}</p>
            <h2>{fullName}</h2>
            <p className="muted">{currentProfile?.email ?? 'member@dailymate.app'}</p>
          </div>
          <span className="status-pill">{currentProfile?.status ?? 'ACTIVE'}</span>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Account Overview</h2>
          </div>
          <ul className="detail-list">
            <li><strong>Email:</strong> {currentProfile?.email ?? '—'}</li>
            <li><strong>Role:</strong> {currentProfile?.role ?? 'USER'}</li>
            <li><strong>Account Status:</strong> {currentProfile?.status ?? 'ACTIVE'}</li>
            <li><strong>Member Since:</strong> {memberSince}</li>
          </ul>
        </div>

        <div className="panel profile-form-panel">
          <div className="panel-header">
            <h2>Personal Details</h2>
          </div>

          <form className="profile-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input
                label="First name"
                value={form.firstName}
                onChange={(event) => handleChange('firstName', event.target.value)}
                maxLength={100}
                required
              />

              <Input
                label="Last name"
                value={form.lastName}
                onChange={(event) => handleChange('lastName', event.target.value)}
                maxLength={100}
                required
              />
            </div>

            {saveSuccess && (
              <p style={{ color: '#16a34a', margin: '0.75rem 0', fontWeight: '500' }}>
                Profile updated successfully!
              </p>
            )}

            {mutation.isError && (
              <p className="error" style={{ color: '#ef4444', margin: '0.75rem 0' }}>
                {mutation.error?.response?.data?.detail || mutation.error?.message || 'Unable to save profile changes.'}
              </p>
            )}

            <div className="profile-actions">
              <Button type="submit" disabled={isSubmitDisabled}>
                {mutation.isPending ? 'Saving…' : 'Save changes'}
              </Button>
            </div>
          </form>
        </div>
      </section>
    </MainLayout>
  )
}
