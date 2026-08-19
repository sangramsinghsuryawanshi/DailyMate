import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthContext } from './authContext'
import { configureApiAuth } from '../services/apiClient'

const STORAGE_KEY = 'dailymate.auth'

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'))

  useEffect(() => {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [session])

  useEffect(() => {
    configureApiAuth({
      getAccessToken: () => session?.accessToken ?? null,
      getRefreshToken: () => session?.refreshToken ?? null,
      onSessionUpdate: (nextSession) => setSession(nextSession),
      onSessionClear: () => {
        setSession(null)
        navigate('/login', { replace: true })
      },
    })
  }, [session, navigate])

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      accessToken: session?.accessToken ?? null,
      refreshToken: session?.refreshToken ?? null,
      signIn: setSession,
      signOut: () => setSession(null),
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
