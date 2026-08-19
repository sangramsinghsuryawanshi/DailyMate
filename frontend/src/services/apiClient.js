import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

let getAccessToken = () => null
let getRefreshToken = () => null
let onSessionUpdate = () => {}
let onSessionClear = () => {}

export function configureApiAuth({ getAccessToken: accessTokenGetter, getRefreshToken: refreshTokenGetter, onSessionUpdate: sessionUpdater, onSessionClear: sessionClearer }) {
  getAccessToken = accessTokenGetter
  getRefreshToken = refreshTokenGetter
  onSessionUpdate = sessionUpdater
  onSessionClear = sessionClearer
}

function isAuthEndpoint(url = '') {
  return ['/auth/login', '/auth/register', '/auth/refresh'].some((path) => url.includes(path))
}

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise = null

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (!originalRequest || error.response?.status !== 401 || originalRequest._retry || isAuthEndpoint(originalRequest.url)) {
      return Promise.reject(error)
    }

    originalRequest._retry = true
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      onSessionClear()
      return Promise.reject(error)
    }

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post(`${apiClient.defaults.baseURL}/auth/refresh`, { refreshToken }, { headers: { 'Content-Type': 'application/json' } })
          .finally(() => {
            refreshPromise = null
          })
      }

      const { data } = await refreshPromise
      onSessionUpdate(data)
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      onSessionClear()
      return Promise.reject(refreshError)
    }
  },
)

export default apiClient
