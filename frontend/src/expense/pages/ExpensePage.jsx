import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../components/Button'
import Input from '../../components/Input'
import MainLayout from '../../layouts/MainLayout'
import { createExpense, deleteExpense, getExpenses, updateExpense } from '../services/expenseApi'

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

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['expenses'],
    queryFn: getExpenses,
  })

  const total = useMemo(
    () => data.reduce((sum, expense) => sum + Number(expense.amount || 0), 0),
    [data],
  )

  const saveMutation = useMutation({
    mutationFn: (payload) => editingId ? updateExpense(editingId, payload) : createExpense(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
      setForm(defaultForm)
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteExpense,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['expenses'] }),
  })

  function handleChange(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    saveMutation.mutate({
      ...form,
      amount: Number(form.amount),
    })
  }

  function handleEdit(expense) {
    setEditingId(expense.id)
    setForm({
      category: expense.category,
      description: expense.description,
      amount: String(expense.amount),
      spentOn: expense.spentOn,
      notes: expense.notes || '',
    })
  }

  if (isLoading) return (
    <MainLayout>
      <main className="page-state"><h1>Loading expenses…</h1></main>
    </MainLayout>
  )

  if (isError) return (
    <MainLayout>
      <main className="page-state"><h1>Expenses unavailable</h1><Link to="/dashboard" className="btn btn-primary">Back to dashboard</Link></main>
    </MainLayout>
  )

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

          <form className="expense-form" onSubmit={handleSubmit}>
            <div className="split-fields">
              <Input label="Category" name="category" value={form.category} onChange={handleChange} required />
              <Input label="Amount" type="number" step="0.01" min="0.01" name="amount" value={form.amount} onChange={handleChange} required />
            </div>

            <Input label="Description" name="description" value={form.description} onChange={handleChange} required />

            <div className="split-fields">
              <Input label="Date" type="date" name="spentOn" value={form.spentOn} onChange={handleChange} required />
              <Input label="Notes" name="notes" value={form.notes} onChange={handleChange} />
            </div>

            <div className="profile-actions">
              <Button type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? 'Saving…' : (editingId ? 'Update expense' : 'Add expense')}</Button>
              {editingId && <Button variant="ghost" onClick={() => { setEditingId(null); setForm(defaultForm) }}>Cancel</Button>}
            </div>
          </form>
        </div>

        <aside className="panel summary-panel">
          <div className="panel-header">
            <h2>Summary</h2>
          </div>
          <div className="detail-list">
            <li><strong>Total this month:</strong> ${total.toFixed(2)}</li>
            <li><strong>Transactions:</strong> {data.length}</li>
            <li><strong>Major category:</strong> Groceries</li>
          </div>

          <div style={{ marginTop: '1rem' }}>
            <h3>Recent</h3>
            <div className="transaction-list">
              {data.map((expense) => (
                <div key={expense.id} className="transaction-item">
                  <div>
                    <strong>{expense.description || expense.category}</strong>
                    <div className="small-muted">{expense.category} · {expense.spentOn}</div>
                  </div>
                  <div className="transaction-actions">
                    <div><strong>${Number(expense.amount).toFixed(2)}</strong></div>
                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.4rem' }}>
                      <Button variant="secondary" onClick={() => handleEdit(expense)}>Edit</Button>
                      <Button variant="ghost" onClick={() => deleteMutation.mutate(expense.id)}>Delete</Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </aside>
      </section>

    </MainLayout>
  )
}
