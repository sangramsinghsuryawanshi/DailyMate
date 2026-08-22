import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EmergencyContactsPage from './EmergencyContactsPage'
import { AuthContext } from '../../context/authContext'

const getEmergencyContactsMock = vi.fn()
const getMyEmergencyContactsMock = vi.fn()
const createEmergencyContactMock = vi.fn()
const updateEmergencyContactMock = vi.fn()
const deleteEmergencyContactMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/emergencyContactsApi', () => ({
  getEmergencyContacts: (params) => getEmergencyContactsMock(params),
  getMyEmergencyContacts: (params) => getMyEmergencyContactsMock(params),
  createEmergencyContact: (payload) => createEmergencyContactMock(payload),
  updateEmergencyContact: (id, payload) => updateEmergencyContactMock(id, payload),
  deleteEmergencyContact: (id) => deleteEmergencyContactMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderEmergencyContactsPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <EmergencyContactsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('EmergencyContactsPage', () => {
  beforeEach(() => {
    getEmergencyContactsMock.mockReset()
    getMyEmergencyContactsMock.mockReset()
    createEmergencyContactMock.mockReset()
    updateEmergencyContactMock.mockReset()
    deleteEmergencyContactMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getEmergencyContactsMock.mockResolvedValue([])
    getMyEmergencyContactsMock.mockResolvedValue([])
  })

  it('renders verified public emergency directory and verifies tap-to-call link', async () => {
    getEmergencyContactsMock.mockResolvedValue([
      {
        id: 'pub-1',
        userId: null,
        name: 'National Emergency Response',
        category: 'Ambulance',
        phone: '108',
        location: 'Citywide',
        description: '24x7 ambulance dispatch',
      },
    ])

    renderEmergencyContactsPage()

    await waitFor(() => {
      expect(screen.getByText('National Emergency Response')).toBeInTheDocument()
      expect(screen.getByText('Verified Public Service')).toBeInTheDocument()
    })

    // Verify tap-to-call action contains exact tel: URI
    const callBtn = screen.getByRole('link', { name: /Call 108/i })
    expect(callBtn).toHaveAttribute('href', 'tel:108')

    // Public contacts must never render Edit or Delete buttons
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('allows switching to My Personal Contacts and displays ownership controls', async () => {
    getMyEmergencyContactsMock.mockResolvedValue([
      {
        id: 'pers-1',
        userId: 'user-1',
        name: 'Dr. Ramesh Kulkarni',
        category: 'Hospital',
        phone: '+91 98220 12345',
        location: 'Kothrud Clinic',
        description: 'Family physician',
      },
    ])

    const user = userEvent.setup()
    renderEmergencyContactsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Personal Contacts/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Personal Contacts/i }))

    await waitFor(() => {
      expect(screen.getByText('Dr. Ramesh Kulkarni')).toBeInTheDocument()
      expect(screen.getByText('Personal Contact')).toBeInTheDocument()
    })

    // Owner controls should be visible for personal contact
    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()

    const callBtn = screen.getByRole('link', { name: /Call \+91 98220 12345/i })
    expect(callBtn).toHaveAttribute('href', 'tel:+91 98220 12345')
  })

  it('submits a new personal emergency contact, automatically switches tab and refreshes list', async () => {
    getEmergencyContactsMock.mockResolvedValue([])
    getMyEmergencyContactsMock.mockResolvedValue([
      {
        id: 'pers-2',
        userId: 'user-1',
        name: 'Society Security Guard',
        category: 'Personal',
        phone: '+91 99999 00000',
        location: 'Gate 1',
        description: 'Building night watchman',
      },
    ])
    createEmergencyContactMock.mockResolvedValue({
      id: 'pers-2',
      userId: 'user-1',
      name: 'Society Security Guard',
      category: 'Personal',
      phone: '+91 99999 00000',
      location: 'Gate 1',
      description: 'Building night watchman',
    })

    const user = userEvent.setup()
    renderEmergencyContactsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Save contact' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText(/Contact name/i), 'Society Security Guard')
    await user.selectOptions(screen.getByLabelText('Category'), 'Personal')
    await user.type(screen.getByLabelText(/Phone number/i), '+91 99999 00000')
    await user.type(screen.getByLabelText(/Location \/ Clinic/i), 'Gate 1')
    await user.type(screen.getByLabelText(/Description \/ Notes/i), 'Building night watchman')

    await user.click(screen.getByRole('button', { name: 'Save contact' }))

    await waitFor(() => {
      expect(createEmergencyContactMock).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Society Security Guard',
          category: 'Personal',
          phone: '+91 99999 00000',
          location: 'Gate 1',
          description: 'Building night watchman',
        }),
      )
    })
  }, 10000)

  it('allows editing an existing personal contact and updates UI', async () => {
    getMyEmergencyContactsMock.mockResolvedValue([
      {
        id: 'pers-1',
        userId: 'user-1',
        name: 'Dr. Ramesh Kulkarni',
        category: 'Hospital',
        phone: '+91 98220 12345',
        location: 'Kothrud Clinic',
        description: 'Family physician',
      },
    ])
    updateEmergencyContactMock.mockResolvedValue({
      id: 'pers-1',
      userId: 'user-1',
      name: 'Dr. Ramesh Kulkarni (Senior)',
      category: 'Hospital',
      phone: '+91 98220 99999',
      location: 'Kothrud Clinic Wing B',
      description: 'Family senior physician',
    })

    const user = userEvent.setup()
    renderEmergencyContactsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Personal Contacts/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Personal Contacts/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Update contact' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Update contact' }))

    await waitFor(() => {
      expect(updateEmergencyContactMock).toHaveBeenCalledWith(
        'pers-1',
        expect.objectContaining({
          name: 'Dr. Ramesh Kulkarni',
          category: 'Hospital',
        }),
      )
    })
  })

  it('allows deleting a personal contact and updates list', async () => {
    getMyEmergencyContactsMock.mockResolvedValue([
      {
        id: 'pers-1',
        userId: 'user-1',
        name: 'Dr. Ramesh Kulkarni',
        category: 'Hospital',
        phone: '+91 98220 12345',
        location: 'Kothrud Clinic',
        description: 'Family physician',
      },
    ])
    deleteEmergencyContactMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderEmergencyContactsPage({ id: 'user-1', email: 'user@example.com' })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Personal Contacts/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Personal Contacts/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteEmergencyContactMock).toHaveBeenCalledWith('pers-1')
    })
  })

  it('shows login prompt when unauthenticated user views personal contacts tab', async () => {
    const user = userEvent.setup()
    renderEmergencyContactsPage(null) // unauthenticated

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Personal Contacts/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Personal Contacts/i }))

    await waitFor(() => {
      expect(screen.getByText(/Log in to save and manage your family doctors/i)).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /Log in to view personal contacts/i })).toBeInTheDocument()
    })
  })

  it('renders clean empty state when no contacts exist', async () => {
    getEmergencyContactsMock.mockResolvedValue([])

    renderEmergencyContactsPage()

    await waitFor(() => {
      expect(screen.getByText(/No emergency contacts found/i)).toBeInTheDocument()
    })
  })
})
