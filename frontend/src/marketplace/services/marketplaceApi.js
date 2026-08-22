import apiClient from '../../services/apiClient'

export const getProviders = () =>
  apiClient.get('/marketplace/providers').then((response) => response.data)

export const getProvider = (id) =>
  apiClient.get(`/marketplace/providers/${id}`).then((response) => response.data)

export const createProvider = (data) =>
  apiClient.post('/marketplace/providers', data).then((response) => response.data)

export const updateProvider = (id, data) =>
  apiClient.patch(`/marketplace/providers/${id}`, data).then((response) => response.data)

export const deleteProvider = (id) =>
  apiClient.delete(`/marketplace/providers/${id}`).then((response) => response.data)
