import apiClient from '../../services/apiClient'

export const getReminders = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/medicine-reminders', { params: query }).then((response) => response.data)
}

export const createReminder = (payload) => apiClient.post('/medicine-reminders', payload).then((response) => response.data)

export const updateReminder = (id, payload) => apiClient.patch(`/medicine-reminders/${id}`, payload).then((response) => response.data)

export const deleteReminder = (id) => apiClient.delete(`/medicine-reminders/${id}`)
