import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AssistantPage from './AssistantPage'
import { AuthContext } from '../../context/authContext'

const getConversationsMock = vi.fn()
const sendAssistantChatMock = vi.fn()
const confirmActionMock = vi.fn()
const cancelActionMock = vi.fn()
const deleteConversationMock = vi.fn()
const getProvidersMock = vi.fn()
const getNotificationsMock = vi.fn()

vi.mock('../services/assistantApi', () => ({
  getAssistantConversations: () => getConversationsMock(),
  sendAssistantChat: (prompt, conversationId) => sendAssistantChatMock(prompt, conversationId),
  confirmAssistantAction: (actionId, idempotencyKey) => confirmActionMock(actionId, idempotencyKey),
  cancelAssistantAction: (actionId) => cancelActionMock(actionId),
  deleteAssistantConversation: (id) => deleteConversationMock(id),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => getProvidersMock(),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => getNotificationsMock(),
}))

function renderAssistantPage(user = { id: 'user-1', email: 'user@example.com' }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ accessToken: user ? 'token' : null, user, signIn: vi.fn(), signOut: vi.fn() }}>
        <MemoryRouter>
          <AssistantPage />
        </MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  )
}

describe('AssistantPage — Tool Execution, Continuity, and Action Cards', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    getProvidersMock.mockResolvedValue([])
    getNotificationsMock.mockResolvedValue({ content: [], totalElements: 0 })
    getConversationsMock.mockResolvedValue([])
  })

  it('renders welcome empty state with suggested prompt pills when no conversations exist', async () => {
    getConversationsMock.mockResolvedValue([])

    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByText('DailyMate Assistant')).toBeInTheDocument()
      expect(screen.getByText(/Welcome to DailyMate Assistant/i)).toBeInTheDocument()
      expect(screen.getByText(/What medicines do I have scheduled today\?/i)).toBeInTheDocument()
    })
  })

  it('renders conversations history, user message bubble, and assistant response bubble', async () => {
    getConversationsMock.mockResolvedValue([
      {
        id: 'convo-1',
        title: 'Check Schedule',
        prompt: 'What medicines are due today?',
        response: 'You have Vitamin D3 scheduled at 08:30.',
        createdAt: '2026-08-21T10:00:00Z',
      },
    ])

    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getAllByText('Check Schedule').length).toBeGreaterThan(0)
      expect(screen.getByText('What medicines are due today?')).toBeInTheDocument()
      expect(screen.getByText('You have Vitamin D3 scheduled at 08:30.')).toBeInTheDocument()
    })
  })

  it('renders action proposal card and executes on confirm click with updated header title', async () => {
    getConversationsMock.mockResolvedValue([])
    sendAssistantChatMock.mockResolvedValue({
      id: 'convo-10',
      title: 'add expense for lunch khichadi 50',
      prompt: 'add expense for my afternoon lunch name khichadi and amount is 50',
      response: 'I have prepared an action to record this expense.',
      proposedAction: {
        actionId: 'action-exp-1',
        actionType: 'RECORD_EXPENSE',
        summary: 'Record ₹50.00 expense for Khichadi (Food & Dining)',
        parametersJson: '{"amount":50,"description":"Khichadi","category":"Food & Dining"}',
        status: 'PENDING',
      },
      createdAt: '2026-08-22T10:00:00Z',
    })
    confirmActionMock.mockResolvedValue({
      actionId: 'action-exp-1',
      actionType: 'RECORD_EXPENSE',
      status: 'EXECUTED',
      resultMessage: 'Successfully recorded expense of ₹50.00 for Khichadi.',
    })

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Ask DailyMate/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/Ask DailyMate/i)
    await user.type(input, 'add expense for my afternoon lunch name khichadi and amount is 50')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    // Verify Action Card renders
    await waitFor(() => {
      expect(screen.getByText('⚡ Action Proposal')).toBeInTheDocument()
      expect(screen.getByText(/Record ₹50.00 expense for Khichadi/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Confirm Action' })).toBeInTheDocument()
    })

    // Click Confirm Action
    await user.click(screen.getByRole('button', { name: 'Confirm Action' }))

    await waitFor(() => {
      expect(confirmActionMock).toHaveBeenCalledWith('action-exp-1', expect.stringContaining('idemp-action-exp-1'))
      expect(screen.getByText('⚡ Action Executed')).toBeInTheDocument()
      expect(screen.getByText(/Successfully recorded expense of ₹50.00 for Khichadi/i)).toBeInTheDocument()
    })
  })

  it('allows cancelling an action proposal card with updated header title', async () => {
    getConversationsMock.mockResolvedValue([])
    sendAssistantChatMock.mockResolvedValue({
      id: 'convo-11',
      title: 'Add expense 500 for Groceries',
      prompt: 'Add expense 500 for Groceries',
      response: 'I have prepared an action to record this expense.',
      proposedAction: {
        actionId: 'action-exp-2',
        actionType: 'RECORD_EXPENSE',
        summary: 'Record ₹500.00 expense for Groceries',
        status: 'PENDING',
      },
      createdAt: '2026-08-22T10:00:00Z',
    })
    cancelActionMock.mockResolvedValue({
      actionId: 'action-exp-2',
      status: 'CANCELLED',
    })

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Ask DailyMate/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/Ask DailyMate/i)
    await user.type(input, 'Add expense 500 for Groceries')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    await waitFor(() => {
      expect(cancelActionMock).toHaveBeenCalledWith('action-exp-2')
      expect(screen.getByText('⚡ Action Cancelled')).toBeInTheDocument()
      expect(screen.getByText('CANCELLED')).toBeInTheDocument()
    })
  })

  it('maintains conversation continuity on subsequent messages', async () => {
    getConversationsMock.mockResolvedValue([
      {
        id: 'convo-100',
        title: 'Initial Chat',
        prompt: 'Hello',
        response: 'Hi! How can I help?',
      },
    ])
    sendAssistantChatMock.mockResolvedValue({
      id: 'convo-100',
      title: 'Initial Chat',
      prompt: 'What are my reminders?',
      response: 'You have no reminders today.',
    })

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Ask DailyMate/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/Ask DailyMate/i)
    await user.type(input, 'What are my reminders?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() => {
      // Must pass existing conversationId 'convo-100'
      expect(sendAssistantChatMock).toHaveBeenCalledWith('What are my reminders?', 'convo-100')
    })
  })

  it('deletes conversation from history', async () => {
    getConversationsMock.mockResolvedValue([
      {
        id: 'convo-1',
        title: 'Check Schedule',
        prompt: 'What medicines are due today?',
        response: 'You have Vitamin D3 scheduled at 08:30.',
      },
    ])
    deleteConversationMock.mockResolvedValue({})

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByTitle('Delete conversation')).toBeInTheDocument()
    })

    await user.click(screen.getByTitle('Delete conversation'))

    await waitFor(() => {
      expect(deleteConversationMock).toHaveBeenCalledWith('convo-1')
    })
  })
})
