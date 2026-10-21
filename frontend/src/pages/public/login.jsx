import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FaArrowLeft, FaArrowRight, FaGraduationCap, FaShieldAlt, FaEye, FaEyeSlash } from 'react-icons/fa'
import { useAuth } from '../../context/auth'

export default function Login() {
  const { login, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form)
      navigate(isAdmin() ? '/admin' : '/dashboard')
    } catch (err) {
      const data = err.response?.data
      const message = data?.message || 'Login failed. Please check your credentials.'
      const email = data?.validationErrors?.email || (form.usernameOrEmail.includes('@') ? form.usernameOrEmail.trim() : '')

      if (err.response?.status === 422 && message.toLowerCase().includes('verify your email')) {
        navigate('/verify-email', {
          state: {
            email,
            message: 'Your account is created, but the email address has not been verified yet. Open the verification email to continue.'
          }
        })
        return
      }

      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-visual se-auth-image-bg">
        <Link to="/" className="auth-back"><FaArrowLeft /> Back to SkillSwap</Link>
        <div className="auth-visual-copy">
          <span className="eyebrow light"><span /> Your learning workspace</span>
          <h1>Learn with people.<br />Grow by teaching.</h1>
          <p>Find relevant peers, exchange practical skills, and keep every learning interaction in one place.</p>
          <div className="auth-visual-points">
            <span><FaShieldAlt /> Secure authentication</span>
            <span><FaGraduationCap /> Skill-based discovery</span>
          </div>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand-lockup brand-lockup">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span><strong>SkillSwap</strong><small>Peer learning network</small></span>
          </div>
          <span className="eyebrow muted">Member access</span>
          <h2>Welcome back</h2>
          <p className="auth-subtitle">Sign in to continue your learning journey.</p>
          {error && <div className="se-alert danger">{error}</div>}
          <form className="se-form auth-form" onSubmit={handleSubmit}>
            <label>
              <span>Username or email</span>
              <input value={form.usernameOrEmail} onChange={(e) => setForm({ ...form, usernameOrEmail: e.target.value })} required autoComplete="username" />
            </label>
            <label>
              <span>Password</span>
              <div className="password-field">
                <input type={showPassword ? 'text' : 'password'} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required autoComplete="current-password" />
                <button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
            </label>
            <div className="auth-form-meta">
              <span>Protected with JWT-based authentication.</span>
              <Link to="/forgot-password">Forgot password?</Link>
            </div>
            <button className="se-btn se-btn-primary se-btn-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign in'} {!loading && <FaArrowRight />}
            </button>
          </form>
          <div className="auth-trust">
            <span><FaShieldAlt /> Protected account access</span>
            <span>New here? <Link to="/register">Create an account</Link></span>
          </div>
        </div>
      </div>
    </div>
  )
}
