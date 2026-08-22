import apiClient from '../../services/apiClient'

export const getGroceryItems = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.search) queryParams.append('search', params.search)
  if (params.category && params.category !== 'ALL') queryParams.append('category', params.category)
  if (params.store && params.store !== 'ALL') queryParams.append('store', params.store)
  const qs = queryParams.toString()
  return apiClient.get(`/grocery/items${qs ? `?${qs}` : ''}`).then((response) => response.data)
}

export const getMyGroceryItems = () => apiClient.get('/grocery/my-items').then((response) => response.data)

export const createGroceryItem = (payload) => apiClient.post('/grocery/items', payload).then((response) => response.data)

export const updateGroceryItem = (id, payload) => apiClient.patch(`/grocery/items/${id}`, payload).then((response) => response.data)

export const deleteGroceryItem = (id) => apiClient.delete(`/grocery/items/${id}`)
