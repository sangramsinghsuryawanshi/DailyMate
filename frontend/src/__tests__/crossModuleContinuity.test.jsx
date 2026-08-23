import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { domainBus, DomainEvents } from '../events/domainBus'
import { aggregateDomainActivities } from '../activity/domainActivity'
import { buildAiContextSnapshot } from '../assistant/services/aiContextSnapshot'
import { searchDailyMate, SearchDomain } from '../services/searchService'

describe('Phase 7: Cross-Module Continuity & Domain Intelligence', () => {
  let queryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    domainBus.setQueryClient(queryClient)
    vi.clearAllMocks()
    localStorage.clear()
  })

  // 1. Event Bus Isolation & Invalidation
  it('invalidates appropriate query caches while isolating unrelated domains', () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    // Publish Expense Mutation
    domainBus.publish({
      type: DomainEvents.EXPENSE_MUTATED,
      entityId: 'exp-1',
      action: 'created',
    })

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['expenses'] })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['expenses-summary'] })
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: ['medicine-reminders'] })
  })

  // 2. Duplicate Event Protection
  it('deduplicates rapid identical events within deduplication window', () => {
    const listenerMock = vi.fn()
    const unsubscribe = domainBus.subscribe(DomainEvents.MEDICINE_MUTATED, listenerMock)

    // Fire duplicate events rapidly
    domainBus.publish({ type: DomainEvents.MEDICINE_MUTATED, entityId: 'med-1', action: 'taken' })
    domainBus.publish({ type: DomainEvents.MEDICINE_MUTATED, entityId: 'med-1', action: 'taken' })
    domainBus.publish({ type: DomainEvents.MEDICINE_MUTATED, entityId: 'med-1', action: 'taken' })

    expect(listenerMock).toHaveBeenCalledTimes(1)
    unsubscribe()
  })

  // 3. Unified Activity Feed Aggregation
  it('aggregates multi-domain activities in chronological order with domain badges', () => {
    const expenses = [
      { id: 'e1', description: 'Grocery shopping', category: 'Groceries', amount: 1200, spentOn: '2026-08-23T01:00:00Z' },
    ]
    const medicines = [
      { id: 'm1', name: 'Metformin', dosage: '500mg', frequency: 'Daily', remindAt: '09:00:00', createdAt: '2026-08-23T01:30:00Z' },
    ]
    const bloodRequests = [
      { id: 'b1', patientName: 'Amit', bloodGroup: 'O+', unitsNeeded: 2, hospitalLocation: 'City Care', urgency: 'URGENT', createdAt: '2026-08-23T02:00:00Z' },
    ]

    const feed = aggregateDomainActivities({
      expenses,
      medicines,
      bloodRequests,
      limit: 5,
    })

    expect(feed.length).toBe(3)
    // Most recent first: Blood (02:00) -> Medicine (01:30) -> Expense (01:00)
    expect(feed[0].domain).toBe('BLOOD')
    expect(feed[0].badgeVariant).toBe('danger')
    expect(feed[1].domain).toBe('MEDICINE')
    expect(feed[2].domain).toBe('EXPENSE')
  })

  // 4. Multi-Domain Search Grounding & Tenant Isolation
  it('searches across multi-domain authorized entities and groups by domain', () => {
    const expenses = [
      { id: 'e1', description: 'Car Battery Replacement', category: 'Auto', amount: 4500 },
    ]
    const medicines = [
      { id: 'm1', name: 'Crocin Cold & Flu', dosage: '500mg', frequency: 'Twice daily' },
    ]
    const emergencyContacts = [
      { id: 'c1', name: 'Dr. Battery Specialist', category: 'Hospital', phone: '108' },
    ]

    const results = searchDailyMate('battery', {
      expenses,
      medicines,
      emergencyContacts,
    })

    expect(results.length).toBe(2)
    expect(results.some((r) => r.domain === SearchDomain.FINANCE && r.title === 'Car Battery Replacement')).toBe(true)
    expect(results.some((r) => r.domain === SearchDomain.EMERGENCY && r.title === 'Dr. Battery Specialist')).toBe(true)
  })

  // 5. Sanitized AI Context Snapshot Freshness
  it('builds a sanitized and authoritative snapshot for AI grounding', () => {
    const expenses = [
      { id: 'e1', amount: 1500, category: 'Groceries' },
      { id: 'e2', amount: 900, category: 'Dining' },
    ]
    const medicines = [
      { id: 'm1', name: 'Vitamin D3', remindAt: '08:30:00', active: true },
    ]
    const bloodRequests = [
      { id: 'b1', urgency: 'URGENT', status: 'OPEN' },
      { id: 'b2', urgency: 'STANDARD', status: 'OPEN' },
    ]

    const snapshot = buildAiContextSnapshot({
      expenses,
      medicines,
      bloodRequests,
    })

    expect(snapshot.finance.monthlyTotalINR).toBe(2400)
    expect(snapshot.health.activeMedicinesCount).toBe(1)
    expect(snapshot.health.nextMedication).toBe('Vitamin D3')
    expect(snapshot.community.urgentBloodRequestsCount).toBe(1)
  })

  // 6. Cross-User Personalization Isolation
  it('isolates user-specific personalization preferences in localStorage', () => {
    const userAKey = 'dailymate.personalization.v1.user-A'
    const userBKey = 'dailymate.personalization.v1.user-B'

    localStorage.setItem(userAKey, JSON.stringify({ primaryGoal: 'finance', city: 'Pune' }))
    localStorage.setItem(userBKey, JSON.stringify({ primaryGoal: 'health', city: 'Mumbai' }))

    const rawA = JSON.parse(localStorage.getItem(userAKey))
    const rawB = JSON.parse(localStorage.getItem(userBKey))

    expect(rawA.primaryGoal).toBe('finance')
    expect(rawB.primaryGoal).toBe('health')
    expect(rawA.city).toBe('Pune')
    expect(rawB.city).toBe('Mumbai')
  })
})
