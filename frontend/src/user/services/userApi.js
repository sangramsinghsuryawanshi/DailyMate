import apiClient from '../../services/apiClient'

export const getProfile = () => apiClient.get('/users/me').then((response) => response.data)

export const updateProfile = (payload) => apiClient.patch('/users/me', payload).then((response) => response.data)
