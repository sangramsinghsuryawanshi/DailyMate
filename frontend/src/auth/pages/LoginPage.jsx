import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { login } from '../services/authApi'
import { useAuth } from '../../hooks/useAuth'
import AuthLayout from '../../layouts/AuthLayout'
import { Button, Input } from '../../design-system'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './Auth.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { signIn } = useAuth()

  const [form, setForm] = useState({ email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const isSessionExpired = searchParams.get('session') === 'expired'

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (session) => {
      signIn(session)
      trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'dashboard_after_login' })
      navigate('/dashboard')
    },
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    mutation.mutate(form)
  }

  return (
    <AuthLayout
      headline="Welcome back to DailyMate"
      subheadline="Sign in to keep your routines, services, and reminders in sync."
    >
      <form className="dm-auth-form" onSubmit={handleSubmit}>
        <div className="dm-auth-form__header">
          <h2>Sign In</h2>
          <p>Use your DailyMate account credentials to continue.</p>
        </div>

        {isSessionExpired && (
          <div className="dm-auth-alert dm-auth-alert--warning" role="alert">
            <span>⏱️ Your session expired. Please sign in again.</span>
          </div>
        )}

        {mutation.isError && (
          <div className="dm-auth-alert dm-auth-alert--error" role="alert">
            <span>Invalid email or password.</span>
          </div>
        )}

        <Input
          id="login-email"
          label="Email"
          type="email"
          required
          value={form.email}
          placeholder="you@example.com"
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          disabled={mutation.isPending}
          autoComplete="email"
        />

        <div className="dm-password-field-wrapper">
          <Input
            id="login-password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            required
            value={form.password}
            placeholder="Enter your password"
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            disabled={mutation.isPending}
            autoComplete="current-password"
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

        <div className="dm-auth-meta-row">
          <Link to="/forgot-password" className="dm-auth-link">
            Forgot password?
          </Link>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={mutation.isPending}
        >
          {mutation.isPending ? 'Signing in…' : 'Sign in'}
        </Button>

        <p className="dm-auth-footer-text">
          Don't have an account?{' '}
          <Link to="/register" className="dm-auth-link dm-auth-link--highlight">
            Create account
          </Link>
        </p>
      </form>
    </AuthLayout>
  )
}
