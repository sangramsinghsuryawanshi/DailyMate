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
  Skeleton,
  Pagination,
  EmptyState,
  ErrorState,
  ResponsiveContainer,
} from '../../design-system'
import { usePagination } from '../../hooks/usePagination'
import { formatINR } from '../../utils/formatters'
import { createExpense, deleteExpense, getExpenses, updateExpense } from '../services/expenseApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './ExpensePage.css'

export const formatCurrency = formatINR

const today = new Date().toISOString().slice(0, 10)
const defaultForm = {
  category: 'Groceries',
  description: '',
  amount: '',
  spentOn: today,
  notes: '',
}

const CATEGORIES = [
  'All',
  'Groceries',
  'Dining',
  'Utilities',
  'Rent',
  'Health',
  'Entertainment',
  'Travel',
  'Shopping',
  'Education',
  'Other',
]

export default function ExpensePage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState(defaultForm)
  const [editingId, setEditingId] = useState(null)
  const [formError, setFormError] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [monthlyBudget, setMonthlyBudget] = useState(25000)
  const [isFormOpen, setIsFormOpen] = useState(false)

  const { page, pageSize, setPage, setPageSize, resetPage } = usePagination({
    initialPage: 0,
    initialPageSize: 20,
    syncWithUrl: true,
  })

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'expenses' })
  }, [])

  const { data: pageData = { content: [], totalElements: 0, totalPages: 0 }, isLoading, isError, refetch } = useQuery({
    queryKey: ['expenses', { page, pageSize }],
    queryFn: () => getExpenses({ page, size: pageSize }),
    placeholderData: (previousData) => previousData,
  })

  const rawExpenses = useMemo(() => {
    if (Array.isArray(pageData)) return pageData
    return pageData.content ?? []
  }, [pageData])

  const totalElements = Array.isArray(pageData) ? pageData.length : (pageData.totalElements ?? rawExpenses.length)
  const totalPages = Array.isArray(pageData) ? 1 : (pageData.totalPages ?? 1)

  // Summary Metrics
  const total = useMemo(
    () => rawExpenses.reduce((sum, expense) => sum + Number(expense.amount || 0), 0),
    [rawExpenses]
  )

  const topCategory = useMemo(() => {
    if (!rawExpenses.length) return 'None'
    const counts = {}
    rawExpenses.forEach((expense) => {
      const cat = expense.category || 'Uncategorized'
      counts[cat] = (counts[cat] || 0) + 1
    })
    return Object.entries(counts).sort((a, b) => b[1] - a[1])[0][0]
  }, [rawExpenses])

  const averageTransaction = useMemo(() => {
    if (!rawExpenses.length) return 0
    return Math.round(total / rawExpenses.length)
  }, [rawExpenses, total])

  const budgetUsagePercent = useMemo(() => {
    if (!monthlyBudget || monthlyBudget <= 0) return 0
    return Math.min(Math.round((total / monthlyBudget) * 100), 100)
  }, [total, monthlyBudget])

  // Filtered expenses on current page
  const filteredExpenses = useMemo(() => {
    return rawExpenses.filter((item) => {
      const matchesCategory =
        selectedCategory === 'All' ||
        (item.category || '').toLowerCase() === selectedCategory.toLowerCase()
      const matchesSearch =
        !searchTerm.trim() ||
        (item.description || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (item.notes || '').toLowerCase().includes(searchTerm.toLowerCase())
      return matchesCategory && matchesSearch
    })
  }, [rawExpenses, selectedCategory, searchTerm])

  const saveMutation = useMutation({
    mutationFn: (payload) => (editingId ? updateExpense(editingId, payload) : createExpense(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] })
      setForm(defaultForm)
      setEditingId(null)
      setFormError('')
      setIsFormOpen(false)
      trackEvent('expense_saved', { isEdit: !!editingId })
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
      trackEvent('expense_deleted')
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

    if (!form.category?.trim() || !form.description?.trim() || !form.spentOn) {
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
    setForm({
      category: expense.category || 'Groceries',
      description: expense.description || '',
      amount: String(expense.amount || ''),
      spentOn: expense.spentOn ? expense.spentOn.slice(0, 10) : today,
      notes: expense.notes || '',
    })
    setFormError('')
    setIsFormOpen(true)
    if (typeof window !== 'undefined' && typeof window.scrollTo === 'function') {
      try {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      } catch (_) {}
    }
  }

  function handleCancelEdit() {
    setEditingId(null)
    setForm(defaultForm)
    setFormError('')
    setIsFormOpen(false)
  }

  function handleDelete(id) {
    deleteMutation.mutate(id)
  }

  return (
    <MainLayout>
      <div className="dm-expense-page">
        <ResponsiveContainer size="wide">
          {/* ===================================================================
              1. PAGE HEADER & STATS
             =================================================================== */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Personal Finance</span>
              <h1 className="dm-page-main-title">Expenses & Budgeting</h1>
              <p className="dm-page-subtitle">
                Track personal spending, inspect category breakdown, and manage monthly budgets.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/assistant?prompt=Review%20my%20recent%20spending%20and%20give%20me%20savings%20tips">
                <Button variant="outline" size="sm" iconLeft="✨">
                  Ask AI About Spending
                </Button>
              </Link>
              <Button
                variant="primary"
                size="sm"
                iconLeft="+"
                onClick={() => {
                  setEditingId(null)
                  setForm(defaultForm)
                  setIsFormOpen(true)
                }}
              >
                New Expense
              </Button>
            </div>
          </div>

          {/* Key Metrics Grid */}
          <div className="dm-expense-stats-grid">
            <StatCard
              domain="expense"
              label="Total Spending"
              value={formatINR(total)}
              trend={`${budgetUsagePercent}% of budget`}
              trendDirection={budgetUsagePercent > 90 ? 'down' : 'up'}
              trendLabel="Monthly Budget"
              icon="💰"
            />
            <StatCard
              domain="expense"
              label="Top Category"
              value={topCategory}
              trend="Frequent"
              trendDirection="neutral"
              trendLabel="Most logged items"
              icon="🏷️"
            />
            <StatCard
              domain="expense"
              label="Transactions:"
              value={String(totalElements)}
              trend="Recorded"
              trendDirection="neutral"
              trendLabel="All time entries"
              icon="📊"
            />
            <StatCard
              domain="expense"
              label="Average / Entry"
              value={formatINR(averageTransaction)}
              trend="Per transaction"
              trendDirection="neutral"
              trendLabel="Calculated average"
              icon="📈"
            />
          </div>

          {/* Monthly Budget Progress Bar */}
          <Card className="dm-budget-progress-card">
            <div className="dm-budget-card-header">
              <div>
                <strong>Monthly Budget Health</strong>
                <span>
                  Spent {formatINR(total)} of {formatINR(monthlyBudget)} limit ({budgetUsagePercent}%)
                </span>
              </div>
              <Badge variant={budgetUsagePercent > 90 ? 'danger' : budgetUsagePercent > 70 ? 'warning' : 'success'}>
                {budgetUsagePercent > 90 ? 'Caution' : budgetUsagePercent > 70 ? 'Moderate' : 'Healthy'}
              </Badge>
            </div>
            <div className="dm-budget-bar-track">
              <div
                className={`dm-budget-bar-fill ${
                  budgetUsagePercent > 90 ? 'danger' : budgetUsagePercent > 70 ? 'warning' : 'success'
                }`}
                style={{ width: `${budgetUsagePercent}%` }}
              />
            </div>
          </Card>

          {/* ===================================================================
              2. TWO-COLUMN WORKFLOW: LOG FORM & TRANSACTIONS TABLE
             =================================================================== */}
          <div className="dm-expense-layout-grid">
            {/* Left / Top Form Card */}
            <Card className="dm-expense-form-card">
              <div className="dm-form-card-header">
                <h2>{editingId ? 'Edit expense' : 'Add new expense'}</h2>
                <p>Record a daily expense with category, amount, and date.</p>
              </div>

              {formError && (
                <div className="dm-form-alert dm-form-alert--error" role="alert">
                  <span>⚠️ {formError}</span>
                </div>
              )}

              <form className="dm-expense-form" onSubmit={handleSubmit}>
                <div className="dm-form-field-group">
                  <Input
                    id="expense-category"
                    name="category"
                    label="Category"
                    required
                    placeholder="e.g. Groceries, Dining, Rent"
                    value={form.category}
                    onChange={handleChange}
                  />

                  <Input
                    id="expense-amount"
                    name="amount"
                    label="Amount (₹)"
                    type="number"
                    step="0.01"
                    required
                    placeholder="0.00"
                    value={form.amount}
                    onChange={handleChange}
                  />
                </div>

                <Input
                  id="expense-description"
                  name="description"
                  label="Description"
                  required
                  placeholder="What was this expense for?"
                  value={form.description}
                  onChange={handleChange}
                />

                <div className="dm-form-field-group">
                  <Input
                    id="expense-spent-on"
                    name="spentOn"
                    label="Date"
                    type="date"
                    required
                    value={form.spentOn}
                    onChange={handleChange}
                  />

                  <Input
                    id="expense-notes"
                    name="notes"
                    label="Notes (Optional)"
                    placeholder="Additional context or invoice ref"
                    value={form.notes}
                    onChange={handleChange}
                  />
                </div>

                <div className="dm-form-submit-row">
                  {editingId && (
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={handleCancelEdit}
                    >
                      Cancel
                    </Button>
                  )}
                  <Button
                    type="submit"
                    variant="primary"
                    size="lg"
                    fullWidth={!editingId}
                    isLoading={saveMutation.isPending}
                  >
                    {editingId
                      ? saveMutation.isPending
                        ? 'Updating…'
                        : 'Update expense'
                      : saveMutation.isPending
                      ? 'Adding…'
                      : 'Add expense'}
                  </Button>
                </div>
              </form>
            </Card>

            {/* Right / Bottom Transactions List */}
            <Card className="dm-expense-list-card">
              <div className="dm-list-header">
                <div>
                  <h2>Transaction Records</h2>
                  <p>Browse, filter, and inspect your logged transactions.</p>
                </div>
                <span className="dm-badge dm-badge--default dm-badge--sm">
                  {filteredExpenses.length} record{filteredExpenses.length === 1 ? '' : 's'}
                </span>
              </div>

              {/* Category Filter Pills & Search */}
              <div className="dm-filter-toolbar">
                <input
                  type="search"
                  className="dm-search-filter-input"
                  placeholder="Search descriptions or notes..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  aria-label="Search expenses"
                />

                <div className="dm-category-pills">
                  {CATEGORIES.map((cat) => (
                    <button
                      key={cat}
                      type="button"
                      className={`dm-category-pill ${selectedCategory === cat ? 'active' : ''}`}
                      onClick={() => {
                        setSelectedCategory(cat)
                        trackEvent('expense_filter_changed', { category: cat })
                      }}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
              </div>

              {/* Transactions Table / List */}
              {isLoading ? (
                <div className="dm-skeleton-stack">
                  <Skeleton variant="card" height="64px" />
                  <Skeleton variant="card" height="64px" />
                  <Skeleton variant="card" height="64px" />
                </div>
              ) : isError ? (
                <div className="dm-error-box">
                  <p>Unable to load expenses. Please check your connection.</p>
                  <Button variant="outline" size="sm" onClick={() => refetch()}>
                    Retry
                  </Button>
                </div>
              ) : filteredExpenses.length > 0 ? (
                <div className="dm-transactions-table-wrapper">
                  <table className="dm-transactions-table">
                    <thead>
                      <tr>
                        <th>Details</th>
                        <th>Category</th>
                        <th>Date</th>
                        <th className="text-right">Amount</th>
                        <th className="text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredExpenses.map((expense) => (
                        <tr key={expense.id}>
                          <td>
                            <div className="dm-tx-desc-wrapper">
                              <strong className="dm-tx-desc">{expense.description}</strong>
                              {expense.notes && <span className="dm-tx-notes">{expense.notes}</span>}
                            </div>
                          </td>
                          <td>
                            <Badge domain="expense" size="sm">
                              {expense.category || 'General'}
                            </Badge>
                          </td>
                          <td className="dm-tx-date">
                            {expense.spentOn ? expense.spentOn.slice(0, 10) : today}
                          </td>
                          <td className="dm-tx-amount text-right">
                            <strong>{formatINR(expense.amount)}</strong>
                          </td>
                          <td className="text-right">
                            <div className="dm-tx-actions">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleEdit(expense)}
                              >
                                Edit
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="dm-btn-danger-hover"
                                onClick={() => handleDelete(expense.id)}
                                isLoading={deleteMutation.isPending && deleteMutation.variables === expense.id}
                              >
                                Delete
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="dm-empty-state-container">
                  <p className="dm-empty-text">
                    {searchTerm || selectedCategory !== 'All'
                      ? 'No expenses match your search or filter.'
                      : 'No expenses recorded yet. Add an expense to get started.'}
                  </p>
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
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
