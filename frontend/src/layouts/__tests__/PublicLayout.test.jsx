import { describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import PublicLayout from '../PublicLayout'
import { AuthContext } from '../../context/authContext'

function renderPublicLayout(user = null) {
  return render(
    <AuthContext.Provider
      value={{
        accessToken: user ? 'mock-token' : null,
        user,
        signIn: vi.fn(),
        signOut: vi.fn(),
      }}
    >
      <MemoryRouter>
        <PublicLayout>
          <div data-testid="public-content">Public Page Content</div>
        </PublicLayout>
      </MemoryRouter>
    </AuthContext.Provider>
  )
}

describe('PublicLayout — Same-Site Public Navigation & Auth-Aware State', () => {
  it('renders marketing navigation when unauthenticated', async () => {
    const user = userEvent.setup()
    renderPublicLayout(null)

    expect(screen.getByTestId('public-content')).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Public Navigation' })
    expect(nav).toBeInTheDocument()

    // Public links
    expect(within(nav).getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/')
    expect(within(nav).getByRole('link', { name: 'Features' })).toHaveAttribute('href', '/#features')
    expect(within(nav).getByRole('link', { name: 'About' })).toHaveAttribute('href', '/about')
    expect(within(nav).getByRole('link', { name: 'Pricing' })).toHaveAttribute('href', '/pricing')

    // Auth actions
    expect(screen.getByRole('link', { name: 'Sign In' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Get Started' })).toHaveAttribute('href', '/register')

    // Toggle More menu
    const moreBtn = screen.getByRole('button', { name: 'More options' })
    await user.click(moreBtn)

    expect(screen.getByRole('menuitem', { name: 'How It Works' })).toHaveAttribute('href', '/how-it-works')
    expect(screen.getByRole('menuitem', { name: 'FAQ' })).toHaveAttribute('href', '/faq')
    expect(screen.getByRole('menuitem', { name: 'Contact' })).toHaveAttribute('href', '/contact')
    expect(screen.getByRole('menuitem', { name: 'Privacy Policy' })).toHaveAttribute('href', '/privacy')
    expect(screen.getByRole('menuitem', { name: 'Terms of Service' })).toHaveAttribute('href', '/terms')
  })

  it('renders product navigation when authenticated while keeping About in More menu', async () => {
    const userEventSetup = userEvent.setup()
    const mockUser = { id: 'u-1', name: 'Sangram', email: 'sangram@example.com' }
    renderPublicLayout(mockUser)

    expect(screen.getByTestId('public-content')).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Application Navigation' })
    expect(nav).toBeInTheDocument()

    // Product links within top nav
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/dashboard')
    expect(within(nav).getByRole('link', { name: 'Assistant' })).toHaveAttribute('href', '/assistant')
    expect(within(nav).getByRole('link', { name: 'Expenses' })).toHaveAttribute('href', '/expenses')
    expect(within(nav).getByRole('link', { name: 'Medicine' })).toHaveAttribute('href', '/medicines')
    expect(within(nav).getByRole('link', { name: 'Community' })).toHaveAttribute('href', '/community-complaints')
    expect(within(nav).getByRole('link', { name: 'Marketplace' })).toHaveAttribute('href', '/marketplace')
    expect(within(nav).getByRole('link', { name: 'Notifications' })).toHaveAttribute('href', '/notifications')

    // User avatar link
    expect(screen.getByRole('link', { name: 'Go to User Profile' })).toHaveAttribute('href', '/profile')

    // Toggle More menu to access About and public pages while logged in
    const moreBtn = screen.getByRole('button', { name: 'More options' })
    await userEventSetup.click(moreBtn)

    expect(screen.getByRole('menuitem', { name: 'About' })).toHaveAttribute('href', '/about')
    expect(screen.getByRole('menuitem', { name: 'Pricing' })).toHaveAttribute('href', '/pricing')
    expect(screen.getByRole('menuitem', { name: 'FAQ' })).toHaveAttribute('href', '/faq')
  })

  it('toggles mobile drawer navigation', async () => {
    const userEventSetup = userEvent.setup()
    renderPublicLayout(null)

    const toggleBtn = screen.getByRole('button', { name: 'Open Menu' })
    await userEventSetup.click(toggleBtn)

    expect(screen.getByRole('dialog', { name: 'Mobile Navigation Menu' })).toBeInTheDocument()
  })
})
