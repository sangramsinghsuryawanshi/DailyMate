import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import LoginPage from '../LoginPage'
import RegisterPage from '../RegisterPage'
import ForgotPasswordPage from '../ForgotPasswordPage'
import OnboardingPage from '../../../features/onboarding/OnboardingPage'
import { AuthContext } from '../../../context/authContext'

const loginMock = vi.fn()
const registerMock = vi.fn()

vi.mock('../../services/authApi', () => ({
  login: (data) => loginMock(data),
  register: (data) => registerMock(data),
}))

function renderWithProviders(ui, { user = null } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          {ui}
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>
  )
}

describe('Auth & Progressive Onboarding Workflow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders Login page with password toggle and submits credentials', async () => {
    loginMock.mockResolvedValue({ accessToken: 'test-token', user: { id: 'u1', email: 'sangram@example.com' } })
    const user = userEvent.setup({ delay: null })

    renderWithProviders(<LoginPage />)

    expect(screen.getByRole('heading', { name: /Sign In/i })).toBeInTheDocument()

    const emailInput = screen.getByLabelText(/^Email/i)
    const passwordInput = screen.getByPlaceholderText(/Enter your password/i)

    await user.type(emailInput, 'sangram@example.com')
    await user.type(passwordInput, 'Secret123!')

    // Toggle password visibility
    const toggleBtn = screen.getByLabelText('Show password')
    await user.click(toggleBtn)
    expect(passwordInput).toHaveAttribute('type', 'text')

    const submitBtn = screen.getByRole('button', { name: 'Sign in' })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        email: 'sangram@example.com',
        password: 'Secret123!',
      })
    })
  })

  it('renders Registration page and submits valid details', async () => {
    registerMock.mockResolvedValue({ accessToken: 'test-token', user: { id: 'u1', firstName: 'Sangram' } })
    const user = userEvent.setup({ delay: null })

    renderWithProviders(<RegisterPage />)

    await user.type(screen.getByLabelText(/First Name/i), 'Sangram')
    await user.type(screen.getByLabelText(/Last Name/i), 'Suryawanshi')
    await user.type(screen.getByLabelText(/Email Address/i), 'sangram@example.com')
    await user.type(screen.getByPlaceholderText(/At least 8 characters/i), 'SecretPassword123!')

    const submitBtn = screen.getByRole('button', { name: /Create account/i })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalledWith({
        firstName: 'Sangram',
        lastName: 'Suryawanshi',
        email: 'sangram@example.com',
        password: 'SecretPassword123!',
      })
    })
  })

  it('handles Forgot Password reset link submission', async () => {
    const user = userEvent.setup({ delay: null })
    renderWithProviders(<ForgotPasswordPage />)

    expect(screen.getByRole('heading', { name: /Reset Password/i })).toBeInTheDocument()

    await user.type(screen.getByLabelText(/Email Address/i), 'sangram@example.com')
    await user.click(screen.getByRole('button', { name: /Send Reset Link/i }))

    await waitFor(() => {
      expect(screen.getByText(/Check your email/i)).toBeInTheDocument()
    })
  })

  it('progresses through 3-step Onboarding flow and saves personalization', async () => {
    const user = userEvent.setup({ delay: null })
    renderWithProviders(<OnboardingPage />, { user: { firstName: 'Sangram' } })

    // Step 1: Select Health priority
    expect(screen.getByText(/What is your main focus today\?/i)).toBeInTheDocument()
    const healthGoal = screen.getByText(/Medicine & Healthcare Routines/i)
    await user.click(healthGoal)
    await user.click(screen.getByRole('button', { name: /Continue →/i }))

    // Step 2: Profile details
    expect(screen.getByText(/A few quick details/i)).toBeInTheDocument()
    await user.type(screen.getByLabelText(/Phone Number/i), '+91 9876543210')
    await user.click(screen.getByRole('button', { name: /Continue →/i }))

    // Step 3: First Useful Micro-Action
    expect(screen.getByText(/Let's set up your first item/i)).toBeInTheDocument()
    expect(screen.getByText(/Schedule your daily medication/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Complete & Go to Dashboard/i }))

    // Verify localStorage personalization payload
    const stored = JSON.parse(localStorage.getItem('dailymate.personalization'))
    expect(stored.primaryGoal).toBe('health')
    expect(stored.phone).toBe('+91 9876543210')
  })
})
