/**
 * DailyMate Unified Domain Activity Model
 * Normalizes multi-domain items into a consistent, sortable activity feed for Command Center & Dashboard.
 */

export function createDomainActivity({
  id,
  domain,
  entityId,
  action,
  title,
  description,
  timestamp,
  status = 'completed',
  href = '#',
  icon = '📌',
  badgeVariant = 'neutral',
}) {
  return {
    id: id || `${domain}-${entityId || Date.now()}`,
    domain,
    entityId,
    action,
    title,
    description,
    timestamp: timestamp ? new Date(timestamp).getTime() : Date.now(),
    status,
    href,
    icon,
    badgeVariant,
  }
}

/**
 * Aggregates multi-domain data sources into a unified chronologically sorted Activity Feed
 */
export function aggregateDomainActivities({
  expenses = [],
  medicines = [],
  bloodRequests = [],
  events = [],
  complaints = [],
  limit = 10,
}) {
  const activities = []

  // 1. Expenses
  expenses.slice(0, 5).forEach((exp) => {
    activities.push(
      createDomainActivity({
        id: `act-exp-${exp.id}`,
        domain: 'EXPENSE',
        entityId: exp.id,
        action: 'spent',
        title: `Logged expense: ${exp.description || exp.category}`,
        description: `₹${Number(exp.amount || 0).toLocaleString('en-IN')} in ${exp.category}`,
        timestamp: exp.spentOn || exp.createdAt,
        href: '/expenses',
        icon: '💰',
        badgeVariant: 'expense',
      })
    )
  })

  // 2. Medicines
  medicines.slice(0, 5).forEach((med) => {
    activities.push(
      createDomainActivity({
        id: `act-med-${med.id}`,
        domain: 'MEDICINE',
        entityId: med.id,
        action: med.active === false ? 'paused' : 'scheduled',
        title: `Medicine routine: ${med.name}`,
        description: `${med.dosage} · ${med.frequency} at ${med.remindAt ? med.remindAt.slice(0, 5) : '08:00'}`,
        timestamp: med.createdAt || Date.now(),
        href: '/medicines',
        icon: '💊',
        badgeVariant: 'health',
      })
    )
  })

  // 3. Blood Requests
  bloodRequests.slice(0, 3).forEach((blood) => {
    activities.push(
      createDomainActivity({
        id: `act-blood-${blood.id}`,
        domain: 'BLOOD',
        entityId: blood.id,
        action: blood.status === 'FULFILLED' ? 'fulfilled' : 'requested',
        title: `Blood appeal for ${blood.patientName} (${blood.bloodGroup})`,
        description: `${blood.unitsNeeded} units needed at ${blood.hospitalLocation}`,
        timestamp: blood.createdAt,
        href: '/blood',
        icon: '🩸',
        badgeVariant: blood.urgency === 'URGENT' ? 'danger' : 'emergency',
      })
    )
  })

  // 4. Local Events
  events.slice(0, 3).forEach((evt) => {
    activities.push(
      createDomainActivity({
        id: `act-evt-${evt.id}`,
        domain: 'EVENT',
        entityId: evt.id,
        action: 'organized',
        title: `Community event: ${evt.title}`,
        description: `${evt.category} at ${evt.location}`,
        timestamp: evt.eventDate || evt.createdAt,
        href: '/events',
        icon: '📅',
        badgeVariant: 'neutral',
      })
    )
  })

  // 5. Complaints
  complaints.slice(0, 3).forEach((comp) => {
    activities.push(
      createDomainActivity({
        id: `act-comp-${comp.id}`,
        domain: 'COMPLAINT',
        entityId: comp.id,
        action: comp.status?.toLowerCase() || 'submitted',
        title: `Reported issue: ${comp.title}`,
        description: `${comp.category} at ${comp.location} · Status: ${comp.status}`,
        timestamp: comp.createdAt,
        href: '/complaints',
        icon: '📢',
        badgeVariant: comp.status === 'RESOLVED' ? 'success' : 'warning',
      })
    )
  })

  // Sort descending by timestamp and slice to limit
  return activities
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, limit)
}
