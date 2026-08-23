import axios from 'axios'

const apiBaseUrl =
  import.meta.env.VITE_API_URL ??
  `http://${window.location.hostname}:8080/api/v1`

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

let getAccessToken = () => null
let getRefreshToken = () => null
let onSessionUpdate = () => { }
let onSessionClear = () => { }

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
    config.headers = config.headers || {}
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
      // after session update, getAccessToken should return the new token
      const newToken = getAccessToken()
      if (newToken) {
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newToken}`
      }
      return apiClient(originalRequest)
    } catch (refreshError) {
      onSessionClear()
      return Promise.reject(refreshError)
    }
  },
)

export default apiClient
