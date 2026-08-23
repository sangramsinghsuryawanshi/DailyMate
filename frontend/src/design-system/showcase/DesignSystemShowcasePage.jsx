import { useState } from 'react'
import {
  Button,
  Input,
  Select,
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
  Badge,
  Avatar,
  Modal,
  ModalFooter,
  Drawer,
  DrawerFooter,
  Skeleton,
  SkeletonCard,
  SearchBar,
  ResponsiveContainer,
  PageHeader,
  StatCard,
  EmptyState,
  ErrorState,
  ConfirmationDialog,
  ActionProposal,
  TrustBadge,
  useToast,
} from '../index'

export default function DesignSystemShowcasePage() {
  const [searchValue, setSearchValue] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const [isConfirmOpen, setIsConfirmOpen] = useState(false)
  const [isThemeDark, setIsThemeDark] = useState(false)

  const toast = useToast()

  const toggleTheme = () => {
    const next = !isThemeDark
    setIsThemeDark(next)
    document.documentElement.setAttribute('data-theme', next ? 'dark' : 'light')
  }

  return (
    <ResponsiveContainer size="wide" style={{ paddingTop: '2rem', paddingBottom: '4rem' }}>
      <PageHeader
        title="DailyMate Design System Showcase"
        eyebrow="Foundation & Tokens"
        description="Unified component library, design tokens, primitives, and UX patterns."
        actions={
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <Button variant="outline" size="sm" onClick={toggleTheme}>
              {isThemeDark ? '☀️ Light Mode' : '🌙 Dark Mode'}
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => toast.success('Design system toast works seamlessly!')}
            >
              Trigger Toast
            </Button>
          </div>
        }
      />

      {/* 1. Design Tokens & Domain Palette */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Domain Accent Palette
        </h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1rem',
          }}
        >
          <StatCard
            domain="expense"
            label="Expenses"
            value="₹24,500"
            trend="+12%"
            trendDirection="up"
            trendLabel="vs last month"
            icon="💰"
          />
          <StatCard
            domain="health"
            label="Medicine"
            value="94%"
            trend="+4%"
            trendDirection="up"
            trendLabel="adherence rate"
            icon="💊"
          />
          <StatCard
            domain="emergency"
            label="Emergency ICE"
            value="2 Contacts"
            trend="Active"
            trendDirection="neutral"
            icon="🚨"
          />
          <StatCard
            domain="community"
            label="Community"
            value="14 Posts"
            trend="2 new"
            trendDirection="up"
            icon="🤝"
          />
          <StatCard
            domain="marketplace"
            label="Marketplace"
            value="48 Providers"
            trend="Verified"
            trendDirection="neutral"
            icon="🛠️"
          />
          <StatCard
            domain="ai"
            label="AI Assistant"
            value="Active"
            trend="Autonomous"
            trendDirection="up"
            icon="✨"
          />
        </div>
      </section>

      {/* 2. Buttons & Primitives */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Buttons & States
        </h2>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <Button variant="primary">Primary Button</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="success">Success</Button>
          <Button variant="primary" isLoading>
            Loading
          </Button>
          <Button variant="primary" disabled>
            Disabled
          </Button>
          <Button variant="primary" size="sm">
            Small
          </Button>
          <Button variant="primary" size="lg">
            Large
          </Button>
        </div>
      </section>

      {/* 3. Form Inputs & Search */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Inputs & Selects
        </h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '1.25rem',
          }}
        >
          <Input
            label="Expense Name"
            placeholder="e.g. Groceries"
            helperText="Enter a descriptive label."
            required
          />
          <Input
            label="Email Address"
            type="email"
            value="invalid-email"
            error="Please enter a valid email address."
          />
          <Select
            label="Category"
            options={[
              { value: 'food', label: 'Food & Dining' },
              { value: 'transport', label: 'Transportation' },
              { value: 'health', label: 'Healthcare' },
            ]}
          />
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.35rem' }}>
              Instant Search (⌘K)
            </label>
            <SearchBar
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              onClear={() => setSearchValue('')}
            />
          </div>
        </div>
      </section>

      {/* 4. Badges, Trust & Avatars */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Badges, Trust States & Avatars
        </h2>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center', marginBottom: '1rem' }}>
          <Badge variant="default">Default</Badge>
          <Badge variant="success" dot>Active</Badge>
          <Badge variant="warning" dot>Pending</Badge>
          <Badge variant="danger">Urgent</Badge>
          <Badge variant="info">Info</Badge>
          <Badge domain="expense">Expense</Badge>
          <Badge domain="health">Health</Badge>
          <Badge domain="ai">Assistant</Badge>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center', marginBottom: '1.5rem' }}>
          <TrustBadge type="saved" />
          <TrustBadge type="verified" />
          <TrustBadge type="urgent" />
          <TrustBadge type="private" />
          <TrustBadge type="executed" />
        </div>

        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <Avatar name="Sangram Suryawanshi" size="xs" status="online" />
          <Avatar name="Sangram Suryawanshi" size="sm" status="online" />
          <Avatar name="Sangram Suryawanshi" size="md" status="busy" />
          <Avatar name="Sangram Suryawanshi" size="lg" status="away" />
          <Avatar name="Sangram Suryawanshi" size="xl" status="offline" />
        </div>
      </section>

      {/* 5. Action Proposals (AI Lifecycle) */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          AI Action Proposal Lifecycle
        </h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '1rem',
          }}
        >
          <ActionProposal
            proposal={{
              actionId: 'exp-1',
              status: 'PENDING',
              summary: 'Record ₹2,400 bulk expenses across 4 items (Lunch ₹50, Dinner ₹50, Petrol ₹800, Groceries ₹1,500)',
            }}
            onConfirm={() =>
              new Promise((res) =>
                setTimeout(
                  () => res({ resultMessage: '4 expenses totaling ₹2,400 recorded successfully.' }),
                  600
                )
              )
            }
          />
          <ActionProposal
            proposal={{
              actionId: 'med-1',
              status: 'EXECUTED',
              summary: 'Add 2 medicine reminders: Vitamin D at 9:00 AM, Calcium at 8:00 PM',
              resultMessage: 'Reminders active in notification schedule.',
            }}
          />
        </div>
      </section>

      {/* 6. Overlays & Dialogs */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Modals, Drawers & Confirmations
        </h2>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Button variant="outline" onClick={() => setIsModalOpen(true)}>
            Open Modal
          </Button>
          <Button variant="outline" onClick={() => setIsDrawerOpen(true)}>
            Open Slide Drawer
          </Button>
          <Button variant="danger" onClick={() => setIsConfirmOpen(true)}>
            Open Confirmation Dialog
          </Button>
        </div>

        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title="Create Expense Record"
          description="Log a new transaction with category and receipt."
        >
          <Input label="Amount (₹)" placeholder="500" required />
          <ModalFooter>
            <Button variant="ghost" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => setIsModalOpen(false)}>
              Save Record
            </Button>
          </ModalFooter>
        </Modal>

        <Drawer
          isOpen={isDrawerOpen}
          onClose={() => setIsDrawerOpen(false)}
          title="Quick Add Action"
          description="Choose a quick action to manage your life."
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <Button variant="outline" fullWidth>
              💰 Add Expense
            </Button>
            <Button variant="outline" fullWidth>
              💊 Add Medicine Reminder
            </Button>
            <Button variant="outline" fullWidth>
              🚨 Post Blood Request
            </Button>
            <Button variant="outline" fullWidth>
              ✨ Ask AI Assistant
            </Button>
          </div>
          <DrawerFooter>
            <Button variant="ghost" onClick={() => setIsDrawerOpen(false)}>
              Close
            </Button>
          </DrawerFooter>
        </Drawer>

        <ConfirmationDialog
          isOpen={isConfirmOpen}
          onClose={() => setIsConfirmOpen(false)}
          onConfirm={() => {
            setIsConfirmOpen(false)
            toast.error('Record deleted permanently.')
          }}
          title="Delete Expense Record?"
          description="This action cannot be undone. Are you sure you want to delete this ₹1,500 grocery record?"
        />
      </section>

      {/* 7. Loading, Skeletons & Empty States */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Loading Skeletons & Feedback States
        </h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '1rem',
            marginBottom: '1.5rem',
          }}
        >
          <SkeletonCard />
          <EmptyState
            icon="🛍️"
            title="No services found"
            description="Try searching for another keyword or location."
            primaryAction={{ label: 'Explore All', onClick: () => {} }}
          />
          <ErrorState
            title="Failed to load notifications"
            message="Server connection timed out."
            onRetry={() => toast.info('Retrying connection...')}
          />
        </div>
      </section>
    </ResponsiveContainer>
  )
}
