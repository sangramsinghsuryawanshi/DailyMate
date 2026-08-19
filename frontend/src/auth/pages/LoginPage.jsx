import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { login } from '../services/authApi'
import { useAuth } from '../../hooks/useAuth'
import AuthLayout from '../../layouts/AuthLayout'
import Button from '../../components/Button'
import Input from '../../components/Input'

export default function LoginPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [form, setForm] = useState({ email: '', password: '' })

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (session) => {
      signIn(session)
      navigate('/dashboard')
    },
  })

  return (
    <AuthLayout
      headline="Welcome back"
      subheadline="Sign in to keep your routines, services, and reminders in sync."
    >
      <form
        className="auth-form"
        onSubmit={(event) => {
          event.preventDefault()
          mutation.mutate(form)
        }}
      >
        <div className="form-header">
          <h2>Sign in</h2>
          <p>Use your DailyMate account to continue.</p>
        </div>

        <Input
          label="Email"
          type="email"
          required
          value={form.email}
          placeholder="you@example.com"
          onChange={(event) => setForm({ ...form, email: event.target.value })}
        />

        <Input
          label="Password"
          type="password"
          required
          value={form.password}
          placeholder="Enter your password"
          onChange={(event) => setForm({ ...form, password: event.target.value })}
        />

        <div className="auth-meta-row">
          <Link to="/register">Forgot password?</Link>
        </div>

        {mutation.isError && <p className="error">Invalid email or password.</p>}

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Signing in…' : 'Sign in'}
        </Button>

        <p className="auth-switcher">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </AuthLayout>
  )
}

