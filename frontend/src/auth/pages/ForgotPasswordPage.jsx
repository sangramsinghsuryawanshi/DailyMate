import { useState } from 'react'
import { Link } from 'react-router-dom'
import AuthLayout from '../../layouts/AuthLayout'
import { Button, Input } from '../../design-system'
import './Auth.css'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [isSubmitted, setIsSubmitted] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = (e) => {
    e.preventDefault()
    setIsLoading(true)
    setTimeout(() => {
      setIsLoading(false)
      setIsSubmitted(true)
    }, 600)
  }

  return (
    <AuthLayout
      headline="Reset your DailyMate password"
      subheadline="We will send a secure password reset link to your email address."
    >
      <div className="dm-auth-form">
        <div className="dm-auth-form__header">
          <h2>Reset Password</h2>
          <p>Enter the email associated with your account.</p>
        </div>

        {isSubmitted ? (
          <div className="dm-auth-success-box" role="status">
            <span className="dm-auth-success-icon" aria-hidden="true">
              ✉️
            </span>
            <h3>Check your email</h3>
            <p>
              We have sent password reset instructions to <strong>{email}</strong>. Please check your inbox and spam folders.
            </p>
            <Link to="/login">
              <Button variant="primary" fullWidth>
                Back to Sign In
              </Button>
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <Input
              id="reset-email"
              label="Email Address"
              type="email"
              required
              value={email}
              placeholder="you@example.com"
              onChange={(e) => setEmail(e.target.value)}
              disabled={isLoading}
              autoComplete="email"
            />

            <div style={{ marginTop: '1.25rem' }}>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                fullWidth
                isLoading={isLoading}
              >
                Send Reset Link
              </Button>
            </div>

            <p className="dm-auth-footer-text">
              Remember your password?{' '}
              <Link to="/login" className="dm-auth-link dm-auth-link--highlight">
                Sign in
              </Link>
            </p>
          </form>
        )}
      </div>
    </AuthLayout>
  )
}
