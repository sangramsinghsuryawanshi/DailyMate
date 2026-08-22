import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import LostFoundPage from './LostFoundPage'
import { AuthContext } from '../../context/authContext'

const getLostFoundPostsMock = vi.fn()
const createLostFoundPostMock = vi.fn()
const updateLostFoundPostMock = vi.fn()
const deleteLostFoundPostMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/lostFoundApi', () => ({
  getLostFoundPosts: () => getLostFoundPostsMock(),
  createLostFoundPost: (payload) => createLostFoundPostMock(payload),
  updateLostFoundPost: (id, payload) => updateLostFoundPostMock(id, payload),
  deleteLostFoundPost: (id) => deleteLostFoundPostMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderLostFoundPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <LostFoundPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('LostFoundPage', () => {
  beforeEach(() => {
    getLostFoundPostsMock.mockReset()
    createLostFoundPostMock.mockReset()
    updateLostFoundPostMock.mockReset()
    deleteLostFoundPostMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getLostFoundPostsMock.mockResolvedValue([])
  })

  it('renders community posts and conditionally shows edit/delete only for owned posts', async () => {
    getLostFoundPostsMock.mockResolvedValue([
      {
        id: 'post-1',
        userId: 'user-1', // owned by logged-in user
        title: 'Lost Blue Backpack',
        itemType: 'Backpack',
        location: 'Central Station',
        description: 'Blue hiking backpack',
        contactName: 'Ava',
        contactPhone: '555-1111',
        createdAt: '2026-08-21T18:00:00Z',
      },
      {
        id: 'post-2',
        userId: 'user-other', // owned by someone else
        title: 'Found Gold Watch',
        itemType: 'Watch',
        location: 'North Plaza',
        description: 'Gold wristwatch near fountain',
        contactName: 'Bob',
        contactPhone: '555-2222',
        createdAt: '2026-08-20T12:00:00Z',
      },
    ])

    renderLostFoundPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByText('Lost Blue Backpack')).toBeInTheDocument()
      expect(screen.getByText('Found Gold Watch')).toBeInTheDocument()
    })

    // Edit and Delete should only appear once (for post-1)
    expect(screen.getAllByRole('button', { name: 'Edit' }).length).toBe(1)
    expect(screen.getAllByRole('button', { name: 'Delete' }).length).toBe(1)
  })

  it('switches to My notices tab and filters correctly', async () => {
    getLostFoundPostsMock.mockResolvedValue([
      {
        id: 'post-1',
        userId: 'user-1',
        title: 'Lost Blue Backpack',
        itemType: 'Backpack',
        location: 'Central Station',
        description: 'Blue hiking backpack',
        contactName: 'Ava',
        contactPhone: '555-1111',
        createdAt: '2026-08-21T18:00:00Z',
      },
      {
        id: 'post-2',
        userId: 'user-other',
        title: 'Found Gold Watch',
        itemType: 'Watch',
        location: 'North Plaza',
        description: 'Gold wristwatch near fountain',
        contactName: 'Bob',
        contactPhone: '555-2222',
        createdAt: '2026-08-20T12:00:00Z',
      },
    ])

    const user = userEvent.setup()
    renderLostFoundPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByText('Lost Blue Backpack')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My notices/i }))

    expect(screen.getByText('Lost Blue Backpack')).toBeInTheDocument()
    expect(screen.queryByText('Found Gold Watch')).not.toBeInTheDocument()
  })

  it('creates new post submitting complete valid payload', async () => {
    getLostFoundPostsMock.mockResolvedValue([])
    createLostFoundPostMock.mockResolvedValue({
      id: 'post-3',
      userId: 'user-1',
      title: 'Lost Black Wallet',
      itemType: 'Wallet',
      location: 'Main St',
      description: 'Leather wallet with cards',
      contactName: 'Sam',
      contactPhone: '555-3333',
    })

    const user = userEvent.setup()
    renderLostFoundPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Post report' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText('Title'), 'Lost Black Wallet')
    await user.type(screen.getByLabelText(/Item type/i), 'Wallet')
    await user.type(screen.getByLabelText('Location'), 'Main St')
    await user.type(screen.getByLabelText('Description'), 'Leather wallet with cards')
    await user.type(screen.getByLabelText('Contact name'), 'Sam')
    await user.type(screen.getByLabelText('Contact phone'), '555-3333')

    await user.click(screen.getByRole('button', { name: 'Post report' }))

    await waitFor(() => {
      expect(createLostFoundPostMock).toHaveBeenCalledWith({
        title: 'Lost Black Wallet',
        itemType: 'Wallet',
        location: 'Main St',
        description: 'Leather wallet with cards',
        contactName: 'Sam',
        contactPhone: '555-3333',
      })
    })
  })

  it('deletes owned post', async () => {
    getLostFoundPostsMock.mockResolvedValue([
      {
        id: 'post-1',
        userId: 'user-1',
        title: 'Lost Blue Backpack',
        itemType: 'Backpack',
        location: 'Central Station',
        description: 'Blue hiking backpack',
        contactName: 'Ava',
        contactPhone: '555-1111',
        createdAt: '2026-08-21T18:00:00Z',
      },
    ])
    deleteLostFoundPostMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderLostFoundPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteLostFoundPostMock).toHaveBeenCalledWith('post-1')
    })
  })

  it('renders clean empty state when no notices exist', async () => {
    getLostFoundPostsMock.mockResolvedValue([])

    renderLostFoundPage()

    await waitFor(() => {
      expect(screen.getByText(/No lost & found notices yet/i)).toBeInTheDocument()
    })
  })
})
