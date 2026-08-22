import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import DashboardPage from './DashboardPage'
import { AuthContext } from '../../context/authContext'

const getProvidersMock = vi.fn()
const getRemindersMock = vi.fn()
const getLocalEventsMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../medicine/services/medicineApi', () => ({
  getReminders: () => getRemindersMock(),
}))

vi.mock('../../events/services/localEventsApi', () => ({
  getLocalEvents: () => getLocalEventsMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderDashboardPage(user = { id: 'u-1', firstName: 'Sangram', email: 'sangram@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('DashboardPage — Data Integrity & Aggregation', () => {
  beforeEach(() => {
    getProvidersMock.mockReset()
    getRemindersMock.mockReset()
    getLocalEventsMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getRemindersMock.mockResolvedValue([])
    getLocalEventsMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
  })

  it('renders real marketplace providers with INR currency and NO fabricated ratings or distances', async () => {
    getProvidersMock.mockResolvedValue([
      {
        id: 'p-1',
        name: 'Apex Electrical Services',
        category: 'Electrician',
        description: 'Quality residential electrical repairs and installations',
        serviceArea: 'Kothrud, Pune',
        hourlyRate: 85.0,
      },
      {
        id: 'p-2',
        name: 'ClearFlow Plumbing',
        category: 'Plumber',
        serviceArea: 'Baner, Pune',
        hourlyRate: 60.0,
      },
    ])

    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: 'Apex Electrical Services' })).toBeInTheDocument()
      expect(screen.getByRole('heading', { level: 3, name: 'ClearFlow Plumbing' })).toBeInTheDocument()
    })

    // Assert INR currency formatting
    expect(screen.getByText('₹85.00/hr')).toBeInTheDocument()
    expect(screen.getByText('₹60.00/hr')).toBeInTheDocument()
    expect(screen.getByText('Area: Kothrud, Pune')).toBeInTheDocument()

    // Assert NO fabricated data (no fake ratings, distances, or booking statuses)
    expect(screen.queryByText(/★/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/km/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Available today/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Booked for/i)).not.toBeInTheDocument()
  })

  it('dynamically aggregates active medicine reminders and excludes inactive ones', async () => {
    getRemindersMock.mockResolvedValue([
      {
        id: 'rem-1',
        name: 'Metformin',
        dosage: '500mg',
        frequency: 'Twice daily',
        remindAt: '08:00',
        active: true,
      },
      {
        id: 'rem-2',
        name: 'Old Antibiotic',
        dosage: '250mg',
        frequency: 'Completed',
        remindAt: '12:00',
        active: false, // inactive - should be excluded
      },
    ])

    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByText(/💊 Metformin/i)).toBeInTheDocument()
      expect(screen.getByText(/500mg · Twice daily/i)).toBeInTheDocument()
      expect(screen.getByText('08:00')).toBeInTheDocument()
    })

    // Inactive reminder must not be displayed in Today schedule
    expect(screen.queryByText(/Old Antibiotic/i)).not.toBeInTheDocument()
  })

  it('dynamically aggregates upcoming published events and excludes past or cancelled ones', async () => {
    const tomorrow = new Date(Date.now() + 86400000).toISOString()
    const yesterday = new Date(Date.now() - 86400000).toISOString()

    getLocalEventsMock.mockResolvedValue([
      {
        id: 'ev-1',
        title: 'Community Tree Plantation',
        category: 'Volunteer',
        location: 'Pashan Lake',
        eventDate: tomorrow,
        status: 'PUBLISHED',
      },
      {
        id: 'ev-2',
        title: 'Past Workshop',
        category: 'Workshop',
        location: 'City Library',
        eventDate: yesterday,
        status: 'PUBLISHED',
      },
      {
        id: 'ev-3',
        title: 'Cancelled Music Festival',
        category: 'Music',
        location: 'Central Park',
        eventDate: tomorrow,
        status: 'CANCELLED',
      },
    ])

    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByText(/📅 Community Tree Plantation/i)).toBeInTheDocument()
      expect(screen.getByText(/📍 Pashan Lake/i)).toBeInTheDocument()
    })

    // Past and cancelled events must not appear on Today schedule
    expect(screen.queryByText(/Past Workshop/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Cancelled Music Festival/i)).not.toBeInTheDocument()
  })

  it('renders unread notifications in Today schedule aggregation', async () => {
    getNotificationsMock.mockResolvedValue({
      content: [
        {
          id: 'n-1',
          title: 'Scheduled Water Outage Notice',
          message: 'Maintenance scheduled tomorrow from 10 AM to 2 PM.',
          read: false,
        },
      ],
      totalElements: 1,
    })

    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByText(/🔔 Scheduled Water Outage Notice/i)).toBeInTheDocument()
      expect(screen.getByText(/Maintenance scheduled tomorrow/i)).toBeInTheDocument()
    })
  })

  it('renders graceful empty state when user has no scheduled items or providers', async () => {
    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByText(/No service providers listed yet/i)).toBeInTheDocument()
      expect(screen.getByText(/No pending reminders or events scheduled for today/i)).toBeInTheDocument()
    })

    expect(screen.getByRole('link', { name: '+ Add reminder' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse events' })).toBeInTheDocument()
  })

  it('isolates provider API failures with a retry button without crashing Today schedule', async () => {
    getProvidersMock.mockRejectedValue(new Error('Network error'))
    getRemindersMock.mockResolvedValue([
      { id: 'rem-1', name: 'Vitamin D3', dosage: '60000 IU', frequency: 'Weekly', remindAt: '09:00', active: true },
    ])

    renderDashboardPage()

    await waitFor(() => {
      expect(screen.getByText('Unable to load recommended services.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
    })

    // Reminders still successfully render!
    expect(screen.getByText(/💊 Vitamin D3/i)).toBeInTheDocument()
  })
})
