import apiClient from '../../services/apiClient'

export const getGroceryItems = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/grocery/items', { params: query }).then((response) => response.data)
}

export const getMyGroceryItems = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/grocery/my-items', { params: query }).then((response) => response.data)
}

export const createGroceryItem = (payload) => apiClient.post('/grocery/items', payload).then((response) => response.data)

export const updateGroceryItem = (id, payload) => apiClient.patch(`/grocery/items/${id}`, payload).then((response) => response.data)

export const deleteGroceryItem = (id) => apiClient.delete(`/grocery/items/${id}`)
