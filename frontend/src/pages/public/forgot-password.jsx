import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { FaArrowLeft, FaArrowRight, FaEnvelope, FaGraduationCap, FaShieldAlt } from 'react-icons/fa'
import { authService } from '../../services/auth'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authService.forgotPassword(email.trim())
      setSubmitted(true)
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to process the request right now.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-visual se-auth-image-bg">
        <Link to="/login" className="auth-back"><FaArrowLeft /> Back to sign in</Link>
        <div className="auth-visual-copy">
          <span className="eyebrow light"><span /> Account recovery</span>
          <h1>Get back to<br />your workspace.</h1>
          <p>We will send a one-time password reset link to the email address registered with your SkillSwap account.</p>
          <div className="auth-visual-points">
            <span><FaShieldAlt /> Single-use reset link</span>
            <span><FaEnvelope /> Expires automatically</span>
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
          <h2>Forgot your password?</h2>
          <p className="auth-subtitle">Enter your account email and we will send a secure reset link.</p>

          {error && <div className="se-alert danger">{error}</div>}

          {submitted ? (
            <div className="se-alert success" style={{ marginTop: 8 }}>
              Check your email for a password reset link. The message is only sent when the address belongs to a SkillSwap account.
            </div>
          ) : (
            <form className="se-form auth-form" onSubmit={handleSubmit}>
              <label>
                <span>Email address</span>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" placeholder="you@example.com" />
              </label>
              <div className="auth-form-meta">
                <span>The reset link is valid for a limited time and can be used once.</span>
              </div>
              <button className="se-btn se-btn-primary se-btn-full" disabled={loading}>
                {loading ? 'Sending link...' : 'Send reset link'} {!loading && <FaArrowRight />}
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
