import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import BloodDonationPage from './BloodDonationPage'
import { AuthContext } from '../../context/authContext'

const getBloodRequestsMock = vi.fn()
const createBloodRequestMock = vi.fn()
const updateBloodRequestMock = vi.fn()
const deleteBloodRequestMock = vi.fn()
const getDonationCentersMock = vi.fn()
const createDonationCenterMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/bloodApi', () => ({
  getBloodRequests: (params) => getBloodRequestsMock(params),
  createBloodRequest: (payload) => createBloodRequestMock(payload),
  updateBloodRequest: (id, payload) => updateBloodRequestMock(id, payload),
  deleteBloodRequest: (id) => deleteBloodRequestMock(id),
  getDonationCenters: () => getDonationCentersMock(),
  createDonationCenter: (payload) => createDonationCenterMock(payload),
  updateDonationCenter: vi.fn(),
  deleteDonationCenter: vi.fn(),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderBloodDonationPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <BloodDonationPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('BloodDonationPage', () => {
  beforeEach(() => {
    getBloodRequestsMock.mockReset()
    createBloodRequestMock.mockReset()
    updateBloodRequestMock.mockReset()
    deleteBloodRequestMock.mockReset()
    getDonationCentersMock.mockReset()
    createDonationCenterMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getBloodRequestsMock.mockResolvedValue([])
    getDonationCentersMock.mockResolvedValue([])
  })

  it('renders blood requests and shows ownership actions only for current user', async () => {
    getBloodRequestsMock.mockResolvedValue([
      {
        id: 'req-1',
        userId: 'user-1', // owned by logged-in user
        patientName: 'Rohan Sharma',
        bloodGroup: 'O+',
        unitsNeeded: 2,
        hospitalLocation: 'City Care Hospital',
        urgency: 'URGENT',
        status: 'OPEN',
        contactName: 'Priya Sharma',
        contactPhone: '555-1234',
        additionalNotes: 'Urgent surgery',
        createdAt: '2026-08-21T10:00:00Z',
      },
      {
        id: 'req-2',
        userId: 'user-other', // owned by someone else
        patientName: 'Anil Kumar',
        bloodGroup: 'A-',
        unitsNeeded: 1,
        hospitalLocation: 'Apollo Clinic',
        urgency: 'STANDARD',
        status: 'OPEN',
        contactName: 'Sunil Kumar',
        contactPhone: '555-5678',
        additionalNotes: '',
        createdAt: '2026-08-20T14:00:00Z',
      },
    ])

    renderBloodDonationPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByText(/Rohan Sharma/i)).toBeInTheDocument()
      expect(screen.getByText(/Anil Kumar/i)).toBeInTheDocument()
      expect(screen.getByText('🔴 URGENT')).toBeInTheDocument()
    })

    // Owner actions should only be rendered for req-1
    expect(screen.getByRole('button', { name: /Mark Fulfilled/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Delete' }).length).toBe(1)
  })

  it('submits a new blood request with complete valid payload', async () => {
    getBloodRequestsMock.mockResolvedValue([])
    createBloodRequestMock.mockResolvedValue({
      id: 'req-3',
      userId: 'user-1',
      patientName: 'Kavita Patel',
      bloodGroup: 'B+',
      unitsNeeded: 3,
      hospitalLocation: 'Sahyadri Hospital',
      urgency: 'URGENT',
      status: 'OPEN',
      contactName: 'Amit Patel',
      contactPhone: '555-9999',
      additionalNotes: 'Immediate requirement',
    })

    const user = userEvent.setup()
    renderBloodDonationPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Submit request' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText('Patient name'), 'Kavita Patel')
    await user.selectOptions(screen.getByLabelText('Blood group'), 'B+')
    await user.clear(screen.getByLabelText('Units needed'))
    await user.type(screen.getByLabelText('Units needed'), '3')
    await user.type(screen.getByLabelText(/Hospital \/ Location/i), 'Sahyadri Hospital')
    await user.selectOptions(screen.getByLabelText('Urgency level'), 'URGENT')
    await user.type(screen.getByLabelText('Contact name'), 'Amit Patel')
    await user.type(screen.getByLabelText('Contact phone'), '555-9999')
    await user.type(screen.getByLabelText('Additional notes'), 'Immediate requirement')

    await user.click(screen.getByRole('button', { name: 'Submit request' }))

    await waitFor(() => {
      expect(createBloodRequestMock).toHaveBeenCalledWith(
        expect.objectContaining({
          patientName: 'Kavita Patel',
          bloodGroup: 'B+',
          unitsNeeded: 3,
          hospitalLocation: 'Sahyadri Hospital',
          urgency: 'URGENT',
          contactName: 'Amit Patel',
          contactPhone: '555-9999',
        }),
      )
    })
  }, 10000)

  it('switches to Donation Centers tab and lists centers', async () => {
    getDonationCentersMock.mockResolvedValue([
      {
        id: 'center-1',
        name: 'Ruby Hall Blood Center',
        location: 'Sassoon Road',
        contact: '020-26163391',
        description: '24x7 blood bank and component facility',
      },
    ])

    const user = userEvent.setup()
    renderBloodDonationPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Donation Centers/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Donation Centers/i }))

    await waitFor(() => {
      expect(screen.getByText(/Ruby Hall Blood Center/i)).toBeInTheDocument()
      expect(screen.getByText(/Sassoon Road/i)).toBeInTheDocument()
    })
  })

  it('renders clean empty state when no blood requests exist', async () => {
    getBloodRequestsMock.mockResolvedValue([])

    renderBloodDonationPage()

    await waitFor(() => {
      expect(screen.getByText(/No active blood requests at this time/i)).toBeInTheDocument()
    })
  })
})
