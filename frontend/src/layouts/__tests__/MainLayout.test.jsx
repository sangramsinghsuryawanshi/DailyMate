import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MainLayout from '../MainLayout'
import { AuthContext } from '../../context/authContext'

const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderMainLayout(user = { id: 'user-1', firstName: 'Sangram', lastName: 'Suryawanshi', role: 'USER', email: 'sangram@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <MainLayout>
            <div data-testid="test-content">Dashboard Content</div>
          </MainLayout>
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>
  )
}

describe('MainLayout — Responsive Application Shell & Global Navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
  })

  it('renders Sidebar with brand, navigation links, and Quick Action button', async () => {
    renderMainLayout()

    expect(screen.getAllByText('DailyMate').length).toBeGreaterThan(0)
    expect(screen.getByTestId('test-content')).toBeInTheDocument()

    // Nav items
    expect(screen.getAllByRole('link', { name: /Dashboard|Home/i }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /AI Assistant|Assistant/i }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /Expenses/i }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /Medicines/i }).length).toBeGreaterThan(0)

    // Quick Action button in sidebar
    expect(screen.getByRole('button', { name: /Universal Quick Actions/i })).toBeInTheDocument()
  })

  it('opens Universal Quick Action modal on Quick Action click', async () => {
    renderMainLayout()

    const quickActionBtn = screen.getByRole('button', { name: /Universal Quick Actions/i })
    await userEvent.click(quickActionBtn)

    await waitFor(() => {
      expect(screen.getByText('Universal Quick Actions')).toBeInTheDocument()
      expect(screen.getByText('Add Expense')).toBeInTheDocument()
      expect(screen.getByText('Add Medicine Reminder')).toBeInTheDocument()
      expect(screen.getByText('Post Blood Request')).toBeInTheDocument()
      expect(screen.getByText('Report Complaint')).toBeInTheDocument()
      expect(screen.getByText('Ask AI Assistant')).toBeInTheDocument()
    })
  })

  it('opens Global Search dialog on search trigger', async () => {
    renderMainLayout()

    const searchInput = screen.getByPlaceholderText(/Search services, tasks, tools.../i)
    await userEvent.click(searchInput)

    await waitFor(() => {
      expect(screen.getByLabelText('Global search input')).toBeInTheDocument()
      expect(screen.getByText('Suggested Destinations')).toBeInTheDocument()
    })
  })

  it('toggles sidebar collapse state and persists to localStorage', async () => {
    renderMainLayout()

    const collapseBtn = screen.getByTitle('Collapse sidebar')
    await userEvent.click(collapseBtn)

    expect(localStorage.getItem('dailymate.sidebar_collapsed')).toBe('true')
  })
})
