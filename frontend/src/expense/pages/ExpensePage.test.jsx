import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ExpensePage from './ExpensePage'
import { AuthContext } from '../../context/authContext'

const getExpensesMock = vi.fn()
const createExpenseMock = vi.fn()
const updateExpenseMock = vi.fn()
const deleteExpenseMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/expenseApi', () => ({
  getExpenses: () => getExpensesMock(),
  createExpense: (data) => createExpenseMock(data),
  updateExpense: (id, data) => updateExpenseMock(id, data),
  deleteExpense: (id) => deleteExpenseMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderExpensePage(user = { id: 'u-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <ExpensePage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('ExpensePage', () => {
  beforeEach(() => {
    getExpensesMock.mockReset()
    createExpenseMock.mockReset()
    updateExpenseMock.mockReset()
    deleteExpenseMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getExpensesMock.mockResolvedValue([])
  })

  it('renders existing persisted expenses and calculates dynamic summary', async () => {
    getExpensesMock.mockResolvedValue([
      {
        id: 'exp-1',
        category: 'Groceries',
        description: 'Supermarket vegetables',
        amount: 850.5,
        spentOn: '2026-08-20',
        notes: 'Fresh stock',
      },
      {
        id: 'exp-2',
        category: 'Groceries',
        description: 'Milk and dairy',
        amount: 150.0,
        spentOn: '2026-08-19',
        notes: null,
      },
      {
        id: 'exp-3',
        category: 'Utilities',
        description: 'Water bill',
        amount: 500.0,
        spentOn: '2026-08-15',
        notes: null,
      },
    ])

    renderExpensePage()

    await waitFor(() => {
      expect(screen.getByText('Supermarket vegetables')).toBeInTheDocument()
      expect(screen.getByText('Water bill')).toBeInTheDocument()
      // Dynamic summary calculations
      expect(screen.getByText('Transactions:')).toBeInTheDocument()
      expect(screen.getByText('3')).toBeInTheDocument()
      expect(screen.getByText('Groceries')).toBeInTheDocument() // Top category
    })
  })

  it('creates new expense sending complete payload', async () => {
    getExpensesMock.mockResolvedValue([])
    createExpenseMock.mockResolvedValue({
      id: 'exp-4',
      category: 'Dining',
      description: 'Dinner with family',
      amount: 1200.0,
      spentOn: '2026-08-21',
      notes: 'Weekend',
    })

    const user = userEvent.setup()
    renderExpensePage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Add expense' })).toBeInTheDocument()
    })

    const categoryInput = screen.getByLabelText('Category')
    const amountInput = screen.getByLabelText(/Amount/i)
    const descriptionInput = screen.getByLabelText('Description')

    await user.clear(categoryInput)
    await user.type(categoryInput, 'Dining')
    await user.type(amountInput, '1200.00')
    await user.type(descriptionInput, 'Dinner with family')

    await user.click(screen.getByRole('button', { name: 'Add expense' }))

    await waitFor(() => {
      expect(createExpenseMock).toHaveBeenCalledWith(
        expect.objectContaining({
          category: 'Dining',
          description: 'Dinner with family',
          amount: 1200,
        }),
      )
    })
  })

  it('allows editing an existing expense', async () => {
    getExpensesMock.mockResolvedValue([
      {
        id: 'exp-1',
        category: 'Groceries',
        description: 'Supermarket vegetables',
        amount: 850.5,
        spentOn: '2026-08-20',
        notes: 'Fresh stock',
      },
    ])
    updateExpenseMock.mockResolvedValue({
      id: 'exp-1',
      category: 'Groceries',
      description: 'Supermarket vegetables & fruits',
      amount: 950.0,
      spentOn: '2026-08-20',
      notes: 'Added fruits',
    })

    const user = userEvent.setup()
    renderExpensePage()

    await waitFor(() => {
      expect(screen.getByText('Supermarket vegetables')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit' }))

    expect(screen.getByRole('heading', { level: 2, name: 'Edit expense' })).toBeInTheDocument()
    const descriptionInput = screen.getByLabelText('Description')
    await user.clear(descriptionInput)
    await user.type(descriptionInput, 'Supermarket vegetables & fruits')

    await user.click(screen.getByRole('button', { name: 'Update expense' }))

    await waitFor(() => {
      expect(updateExpenseMock).toHaveBeenCalledWith(
        'exp-1',
        expect.objectContaining({
          description: 'Supermarket vegetables & fruits',
        }),
      )
    })
  })

  it('deletes an expense', async () => {
    getExpensesMock.mockResolvedValue([
      {
        id: 'exp-1',
        category: 'Groceries',
        description: 'Supermarket vegetables',
        amount: 850.5,
        spentOn: '2026-08-20',
      },
    ])
    deleteExpenseMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderExpensePage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteExpenseMock).toHaveBeenCalledWith('exp-1')
    })
  })

  it('renders clean empty state when no expenses exist', async () => {
    getExpensesMock.mockResolvedValue([])

    renderExpensePage()

    await waitFor(() => {
      expect(screen.getByText('No expenses recorded yet. Add an expense to get started.')).toBeInTheDocument()
      expect(screen.getByText('None')).toBeInTheDocument()
    })
  })
})
