import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { authService } from '../services/auth'
import { userService } from '../services/users'
import { disconnectSocketClient } from '../websocket/socket'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const loadProfile = useCallback(async () => {
    const token = localStorage.getItem('se_token')
    if (!token) {
      setLoading(false)
      return
    }
    try {
      const profile = await userService.me()
      setUser(profile)
    } catch (err) {
      localStorage.removeItem('se_token')
      localStorage.removeItem('se_user')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadProfile()
  }, [loadProfile])

  const login = async (payload) => {
    const data = await authService.login(payload)
    localStorage.setItem('se_token', data.token)
    localStorage.setItem('se_user', JSON.stringify(data))
    await loadProfile()
    return data
  }

  const register = async (payload) => {
    const data = await authService.register(payload)
    // Registration intentionally does not authenticate the user until email verification.
    localStorage.removeItem('se_token')
    localStorage.removeItem('se_user')
    setUser(null)
    return data
  }

  const logout = () => {
    disconnectSocketClient()
    localStorage.removeItem('se_token')
    localStorage.removeItem('se_user')
    setUser(null)
    window.location.href = '/login'
  }

  const refreshProfile = async () => {
    const profile = await userService.me()
    setUser(profile)
    return profile
  }

  const isAdmin = () => {
    const raw = localStorage.getItem('se_user')
    if (!raw) return false
    try {
      const parsed = JSON.parse(raw)
      return (parsed.roles || []).includes('ADMIN')
    } catch {
      return false
    }
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshProfile, isAdmin, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
