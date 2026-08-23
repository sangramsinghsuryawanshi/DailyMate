import { useNavigate } from 'react-router-dom'
import { Modal, Drawer, Button } from '../../design-system'
import './QuickActionMenu.css'

export const QUICK_ACTIONS = [
  {
    id: 'expense',
    title: 'Add Expense',
    description: 'Log food, petrol, groceries, or bills',
    icon: '💰',
    domain: 'expense',
    to: '/expenses?action=add',
  },
  {
    id: 'medicine',
    title: 'Add Medicine Reminder',
    description: 'Set dosages, frequencies, and alert times',
    icon: '💊',
    domain: 'health',
    to: '/medicines?action=add',
  },
  {
    id: 'grocery',
    title: 'Add Grocery Item',
    description: 'Track household supplies & price comparison',
    icon: '🛒',
    domain: 'expense',
    to: '/grocery',
  },
  {
    id: 'job',
    title: 'Post Job Opening',
    description: 'Hire local workers or share vacancies',
    icon: '💼',
    domain: 'community',
    to: '/jobs',
  },
  {
    id: 'emergency',
    title: 'Add Emergency Contact',
    description: 'Register ICE contacts and hotlines',
    icon: '🚨',
    domain: 'emergency',
    to: '/emergency-contacts?action=add',
  },
  {
    id: 'blood',
    title: 'Post Blood Request',
    description: 'Urgent hospital or donor appeal',
    icon: '🩸',
    domain: 'emergency',
    to: '/blood?action=add',
  },
  {
    id: 'complaint',
    title: 'Report Complaint',
    description: 'Street light, garbage, water, or civic issues',
    icon: '📢',
    domain: 'community',
    to: '/community-complaints?action=add',
  },
  {
    id: 'lost-found',
    title: 'Post Lost / Found Item',
    description: 'Report missing wallet, bag, or items',
    icon: '🔍',
    domain: 'community',
    to: '/lost-found?action=add',
  },
  {
    id: 'event',
    title: 'Create Community Event',
    description: 'Tournament, blood drive, or meetup',
    icon: '📅',
    domain: 'community',
    to: '/events?action=add',
  },
  {
    id: 'marketplace',
    title: 'Find Local Service',
    description: 'Plumbers, electricians, tutors, cleaners',
    icon: '🛠️',
    domain: 'marketplace',
    to: '/marketplace',
  },
  {
    id: 'assistant',
    title: 'Ask AI Assistant',
    description: 'Natural language bulk actions & questions',
    icon: '✨',
    domain: 'ai',
    to: '/assistant',
  },
]

export function QuickActionMenu({
  isOpen,
  onClose,
  isMobile = false,
}) {
  const navigate = useNavigate()

  const handleSelect = (to) => {
    onClose?.()
    navigate(to)
  }

  const content = (
    <div className="dm-quick-action-grid">
      {QUICK_ACTIONS.map((item) => (
        <button
          key={item.id}
          type="button"
          className={`dm-quick-action-item dm-quick-action-item--${item.domain}`}
          onClick={() => handleSelect(item.to)}
        >
          <div className="dm-quick-action-item__icon-box">
            <span className="dm-quick-action-item__icon" aria-hidden="true">
              {item.icon}
            </span>
          </div>
          <div className="dm-quick-action-item__text">
            <strong className="dm-quick-action-item__title">{item.title}</strong>
            <span className="dm-quick-action-item__desc">{item.description}</span>
          </div>
        </button>
      ))}
    </div>
  )

  if (isMobile) {
    return (
      <Drawer
        isOpen={isOpen}
        onClose={onClose}
        title="Universal Quick Actions"
        placement="bottom"
        size="lg"
      >
        {content}
      </Drawer>
    )
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Universal Quick Actions"
      size="lg"
    >
      {content}
    </Modal>
  )
}

export default QuickActionMenu
