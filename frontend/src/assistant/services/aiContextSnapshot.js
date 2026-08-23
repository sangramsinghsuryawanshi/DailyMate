/**
 * DailyMate AI Context Snapshot Builder
 * Produces a sanitized, authoritative summary of current user state for AI Assistant prompts.
 */

export function buildAiContextSnapshot({
  expenses = [],
  medicines = [],
  bloodRequests = [],
  emergencyContacts = [],
  notifications = [],
}) {
  const currentMonthTotal = expenses.reduce((acc, curr) => acc + (Number(curr.amount) || 0), 0)
  const activeMedicines = medicines.filter((m) => m.active !== false)
  const urgentBloodCount = bloodRequests.filter((b) => b.urgency === 'URGENT' && b.status !== 'FULFILLED').length
  const unreadNotifications = notifications.filter((n) => !n.read).length

  return {
    timestamp: new Date().toISOString(),
    finance: {
      totalExpensesCount: expenses.length,
      monthlyTotalINR: currentMonthTotal,
      recentCategories: [...new Set(expenses.map((e) => e.category))].slice(0, 3),
    },
    health: {
      activeMedicinesCount: activeMedicines.length,
      nextMedication: activeMedicines[0]?.name || null,
      nextDoseTime: activeMedicines[0]?.remindAt || null,
    },
    community: {
      urgentBloodRequestsCount: urgentBloodCount,
      registeredIceContactsCount: emergencyContacts.length,
    },
    system: {
      unreadNotificationsCount: unreadNotifications,
    },
  }
}
