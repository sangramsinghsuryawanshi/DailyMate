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
  const [renameTarget, setRenameTarget] = useState(null)
  const [renameDraft, setRenameDraft] = useState('')
  const [renameError, setRenameError] = useState('')
  const queryClient = useQueryClient()

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['medicine-reminders'],
    queryFn: getReminders,
  })

  const createMutation = useMutation({
    mutationFn: createReminder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      setForm(defaultForm)
    },
  })

  const toggleMutation = useMutation({
    mutationFn: ({ id, payload }) => updateReminder(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
    },
  })

  const renameMutation = useMutation({
    mutationFn: ({ id, name }) => updateReminder(id, { name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medicine-reminders'] })
      setRenameTarget(null)
      setRenameDraft('')
      setRenameError('')
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

  function handleSubmit(event) {
    event.preventDefault()
    createMutation.mutate(form)
  }

  function openRenameDialog(reminder) {
    setRenameTarget(reminder)
    setRenameDraft(reminder.name)
    setRenameError('')
  }

  function handleRenameSubmit(event) {
    event.preventDefault()
    const trimmed = renameDraft.trim()

    if (!trimmed) {
      setRenameError('Please enter a reminder name.')
      return
    }

    const duplicate = data.some(
      (item) => item.id !== renameTarget.id && item.name?.toLowerCase() === trimmed.toLowerCase(),
    )

    if (duplicate) {
      setRenameError('This reminder name is already in use.')
      return
    }

    renameMutation.mutate({ id: renameTarget.id, name: trimmed })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading reminders…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Medicine reminders unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

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

          <form className="medicine-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Medicine name" name="name" value={form.name} onChange={handleChange} required />
              <Input label="Dosage" name="dosage" value={form.dosage} onChange={handleChange} required />
            </div>

            <div className="split-fields">
              <Input label="Frequency" name="frequency" value={form.frequency} onChange={handleChange} />
              <Input label="Reminder time" type="time" name="remindAt" value={form.remindAt} onChange={handleChange} required />
            </div>

            <label className="field">
              <span>Notes</span>
              <textarea name="notes" value={form.notes} onChange={handleChange} rows="3" />
            </label>

            <label className="field">
              <span />
              <label style={{ display: 'flex', gap: '0.6rem', alignItems: 'center' }}>
                <input type="checkbox" name="active" checked={form.active} onChange={handleChange} />
                Keep this reminder active
              </label>
            </label>

            <div className="profile-actions">
              <Button type="submit" disabled={createMutation.isPending}>{createMutation.isPending ? 'Saving…' : 'Add reminder'}</Button>
            </div>
          </form>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Upcoming</h2>
          </div>

          {data.length === 0 ? (
            <div className="empty-state">
              <h3>No reminders yet</h3>
              <p className="muted">Add a reminder to see upcoming medicine times here.</p>
            </div>
          ) : (
            <div className="reminder-list">
              {data.map((reminder) => (
                <div key={reminder.id} className="reminder-item">
                  <div>
                    <strong>{reminder.name}</strong>
                    <div className="small-muted">{reminder.dosage} · {reminder.frequency} · {reminder.remindAt}</div>
                    {reminder.notes && <div className="small-muted">{reminder.notes}</div>}
                  </div>

                  <div className="reminder-actions">
                    <Button variant="secondary" onClick={() => openRenameDialog(reminder)}>Rename</Button>
                    <Button variant="secondary" onClick={() => toggleMutation.mutate({ id: reminder.id, payload: { ...reminder, active: !reminder.active } })}>{reminder.active ? 'Pause' : 'Resume'}</Button>
                    <Button variant="ghost" onClick={() => deleteMutation.mutate(reminder.id)}>Delete</Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {renameTarget && (
        <div className="rename-dialog-backdrop" onClick={() => { setRenameTarget(null); setRenameError('') }}>
          <div className="rename-dialog" onClick={(event) => event.stopPropagation()}>
            <div className="panel-header">
              <h2>Rename reminder</h2>
              <button type="button" className="icon-button" aria-label="Close" onClick={() => { setRenameTarget(null); setRenameError('') }}>×</button>
            </div>

            <form className="rename-form" onSubmit={handleRenameSubmit}>
              <Input
                label="New reminder name"
                value={renameDraft}
                onChange={(event) => {
                  setRenameDraft(event.target.value)
                  if (renameError) setRenameError('')
                }}
                maxLength={100}
                placeholder="Enter a new name"
              />

              {renameError && <p className="error">{renameError}</p>}

              <div className="profile-actions">
                <Button type="button" variant="ghost" onClick={() => { setRenameTarget(null); setRenameError('') }}>Cancel</Button>
                <Button type="submit" disabled={renameMutation.isPending}>
                  {renameMutation.isPending ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </MainLayout>
  )
}
