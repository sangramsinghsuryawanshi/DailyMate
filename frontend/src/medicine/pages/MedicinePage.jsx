import { useState, useMemo, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import {
  Button,
  Input,
  Select,
  Card,
  StatCard,
  Badge,
  Modal,
  Skeleton,
  Pagination,
  EmptyState,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import { createReminder, deleteReminder, getReminders, updateReminder } from '../services/medicineApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './MedicinePage.css'

const defaultForm = {
  name: '',
  dosage: '',
  frequency: 'Daily',
  remindAt: '08:00',
  notes: '',
  active: true,
}

const FREQUENCY_OPTIONS = [
  'Daily',
  'Twice daily',
  'Three times daily',
  'Weekly',
  'As needed',
]

export default function MedicinePage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editTarget, setEditTarget] = useState(null)
  const [editForm, setEditForm] = useState(null)
  const [formError, setFormError] = useState('')
  const [editError, setEditError] = useState('')
  const [takenIds, setTakenIds] = useState({})
  const [snoozedIds, setSnoozedIds] = useState({})

  const { page, pageSize, setPage, setPageSize } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'medicines' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['medicine-reminders', { page, pageSize }],
    queryFn: () => getReminders({ page, size: pageSize }),
    placeholderData: (previousData) => previousData,
  })

  const reminders = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? reminders.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  // Summary Metrics
  const activeReminders = useMemo(() => reminders.filter((r) => r.active !== false), [reminders])
  const takenCount = useMemo(() => Object.keys(takenIds).length, [takenIds])
  const adherencePercent = useMemo(() => {
    if (activeReminders.length === 0) return 100
    return Math.min(Math.round((takenCount / activeReminders.length) * 100), 100)
  }, [takenCount, activeReminders.length])

  const createMutation = useMutation({
    mutationFn: createReminder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      setForm(defaultForm)
      setFormError('')
      trackEvent('medicine_reminder_created')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to create reminder.')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateReminder(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      setEditTarget(null)
      setEditForm(null)
      setEditError('')
      trackEvent('medicine_reminder_updated')
    },
    onError: (err) => {
      setEditError(err.response?.data?.detail || err.response?.data?.message || 'Failed to update reminder.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteReminder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      trackEvent('medicine_reminder_deleted')
    },
  })

  function handleChange(event) {
    const { name, value, type, checked } = event.target
    setForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  function handleEditChange(event) {
    const { name, value, type, checked } = event.target
    setEditForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    const payload = {
      name: form.name.trim(),
      dosage: form.dosage.trim(),
      frequency: form.frequency.trim(),
      remindAt: form.remindAt,
      notes: form.notes?.trim() || null,
      active: form.active,
    }

    if (!payload.name || !payload.dosage || !payload.frequency || !payload.remindAt) {
      setFormError('Please fill out all required fields.')
      return
    }

    createMutation.mutate(payload)
  }

  function openEditDialog(reminder) {
    setEditTarget(reminder)
    setEditForm({
      name: reminder.name,
      dosage: reminder.dosage,
      frequency: reminder.frequency,
      remindAt: reminder.remindAt ? reminder.remindAt.slice(0, 5) : '08:00',
      notes: reminder.notes || '',
      active: reminder.active !== false,
    })
    setEditError('')
  }

  function handleEditSubmit(event) {
    event.preventDefault()
    setEditError('')

    const payload = {
      name: editForm.name.trim(),
      dosage: editForm.dosage.trim(),
      frequency: editForm.frequency.trim(),
      remindAt: editForm.remindAt,
      notes: editForm.notes?.trim() || null,
      active: editForm.active,
    }

    if (!payload.name || !payload.dosage || !payload.frequency || !payload.remindAt) {
      setEditError('Please fill out all required fields.')
      return
    }

    updateMutation.mutate({ id: editTarget.id, payload })
  }

  function handleTogglePause(reminder) {
    const payload = {
      name: reminder.name,
      dosage: reminder.dosage,
      frequency: reminder.frequency,
      remindAt: reminder.remindAt ? reminder.remindAt.slice(0, 5) : '08:00',
      notes: reminder.notes || null,
      active: !reminder.active,
    }
    updateMutation.mutate({ id: reminder.id, payload })
  }

  function handleTake(id) {
    setTakenIds((prev) => ({ ...prev, [id]: true }))
    trackEvent('medicine_action_taken', { action: 'take', id })
  }

  function handleSnooze(id) {
    setSnoozedIds((prev) => ({ ...prev, [id]: true }))
    trackEvent('medicine_action_taken', { action: 'snooze', id })
  }

  return (
    <MainLayout>
      <div className="dm-medicine-page">
        <ResponsiveContainer size="wide">
          {/* ===================================================================
              1. PAGE HEADER & STATS
             =================================================================== */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Health & Routines</span>
              <h1 className="dm-page-main-title">Medicine Reminders</h1>
              <p className="dm-page-subtitle">
                Effortless daily adherence, smart dosage schedules, and health routine tracking.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/assistant?prompt=What%20medicines%20do%20I%20have%20scheduled%20for%20today%3F">
                <Button variant="outline" size="sm" iconLeft="✨">
                  Ask AI Schedule
                </Button>
              </Link>
            </div>
          </div>

          {/* Key Health Metrics Grid */}
          <div className="dm-medicine-stats-grid">
            <StatCard
              domain="health"
              label="Active Prescriptions"
              value={String(activeReminders.length)}
              trend="Scheduled"
              trendDirection="up"
              trendLabel="Daily medications"
              icon="💊"
            />
            <StatCard
              domain="health"
              label="Today's Adherence"
              value={`${adherencePercent}%`}
              trend={`${takenCount} of ${activeReminders.length} taken`}
              trendDirection={adherencePercent >= 80 ? 'up' : 'neutral'}
              trendLabel="Daily completion"
              icon="🎯"
            />
            <StatCard
              domain="health"
              label="Next Due Time"
              value={activeReminders[0]?.remindAt ? activeReminders[0].remindAt.slice(0, 5) : 'None'}
              trend="Upcoming"
              trendDirection="neutral"
              trendLabel="Next reminder"
              icon="⏰"
            />
            <StatCard
              domain="health"
              label="Adherence Streak"
              value="14 Days"
              trend="Consistent"
              trendDirection="up"
              trendLabel="Streak record"
              icon="🔥"
            />
          </div>

          {/* ===================================================================
              2. TWO-COLUMN LAYOUT: SCHEDULE & ADD FORM
             =================================================================== */}
          <div className="dm-medicine-layout-grid">
            {/* Left Column: Action-Oriented Schedule Cards */}
            <div className="dm-medicine-schedule-col">
              <Card className="dm-medicine-panel">
                <div className="dm-panel-header">
                  <div>
                    <h2 className="dm-panel-title">Today's Dosage Schedule</h2>
                    <p className="dm-panel-desc">Tap Take or Snooze to record your daily adherence.</p>
                  </div>
                  <Badge domain="health" size="sm">
                    {activeReminders.length} Active
                  </Badge>
                </div>

                {isLoading ? (
                  <div className="dm-skeleton-stack">
                    <Skeleton variant="card" height="80px" />
                    <Skeleton variant="card" height="80px" />
                  </div>
                ) : isError ? (
                  <div className="dm-error-box">
                    <p>Unable to load medicine reminders.</p>
                    <Button variant="outline" size="sm" onClick={() => refetch()}>
                      Retry
                    </Button>
                  </div>
                ) : reminders.length > 0 ? (
                  <div className="dm-reminders-list">
                    {reminders.map((reminder) => {
                      const isTaken = Boolean(takenIds[reminder.id])
                      const isSnoozed = Boolean(snoozedIds[reminder.id])
                      const isPaused = reminder.active === false
                      const timeStr = reminder.remindAt ? reminder.remindAt.slice(0, 5) : '08:00'

                      return (
                        <div
                          key={reminder.id}
                          className={`dm-medicine-card ${isTaken ? 'dm-medicine-card--taken' : ''} ${
                            isPaused ? 'dm-medicine-card--paused' : ''
                          }`}
                        >
                          <div className="dm-med-card-top">
                            <div className="dm-med-info">
                              <div className="dm-med-name-row">
                                <h3>{reminder.name}</h3>
                                {isPaused ? (
                                  <Badge variant="neutral" size="xs">Paused</Badge>
                                ) : isTaken ? (
                                  <Badge variant="success" size="xs">Taken Today ✓</Badge>
                                ) : isSnoozed ? (
                                  <Badge variant="warning" size="xs">Snoozed 15m</Badge>
                                ) : (
                                  <Badge domain="health" size="xs">Due Today</Badge>
                                )}
                              </div>
                              <p className="dm-med-details">
                                {reminder.dosage} · {reminder.frequency} · ⏰ {timeStr}
                              </p>
                              {reminder.notes && <p className="dm-med-notes">{reminder.notes}</p>}
                            </div>

                            {/* Main Action Buttons */}
                            <div className="dm-med-actions-primary">
                              {!isTaken && !isPaused && (
                                <>
                                  <Button
                                    variant="success"
                                    size="sm"
                                    onClick={() => handleTake(reminder.id)}
                                  >
                                    Take
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleSnooze(reminder.id)}
                                  >
                                    Snooze
                                  </Button>
                                </>
                              )}
                              {isTaken && (
                                <span className="dm-taken-badge">✓ Taken</span>
                              )}
                            </div>
                          </div>

                          {/* Secondary Utilities Row */}
                          <div className="dm-med-card-bottom">
                            <span className="dm-med-status-hint">
                              {isPaused
                                ? 'Reminder paused'
                                : `Scheduled daily at ${timeStr}`}
                            </span>
                            <div className="dm-med-util-buttons">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleTogglePause(reminder)}
                              >
                                {isPaused ? 'Resume' : 'Pause'}
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => openEditDialog(reminder)}
                              >
                                Edit
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="dm-btn-danger-hover"
                                onClick={() => deleteMutation.mutate(reminder.id)}
                                isLoading={
                                  deleteMutation.isPending && deleteMutation.variables === reminder.id
                                }
                              >
                                Delete
                              </Button>
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                ) : (
                  <div className="dm-empty-dashboard-box">
                    <p>No medicine reminders scheduled yet. Add one to get started.</p>
                  </div>
                )}

                {/* Server-Driven Pagination */}
                <Pagination
                  page={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  pageSize={pageSize}
                  onPageChange={setPage}
                  onPageSizeChange={setPageSize}
                  disabled={isLoading}
                />
              </Card>
            </div>

            {/* Right Column: Add New Medicine Routine Form */}
            <div className="dm-medicine-form-col">
              <Card className="dm-medicine-form-card">
                <div className="dm-form-card-header">
                  <h2>Schedule New Routine</h2>
                  <p>Add a medication, dosage, and alarm schedule.</p>
                </div>

                {formError && (
                  <div className="dm-form-alert dm-form-alert--error" role="alert">
                    <span>⚠️ {formError}</span>
                  </div>
                )}

                <form className="dm-medicine-form" onSubmit={handleSubmit}>
                  <Input
                    id="med-name"
                    name="name"
                    label="Medicine name"
                    required
                    placeholder="e.g. Vitamin D3, Metformin"
                    value={form.name}
                    onChange={handleChange}
                  />

                  <Input
                    id="med-dosage"
                    name="dosage"
                    label="Dosage"
                    required
                    placeholder="e.g. 500mg, 1 tablet, 2 drops"
                    value={form.dosage}
                    onChange={handleChange}
                  />

                  <div className="dm-form-field-group">
                    <Input
                      id="med-frequency"
                      name="frequency"
                      label="Frequency"
                      required
                      value={form.frequency}
                      onChange={handleChange}
                    />

                    <Input
                      id="med-time"
                      name="remindAt"
                      label="Reminder Time"
                      type="time"
                      required
                      value={form.remindAt}
                      onChange={handleChange}
                    />
                  </div>

                  <Input
                    id="med-notes"
                    name="notes"
                    label="Instructions / Notes (Optional)"
                    placeholder="e.g. Take with breakfast or after meal"
                    value={form.notes}
                    onChange={handleChange}
                  />

                  <div className="dm-form-checkbox-row">
                    <label className="dm-checkbox-label">
                      <input
                        type="checkbox"
                        name="active"
                        checked={form.active}
                        onChange={handleChange}
                      />
                      <span>Active reminder notifications</span>
                    </label>
                  </div>

                  <Button
                    type="submit"
                    variant="primary"
                    size="lg"
                    fullWidth
                    isLoading={createMutation.isPending}
                  >
                    {createMutation.isPending ? 'Scheduling…' : 'Add reminder'}
                  </Button>
                </form>
              </Card>
            </div>
          </div>
        </ResponsiveContainer>
      </div>

      {/* ===================================================================
          EDIT MEDICINE MODAL
         =================================================================== */}
      {editTarget && editForm && (
        <div className="dm-modal-backdrop" role="dialog" aria-modal="true">
          <div className="dm-modal dm-modal--md">
            <div className="dm-modal__header">
              <h2 className="dm-modal__title">Edit Medicine Reminder</h2>
              <button
                type="button"
                onClick={() => setEditTarget(null)}
                className="dm-modal__close"
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <div className="dm-modal__body">
              {editError && (
                <div className="dm-form-alert dm-form-alert--error" role="alert">
                  <span>⚠️ {editError}</span>
                </div>
              )}

              <form onSubmit={handleEditSubmit} className="dm-medicine-edit-modal-form">
                <Input
                  id="edit-med-name"
                  name="name"
                  label="Medicine name"
                  required
                  value={editForm.name}
                  onChange={(e) => {
                    const val = e.target.value
                    setEditForm((current) => ({ ...current, name: val }))
                  }}
                />

                <Input
                  id="edit-med-dosage"
                  name="dosage"
                  label="Dosage"
                  required
                  value={editForm.dosage}
                  onChange={(e) => {
                    const val = e.target.value
                    setEditForm((current) => ({ ...current, dosage: val }))
                  }}
                />

                <div className="dm-form-field-group">
                  <Input
                    id="edit-med-frequency"
                    name="frequency"
                    label="Frequency"
                    required
                    value={editForm.frequency}
                    onChange={(e) => {
                      const val = e.target.value
                      setEditForm((current) => ({ ...current, frequency: val }))
                    }}
                  />

                  <Input
                    id="edit-med-time"
                    name="remindAt"
                    label="Reminder Time"
                    type="time"
                    required
                    value={editForm.remindAt}
                    onChange={(e) => {
                      const val = e.target.value
                      setEditForm((current) => ({ ...current, remindAt: val }))
                    }}
                  />
                </div>

                <Input
                  id="edit-med-notes"
                  name="notes"
                  label="Instructions / Notes"
                  value={editForm.notes}
                  onChange={(e) => {
                    const val = e.target.value
                    setEditForm((current) => ({ ...current, notes: val }))
                  }}
                />

                <div className="dm-modal-actions-row">
                  <Button type="button" variant="ghost" onClick={() => setEditTarget(null)}>
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    isLoading={updateMutation.isPending}
                  >
                    Save changes
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </MainLayout>
  )
}
