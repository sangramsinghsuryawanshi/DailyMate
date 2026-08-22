import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { formatINR } from '../../utils/formatters'
import { createExpense, deleteExpense, getExpenses, updateExpense } from '../services/expenseApi'

export const formatCurrency = formatINR

const today = new Date().toISOString().slice(0, 10)
const defaultForm = {
  category: 'Groceries',
  description: '',
  amount: '',
  spentOn: today,
  notes: '',
}

export default function ExpensePage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [formError, setFormError] = useState('')

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['expenses'],
    queryFn: getExpenses,
  })

  const total = useMemo(
    () => data.reduce((sum, expense) => sum + Number(expense.amount || 0), 0),
    [data],
  )

  const topCategory = useMemo(() => {
    if (!data.length) return 'None'
    const counts = {}
    data.forEach((expense) => {
      const cat = expense.category || 'Uncategorized'
      counts[cat] = (counts[cat] || 0) + 1
    })
    return Object.entries(counts).sort((a, b) => b[1] - a[1])[0][0]
  }, [data])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateExpense(editingId, payload) : createExpense(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
    },
    onError: (err) => {
      setFormError(err.response?.data?.detail || err.response?.data?.message || 'Failed to save expense.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteExpense,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
      if (editingId) {
        setEditingId(null)
        setForm(defaultForm)
      }
    },
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    const numAmount = Number(form.amount)
    if (isNaN(numAmount) || numAmount <= 0) {
      setFormError('Amount must be greater than zero.')
      return
    }

    if (!form.category.trim() || !form.description.trim() || !form.spentOn) {
      setFormError('Please fill out all required fields.')
      return
    }

    saveMutation.mutate({
      category: form.category.trim(),
      description: form.description.trim(),
      amount: numAmount,
      spentOn: form.spentOn,
      notes: form.notes?.trim() || null,
    })
  }

  function handleEdit(expense) {
    setEditingId(expense.id)
    setFormError('')
    setForm({
      category: expense.category,
      description: expense.description,
      amount: String(expense.amount),
      spentOn: expense.spentOn,
      notes: expense.notes || '',
    })
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
  }

  if (isLoading) {
    return (
      <MainLayout>
        <main className="page-state"><h1>Loading expenses…</h1></main>
      </MainLayout>
    )
  }

  if (isError) {
    return (
      <MainLayout>
        <main className="page-state">
          <h1>Expenses unavailable</h1>
          <Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link>
        </main>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <section className="page-cover">
        <div>
          <p className="eyebrow">Finances</p>
          <h1>Expenses</h1>
          <p className="subtle-text">Track spending and see a quick summary of recent transactions.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost">Back</Link>
      </section>

      <section className="expense-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>{editingId ? 'Edit expense' : 'Add expense'}</h2>
          </div>

          {formError && (
            <div style={{ color: '#ef4444', marginBottom: '1rem', padding: '0.5rem', background: '#fee2e2', borderRadius: '4px' }}>
              {formError}
            </div>
          )}

          <form className="expense-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Category" name="category" value={form.category} onChange={handleChange} maxLength={40} required />
              <Input label="Amount (₹)" type="number" step="0.01" min="0.01" name="amount" value={form.amount} onChange={handleChange} required />
            </div>

            <Input label="Description" name="description" value={form.description} onChange={handleChange} maxLength={120} required />

            <div className="split-fields">
              <Input label="Date" type="date" name="spentOn" value={form.spentOn} onChange={handleChange} required />
              <Input label="Notes" name="notes" value={form.notes} onChange={handleChange} maxLength={500} />
            </div>

            <div className="profile-actions" style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : (editingId ? 'Update expense' : 'Add expense')}
              </Button>
              {editingId && (
                <Button type="button" variant="ghost" onClick={handleCancelEdit}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Summary</h2>
          </div>
          <ul className="detail-list">
            <li><strong>Total tracked:</strong> {formatCurrency(total)}</li>
            <li><strong>Transactions:</strong> {data.length}</li>
            <li><strong>Top category:</strong> {topCategory}</li>
          </ul>

          <div style={{ marginTop: '1.5rem' }}>
            <h3>Recent Transactions</h3>
            {data.length === 0 ? (
              <div className="empty-state" style={{ marginTop: '1rem' }}>
                <p className="muted">No expenses recorded yet. Add an expense to get started.</p>
              </div>
            ) : (
              <div className="transaction-list" style={{ marginTop: '0.75rem' }}>
                {data.map((expense) => (
                  <div key={expense.id} className="transaction-item">
                    <div>
                      <strong>{expense.description || expense.category}</strong>
                      <div className="small-muted">{expense.category} · {expense.spentOn}</div>
                      {expense.notes && <div className="small-muted">{expense.notes}</div>}
                    </div>
                    <div className="transaction-actions">
                      <div style={{ fontWeight: '600', textAlign: 'right' }}>{formatCurrency(expense.amount)}</div>
                      <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.4rem', justifyContent: 'flex-end' }}>
                        <Button variant="secondary" onClick={() => handleEdit(expense)}>Edit</Button>
                        <Button variant="ghost" onClick={() => deleteMutation.mutate(expense.id)}>Delete</Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </aside>
      </section>
    </MainLayout>
  )
}
