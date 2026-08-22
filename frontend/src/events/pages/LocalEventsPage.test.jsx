import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import LocalEventsPage from './LocalEventsPage'
import { AuthContext } from '../../context/authContext'

const getLocalEventsMock = vi.fn()
const createLocalEventMock = vi.fn()
const updateLocalEventMock = vi.fn()
const deleteLocalEventMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/localEventsApi', () => ({
  getLocalEvents: (params) => getLocalEventsMock(params),
  createLocalEvent: (payload) => createLocalEventMock(payload),
  updateLocalEvent: (id, payload) => updateLocalEventMock(id, payload),
  deleteLocalEvent: (id) => deleteLocalEventMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderLocalEventsPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <LocalEventsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('LocalEventsPage', () => {
  beforeEach(() => {
    getLocalEventsMock.mockReset()
    createLocalEventMock.mockReset()
    updateLocalEventMock.mockReset()
    deleteLocalEventMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getLocalEventsMock.mockResolvedValue([])
  })

  it('renders upcoming events and renders organizer action buttons only for own events', async () => {
    const futureDate = new Date(Date.now() + 86400000 * 5).toISOString()

    getLocalEventsMock.mockResolvedValue([
      {
        id: 'event-1',
        userId: 'user-1', // owned by logged-in user
        title: 'Community Cleanup Drive',
        category: 'Volunteer',
        location: 'Riverside Park',
        eventDate: futureDate,
        description: 'Join neighbors to clean up riverside',
        status: 'PUBLISHED',
        createdAt: '2026-08-21T10:00:00Z',
      },
      {
        id: 'event-2',
        userId: 'user-other', // owned by someone else
        title: 'Neighborhood Football Match',
        category: 'Sports',
        location: 'Sports Complex',
        eventDate: futureDate,
        description: 'Friendly match between zones',
        status: 'PUBLISHED',
        createdAt: '2026-08-20T12:00:00Z',
      },
    ])

    renderLocalEventsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByText('Community Cleanup Drive')).toBeInTheDocument()
      expect(screen.getByText('Neighborhood Football Match')).toBeInTheDocument()
    })

    // Edit and Cancel Event should only be rendered for event-1
    expect(screen.getAllByRole('button', { name: 'Edit' }).length).toBe(1)
    expect(screen.getByRole('button', { name: 'Cancel Event' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Delete' }).length).toBe(1)
  })

  it('submits a new event with complete valid payload', async () => {
    getLocalEventsMock.mockResolvedValue([])
    createLocalEventMock.mockResolvedValue({
      id: 'event-3',
      userId: 'user-1',
      title: 'Coding Workshop',
      category: 'Workshop',
      location: 'Community Center',
      eventDate: '2026-09-10T14:00:00.000Z',
      description: 'Intro to Web Development',
      status: 'PUBLISHED',
    })

    const user = userEvent.setup()
    renderLocalEventsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Publish event' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText('Event title'), 'Coding Workshop')
    await user.selectOptions(screen.getByLabelText('Category'), 'Workshop')
    await user.type(screen.getByLabelText(/Location \/ Venue/i), 'Community Center')
    await user.type(screen.getByLabelText(/Date & Time/i), '2026-09-10T14:00')
    await user.type(screen.getByLabelText('Description'), 'Intro to Web Development')

    await user.click(screen.getByRole('button', { name: 'Publish event' }))

    await waitFor(() => {
      expect(createLocalEventMock).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Coding Workshop',
          category: 'Workshop',
          location: 'Community Center',
          description: 'Intro to Web Development',
        }),
      )
    })
  }, 10000)

  it('allows organizer to cancel a published event', async () => {
    const futureDate = new Date(Date.now() + 86400000 * 5).toISOString()

    getLocalEventsMock.mockResolvedValue([
      {
        id: 'event-1',
        userId: 'user-1',
        title: 'Community Cleanup Drive',
        category: 'Volunteer',
        location: 'Riverside Park',
        eventDate: futureDate,
        description: 'Join neighbors to clean up riverside',
        status: 'PUBLISHED',
      },
    ])
    updateLocalEventMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderLocalEventsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Cancel Event' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Cancel Event' }))

    await waitFor(() => {
      expect(updateLocalEventMock).toHaveBeenCalledWith(
        'event-1',
        expect.objectContaining({
          status: 'CANCELLED',
        }),
      )
    })
  })

  it('renders clean empty state when no events match', async () => {
    getLocalEventsMock.mockResolvedValue([])

    renderLocalEventsPage()

    await waitFor(() => {
      expect(screen.getByText(/No events found/i)).toBeInTheDocument()
    })
  })
})
