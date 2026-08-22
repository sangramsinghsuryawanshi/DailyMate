import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NotificationsPage from './NotificationsPage'
import { AuthContext } from '../../context/authContext'

const getNotificationsMock = vi.fn()
const updateNotificationMock = vi.fn()
const markAllReadMock = vi.fn()
const deleteNotificationMock = vi.fn()
const getProvidersMock = vi.fn()

vi.mock('../services/notificationsApi', () => ({
  getNotifications: (page, size) => getNotificationsMock(page, size),
  updateNotification: (id, payload) => updateNotificationMock(id, payload),
  markAllRead: () => markAllReadMock(),
  deleteNotification: (id) => deleteNotificationMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

function renderNotificationsPage(user = { id: 'u-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <NotificationsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    getNotificationsMock.mockReset()
    updateNotificationMock.mockReset()
    markAllReadMock.mockReset()
    deleteNotificationMock.mockReset()
    getProvidersMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({
      content: [],
      totalElements: 0,
      page: 0,
      size: 20,
    })
  })

  it('renders notifications with type badge, read status, and filters by unread', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [
        {
          id: 'n-1',
          title: 'Medicine Due',
          message: 'Time for evening dosage',
          type: 'reminder',
          read: false,
          createdAt: '2026-08-21T18:00:00Z',
          targetUrl: '/medicines',
        },
        {
          id: 'n-2',
          title: 'Welcome to DailyMate',
          message: 'Your account is ready',
          type: 'info',
          read: true,
          createdAt: '2026-08-20T10:00:00Z',
          targetUrl: null,
        },
      ],
      totalElements: 2,
      page: 0,
      size: 20,
    })

    const user = userEvent.setup()
    renderNotificationsPage()

    await waitFor(() => {
      expect(screen.getByText('Medicine Due')).toBeInTheDocument()
      expect(screen.getByText('Welcome to DailyMate')).toBeInTheDocument()
      expect(screen.getByText(/1 unread/i)).toBeInTheDocument()
    })

    // Filter by Unread
    await user.click(screen.getByRole('button', { name: 'Unread' }))
    expect(screen.getByText('Medicine Due')).toBeInTheDocument()
    expect(screen.queryByText('Welcome to DailyMate')).not.toBeInTheDocument()
  })

  it('marks single notification as read with complete request contract', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [
        {
          id: 'n-1',
          title: 'Medicine Due',
          message: 'Time for evening dosage',
          type: 'reminder',
          read: false,
          createdAt: '2026-08-21T18:00:00Z',
          targetType: 'MEDICINE',
          targetId: 'med-123',
          targetUrl: '/medicines',
        },
      ],
      totalElements: 1,
      page: 0,
      size: 20,
    })
    updateNotificationMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderNotificationsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Mark as read' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Mark as read' }))

    await waitFor(() => {
      expect(updateNotificationMock).toHaveBeenCalledWith('n-1', {
        title: 'Medicine Due',
        message: 'Time for evening dosage',
        type: 'reminder',
        read: true,
        targetType: 'MEDICINE',
        targetId: 'med-123',
        targetUrl: '/medicines',
      })
    })
  })

  it('marks all notifications as read using markAllRead API', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [
        {
          id: 'n-1',
          title: 'Medicine Due',
          message: 'Time for evening dosage',
          type: 'reminder',
          read: false,
          createdAt: '2026-08-21T18:00:00Z',
        },
      ],
      totalElements: 1,
      page: 0,
      size: 20,
    })
    markAllReadMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderNotificationsPage()

    await waitFor(() => {
      expect(screen.getAllByRole('button', { name: 'Mark all as read' }).length).toBeGreaterThan(0)
    })

    const markAllBtn = screen.getAllByRole('button', { name: 'Mark all as read' })[0]
    await user.click(markAllBtn)

    await waitFor(() => {
      expect(markAllReadMock).toHaveBeenCalled()
    })
  })

  it('dismisses (deletes) a notification', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [
        {
          id: 'n-1',
          title: 'Medicine Due',
          message: 'Time for evening dosage',
          type: 'reminder',
          read: true,
          createdAt: '2026-08-21T18:00:00Z',
        },
      ],
      totalElements: 1,
      page: 0,
      size: 20,
    })
    deleteNotificationMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderNotificationsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Dismiss' }))

    await waitFor(() => {
      expect(deleteNotificationMock).toHaveBeenCalledWith('n-1')
    })
  })

  it('renders clean empty state when no notifications exist', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [],
      totalElements: 0,
      page: 0,
      size: 20,
    })

    renderNotificationsPage()

    await waitFor(() => {
      expect(screen.getByText(/You're all caught up/i)).toBeInTheDocument()
    })
  })
})
