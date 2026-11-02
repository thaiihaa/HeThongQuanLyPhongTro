import { createContext } from 'react'
import type { User } from './api'

export type RegisterValues = {
  username: string
  email: string
  phone: string
  password: string
  confirmPassword: string
  fullName: string
}

export type AuthState = {
  token: string | null
  user: User | null
  status: 'checking' | 'anonymous' | 'authenticated'
}

export type AuthContextValue = AuthState & {
  login: (login: string, password: string) => Promise<User>
  register: (values: RegisterValues) => Promise<void>
  changePassword: (currentPassword: string, newPassword: string, confirmPassword: string) => Promise<void>
  logout: () => Promise<void>
}

export const storageKey = 'qlpt.session-token'
export const AuthContext = createContext<AuthContextValue | null>(null)
