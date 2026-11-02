import { useEffect, useState, type ReactNode } from 'react'
import { apiRequest, type AuthResponse, type User } from './api'
import { AuthContext, storageKey, type AuthState, type RegisterValues } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(() => {
    const token = sessionStorage.getItem(storageKey)
    return { token, user: null, status: token ? 'checking' : 'anonymous' }
  })

  useEffect(() => {
    if (!auth.token || auth.user) return
    let active = true

    void apiRequest<User>('/auth/me', {}, auth.token)
      .then((user) => {
        if (active) setAuth({ token: auth.token, user, status: 'authenticated' })
      })
      .catch(() => {
        if (active) {
          sessionStorage.removeItem(storageKey)
          setAuth({ token: null, user: null, status: 'anonymous' })
        }
      })

    return () => { active = false }
  }, [auth.token, auth.user])

  useEffect(() => {
    function handleInvalidSession() {
      sessionStorage.removeItem(storageKey)
      setAuth({ token: null, user: null, status: 'anonymous' })
    }

    window.addEventListener('qlpt:session-invalid', handleInvalidSession)
    return () => window.removeEventListener('qlpt:session-invalid', handleInvalidSession)
  }, [])

  function acceptSession(response: AuthResponse) {
    sessionStorage.setItem(storageKey, response.accessToken)
    setAuth({ token: response.accessToken, user: response.user, status: 'authenticated' })
  }

  async function login(loginValue: string, password: string): Promise<User> {
    const response = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ login: loginValue, password }),
    })
    acceptSession(response)
    return response.user
  }

  async function register(values: RegisterValues) {
    const response = await apiRequest<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ ...values, email: values.email || null, phone: values.phone || null }),
    })
    acceptSession(response)
  }

  async function changePassword(currentPassword: string, newPassword: string, confirmPassword: string) {
    if (!auth.token) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    await apiRequest<{ code: string; message: string }>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
    }, auth.token)
    sessionStorage.removeItem(storageKey)
    setAuth({ token: null, user: null, status: 'anonymous' })
  }

  async function logout() {
    const token = auth.token
    try {
      if (token) await apiRequest<{ code: string; message: string }>('/auth/logout', { method: 'POST' }, token)
    } finally {
      sessionStorage.removeItem(storageKey)
      setAuth({ token: null, user: null, status: 'anonymous' })
    }
  }

  return (
    <AuthContext.Provider value={{ ...auth, login, register, changePassword, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
