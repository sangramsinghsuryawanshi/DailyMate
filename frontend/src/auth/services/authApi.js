import apiClient from '../../services/apiClient'

export const register = (payload) => apiClient.post('/auth/register', payload).then((response) => response.data)

export const login = (payload) => apiClient.post('/auth/login', payload).then((response) => response.data)

export const refresh = (refreshToken) => apiClient.post('/auth/refresh', { refreshToken }).then((response) => response.data)

export const logout = (refreshToken) => apiClient.post('/auth/logout', { refreshToken })
