import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AssistantPage from './AssistantPage'
import { AuthContext } from '../../context/authContext'

const getConversationsMock = vi.fn()
const getConversationMessagesMock = vi.fn()
const sendAssistantChatMock = vi.fn()
const deleteConversationMock = vi.fn()

vi.mock('../services/assistantApi', () => ({
  getAssistantConversations: () => getConversationsMock(),
  getAssistantConversationMessages: (id) => getConversationMessagesMock(id),
  sendAssistantChat: (prompt, conversationId) => sendAssistantChatMock(prompt, conversationId),
  deleteAssistantConversation: (id) => deleteConversationMock(id),
  confirmAssistantAction: vi.fn(),
  cancelAssistantAction: vi.fn(),
}))

vi.mock('../../marketplace/services/marketplaceApi', () => ({
  getProviders: () => Promise.resolve([]),
}))

vi.mock('../../notification/services/notificationsApi', () => ({
  getNotifications: () => Promise.resolve({ content: [], totalElements: 0 }),
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

describe('AssistantPage — Persistent Multi-Turn Chat History', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getConversationsMock.mockResolvedValue([])
    getConversationMessagesMock.mockResolvedValue([])
  })

  it('loads and renders complete chronological message thread for selected conversation', async () => {
    const convo = {
      id: 'convo-1',
      title: 'Who can I call in an emergency?',
      createdAt: '2026-08-22T10:00:00Z',
    }

    const messages = [
      { id: 'm1', role: 'USER', content: 'Who can I call in an emergency?', createdAt: '2026-08-22T10:00:00Z' },
      { id: 'm2', role: 'ASSISTANT', content: 'National Emergency: 112, Police: 100', createdAt: '2026-08-22T10:00:01Z' },
      { id: 'm3', role: 'USER', content: 'What about my emergency contacts?', createdAt: '2026-08-22T10:01:00Z' },
      { id: 'm4', role: 'ASSISTANT', content: 'You have 2 ICE contacts registered.', createdAt: '2026-08-22T10:01:02Z' },
    ]

    getConversationsMock.mockResolvedValue([convo])
    getConversationMessagesMock.mockResolvedValue(messages)

    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getAllByText('Who can I call in an emergency?').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText(/National Emergency: 112/i)).toBeInTheDocument()
      expect(screen.getByText('What about my emergency contacts?')).toBeInTheDocument()
      expect(screen.getByText(/You have 2 ICE contacts registered/i)).toBeInTheDocument()
    }, { timeout: 10000 })
  })

  it('sending additional message maintains conversation ID and appends turn', async () => {
    const convo = {
      id: 'convo-1',
      title: 'Who can I call in an emergency?',
      createdAt: '2026-08-22T10:00:00Z',
    }

    getConversationsMock.mockResolvedValue([convo])
    getConversationMessagesMock.mockResolvedValue([
      { id: 'm1', role: 'USER', content: 'Who can I call in an emergency?' },
      { id: 'm2', role: 'ASSISTANT', content: 'Call 112' },
    ])

    sendAssistantChatMock.mockResolvedValue({
      id: 'convo-1',
      title: 'Who can I call in an emergency?',
      prompt: 'Add ICE contact',
      response: 'I have prepared an action to add ICE contact',
      createdAt: '2026-08-22T10:02:00Z',
    })

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByText('Call 112')).toBeInTheDocument()
    })

    const input = screen.getByRole('textbox', { name: /ask dailymate a question/i })
    await user.type(input, 'Add ICE contact')
    await user.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      // Must send chat with the existing conversationId
      expect(sendAssistantChatMock).toHaveBeenCalledWith('Add ICE contact', 'convo-1')
    })
  })

  it('clicking + New chat clears thread and starts fresh conversation', async () => {
    const convo = {
      id: 'convo-1',
      title: 'Existing Chat',
      createdAt: '2026-08-22T10:00:00Z',
    }

    getConversationsMock.mockResolvedValue([convo])
    getConversationMessagesMock.mockResolvedValue([
      { id: 'm1', role: 'USER', content: 'Previous Question' },
      { id: 'm2', role: 'ASSISTANT', content: 'Previous Answer' },
    ])

    const user = userEvent.setup({ delay: null })
    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByText('Previous Question')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /\+ New chat/i }))

    await waitFor(() => {
      expect(screen.getByText(/Welcome to DailyMate Assistant/i)).toBeInTheDocument()
      expect(screen.getByText('New conversation')).toBeInTheDocument()
    })
  })

  it('executed proposal renders completed state and does not render Confirm Action button after reload/navigation', async () => {
    const convo = {
      id: 'convo-bulk-1',
      title: 'Bulk Import',
      createdAt: '2026-08-22T10:00:00Z',
    }

    const messages = [
      { id: 'm1', role: 'USER', content: 'bulk import expenses lunch 50, dinner 50, watch 1000' },
      {
        id: 'm2',
        role: 'ASSISTANT',
        content: 'I found 3 expenses totaling ₹1,100.00.',
        proposedAction: {
          actionId: 'act-123',
          actionType: 'BULK_RECORD_EXPENSES',
          summary: 'Bulk import 3 expenses totaling ₹1,100.00',
          status: 'EXECUTED',
          requiresConfirmation: true,
          expiresAt: '2026-08-22T10:30:00Z',
        },
      },
    ]

    getConversationsMock.mockResolvedValue([convo])
    getConversationMessagesMock.mockResolvedValue(messages)

    renderAssistantPage()

    await waitFor(() => {
      expect(screen.getByText(/Action Executed/i)).toBeInTheDocument()
      expect(screen.getByText('EXECUTED')).toBeInTheDocument()
      // The Confirm Action button MUST NOT be rendered
      expect(screen.queryByRole('button', { name: /Confirm Action/i })).not.toBeInTheDocument()
    })
  })
})
