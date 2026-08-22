import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AdminPage from './AdminPage'
import { AuthContext } from '../../context/authContext'

const getAdminStatsMock = vi.fn()
const getAdminComplaintsMock = vi.fn()
const updateComplaintStatusMock = vi.fn()
const getAdminJobsMock = vi.fn()
const updateAdminJobStatusMock = vi.fn()
const deleteAdminJobMock = vi.fn()
const getAdminBloodRequestsMock = vi.fn()
const updateAdminBloodRequestStatusMock = vi.fn()
const deleteAdminBloodRequestMock = vi.fn()
const getAdminEventsMock = vi.fn()
const updateAdminEventStatusMock = vi.fn()
const deleteAdminEventMock = vi.fn()
const getAdminLostFoundMock = vi.fn()
const deleteAdminLostFoundMock = vi.fn()
const getAdminUsersMock = vi.fn()
const updateAdminUserStatusMock = vi.fn()

const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/adminApi', () => ({
  getAdminStats: () => getAdminStatsMock(),
  getAdminComplaints: () => getAdminComplaintsMock(),
  updateComplaintStatus: (id, status) => updateComplaintStatusMock(id, status),
  getAdminJobs: () => getAdminJobsMock(),
  updateAdminJobStatus: (id, status) => updateAdminJobStatusMock(id, status),
  deleteAdminJob: (id) => deleteAdminJobMock(id),
  getAdminBloodRequests: () => getAdminBloodRequestsMock(),
  updateAdminBloodRequestStatus: (id, status) => updateAdminBloodRequestStatusMock(id, status),
  deleteAdminBloodRequest: (id) => deleteAdminBloodRequestMock(id),
  getAdminEvents: () => getAdminEventsMock(),
  updateAdminEventStatus: (id, status) => updateAdminEventStatusMock(id, status),
  deleteAdminEvent: (id) => deleteAdminEventMock(id),
  getAdminLostFound: () => getAdminLostFoundMock(),
  deleteAdminLostFound: (id) => deleteAdminLostFoundMock(id),
  getAdminUsers: () => getAdminUsersMock(),
  updateAdminUserStatus: (id, status) => updateAdminUserStatusMock(id, status),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderAdminPage(user = { id: 'admin-1', email: 'admin@example.com', role: 'ADMIN' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'admin-token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <AdminPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('AdminPage — Moderation & Cross-Module Governance', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })

    getAdminStatsMock.mockResolvedValue({
      totalUsers: 10,
      activeUsers: 9,
      suspendedUsers: 1,
      totalComplaints: 5,
      openComplaints: 2,
      inReviewComplaints: 1,
      resolvedComplaints: 2,
      rejectedComplaints: 0,
      totalLostFound: 3,
      totalJobs: 4,
      openJobs: 3,
      closedJobs: 1,
      totalBloodRequests: 2,
      openBloodRequests: 1,
      fulfilledBloodRequests: 1,
      cancelledBloodRequests: 0,
      totalEvents: 3,
      publishedEvents: 2,
      cancelledEvents: 1,
      completedEvents: 0,
    })

    getAdminComplaintsMock.mockResolvedValue([])
    getAdminJobsMock.mockResolvedValue([])
    getAdminBloodRequestsMock.mockResolvedValue([])
    getAdminEventsMock.mockResolvedValue([])
    getAdminLostFoundMock.mockResolvedValue([])
    getAdminUsersMock.mockResolvedValue([])
  })

  it('renders non-admin access denied card when user role is USER', async () => {
    renderAdminPage({ id: 'user-1', email: 'user@example.com', role: 'USER' })

    expect(screen.getByText('Administrator Privileges Required')).toBeInTheDocument()
    expect(screen.getByText(/Your current user account does not have permission/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Return to Dashboard' })).toBeInTheDocument()
  })

  it('renders overview statistics with database-backed numbers for ADMIN user', async () => {
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByText('Community Admin Moderation Hub')).toBeInTheDocument()
      expect(screen.getByText('Registered Users')).toBeInTheDocument()
      expect(screen.getByText('10')).toBeInTheDocument()
      expect(screen.getByText('9 active · 1 suspended')).toBeInTheDocument()
      expect(screen.getByText(/complaints pending review/i)).toBeInTheDocument()
    })
  })

  it('switches to complaints tab and moderates complaint status', async () => {
    getAdminComplaintsMock.mockResolvedValue([
      {
        id: 'c-10',
        title: 'Damaged Guardrail',
        category: 'Safety',
        location: 'Highway 4',
        description: 'Guardrail damaged after accident',
        status: 'OPEN',
      },
    ])
    updateComplaintStatusMock.mockResolvedValue({ id: 'c-10', status: 'IN_REVIEW' })

    const user = userEvent.setup({ delay: null })
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Complaints/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Complaints/i }))

    await waitFor(() => {
      expect(screen.getByText('Damaged Guardrail')).toBeInTheDocument()
      expect(screen.getByText('Safety')).toBeInTheDocument()
    })

    const select = screen.getByRole('combobox')
    await user.selectOptions(select, 'IN_REVIEW')

    await waitFor(() => {
      expect(updateComplaintStatusMock).toHaveBeenCalledWith('c-10', 'IN_REVIEW')
    })
  })

  it('switches to jobs tab, renders INR salary, toggles status and deletes job', async () => {
    getAdminJobsMock.mockResolvedValue([
      {
        id: 'job-1',
        title: 'Community Electrician',
        category: 'Services',
        type: 'Full-time',
        location: 'Kothrud, Pune',
        salary: 28000.0,
        companyName: 'Pune Metro Facilities',
        status: 'OPEN',
        description: 'Commercial maintenance',
      },
    ])
    updateAdminJobStatusMock.mockResolvedValue({ id: 'job-1', status: 'CLOSED' })
    deleteAdminJobMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Jobs/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Jobs/i }))

    await waitFor(() => {
      expect(screen.getByText('Community Electrician')).toBeInTheDocument()
      expect(screen.getByText('₹28,000.00')).toBeInTheDocument()
    })

    // Toggle to CLOSED
    await user.click(screen.getByRole('button', { name: 'Close Job' }))
    await waitFor(() => {
      expect(updateAdminJobStatusMock).toHaveBeenCalledWith('job-1', 'CLOSED')
    })

    // Delete job
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => {
      expect(deleteAdminJobMock).toHaveBeenCalledWith('job-1')
    })
  })

  it('switches to blood requests tab, updates status and deletes request', async () => {
    getAdminBloodRequestsMock.mockResolvedValue([
      {
        id: 'blood-1',
        patientName: 'Ramesh Patil',
        bloodGroup: 'B+',
        unitsNeeded: 3,
        hospitalLocation: 'Deenanath Mangeshkar Hospital',
        urgency: 'URGENT',
        status: 'OPEN',
        contactName: 'Suresh Patil',
        contactPhone: '+91-9876543210',
        additionalNotes: 'Emergency unit replacement',
      },
    ])
    updateAdminBloodRequestStatusMock.mockResolvedValue({ id: 'blood-1', status: 'FULFILLED' })
    deleteAdminBloodRequestMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Blood Requests/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Blood Requests/i }))

    await waitFor(() => {
      expect(screen.getByText(/Patient: Ramesh Patil/i)).toBeInTheDocument()
      expect(screen.getByText('🩸 B+')).toBeInTheDocument()
    })

    // Mark fulfilled
    await user.click(screen.getByRole('button', { name: 'Mark Fulfilled' }))
    await waitFor(() => {
      expect(updateAdminBloodRequestStatusMock).toHaveBeenCalledWith('blood-1', 'FULFILLED')
    })

    // Delete
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => {
      expect(deleteAdminBloodRequestMock).toHaveBeenCalledWith('blood-1')
    })
  })

  it('switches to events tab, toggles event status and deletes event', async () => {
    getAdminEventsMock.mockResolvedValue([
      {
        id: 'ev-1',
        title: 'Society Annual Gathering',
        category: 'Cultural',
        location: 'Club House',
        eventDate: '2026-09-01T18:00:00Z',
        status: 'PUBLISHED',
        description: 'Dinner and music',
      },
    ])
    updateAdminEventStatusMock.mockResolvedValue({ id: 'ev-1', status: 'CANCELLED' })
    deleteAdminEventMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Events/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Events/i }))

    await waitFor(() => {
      expect(screen.getByText('Society Annual Gathering')).toBeInTheDocument()
    })

    // Cancel event
    await user.click(screen.getByRole('button', { name: 'Cancel Event' }))
    await waitFor(() => {
      expect(updateAdminEventStatusMock).toHaveBeenCalledWith('ev-1', 'CANCELLED')
    })

    // Delete event
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => {
      expect(deleteAdminEventMock).toHaveBeenCalledWith('ev-1')
    })
  })

  it('switches to lost & found tab and deletes listing', async () => {
    getAdminLostFoundMock.mockResolvedValue([
      {
        id: 'lf-1',
        title: 'Lost House Keys',
        itemType: 'Keys',
        location: 'Main Gate',
        contactName: 'Pooja Sharma',
        contactPhone: '9822001122',
        description: 'Set of 3 keys with blue keychain',
      },
    ])
    deleteAdminLostFoundMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderAdminPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Lost & Found/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Lost & Found/i }))

    await waitFor(() => {
      expect(screen.getByText('Lost House Keys')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Delete \/ Remove Listing/i }))
    await waitFor(() => {
      expect(deleteAdminLostFoundMock).toHaveBeenCalledWith('lf-1')
    })
  })

  it('switches to users tab, shows self/admin protection and suspends standard user', async () => {
    getAdminUsersMock.mockResolvedValue([
      {
        id: 'admin-1',
        email: 'admin@example.com',
        firstName: 'Super',
        lastName: 'Admin',
        role: 'ADMIN',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
      },
      {
        id: 'user-2',
        email: 'baduser@example.com',
        firstName: 'Bad',
        lastName: 'Actor',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2026-02-01T00:00:00Z',
      },
    ])
    updateAdminUserStatusMock.mockResolvedValue({ id: 'user-2', status: 'SUSPENDED' })

    const user = userEvent.setup({ delay: null })
    renderAdminPage({ id: 'admin-1', email: 'admin@example.com', role: 'ADMIN' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Users/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Users/i }))

    await waitFor(() => {
      expect(screen.getByText('Super Admin')).toBeInTheDocument()
      expect(screen.getByText('Cannot modify self')).toBeInTheDocument()
      expect(screen.getByText('Bad Actor')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Suspend Account' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Suspend Account' }))
    await waitFor(() => {
      expect(updateAdminUserStatusMock).toHaveBeenCalledWith('user-2', 'SUSPENDED')
    })
  })
})
