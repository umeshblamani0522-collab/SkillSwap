import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FaArrowLeft, FaArrowRight, FaCheck, FaGraduationCap, FaEye, FaEyeSlash } from 'react-icons/fa'
import { useAuth } from '../../context/auth'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '', fullName: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await register(form)
      navigate('/verify-email', { state: { email: form.email, message: data?.message } })
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page register-page">
      <div className="auth-visual register-visual">
        <Link to="/" className="auth-back"><FaArrowLeft /> Back to SkillSwap</Link>
        <div className="auth-visual-copy">
          <span className="eyebrow light"><span /> Start with your skills</span>
          <h1>Your skills are<br />your network.</h1>
          <p>Create a profile that makes it easy to teach what you know and discover what you want to learn.</p>
          <div className="register-benefits">
            <span><FaCheck /> List teach & learn skills</span>
            <span><FaCheck /> Get compatible recommendations</span>
            <span><FaCheck /> Build reputation over time</span>
          </div>
        </div>
      </div>

      <div className="auth-panel">
        <div className="auth-form-wrap wide">
          <div className="mobile-brand-lockup brand-lockup">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span><strong>SkillSwap</strong><small>Peer learning network</small></span>
          </div>
          <span className="eyebrow muted">Create account</span>
          <h2>Join the network</h2>
          <p className="auth-subtitle">Set up your profile in a few simple steps. Use an email address you can access because SkillSwap requires email verification before sign-in.</p>
          {error && <div className="se-alert danger">{error}</div>}
          <form className="se-form auth-form register-form" onSubmit={handleSubmit}>
            <label><span>Full name</span><input autoComplete="name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></label>
            <label><span>Username</span><input minLength={3} autoComplete="username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required /></label>
            <label><span>Email address</span><input type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
            <label><span>Password</span>
              <div className="password-field">
                <input type={showPassword ? 'text' : 'password'} minLength={8} autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
                <button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
              <small>Use at least 8 characters.</small>
            </label>
            <button className="se-btn se-btn-primary se-btn-full" disabled={loading}>
              {loading ? 'Creating account...' : 'Create account'} {!loading && <FaArrowRight />}
            </button>
          </form>
          <div className="auth-trust">
            <span><FaCheck /> Free to create an account</span>
            <span>Already a member? <Link to="/login">Sign in</Link></span>
          </div>
        </div>
      </div>
    </div>
  )
}
