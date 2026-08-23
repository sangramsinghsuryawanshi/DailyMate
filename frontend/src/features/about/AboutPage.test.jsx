import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AboutPage from './AboutPage'
import { AuthContext } from '../../context/authContext'

function renderWithAuth(user = null) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider
        value={{
          accessToken: user ? 'mock-token' : null,
          user,
          signIn: vi.fn(),
          signOut: vi.fn(),
        }}
      >
        <MemoryRouter>
          <AboutPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>
  )
}

describe('AboutPage — Official DailyMate Product Story', () => {
  it('renders hero section with badge, main title, subtitle, and CTA buttons when logged out', () => {
    renderWithAuth(null)

    expect(screen.getAllByText('About DailyMate').length).toBeGreaterThan(0)
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /One place for everything that matters in your everyday life/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getAllByRole('link', { name: 'Get Started' }).length,
    ).toBeGreaterThan(0)
    expect(
      screen.getByRole('link', { name: 'Explore Capabilities' }),
    ).toHaveAttribute('href', '#capabilities')
  })

  it('renders inside MainLayout with Sidebar when user is authenticated', () => {
    renderWithAuth({ id: 'u-1', name: 'Sangram', email: 'sangram@example.com' })

    // Sidebar and TopBar exist
    expect(screen.getByRole('complementary', { name: 'Main Sidebar' })).toBeInTheDocument()
    expect(screen.getByRole('banner', { name: 'App TopBar' })).toBeInTheDocument()

    // About content is still present inside main workspace
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /One place for everything that matters in your everyday life/i,
      }),
    ).toBeInTheDocument()
  })

  it('renders What Is DailyMate and Problem vs Solution comparison', () => {
    renderWithAuth(null)

    expect(screen.getByRole('heading', { level: 2, name: 'What Is DailyMate?' })).toBeInTheDocument()
    expect(screen.getByText(/From fragmented apps to connected living/i)).toBeInTheDocument()
    expect(screen.getByText(/Before DailyMate/i)).toBeInTheDocument()
    expect(screen.getAllByText(/With DailyMate/i).length).toBeGreaterThan(0)
  })

  it('renders Vision, Mission, and Key Capabilities', () => {
    renderWithAuth(null)

    expect(screen.getByText('Our Vision')).toBeInTheDocument()
    expect(screen.getByText('Our Mission')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Everything you need to run your day' })).toBeInTheDocument()
    expect(screen.getByText('Personal Healthcare Routines')).toBeInTheDocument()
    expect(screen.getByText('Financial Management')).toBeInTheDocument()
    expect(screen.getByText('Local Services Marketplace')).toBeInTheDocument()
    expect(screen.getByText('Emergency ICE Directory')).toBeInTheDocument()
    expect(screen.getByText('Controlled AI Assistance')).toBeInTheDocument()
  })

  it('renders Core Values and How DailyMate Works workflow', () => {
    renderWithAuth(null)

    expect(screen.getByRole('heading', { level: 2, name: 'The values that guide DailyMate' })).toBeInTheDocument()
    expect(screen.getByText('Simplicity')).toBeInTheDocument()
    expect(screen.getAllByText('Trust & Privacy').length).toBeGreaterThan(0)
    expect(screen.getByText('Responsible Intelligence')).toBeInTheDocument()

    expect(screen.getByRole('heading', { level: 2, name: 'How DailyMate Works' })).toBeInTheDocument()
    expect(screen.getByText('Create your account')).toBeInTheDocument()
    expect(screen.getByText('Personalize your workspace')).toBeInTheDocument()
    expect(screen.getByText('Organize your everyday life')).toBeInTheDocument()
    expect(screen.getByText('Get smarter assistance')).toBeInTheDocument()
  })

  it('renders Trust, Privacy, Tech Stack, and bottom CTA banner', () => {
    renderWithAuth(null)

    expect(screen.getByRole('heading', { level: 2, name: 'Built with privacy-conscious security' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Enterprise-grade modern stack' })).toBeInTheDocument()
    expect(screen.getByText('Spring Boot 3')).toBeInTheDocument()
    expect(screen.getByText('React 19')).toBeInTheDocument()

    expect(
      screen.getByRole('heading', { level: 2, name: 'Make everyday life a little easier.' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Get Started with DailyMate' }),
    ).toHaveAttribute('href', '/register')
  })
})
