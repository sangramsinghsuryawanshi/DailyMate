import apiClient from '../../services/apiClient'

export const getProviders = () => apiClient.get('/marketplace/providers').then((response) => response.data)
