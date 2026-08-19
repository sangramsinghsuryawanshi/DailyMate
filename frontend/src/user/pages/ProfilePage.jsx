import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { updateProfile, getProfile } from '../services/userApi'
import { useAuth } from '../../hooks/useAuth'

export default function ProfilePage() {
  const { user, signIn } = useAuth()
  const [form, setForm] = useState({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '' })

  const { isLoading, isError } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !!user,
    staleTime: 60_000,
    onSuccess: (profile) => {
      setForm({ firstName: profile.firstName ?? '', lastName: profile.lastName ?? '' })
    },
  })

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (profile) => {
      signIn((current) => (current ? { ...current, user: profile } : current))
    },
  })

  const fullName = useMemo(
    () => [form.firstName, form.lastName].filter(Boolean).join(' ') || 'DailyMate member',
    [form.firstName, form.lastName],
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
    setForm((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
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
            <p className="eyebrow">Member</p>
            <h2>{fullName}</h2>
            <p className="muted">{user?.email ?? 'member@dailymate.app'}</p>
          </div>
          <span className="status-pill">Active</span>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Quick overview</h2>
          </div>
          <ul className="detail-list">
            <li><strong>Focus:</strong> Home support and everyday planning</li>
            <li><strong>Saved preferences:</strong> 18 items</li>
            <li><strong>Care timeline:</strong> Updated this week</li>
          </ul>
        </div>

        <div className="panel profile-form-panel">
          <div className="panel-header">
            <h2>Personal details</h2>
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

            {mutation.isError && <p className="error">Unable to save your profile changes.</p>}

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
