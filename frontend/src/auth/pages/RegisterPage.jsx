import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { register } from '../services/authApi'
import { useAuth } from '../../hooks/useAuth'
import AuthLayout from '../../layouts/AuthLayout'
import Button from '../../components/Button'
import Input from '../../components/Input'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: (session) => {
      signIn(session)
      navigate('/dashboard')
    },
  })

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  return (
    <AuthLayout
      headline="Create your DailyMate account"
      subheadline="Bring your routines, local services, and daily life into one calm place."
    >
      <form
        className="auth-form"
        onSubmit={(event) => {
          event.preventDefault()
          mutation.mutate(form)
        }}
      >
        <div className="form-header">
          <h2>Get started</h2>
          <p>Start with a few details and jump into your dashboard.</p>
        </div>

        <div className="split-fields">
          <Input label="First name" required value={form.firstName} onChange={update('firstName')} />
          <Input label="Last name" required value={form.lastName} onChange={update('lastName')} />
        </div>

        <Input label="Email" type="email" required value={form.email} placeholder="you@example.com" onChange={update('email')} />

        <Input
          label="Password"
          type="password"
          minLength="12"
          required
          value={form.password}
          placeholder="At least 12 characters"
          onChange={update('password')}
        />

        {mutation.isError && <p className="error">Unable to create the account. Try a different email or password.</p>}

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating your account…' : 'Create account'}
        </Button>

        <p className="auth-switcher">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </AuthLayout>
  )
}

