import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  Button,
  Input,
  Select,
  Card,
  Badge,
  Avatar,
  Modal,
  Drawer,
  Skeleton,
  SearchBar,
  ResponsiveContainer,
  PageHeader,
  StatCard,
  EmptyState,
  ErrorState,
  ConfirmationDialog,
  ActionProposal,
  TrustBadge,
  ToastProvider,
  useToast,
} from '../index'
import { tokens } from '../tokens'

describe('DailyMate Design System — Tokens, Primitives & Patterns', () => {
  it('exposes design tokens mapping correctly', () => {
    expect(tokens.colors.primary).toContain('--dm-color-primary')
    expect(tokens.colors.domainExpense).toContain('--dm-domain-expense')
    expect(tokens.typography.fontFamily.sans).toBeDefined()
    expect(tokens.containers.dashboard).toBe('1200px')
  })

  it('renders Button with variants, loading and click handlers', async () => {
    const handleClick = vi.fn()
    const { rerender } = render(
      <Button variant="primary" onClick={handleClick}>
        Save Record
      </Button>
    )

    const btn = screen.getByRole('button', { name: 'Save Record' })
    expect(btn).toHaveClass('dm-button--primary')
    await userEvent.click(btn)
    expect(handleClick).toHaveBeenCalledTimes(1)

    rerender(
      <Button variant="danger" isLoading>
        Save Record
      </Button>
    )
    const loadingBtn = screen.getByRole('button', { name: 'Save Record' })
    expect(loadingBtn).toBeDisabled()
    expect(loadingBtn).toHaveAttribute('aria-busy', 'true')
  })

  it('renders Input with labels, helper text, and error states', () => {
    const { rerender } = render(
      <Input label="Expense Amount" helperText="Enter numeric amount" required />
    )
    expect(screen.getByLabelText(/Expense Amount/i)).toBeInTheDocument()
    expect(screen.getByText('Enter numeric amount')).toBeInTheDocument()

    rerender(<Input label="Expense Amount" error="Amount is required" />)
    expect(screen.getByRole('alert')).toHaveTextContent('Amount is required')
  })

  it('renders Select with options and handles selection', async () => {
    const handleChange = vi.fn()
    render(
      <Select
        label="Category"
        onChange={handleChange}
        options={[
          { value: 'food', label: 'Food & Dining' },
          { value: 'travel', label: 'Travel' },
        ]}
      />
    )
    const select = screen.getByLabelText('Category')
    expect(select).toBeInTheDocument()
    await userEvent.selectOptions(select, 'travel')
    expect(handleChange).toHaveBeenCalled()
  })

  it('renders Card with header, content, and interactive states', async () => {
    const handleClick = vi.fn()
    render(
      <Card interactive onClick={handleClick}>
        <h3>Card Title</h3>
        <p>Card Content</p>
      </Card>
    )
    const card = screen.getByRole('button')
    expect(card).toHaveClass('dm-card--interactive')
    await userEvent.click(card)
    expect(handleClick).toHaveBeenCalledTimes(1)
  })

  it('renders Badge and TrustBadge variants', () => {
    render(
      <div>
        <Badge variant="success" dot>Active</Badge>
        <Badge domain="expense">Expense</Badge>
        <TrustBadge type="verified" />
        <TrustBadge type="saved" />
      </div>
    )
    expect(screen.getByText('Active')).toBeInTheDocument()
    expect(screen.getByText('Expense')).toBeInTheDocument()
    expect(screen.getByText('Verified Provider')).toBeInTheDocument()
    expect(screen.getByText('Saved')).toBeInTheDocument()
  })

  it('renders Avatar with image or fallback initials', () => {
    const { rerender } = render(<Avatar name="Sangram Suryawanshi" />)
    expect(screen.getByText('SS')).toBeInTheDocument()

    rerender(<Avatar name="Single" />)
    expect(screen.getByText('SI')).toBeInTheDocument()
  })

  it('renders Modal and traps Escape key close', () => {
    const handleClose = vi.fn()
    render(
      <Modal isOpen={true} onClose={handleClose} title="Test Modal">
        <p>Modal content</p>
      </Modal>
    )
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Test Modal')).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(handleClose).toHaveBeenCalled()
  })

  it('renders Drawer with slide-over panel and closes on ESC', () => {
    const handleClose = vi.fn()
    render(
      <Drawer isOpen={true} onClose={handleClose} title="Side Panel">
        <p>Drawer content</p>
      </Drawer>
    )
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Side Panel')).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(handleClose).toHaveBeenCalled()
  })

  it('renders SearchBar and handles clear action', async () => {
    const handleClear = vi.fn()
    render(
      <SearchBar value="Khichadi" onChange={vi.fn()} onClear={handleClear} />
    )
    const clearBtn = screen.getByRole('button', { name: /Clear search/i })
    await userEvent.click(clearBtn)
    expect(handleClear).toHaveBeenCalled()
  })

  it('renders StatCard with value, domain accent, and trend indicator', () => {
    render(
      <StatCard
        domain="expense"
        label="Monthly Total"
        value="₹24,500"
        trend="+12%"
        trendDirection="up"
        icon="💰"
      />
    )
    expect(screen.getByText('Monthly Total')).toBeInTheDocument()
    expect(screen.getByText('₹24,500')).toBeInTheDocument()
    expect(screen.getByText(/12%/)).toBeInTheDocument()
  })

  it('renders EmptyState and ErrorState with action buttons', async () => {
    const handleAction = vi.fn()
    const handleRetry = vi.fn()

    const { rerender } = render(
      <EmptyState
        title="No expenses logged"
        description="Add your first transaction."
        primaryAction={{ label: 'Add Expense', onClick: handleAction }}
      />
    )
    expect(screen.getByText('No expenses logged')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Add Expense' }))
    expect(handleAction).toHaveBeenCalledTimes(1)

    rerender(
      <ErrorState
        title="Failed to load items"
        onRetry={handleRetry}
      />
    )
    expect(screen.getByText('Failed to load items')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try Again' }))
    expect(handleRetry).toHaveBeenCalledTimes(1)
  })

  it('renders ActionProposal with PENDING lifecycle and triggers confirm', async () => {
    const handleConfirm = vi.fn().mockResolvedValue({ resultMessage: 'Records added.' })
    render(
      <ActionProposal
        proposal={{
          actionId: 'action-123',
          status: 'PENDING',
          summary: 'Bulk record 4 expenses totaling ₹2,400',
        }}
        onConfirm={handleConfirm}
      />
    )

    expect(screen.getByText('Action Proposal')).toBeInTheDocument()
    expect(screen.getByText('PENDING')).toBeInTheDocument()
    expect(screen.getByText('Bulk record 4 expenses totaling ₹2,400')).toBeInTheDocument()

    const confirmBtn = screen.getByRole('button', { name: 'Confirm Action' })
    await userEvent.click(confirmBtn)
    expect(handleConfirm).toHaveBeenCalledWith('action-123')
  })

  it('provides working Toast notifications', async () => {
    function TestComponent() {
      const toast = useToast()
      return (
        <button onClick={() => toast.success('Operation succeeded!')}>
          Show Toast
        </button>
      )
    }

    render(
      <ToastProvider>
        <TestComponent />
      </ToastProvider>
    )

    await userEvent.click(screen.getByRole('button', { name: 'Show Toast' }))
    expect(screen.getByText('Operation succeeded!')).toBeInTheDocument()
  })
})
