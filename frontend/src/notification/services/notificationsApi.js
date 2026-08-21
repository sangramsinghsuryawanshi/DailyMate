import apiClient from '../../services/apiClient'

export const getNotifications = (page = 0, size = 20) => apiClient.get(`/notifications?page=${page}&size=${size}`).then((response) => response.data)

export const markAllRead = () => apiClient.post('/notifications/mark-all-read')

export const createNotification = (payload) => apiClient.post('/notifications', payload).then((response) => response.data)

export const updateNotification = (id, payload) => apiClient.patch(`/notifications/${id}`, payload).then((response) => response.data)

export const deleteNotification = (id) => apiClient.delete(`/notifications/${id}`)

