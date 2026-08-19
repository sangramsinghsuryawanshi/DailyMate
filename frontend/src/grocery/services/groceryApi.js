import apiClient from '../../services/apiClient'

export const getGroceryItems = () => apiClient.get('/grocery/items').then((response) => response.data)

export const createGroceryItem = (payload) => apiClient.post('/grocery/items', payload).then((response) => response.data)

export const updateGroceryItem = (id, payload) => apiClient.patch(`/grocery/items/${id}`, payload).then((response) => response.data)

export const deleteGroceryItem = (id) => apiClient.delete(`/grocery/items/${id}`)
