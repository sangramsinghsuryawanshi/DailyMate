import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ProviderDetailPage from './ProviderDetailPage'
import { AuthContext } from '../../context/authContext'

const getProviderMock = vi.fn()
const getProvidersMock = vi.fn()
const updateProviderMock = vi.fn()
const deleteProviderMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/marketplaceApi', () => ({
  getProvider: (id) => getProviderMock(id),
  getProviders: () => getProvidersMock(),
  updateProvider: (id, data) => updateProviderMock(id, data),
  deleteProvider: (id) => deleteProviderMock(id),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderProviderDetailPage(user = null, providerId = 'p-1') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter initialEntries={[`/marketplace/${providerId}`]}>
          <Routes>
            <Route path="/marketplace/:id" element={<ProviderDetailPage />} />
            <Route path="/marketplace" element={<div>Marketplace Home</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('ProviderDetailPage', () => {
  beforeEach(() => {
    getProviderMock.mockReset()
    getProvidersMock.mockReset()
    updateProviderMock.mockReset()
    deleteProviderMock.mockReset()
    getNotificationsMock.mockReset()
    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
  })

  it('renders real provider profile and does not show fabricated reviews', async () => {
    getProviderMock.mockResolvedValue({
      id: 'p-1',
      userId: 'other-user',
      name: 'CityLine Electric',
      category: 'Electrician',
      description: 'Residential electrical repairs and wiring',
      serviceArea: 'Downtown',
      phone: '+1-555-0181',
      email: 'help@citylineelectric.example',
      hourlyRate: 60.0,
      createdAt: '2026-08-21T00:00:00Z',
    })

    renderProviderDetailPage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1, name: 'CityLine Electric' })).toBeInTheDocument()
      expect(screen.getByText('Service area: Downtown')).toBeInTheDocument()
      expect(screen.getByText('₹60.00/hr')).toBeInTheDocument()
      expect(screen.getByText('Call +1-555-0181')).toBeInTheDocument()
    })

    // Assert NO fake reviews or fake rating stars
    expect(screen.queryByText(/Marina R./i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Daniel K./i)).not.toBeInTheDocument()
    expect(screen.queryByText(/★/i)).not.toBeInTheDocument()
  })

  it('allows owner to enter edit mode and update profile', async () => {
    getProviderMock.mockResolvedValue({
      id: 'p-1',
      userId: 'u-owner',
      name: 'CityLine Electric',
      category: 'Electrician',
      description: 'Residential electrical repairs and wiring',
      serviceArea: 'Downtown',
      phone: '+1-555-0181',
      email: 'help@citylineelectric.example',
      hourlyRate: 60.0,
    })
    updateProviderMock.mockResolvedValue({
      id: 'p-1',
      userId: 'u-owner',
      name: 'CityLine Electric Pro',
      category: 'Electrician',
      description: 'Expanded commercial and residential electrical repairs',
      serviceArea: 'Citywide',
      phone: '+1-555-0181',
      email: 'help@citylineelectric.example',
      hourlyRate: 70.0,
    })

    const user = userEvent.setup()
    renderProviderDetailPage({ id: 'u-owner', email: 'owner@example.com' }, 'p-1')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit Listing' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit Listing' }))
    expect(screen.getByText('Edit Provider Profile')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Business \/ Provider Name/i)
    await user.clear(nameInput)
    await user.type(nameInput, 'CityLine Electric Pro')
    await user.click(screen.getByRole('button', { name: 'Save Changes' }))

    await waitFor(() => {
      expect(updateProviderMock).toHaveBeenCalledWith(
        'p-1',
        expect.objectContaining({
          name: 'CityLine Electric Pro',
        }),
      )
    })
  })
})
