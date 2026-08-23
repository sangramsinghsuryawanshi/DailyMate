/**
 * DailyMate Multi-Domain Global Search Service
 * Aggregates authorized local domain records and destination routes into a grouped search result.
 */

export const SearchDomain = {
  NAVIGATION: 'Navigation',
  FINANCE: 'Expenses & Finance',
  HEALTH: 'Medicines & Health',
  EMERGENCY: 'Emergency & ICE',
  MARKETPLACE: 'Marketplace & Services',
  COMMUNITY: 'Community & Events',
}

const DEFAULT_NAVIGATION_PAGES = [
  { id: 'nav-dashboard', title: 'Command Center Dashboard', category: 'Overview', to: '/dashboard', domain: SearchDomain.NAVIGATION, icon: '📊' },
  { id: 'nav-expenses', title: 'Expenses & Monthly Budget', category: 'Finance', to: '/expenses', domain: SearchDomain.FINANCE, icon: '💰' },
  { id: 'nav-medicines', title: 'Medicine Adherence & Schedule', category: 'Health', to: '/medicines', domain: SearchDomain.HEALTH, icon: '💊' },
  { id: 'nav-emergency', title: 'Emergency ICE Contacts & Hotlines', category: 'Emergency', to: '/emergency-contacts', domain: SearchDomain.EMERGENCY, icon: '🚨' },
  { id: 'nav-marketplace', title: 'Local Services Marketplace', category: 'Marketplace', to: '/marketplace', domain: SearchDomain.MARKETPLACE, icon: '🛠️' },
  { id: 'nav-blood', title: 'Blood Donation Appeals', category: 'Community', to: '/blood', domain: SearchDomain.COMMUNITY, icon: '🩸' },
  { id: 'nav-lostfound', title: 'Lost & Found Notices', category: 'Community', to: '/lost-found', domain: SearchDomain.COMMUNITY, icon: '🔍' },
  { id: 'nav-events', title: 'Community Events & Activities', category: 'Community', to: '/events', domain: SearchDomain.COMMUNITY, icon: '📅' },
  { id: 'nav-complaints', title: 'Civic Complaints & Issues', category: 'Community', to: '/complaints', domain: SearchDomain.COMMUNITY, icon: '📢' },
  { id: 'nav-grocery', title: 'Grocery Price Comparison', category: 'Grocery', to: '/grocery', domain: SearchDomain.FINANCE, icon: '🛒' },
  { id: 'nav-jobs', title: 'Community Jobs Board', category: 'Jobs', to: '/jobs', domain: SearchDomain.MARKETPLACE, icon: '💼' },
  { id: 'nav-assistant', title: 'AI Assistant & Automation', category: 'AI', to: '/assistant', domain: SearchDomain.NAVIGATION, icon: '✨' },
]

export function searchDailyMate(
  query = '',
  {
    providers = [],
    expenses = [],
    medicines = [],
    emergencyContacts = [],
    bloodRequests = [],
    events = [],
    extraItems = [],
  } = {}
) {
  const normalizedQuery = query.trim().toLowerCase()

  if (!normalizedQuery) {
    return []
  }

  const entries = [
    // 1. Navigation Pages
    ...DEFAULT_NAVIGATION_PAGES,

    // 2. Marketplace Providers
    ...providers.map((p) => ({
      id: `p-${p.id}`,
      title: p.name,
      category: p.category || 'Local Service',
      description: `${p.serviceArea || 'Local'} · ₹${p.hourlyRate || 0}/hr`,
      to: `/marketplace/${p.id}`,
      domain: SearchDomain.MARKETPLACE,
      icon: '🛠️',
      tags: [p.name, p.category, p.serviceArea, p.description],
    })),

    // 3. User Expenses
    ...expenses.map((e) => ({
      id: `exp-${e.id}`,
      title: e.description || e.category,
      category: e.category,
      description: `₹${Number(e.amount || 0).toLocaleString('en-IN')} on ${e.spentOn || 'recent'}`,
      to: '/expenses',
      domain: SearchDomain.FINANCE,
      icon: '💰',
      tags: [e.description, e.category, e.notes],
    })),

    // 4. Medicines
    ...medicines.map((m) => ({
      id: `med-${m.id}`,
      title: m.name,
      category: 'Prescription',
      description: `${m.dosage} · ${m.frequency} at ${m.remindAt ? m.remindAt.slice(0, 5) : '08:00'}`,
      to: '/medicines',
      domain: SearchDomain.HEALTH,
      icon: '💊',
      tags: [m.name, m.dosage, m.frequency, m.notes],
    })),

    // 5. Emergency Contacts
    ...emergencyContacts.map((c) => ({
      id: `emg-${c.id}`,
      title: c.name,
      category: c.category || 'Emergency ICE',
      description: `${c.phone} · ${c.location || 'Local'}`,
      to: '/emergency-contacts',
      domain: SearchDomain.EMERGENCY,
      icon: '🚨',
      tags: [c.name, c.category, c.phone, c.description],
    })),

    // 6. Blood Appeals
    ...bloodRequests.map((b) => ({
      id: `bld-${b.id}`,
      title: `Blood Appeal: ${b.bloodGroup}`,
      category: b.urgency === 'URGENT' ? 'Urgent Blood' : 'Blood Request',
      description: `${b.unitsNeeded} units for ${b.patientName} at ${b.hospitalLocation}`,
      to: '/blood',
      domain: SearchDomain.COMMUNITY,
      icon: '🩸',
      tags: [b.patientName, b.bloodGroup, b.hospitalLocation, b.urgency],
    })),

    // 7. Events
    ...events.map((evt) => ({
      id: `evt-${evt.id}`,
      title: evt.title,
      category: evt.category || 'Event',
      description: `${evt.location} on ${evt.eventDate ? evt.eventDate.slice(0, 10) : 'upcoming'}`,
      to: '/events',
      domain: SearchDomain.COMMUNITY,
      icon: '📅',
      tags: [evt.title, evt.category, evt.location, evt.description],
    })),

    // 8. Custom Extra Items
    ...(Array.isArray(extraItems) ? extraItems : []),
  ]

  return entries
    .filter((entry) => {
      const haystack = [
        entry.title,
        entry.category,
        entry.description,
        ...(entry.tags || []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      return haystack.includes(normalizedQuery)
    })
    .slice(0, 8)
}
