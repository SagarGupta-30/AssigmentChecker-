import { createContext, useContext, useMemo, useState } from 'react'
import apiClient from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  })

  const setAuth = (nextToken, nextUser) => {
    setToken(nextToken)
    setUser(nextUser)
    localStorage.setItem('token', nextToken)
    localStorage.setItem('user', JSON.stringify(nextUser))
  }

  const clearAuth = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const login = async (payload) => {
    const response = await apiClient.post('/api/auth/login', payload)
    setAuth(response.data.token, response.data.user)
    return response.data
  }

  const register = async (payload) => {
    const response = await apiClient.post('/api/auth/register', payload)
    setAuth(response.data.token, response.data.user)
    return response.data
  }

  const value = useMemo(() => ({
    token,
    user,
    login,
    register,
    logout: clearAuth,
    isAuthenticated: Boolean(token)
  }), [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
