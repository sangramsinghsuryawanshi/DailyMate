/**
 * DailyMate Domain Event Bus
 *
 * Decoupled event coordination and invalidation layer.
 * React Query remains the authoritative data store; domainBus orchestrates
 * cross-feature query invalidations, analytics dispatches, and activity signals.
 */

export const EventCategory = {
  ENTITY_MUTATED: 'ENTITY_MUTATED',
  ENTITY_DELETED: 'ENTITY_DELETED',
  ENTITY_STATUS_CHANGED: 'ENTITY_STATUS_CHANGED',
  NOTIFICATION_CHANGED: 'NOTIFICATION_CHANGED',
  PERSONALIZATION_CHANGED: 'PERSONALIZATION_CHANGED',
  CONVERSATION_CHANGED: 'CONVERSATION_CHANGED',
  ACTION_EXECUTED: 'ACTION_EXECUTED',
}

export const DomainEvents = {
  EXPENSE_MUTATED: 'EXPENSE_MUTATED',
  MEDICINE_MUTATED: 'MEDICINE_MUTATED',
  BLOOD_MUTATED: 'BLOOD_MUTATED',
  EVENT_MUTATED: 'EVENT_MUTATED',
  COMPLAINT_MUTATED: 'COMPLAINT_MUTATED',
  NOTIFICATION_MUTATED: 'NOTIFICATION_MUTATED',
  PERSONALIZATION_CHANGED: 'PERSONALIZATION_CHANGED',
  ACTION_EXECUTED: 'ACTION_EXECUTED',
}

class DomainEventBus {
  constructor() {
    this.listeners = new Map()
    this.queryClient = null
    this.processedEvents = new Set()
  }

  setQueryClient(client) {
    this.queryClient = client
  }

  /**
   * Subscribe to specific event type or all events ('*')
   */
  subscribe(eventType, callback) {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set())
    }
    this.listeners.get(eventType).add(callback)

    return () => {
      this.listeners.get(eventType)?.delete(callback)
    }
  }

  /**
   * Publish a domain event and trigger associated query invalidations
   */
  publish(event) {
    const enrichedEvent = {
      type: event.type,
      domain: event.domain || this._inferDomain(event.type),
      action: event.action || 'updated',
      entityId: event.entityId || null,
      timestamp: event.timestamp || Date.now(),
      metadata: event.metadata || {},
    }

    // Deduplication check (within 100ms window)
    const eventKey = `${enrichedEvent.type}:${enrichedEvent.entityId}:${enrichedEvent.action}`
    if (this.processedEvents.has(eventKey)) {
      return
    }
    this.processedEvents.add(eventKey)
    setTimeout(() => this.processedEvents.delete(eventKey), 100)

    // Automatically trigger authoritative React Query invalidations if client is connected
    if (this.queryClient) {
      this._invalidateQueriesForEvent(enrichedEvent)
    }

    // Notify specific type listeners
    if (this.listeners.has(enrichedEvent.type)) {
      this.listeners.get(enrichedEvent.type).forEach((cb) => {
        try {
          cb(enrichedEvent)
        } catch (e) {
          console.error(`Error in domain event listener for ${enrichedEvent.type}:`, e)
        }
      })
    }

    // Notify wildcard listeners
    if (this.listeners.has('*')) {
      this.listeners.get('*').forEach((cb) => {
        try {
          cb(enrichedEvent)
        } catch (e) {
          console.error(`Error in wildcard domain event listener:`, e)
        }
      })
    }
  }

  _inferDomain(eventType) {
    if (eventType.startsWith('EXPENSE')) return 'EXPENSE'
    if (eventType.startsWith('MEDICINE')) return 'MEDICINE'
    if (eventType.startsWith('BLOOD')) return 'BLOOD'
    if (eventType.startsWith('EVENT')) return 'EVENT'
    if (eventType.startsWith('COMPLAINT')) return 'COMPLAINT'
    if (eventType.startsWith('NOTIFICATION')) return 'NOTIFICATION'
    if (eventType.startsWith('PERSONALIZATION')) return 'USER'
    return 'SYSTEM'
  }

  _invalidateQueriesForEvent(event) {
    switch (event.type) {
      case DomainEvents.EXPENSE_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['expenses'] })
        this.queryClient.invalidateQueries({ queryKey: ['expenses-summary'] })
        break
      case DomainEvents.MEDICINE_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
        break
      case DomainEvents.BLOOD_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
        break
      case DomainEvents.EVENT_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['local-events'] })
        break
      case DomainEvents.COMPLAINT_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
        break
      case DomainEvents.NOTIFICATION_MUTATED:
        this.queryClient.invalidateQueries({ queryKey: ['notifications'] })
        break
      case DomainEvents.PERSONALIZATION_CHANGED:
        this.queryClient.invalidateQueries({ queryKey: ['profile'] })
        break
      default:
        break
    }
  }
}

export const domainBus = new DomainEventBus()
