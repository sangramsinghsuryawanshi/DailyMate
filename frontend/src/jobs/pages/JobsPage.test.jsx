import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import JobsPage from './JobsPage'
import { AuthContext } from '../../context/authContext'

const getJobsMock = vi.fn()
const getMyJobsMock = vi.fn()
const createJobMock = vi.fn()
const updateJobMock = vi.fn()
const deleteJobMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/jobsApi', () => ({
  getJobs: (params) => getJobsMock(params),
  getMyJobs: () => getMyJobsMock(),
  createJob: (payload) => createJobMock(payload),
  updateJob: (id, payload) => updateJobMock(id, payload),
  deleteJob: (id) => deleteJobMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderJobsPage(user = { id: 'u-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <JobsPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('JobsPage — Community Jobs Board', () => {
  beforeEach(() => {
    getJobsMock.mockReset()
    getMyJobsMock.mockReset()
    createJobMock.mockReset()
    updateJobMock.mockReset()
    deleteJobMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getJobsMock.mockResolvedValue([])
    getMyJobsMock.mockResolvedValue([])
  })

  it('renders public job listings with INR formatted salary and contact buttons', async () => {
    getJobsMock.mockResolvedValue([
      {
        id: 'job-1',
        userId: 'other-user',
        title: 'Community Center Caretaker',
        category: 'Services',
        location: 'Kothrud, Pune',
        type: 'Full-time',
        salary: 28000.0,
        companyName: 'Kothrud Resident Welfare',
        contactPhone: '+91-9876543210',
        contactEmail: 'contact@krw.org',
        status: 'OPEN',
        description: 'Manage facility maintenance, key handovers, and community hall bookings.',
        createdAt: '2026-08-20T00:00:00Z',
      },
    ])

    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: 'Community Center Caretaker' })).toBeInTheDocument()
    })

    // Assert INR currency formatting
    expect(screen.getByText('₹28,000.00')).toBeInTheDocument()
    expect(screen.getAllByText('Full-time').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/Kothrud Resident Welfare/i)).toBeInTheDocument()
    expect(screen.getByText(/Kothrud, Pune/i)).toBeInTheDocument()

    // Assert one-tap contact buttons
    expect(screen.getByRole('link', { name: /Call \+91-9876543210/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Email/i })).toBeInTheDocument()

    // Public feed must not show owner controls for other user's post
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('displays category filter pills, type filters, and search bar', async () => {
    getJobsMock.mockResolvedValue([])
    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1, name: 'Community Jobs Board' })).toBeInTheDocument()
    })

    expect(screen.getByLabelText('Search jobs')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Services' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Technical' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retail' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Full-time' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Part-time' })).toBeInTheDocument()
  })

  it('submits a new job posting with full payload including salary and company info', async () => {
    getJobsMock.mockResolvedValue([])
    getMyJobsMock.mockResolvedValue([])
    createJobMock.mockResolvedValue({
      id: 'job-2',
      userId: 'u-1',
      title: 'After-School Math Tutor',
      category: 'Education & Tutoring',
      location: 'Baner, Pune',
      type: 'Part-time',
      salary: 15000,
      companyName: 'BrightMinds Academy',
      contactPhone: '+91-9988776655',
      contactEmail: 'hr@brightminds.in',
      status: 'OPEN',
      description: 'Teach middle school mathematics 3 days a week.',
    })

    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Publish Job' })).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/Job title/i), { target: { value: 'After-School Math Tutor' } })
    fireEvent.change(screen.getByLabelText(/Location/i), { target: { value: 'Baner, Pune' } })
    fireEvent.change(screen.getByLabelText(/Salary/i), { target: { value: '15000' } })
    fireEvent.change(screen.getByLabelText(/Company/i), { target: { value: 'BrightMinds Academy' } })
    fireEvent.change(screen.getByLabelText(/Contact phone/i), { target: { value: '+91-9988776655' } })
    fireEvent.change(screen.getByLabelText(/Contact email/i), { target: { value: 'hr@brightminds.in' } })
    fireEvent.change(screen.getByLabelText(/Job description/i), { target: { value: 'Teach middle school mathematics 3 days a week.' } })

    const user = userEvent.setup({ delay: null })
    await user.click(screen.getByRole('button', { name: 'Publish Job' }))

    await waitFor(() => {
      expect(createJobMock).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'After-School Math Tutor',
          location: 'Baner, Pune',
          salary: 15000,
          companyName: 'BrightMinds Academy',
          contactPhone: '+91-9988776655',
          contactEmail: 'hr@brightminds.in',
          description: 'Teach middle school mathematics 3 days a week.',
        }),
      )
    })
  })

  it('switches to My Postings tab and shows owner Edit/Delete controls', async () => {
    getJobsMock.mockResolvedValue([])
    getMyJobsMock.mockResolvedValue([
      {
        id: 'job-3',
        userId: 'u-1',
        title: 'Delivery Driver',
        category: 'Services',
        location: 'Aundh, Pune',
        type: 'Gig / Task',
        salary: 12000.0,
        companyName: 'QuickDrop',
        status: 'OPEN',
        description: 'Deliver evening grocery orders in Aundh.',
        createdAt: '2026-08-21T00:00:00Z',
      },
    ])

    const user = userEvent.setup({ delay: null })
    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Postings/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Postings/i }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: 'Delivery Driver' })).toBeInTheDocument()
    })

    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
  })

  it('allows owner to edit personal posting and transition status to CLOSED', async () => {
    getJobsMock.mockResolvedValue([])
    getMyJobsMock.mockResolvedValue([
      {
        id: 'job-4',
        userId: 'u-1',
        title: 'Apartment Painter',
        category: 'Services',
        location: 'Wakad, Pune',
        type: 'Contract',
        salary: 20000.0,
        status: 'OPEN',
        description: 'Painting 3BHK flat.',
        createdAt: '2026-08-21T00:00:00Z',
      },
    ])
    updateJobMock.mockResolvedValue({
      id: 'job-4',
      userId: 'u-1',
      title: 'Apartment Painter',
      category: 'Services',
      location: 'Wakad, Pune',
      type: 'Contract',
      salary: 20000.0,
      status: 'CLOSED',
      description: 'Painting 3BHK flat.',
    })

    const user = userEvent.setup({ delay: null })
    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Postings/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Postings/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Update Posting' })).toBeInTheDocument()
    })

    // Change status to CLOSED
    await user.selectOptions(screen.getByLabelText(/Status/i), 'CLOSED')
    await user.click(screen.getByRole('button', { name: 'Update Posting' }))

    await waitFor(() => {
      expect(updateJobMock).toHaveBeenCalledWith(
        'job-4',
        expect.objectContaining({
          status: 'CLOSED',
        }),
      )
    })
  })

  it('allows owner to delete personal posting and calls deleteJob', async () => {
    getJobsMock.mockResolvedValue([])
    getMyJobsMock.mockResolvedValue([
      {
        id: 'job-5',
        userId: 'u-1',
        title: 'Temporary Office Assistant',
        category: 'Office & Admin',
        location: 'Shivajinagar',
        type: 'Part-time',
        salary: 10000.0,
        status: 'OPEN',
        description: 'Filing and document scanning.',
      },
    ])
    deleteJobMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /My Postings/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Postings/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteJobMock).toHaveBeenCalledWith('job-5')
    })
  })

  it('renders graceful empty state when no jobs exist', async () => {
    getJobsMock.mockResolvedValue([])
    renderJobsPage()

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 3, name: 'No jobs found' })).toBeInTheDocument()
    })
  })
})
