import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MedicinePage from './MedicinePage'
import { AuthContext } from '../../context/authContext'

const getRemindersMock = vi.fn()
const createReminderMock = vi.fn()
const updateReminderMock = vi.fn()
const deleteReminderMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/medicineApi', () => ({
  getReminders: () => getRemindersMock(),
  createReminder: (data) => createReminderMock(data),
  updateReminder: (id, data) => updateReminderMock(id, data),
  deleteReminder: (id) => deleteReminderMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderMedicinePage(user = { id: 'u-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <MedicinePage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('MedicinePage', () => {
  beforeEach(() => {
    getRemindersMock.mockReset()
    createReminderMock.mockReset()
    updateReminderMock.mockReset()
    deleteReminderMock.mockReset()
    getProvidersMock.mockReset()
    getNotificationsMock.mockReset()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getRemindersMock.mockResolvedValue([])
  })

  it('renders existing persisted medicine reminders', async () => {
    getRemindersMock.mockResolvedValue([
      {
        id: 'med-1',
        name: 'Vitamin D3',
        dosage: '2000 IU',
        frequency: 'Daily',
        remindAt: '08:30:00',
        notes: 'With breakfast',
        active: true,
      },
    ])

    renderMedicinePage()

    await waitFor(() => {
      expect(screen.getByText('Vitamin D3')).toBeInTheDocument()
      expect(screen.getByText(/2000 IU · Daily · ⏰ 08:30/)).toBeInTheDocument()
      expect(screen.getByText('With breakfast')).toBeInTheDocument()
    })
  })

  it('creates new reminder sending complete payload', async () => {
    getRemindersMock.mockResolvedValue([])
    createReminderMock.mockResolvedValue({
      id: 'med-2',
      name: 'Omega 3',
      dosage: '1000mg',
      frequency: 'Daily',
      remindAt: '09:00',
      notes: 'After meal',
      active: true,
    })

    const user = userEvent.setup()
    renderMedicinePage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Add reminder' })).toBeInTheDocument()
    })

    await user.type(screen.getByLabelText('Medicine name'), 'Omega 3')
    await user.type(screen.getByLabelText('Dosage'), '1000mg')
    await user.click(screen.getByRole('button', { name: 'Add reminder' }))

    await waitFor(() => {
      expect(createReminderMock).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Omega 3',
          dosage: '1000mg',
          frequency: 'Daily',
          remindAt: '08:00',
          active: true,
        }),
      )
    })
  })

  it('opens edit modal and saves complete updated payload', async () => {
    getRemindersMock.mockResolvedValue([
      {
        id: 'med-1',
        name: 'Vitamin D3',
        dosage: '2000 IU',
        frequency: 'Daily',
        remindAt: '08:30:00',
        notes: 'With breakfast',
        active: true,
      },
    ])
    updateReminderMock.mockResolvedValue({
      id: 'med-1',
      name: 'Vitamin D3 Extra',
      dosage: '4000 IU',
      frequency: 'Daily',
      remindAt: '08:30:00',
      notes: 'With breakfast',
      active: true,
    })

    const user = userEvent.setup()
    renderMedicinePage()

    await waitFor(() => {
      expect(screen.getByText('Vitamin D3')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Edit' }))
    expect(screen.getByText('Edit Medicine Reminder')).toBeInTheDocument()

    const nameInputs = screen.getAllByLabelText('Medicine name')
    const editNameInput = nameInputs[nameInputs.length - 1] // Edit modal input
    await user.clear(editNameInput)
    await user.type(editNameInput, 'Vitamin D3 Extra')

    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(updateReminderMock).toHaveBeenCalledWith(
        'med-1',
        expect.objectContaining({
          name: 'Vitamin D3 Extra',
          dosage: '2000 IU',
          frequency: 'Daily',
          remindAt: '08:30',
          active: true,
        }),
      )
    })
  })

  it('toggles pause and resume with full payload', async () => {
    getRemindersMock.mockResolvedValue([
      {
        id: 'med-1',
        name: 'Vitamin D3',
        dosage: '2000 IU',
        frequency: 'Daily',
        remindAt: '08:30:00',
        notes: 'With breakfast',
        active: true,
      },
    ])
    updateReminderMock.mockResolvedValue({
      id: 'med-1',
      name: 'Vitamin D3',
      dosage: '2000 IU',
      frequency: 'Daily',
      remindAt: '08:30:00',
      notes: 'With breakfast',
      active: false,
    })

    const user = userEvent.setup()
    renderMedicinePage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Pause' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Pause' }))

    await waitFor(() => {
      expect(updateReminderMock).toHaveBeenCalledWith(
        'med-1',
        expect.objectContaining({
          name: 'Vitamin D3',
          dosage: '2000 IU',
          frequency: 'Daily',
          remindAt: '08:30',
          active: false,
        }),
      )
    })
  })

  it('deletes reminder', async () => {
    getRemindersMock.mockResolvedValue([
      {
        id: 'med-1',
        name: 'Vitamin D3',
        dosage: '2000 IU',
        frequency: 'Daily',
        remindAt: '08:30:00',
        active: true,
      },
    ])
    deleteReminderMock.mockResolvedValue({})

    const user = userEvent.setup()
    renderMedicinePage()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => {
      expect(deleteReminderMock).toHaveBeenCalledWith('med-1')
    })
  })
})
