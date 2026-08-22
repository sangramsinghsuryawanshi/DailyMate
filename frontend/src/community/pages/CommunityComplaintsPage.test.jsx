import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import CommunityComplaintsPage from './CommunityComplaintsPage'
import { AuthContext } from '../../context/authContext'

const getComplaintsMock = vi.fn()
const createComplaintMock = vi.fn()
const updateComplaintMock = vi.fn()
const deleteComplaintMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/communityComplaintsApi', () => ({
  getCommunityComplaints: () => getComplaintsMock(),
  createCommunityComplaint: (data) => createComplaintMock(data),
  updateCommunityComplaint: (id, data) => updateComplaintMock(id, data),
  deleteCommunityComplaint: (id) => deleteComplaintMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderCommunityComplaintsPage(user = { id: 'u-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <CommunityComplaintsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('CommunityComplaintsPage', () => {
  beforeEach(() => {
    getComplaintsMock.mockReset()
    createComplaintMock.mockReset()
    updateComplaintMock.mockReset()
    deleteComplaintMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getComplaintsMock.mockResolvedValue([])
  })

  it('renders existing complaints feed with status badges and filters by status', async () => {
    getComplaintsMock.mockResolvedValue([
      {
        id: 'c-1',
        title: 'Broken Street Light',
        category: 'Infrastructure',
        location: 'Oak Avenue',
        description: 'Light flickers continuously',
        status: 'OPEN',
      },
      {
        id: 'c-2',
        title: 'Pothole on Main St',
        category: 'Roads',
        location: 'Main St',
        description: 'Large pothole near crosswalk',
        status: 'RESOLVED',
      },
    ])

    const user = userEvent.setup()
    renderCommunityComplaintsPage()

    await waitFor(() => {
      expect(screen.getByText('Broken Street Light')).toBeInTheDocument()
      expect(screen.getByText('Pothole on Main St')).toBeInTheDocument()
      expect(screen.getAllByText('OPEN').length).toBeGreaterThan(0)
      expect(screen.getAllByText('RESOLVED').length).toBeGreaterThan(0)
    })

    // Click OPEN filter button
    await user.click(screen.getByRole('button', { name: 'OPEN' }))
    expect(screen.getByText('Broken Street Light')).toBeInTheDocument()
    expect(screen.queryByText('Pothole on Main St')).not.toBeInTheDocument()
  })

  it('submits a new community complaint report', async () => {
    getComplaintsMock.mockResolvedValue([])
    createComplaintMock.mockResolvedValue({
      id: 'c-3',
      title: 'Water Leak',
      category: 'Utilities',
      location: 'Pine Street',
      description: 'Pipe leaking onto sidewalk',
      status: 'OPEN',
    })

    const user = userEvent.setup()
    renderCommunityComplaintsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Submit report' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText('Title'), 'Water Leak')
    await user.type(screen.getByLabelText('Category'), 'Utilities')
    await user.type(screen.getByLabelText('Location'), 'Pine Street')
    await user.type(screen.getByLabelText('Description'), 'Pipe leaking onto sidewalk')

    await user.click(screen.getByRole('button', { name: 'Submit report' }))

    await waitFor(() => {
      expect(createComplaintMock).toHaveBeenCalledWith({
        title: 'Water Leak',
        category: 'Utilities',
        location: 'Pine Street',
        description: 'Pipe leaking onto sidewalk',
      })
    })
  })

  it('deletes a complaint', async () => {
    getComplaintsMock.mockResolvedValue([
      {
        id: 'c-1',
        title: 'Broken Street Light',
        category: 'Infrastructure',
        location: 'Oak Avenue',
        description: 'Light flickers',
        status: 'OPEN',
      },
    ])
    deleteComplaintMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderCommunityComplaintsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteComplaintMock).toHaveBeenCalledWith('c-1')
    })
  })
})
