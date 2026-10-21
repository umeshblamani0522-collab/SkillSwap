import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/auth'

export default function ProtectedRoute({ children, adminOnly = false, userOnly = false }) {
  const { isAuthenticated, loading, isAdmin } = useAuth()

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '100vh' }}>
        <div className="spinner-border text-primary" role="status" />
      </div>
    )
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin()) return <Navigate to="/dashboard" replace />
  if (userOnly && isAdmin()) return <Navigate to="/admin" replace />
  return children
}
