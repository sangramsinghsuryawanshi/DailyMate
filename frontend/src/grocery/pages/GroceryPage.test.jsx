import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import GroceryPage from './GroceryPage'
import { AuthContext } from '../../context/authContext'

const getGroceryItemsMock = vi.fn()
const getMyGroceryItemsMock = vi.fn()
const createGroceryItemMock = vi.fn()
const updateGroceryItemMock = vi.fn()
const deleteGroceryItemMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/groceryApi', () => ({
  getGroceryItems: (params) => getGroceryItemsMock(params),
  getMyGroceryItems: () => getMyGroceryItemsMock(),
  createGroceryItem: (payload) => createGroceryItemMock(payload),
  updateGroceryItem: (id, payload) => updateGroceryItemMock(id, payload),
  deleteGroceryItem: (id) => deleteGroceryItemMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderGroceryPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <GroceryPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('GroceryPage', () => {
  beforeEach(() => {
    getGroceryItemsMock.mockReset()
    getMyGroceryItemsMock.mockReset()
    createGroceryItemMock.mockReset()
    updateGroceryItemMock.mockReset()
    deleteGroceryItemMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getGroceryItemsMock.mockResolvedValue([])
    getMyGroceryItemsMock.mockResolvedValue([])
  })

  it('renders price comparison view with INR formatting and best-price badge', async () => {
    getGroceryItemsMock.mockResolvedValue([
      { id: 'g1', userId: 'u1', name: 'Amul Taaza Milk', category: 'Dairy & Eggs', store: 'D-Mart', price: 66.0, unit: '1 L', location: 'Kothrud', createdAt: '2026-01-01' },
      { id: 'g2', userId: 'u2', name: 'Amul Taaza Milk', category: 'Dairy & Eggs', store: 'Fresh Mart', price: 72.0, unit: '1 L', location: 'Aundh', createdAt: '2026-01-02' },
    ])

    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText('Amul Taaza Milk')).toBeInTheDocument()
    })

    // INR formatting
    expect(screen.getByText('₹66.00')).toBeInTheDocument()
    expect(screen.getByText('₹72.00')).toBeInTheDocument()

    // Best price badge (🏷️) on cheapest store
    expect(screen.getByTitle('Best price')).toBeInTheDocument()
  })

  it('displays category filter pills and search input on compare tab', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText('Grocery Price Comparison')).toBeInTheDocument()
    })

    expect(screen.getByLabelText('Search grocery products')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'All' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Dairy & Eggs' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Grains & Pulses' })).toBeInTheDocument()
  })

  it('submits a new grocery price with complete payload including unit', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    getMyGroceryItemsMock.mockResolvedValue([])
    createGroceryItemMock.mockResolvedValue({
      id: 'g3', userId: 'user-1', name: 'Toor Dal', category: 'Grains & Pulses', store: 'Local Kirana', price: 160, unit: '1 kg', location: 'Baner',
    })

    const user = userEvent.setup()
    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Submit price' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText(/Product name/i), 'Toor Dal')
    await user.type(screen.getByLabelText(/Store name/i), 'Local Kirana')
    await user.type(screen.getByLabelText(/Price/i), '160')
    await user.type(screen.getByLabelText(/Location/i), 'Baner')

    await user.click(screen.getByRole('button', { name: 'Submit price' }))

    await waitFor(() => {
      expect(createGroceryItemMock).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Toor Dal',
          category: 'Grains & Pulses',
          store: 'Local Kirana',
          price: 160,
          unit: '1 kg',
          location: 'Baner',
        }),
      )
    })
  })

  it('switches to My Submissions tab and shows owner Edit/Delete controls', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    getMyGroceryItemsMock.mockResolvedValue([
      { id: 'g4', userId: 'user-1', name: 'Basmati Rice', category: 'Grains & Pulses', store: 'Big Bazaar', price: 240, unit: '5 kg', location: 'Shivajinagar', createdAt: '2026-01-01' },
    ])

    const user = userEvent.setup()
    renderGroceryPage()

    // Wait for loading to finish
    await waitFor(() => {
      expect(screen.getByText('Grocery Price Comparison')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Submissions/i }))

    await waitFor(() => {
      expect(screen.getByText('Basmati Rice')).toBeInTheDocument()
    })

    expect(screen.getByText(/per 5 kg/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
  })

  it('hides edit/delete controls on price comparison view for non-owned items', async () => {
    getGroceryItemsMock.mockResolvedValue([
      { id: 'g5', userId: 'other-user', name: 'Colgate Toothpaste', category: 'Personal Care', store: 'Medical Shop', price: 95, unit: '1 unit', location: 'MG Road', createdAt: '2026-01-01' },
    ])

    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText('Colgate Toothpaste')).toBeInTheDocument()
    })

    // Price comparison view should NOT show Edit/Delete
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('allows editing a personal submission and calls updateGroceryItem', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    getMyGroceryItemsMock.mockResolvedValue([
      { id: 'g6', userId: 'user-1', name: 'Sunflower Oil', category: 'Grains & Pulses', store: 'D-Mart', price: 145, unit: '1 L', location: 'Aundh', createdAt: '2026-01-01' },
    ])
    updateGroceryItemMock.mockResolvedValue({
      id: 'g6', userId: 'user-1', name: 'Sunflower Oil', category: 'Grains & Pulses', store: 'D-Mart', price: 139, unit: '1 L', location: 'Aundh',
    })

    const user = userEvent.setup()
    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText('Grocery Price Comparison')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Submissions/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Update price' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Update price' }))

    await waitFor(() => {
      expect(updateGroceryItemMock).toHaveBeenCalledWith(
        'g6',
        expect.objectContaining({
          name: 'Sunflower Oil',
          store: 'D-Mart',
          unit: '1 L',
        }),
      )
    })
  })

  it('allows deleting a personal submission and calls deleteGroceryItem', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    getMyGroceryItemsMock.mockResolvedValue([
      { id: 'g7', userId: 'user-1', name: 'Atta', category: 'Grains & Pulses', store: 'Big Bazaar', price: 240, unit: '5 kg', location: 'Shivajinagar', createdAt: '2026-01-01' },
    ])
    deleteGroceryItemMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText('Grocery Price Comparison')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /My Submissions/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteGroceryItemMock).toHaveBeenCalledWith('g7')
    })
  })

  it('renders empty state when no grocery prices exist', async () => {
    getGroceryItemsMock.mockResolvedValue([])
    renderGroceryPage()

    await waitFor(() => {
      expect(screen.getByText(/No grocery prices yet/i)).toBeInTheDocument()
    })
  })
})
