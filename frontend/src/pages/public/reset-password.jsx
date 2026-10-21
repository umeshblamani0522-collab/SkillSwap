import React, { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { FaArrowLeft, FaArrowRight, FaEye, FaEyeSlash, FaGraduationCap, FaShieldAlt } from 'react-icons/fa'
import { authService } from '../../services/auth'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') || '', [searchParams])
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!token) {
      setError('This reset link is missing its security token.')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setLoading(true)
    try {
      await authService.resetPassword({ token, password })
      setStatus('success')
    } catch (err) {
      setError(err.response?.data?.message || 'This reset link is invalid or has expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-visual se-auth-image-bg">
        <Link to="/login" className="auth-back"><FaArrowLeft /> Back to sign in</Link>
        <div className="auth-visual-copy">
          <span className="eyebrow light"><span /> Secure account recovery</span>
          <h1>Create a new<br />password.</h1>
          <p>Choose a strong password to restore access to your SkillSwap learning workspace.</p>
          <div className="auth-visual-points">
            <span><FaShieldAlt /> BCrypt-protected storage</span>
            <span><FaShieldAlt /> One-time reset token</span>
          </div>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand-lockup brand-lockup">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span><strong>SkillSwap</strong><small>Peer learning network</small></span>
          </div>
          <span className="eyebrow muted">Password recovery</span>
          <h2>Set a new password</h2>
          <p className="auth-subtitle">Use this secure reset link to update your account password.</p>

          {error && <div className="se-alert danger">{error}</div>}
          {status === 'success' ? (
            <div>
              <div className="se-alert success">Your password has been changed successfully. You can now sign in with the new password.</div>
              <Link to="/login" className="se-btn se-btn-primary se-btn-full">Continue to sign in <FaArrowRight /></Link>
            </div>
          ) : (
            <form className="se-form auth-form" onSubmit={handleSubmit}>
              <label>
                <span>New password</span>
                <div className="password-field">
                  <input type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} autoComplete="new-password" />
                  <button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <FaEyeSlash /> : <FaEye />}</button>
                </div>
              </label>
              <label>
                <span>Confirm password</span>
                <div className="password-field">
                  <input type={showConfirm ? 'text' : 'password'} value={confirm} onChange={(e) => setConfirm(e.target.value)} required minLength={8} autoComplete="new-password" />
                  <button type="button" className="password-toggle" onClick={() => setShowConfirm(!showConfirm)} aria-label={showConfirm ? 'Hide password' : 'Show password'}>{showConfirm ? <FaEyeSlash /> : <FaEye />}</button>
                </div>
              </label>
              <div className="auth-form-meta"><span>Minimum 8 characters. The reset link can be used only once.</span></div>
              <button className="se-btn se-btn-primary se-btn-full" disabled={loading}>
                {loading ? 'Updating password...' : 'Update password'} {!loading && <FaArrowRight />}
              </button>
            </form>
          )}

          <div className="auth-trust">
            <span><FaShieldAlt /> Secure account recovery</span>
            <span><Link to="/login">Back to sign in</Link></span>
          </div>
        </div>
      </div>
    </div>
  )
}
