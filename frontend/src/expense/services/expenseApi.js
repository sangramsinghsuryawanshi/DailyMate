import apiClient from '../../services/apiClient'

export const getExpenses = () => apiClient.get('/expenses').then((response) => response.data)

export const createExpense = (payload) => apiClient.post('/expenses', payload).then((response) => response.data)

export const updateExpense = (id, payload) => apiClient.patch(`/expenses/${id}`, payload).then((response) => response.data)

export const deleteExpense = (id) => apiClient.delete(`/expenses/${id}`)
