import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ProfilePage from './ProfilePage'
import { AuthContext } from '../../context/authContext'

const getProfileMock = vi.fn()
const updateProfileMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/userApi', () => ({
  getProfile: () => getProfileMock(),
  updateProfile: (data) => updateProfileMock(data),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderProfilePage(user = null) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('ProfilePage', () => {
  beforeEach(() => {
    getProfileMock.mockReset()
    updateProfileMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()
    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
  })

  it('renders authentic user profile and does not display fabricated stats', async () => {
    getProfileMock.mockResolvedValue({
      id: 'u-101',
      email: 'alex@example.com',
      firstName: 'Alex',
      lastName: 'Morgan',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2026-01-15T10:00:00Z',
    })

    renderProfilePage({ id: 'u-101', email: 'alex@example.com', firstName: 'Alex', lastName: 'Morgan' })

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: 'Alex Morgan' })).toBeInTheDocument()
      expect(screen.getAllByText('alex@example.com').length).toBeGreaterThan(0)
      expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0)
    })

    // Assert NO fake statistics
    expect(screen.queryByText(/Saved preferences/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Focus: Home support/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Care timeline/i)).not.toBeInTheDocument()
  })

  it('allows user to modify name fields and submit update', async () => {
    getProfileMock.mockResolvedValue({
      id: 'u-101',
      email: 'alex@example.com',
      firstName: 'Alex',
      lastName: 'Morgan',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2026-01-15T10:00:00Z',
    })
    updateProfileMock.mockResolvedValue({
      id: 'u-101',
      email: 'alex@example.com',
      firstName: 'Alexander',
      lastName: 'Morgan-Smith',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2026-01-15T10:00:00Z',
    })

    const user = userEvent.setup()
    renderProfilePage({ id: 'u-101', email: 'alex@example.com' })

    await waitFor(() => {
      expect(screen.getByLabelText('First name')).toHaveValue('Alex')
    })

    const firstNameInput = screen.getByLabelText('First name')
    const lastNameInput = screen.getByLabelText('Last name')

    await user.clear(firstNameInput)
    await user.type(firstNameInput, 'Alexander')
    await user.clear(lastNameInput)
    await user.type(lastNameInput, 'Morgan-Smith')

    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(updateProfileMock).toHaveBeenCalledWith({
        firstName: 'Alexander',
        lastName: 'Morgan-Smith',
      })
      expect(screen.getByText('Profile updated successfully!')).toBeInTheDocument()
    })
  })
})
