import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createReminder, deleteReminder, getReminders, updateReminder } from '../services/medicineApi'

const defaultForm = {
  name: '',
  dosage: '',
  frequency: 'Daily',
  remindAt: '08:00',
  notes: '',
  active: true,
}

export default function MedicinePage() {
  const [form, setForm] = useState(defaultForm)
  const [editTarget, setEditTarget] = useState(null)
  const [editForm, setEditForm] = useState(null)
  const [formError, setFormError] = useState('')
  const [editError, setEditError] = useState('')
  const queryClient = useQueryClient()

  const { data: reminders = [], isLoading, isError } = useQuery({
    queryKey: ['medicine-reminders'],
    queryFn: getReminders,
  })

  const createMutation = useMutation({
    mutationFn: createReminder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      setForm(defaultForm)
      setFormError('')
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
    },
    onError: (err) => {
      setEditError(err.response?.data?.detail || err.response?.data?.message || 'Failed to update reminder.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteReminder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
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
      notes: form.notes.trim() || null,
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
      name: reminder.name || '',
      dosage: reminder.dosage || '',
      frequency: reminder.frequency || 'Daily',
      remindAt: reminder.remindAt ? reminder.remindAt.slice(0, 5) : '08:00',
      notes: reminder.notes || '',
      active: reminder.active ?? true,
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
      notes: editForm.notes.trim() || null,
      active: editForm.active,
    }

    if (!payload.name || !payload.dosage || !payload.frequency || !payload.remindAt) {
      setEditError('Please fill out all required fields.')
      return
    }

    updateMutation.mutate({ id: editTarget.id, payload })
  }

  function handleToggleActive(reminder) {
    const payload = {
      name: reminder.name,
      dosage: reminder.dosage,
      frequency: reminder.frequency,
      remindAt: reminder.remindAt ? reminder.remindAt.slice(0, 5) : '08:00',
      notes: reminder.notes,
      active: !reminder.active,
    }
    updateMutation.mutate({ id: reminder.id, payload })
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading reminders…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Medicine reminders unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Health</p>
          <h1>Medicines & reminders</h1>
          <p className="subtle-text">Keep track of medications, set reminders, and stay on schedule.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="medicine-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>Add reminder</h2>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form className="medicine-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Medicine name" name="name" value={form.name} onChange={handleChange} maxLength={80} required />
              <Input label="Dosage" name="dosage" value={form.dosage} onChange={handleChange} maxLength={80} required />
            </div>

            <div className="split-fields">
              <Input label="Frequency" name="frequency" value={form.frequency} onChange={handleChange} maxLength={40} required />
              <Input label="Reminder time" type="time" name="remindAt" value={form.remindAt} onChange={handleChange} required />
            </div>

            <label className="field">
              <span>Notes</span>
              <textarea name="notes" value={form.notes} onChange={handleChange} rows="3" maxLength={500} placeholder="Special instructions, e.g. with food" />
            </label>

            <label className="field">
              <span />
              <label style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', cursor: 'pointer' }}>
                <input type="checkbox" name="active" checked={form.active} onChange={handleChange} />
                Keep this reminder active
              </label>
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Saving…' : 'Add reminder'}
              </Button>
            </div>
          </form>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Upcoming Reminders</h2>
            <span>{reminders.length} scheduled</span>
          </div>

          {reminders.length === 0 ? (
            <div className="empty-state">
              <h3>No reminders yet</h3>
              <p className="muted">Add a medication reminder on the left to organize your daily schedule.</p>
            </div>
          ) : (
            <div className="reminder-list">
              {reminders.map((reminder) => (
                <div key={reminder.id} className="reminder-item" style={{ opacity: reminder.active ? 1 : 0.6 }}>
                  <div>
                    <strong>{reminder.name}</strong>
                    {!reminder.active && <span className="status-pill" style={{ marginLeft: '0.5rem', fontSize: '0.75rem' }}>Paused</span>}
                    <div className="small-muted">{reminder.dosage} · {reminder.frequency} · ⏰ {reminder.remindAt?.slice(0, 5)}</div>
                    {reminder.notes && <div className="small-muted">{reminder.notes}</div>}
                  </div>

                  <div className="reminder-actions">
                    <Button variant="secondary" onClick={() => openEditDialog(reminder)}>Edit</Button>
                    <Button variant="secondary" onClick={() => handleToggleActive(reminder)}>
                      {reminder.active ? 'Pause' : 'Resume'}
                    </Button>
                    <Button variant="ghost" onClick={() => deleteMutation.mutate(reminder.id)}>Delete</Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {editTarget && editForm && (
        <div className="rename-dialog-backdrop" onClick={() => { setEditTarget(null); setEditForm(null); setEditError('') }}>
          <div className="rename-dialog" style={{ maxWidth: '560px' }} onClick={(event) => event.stopPropagation()}>
            <div className="panel-header">
              <h2>Edit Medicine Reminder</h2>
              <button type="button" className="icon-button" aria-label="Close" onClick={() => { setEditTarget(null); setEditForm(null); setEditError('') }}>×</button>
            </div>

            {editError && (
              <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
                {editError}
              </div>
            )}

            <form className="medicine-form" onSubmit={handleEditSubmit} style={{ marginTop: '1rem' }}>
              <div className="split-fields">
                <Input label="Medicine name" name="name" value={editForm.name} onChange={handleEditChange} maxLength={80} required />
                <Input label="Dosage" name="dosage" value={editForm.dosage} onChange={handleEditChange} maxLength={80} required />
              </div>

              <div className="split-fields">
                <Input label="Frequency" name="frequency" value={editForm.frequency} onChange={handleEditChange} maxLength={40} required />
                <Input label="Reminder time" type="time" name="remindAt" value={editForm.remindAt} onChange={handleEditChange} required />
              </div>

              <label className="field">
                <span>Notes</span>
                <textarea name="notes" value={editForm.notes} onChange={handleEditChange} rows="3" maxLength={500} />
              </label>

              <label className="field">
                <span />
                <label style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', cursor: 'pointer' }}>
                  <input type="checkbox" name="active" checked={editForm.active} onChange={handleEditChange} />
                  Reminder active
                </label>
              </label>

              <div className="profile-actions" style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <Button type="button" variant="ghost" onClick={() => { setEditTarget(null); setEditForm(null); setEditError('') }}>Cancel</Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </MainLayout>
  )
}
