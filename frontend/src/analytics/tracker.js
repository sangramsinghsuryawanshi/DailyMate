/**
 * DailyMate Product Analytics & Event Tracking Abstraction
 * Decouples event dispatching from specific analytics providers.
 */

const isDevelopment = typeof import.meta !== 'undefined' && import.meta.env ? Boolean(import.meta.env.DEV) : true

const eventListeners = new Set()

export function subscribeToEvents(callback) {
  eventListeners.add(callback)
  return () => eventListeners.delete(callback)
}

export function trackEvent(eventName, properties = {}) {
  const payload = {
    event: eventName,
    properties: {
      ...properties,
      timestamp: new Date().toISOString(),
      url: window.location.pathname,
    },
  }

  if (isDevelopment) {
    // Helpful developer logging without crashing
    // console.debug(`[Analytics] ${eventName}`, payload.properties)
  }

  // Notify registered observers/providers
  eventListeners.forEach((listener) => {
    try {
      listener(payload)
    } catch (e) {
      console.error('[Analytics] Listener error', e)
    }
  })

  return payload
}

export const AnalyticsEvents = {
  PAGE_VIEW: 'page_view',
  SIGNUP_STARTED: 'signup_started',
  SIGNUP_COMPLETED: 'signup_completed',
  ONBOARDING_COMPLETED: 'onboarding_completed',
  QUICK_ACTION_OPENED: 'quick_action_opened',
  QUICK_ACTION_TRIGGERED: 'quick_action_triggered',
  AI_QUERY_SENT: 'ai_query_sent',
  ASSISTANT_MESSAGE_SENT: 'assistant_message_sent',
  ASSISTANT_PROPOSAL_VIEWED: 'assistant_proposal_viewed',
  ASSISTANT_ACTION_CONFIRMED: 'assistant_action_confirmed',
  ASSISTANT_ACTION_CANCELLED: 'assistant_action_cancelled',
  EXPENSE_ADDED: 'expense_added',
  MEDICINE_SCHEDULED: 'medicine_scheduled',
  MEDICINE_TAKEN: 'medicine_taken',
  BLOOD_REQUEST_POSTED: 'blood_request_posted',
  COMPLAINT_SUBMITTED: 'complaint_submitted',
  PROVIDER_VIEWED: 'provider_viewed',
  PROVIDER_CONTACTED: 'provider_contacted',
  THEME_TOGGLED: 'theme_toggled',
}
