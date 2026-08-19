import { describe, expect, it, vi, beforeEach } from 'vitest'

const { axiosInstance, axiosPostMock } = vi.hoisted(() => {
  const axiosInstance = Object.assign(vi.fn(() => Promise.resolve({ data: { ok: true } })), {
    defaults: { baseURL: 'http://localhost:8080/api/v1' },
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  })

  return {
    axiosInstance,
    axiosPostMock: vi.fn(),
  }
})

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => axiosInstance),
    post: axiosPostMock,
  },
}))

import { configureApiAuth } from './apiClient'

describe('apiClient auth interceptor', () => {
  beforeEach(() => {
    axiosPostMock.mockReset()
    axiosInstance.mockClear()
  })

  it('attaches the access token to outgoing requests', () => {
    configureApiAuth({
      getAccessToken: () => 'access-token',
      getRefreshToken: () => 'refresh-token',
      onSessionUpdate: vi.fn(),
      onSessionClear: vi.fn(),
    })

    const requestHandler = axiosInstance.interceptors.request.use.mock.calls.at(-1)[0]
    const config = requestHandler({ headers: {} })
    expect(config.headers.Authorization).toBe('Bearer access-token')
  })

  it('refreshes the session after a 401 response', async () => {
    const onSessionUpdate = vi.fn()
    const onSessionClear = vi.fn()

    configureApiAuth({
      getAccessToken: () => 'expired-token',
      getRefreshToken: () => 'refresh-token',
      onSessionUpdate,
      onSessionClear,
    })

    axiosPostMock.mockResolvedValueOnce({
      data: {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
      },
    })

    const responseHandler = axiosInstance.interceptors.response.use.mock.calls.at(-1)[1]
    const error = {
      config: { url: '/users/me', headers: {} },
      response: { status: 401 },
    }

    await responseHandler(error)

    expect(axiosPostMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/auth/refresh',
      { refreshToken: 'refresh-token' },
      { headers: { 'Content-Type': 'application/json' } },
    )
    expect(onSessionUpdate).toHaveBeenCalledWith({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
    })
    expect(onSessionClear).not.toHaveBeenCalled()
  })
})
