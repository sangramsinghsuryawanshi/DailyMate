import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../routes/ProtectedRoute'
import { AuthContext } from '../context/authContext'

function renderProtectedRoute(accessToken) {
  return render(
    <AuthContext.Provider value={{ accessToken, user: accessToken ? { firstName: 'Daily' } : null, signIn: vi.fn(), signOut: vi.fn() }}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<div>Dashboard content</div>} />
          </Route>
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ProtectedRoute', () => {
  it('renders protected content when authenticated', () => {
    renderProtectedRoute('token')
    expect(screen.getByText('Dashboard content')).toBeInTheDocument()
  })

  it('redirects to login when unauthenticated', () => {
    renderProtectedRoute(null)
    expect(screen.getByText('Login page')).toBeInTheDocument()
  })
})
