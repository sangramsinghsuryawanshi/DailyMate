import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { register } from '../services/authApi'
import { useAuth } from '../../hooks/useAuth'
import AuthLayout from '../../layouts/AuthLayout'
import { Button, Input } from '../../design-system'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './Auth.css'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()

  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: (session) => {
      signIn(session)
      trackEvent(AnalyticsEvents.SIGNUP_COMPLETED)
      navigate('/onboarding')
    },
  })

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const handleSubmit = (e) => {
    e.preventDefault()
    mutation.mutate(form)
  }

  const isPasswordLongEnough = form.password.length >= 8

  return (
    <AuthLayout
      headline="Create your DailyMate account"
      subheadline="Bring your routines, local services, and daily life into one calm place."
    >
      <form className="dm-auth-form" onSubmit={handleSubmit}>
        <div className="dm-auth-form__header">
          <h2>Get Started</h2>
          <p>Create your account in seconds to begin.</p>
        </div>

        <div className="dm-split-fields">
          <Input
            id="register-first-name"
            label="First Name"
            required
            value={form.firstName}
            placeholder="Sangram"
            onChange={update('firstName')}
            disabled={mutation.isPending}
            autoComplete="given-name"
          />
          <Input
            id="register-last-name"
            label="Last Name"
            required
            value={form.lastName}
            placeholder="Suryawanshi"
            onChange={update('lastName')}
            disabled={mutation.isPending}
            autoComplete="family-name"
          />
        </div>

        <Input
          id="register-email"
          label="Email Address"
          type="email"
          required
          value={form.email}
          placeholder="you@example.com"
          onChange={update('email')}
          disabled={mutation.isPending}
          autoComplete="email"
        />

        <div className="dm-password-field-wrapper">
          <Input
            id="register-password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            minLength="8"
            required
            value={form.password}
            placeholder="At least 8 characters"
            onChange={update('password')}
            disabled={mutation.isPending}
            helperText={
              form.password.length > 0 && !isPasswordLongEnough
                ? 'Password must be at least 8 characters'
                : undefined
            }
            autoComplete="new-password"
          />
          <button
            type="button"
            className="dm-password-toggle-btn"
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? '👁️‍🗨️' : '👁️'}
          </button>
        </div>

        {mutation.isError && (
          <div className="dm-auth-alert dm-auth-alert--error" role="alert">
            <span>⚠️ Unable to create the account. Try a different email or check password requirements.</span>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={mutation.isPending}
        >
          {mutation.isPending ? 'Creating your account…' : 'Create account'}
        </Button>

        <p className="dm-auth-footer-text">
          Already have an account?{' '}
          <Link to="/login" className="dm-auth-link dm-auth-link--highlight">
            Sign in
          </Link>
        </p>
      </form>
    </AuthLayout>
  )
}
