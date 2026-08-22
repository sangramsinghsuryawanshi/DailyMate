import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MarketplacePage from './MarketplacePage'
import { AuthContext } from '../../context/authContext'

const getProvidersMock = vi.fn()
const createProviderMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
  createProvider: (data) => createProviderMock(data),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderMarketplacePage(user = null) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <MarketplacePage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('MarketplacePage', () => {
  beforeEach(() => {
    getProvidersMock.mockReset()
    createProviderMock.mockReset()
    getNotificationsMock.mockReset()
    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
  })

  it('renders real provider listings without fake ratings or distances', async () => {
    getProvidersMock.mockResolvedValue([
      {
        id: 'p-1',
        name: 'Apex Electrical Services',
        category: 'Electrician',
        description: 'Quality residential electrical repairs and installations',
        serviceArea: 'Metro West',
        phone: '+1-555-0101',
        email: 'apex@example.com',
        hourlyRate: 75.0,
      },
    ])

    renderMarketplacePage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: 'Apex Electrical Services' })).toBeInTheDocument()
      expect(screen.getByText('₹75.00/hr')).toBeInTheDocument()
      expect(screen.getByText('Area: Metro West')).toBeInTheDocument()
    })

    // Assert NO fake ratings or fake distances
    expect(screen.queryByText(/km away/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/★/i)).not.toBeInTheDocument()
  })

  it('allows authenticated user to open listing form and submit new provider', async () => {
    getProvidersMock.mockResolvedValue([])
    createProviderMock.mockResolvedValue({
      id: 'p-2',
      name: 'Quick Plumb',
      category: 'Plumber',
      description: 'Emergency plumbing repairs',
      serviceArea: 'Downtown',
      hourlyRate: 60.0,
    })

    const user = userEvent.setup()
    renderMarketplacePage({ id: 'u-1', email: 'owner@example.com' })

    await waitFor(() => {
      expect(screen.getByText('+ List your service')).toBeInTheDocument()
    })

    await user.click(screen.getByText('+ List your service'))
    expect(screen.getByText('Create service provider profile')).toBeInTheDocument()

    await user.type(screen.getByLabelText(/Business \/ Provider Name/i), 'Quick Plumb')
    await user.type(screen.getByLabelText(/Service Area/i), 'Downtown')
    await user.type(screen.getByLabelText(/Description/i), 'Emergency plumbing repairs')
    await user.click(screen.getByRole('button', { name: 'Publish Listing' }))

    await waitFor(() => {
      expect(createProviderMock).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Quick Plumb',
          serviceArea: 'Downtown',
          description: 'Emergency plumbing repairs',
        }),
      )
    })
  })
})
