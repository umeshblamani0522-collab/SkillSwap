import React, { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { FaArrowRight, FaCheck, FaEnvelope, FaGraduationCap, FaShieldAlt, FaTimes } from 'react-icons/fa'
import { authService } from '../../services/auth'

export default function VerifyEmail() {
  const [params] = useSearchParams()
  const location = useLocation()
  const token = params.get('token') || ''
  const [status, setStatus] = useState(token ? 'loading' : 'missing')
  const [error, setError] = useState('')
  const [resendLoading, setResendLoading] = useState(false)
  const [resendDone, setResendDone] = useState(false)
  const email = location.state?.email || ''

  useEffect(() => {
    if (!token) return
    let active = true
    authService.verifyEmail(token)
      .then(() => active && setStatus('success'))
      .catch((err) => {
        if (!active) return
        setError(err.response?.data?.message || 'This verification link is invalid or has expired.')
        setStatus('error')
      })
    return () => { active = false }
  }, [token])

  const intro = location.state?.message || (email ? `We sent a verification link to ${email}. Open it to finish creating your account.` : 'Open the verification link from the email sent by SkillSwap to finish creating your account.')

  const resend = async () => {
    if (!email) return
    setResendLoading(true)
    setResendDone(false)
    try { await authService.resendVerification(email); setResendDone(true) }
    finally { setResendLoading(false) }
  }

  return (
    <div className="auth-page">
      <div className="auth-visual se-auth-image-bg">
        <Link to="/" className="auth-back"><FaArrowRight style={{ transform: 'rotate(180deg)' }} /> Back to SkillSwap</Link>
        <div className="auth-visual-copy">
          <span className="eyebrow light"><span /> Email verification</span>
          <h1>One account.<br />One verified email.</h1>
          <p>Your email address must be verified before SkillSwap allows account access.</p>
          <div className="auth-visual-points">
            <span><FaShieldAlt /> Ownership verification</span>
            <span><FaEnvelope /> One-time verification link</span>
          </div>
        </div>
      </div>
      <div className="auth-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand-lockup brand-lockup">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span><strong>SkillSwap</strong><small>Peer learning network</small></span>
          </div>
          <span className="eyebrow muted">Verify your email</span>
          <h2>{status === 'success' ? 'Email verified' : status === 'loading' ? 'Verifying your email' : 'Email verification required'}</h2>
          <p className="auth-subtitle">{status === 'success' ? 'Your account is now verified and ready to use.' : intro}</p>

          {status === 'loading' && <div className="se-alert">Checking your secure verification link...</div>}
          {status === 'success' && <div className="se-alert success"><FaCheck /> Your email address has been verified successfully.</div>}
          {(status === 'error' || status === 'missing') && <div className="se-alert danger"><FaTimes /> {error || 'Open the verification link from the email sent by SkillSwap.'}</div>}

          <div style={{ marginTop: 18 }}>
            {status === 'success' ? (
              <Link to="/login" className="se-btn se-btn-primary se-btn-full">Continue to sign in <FaArrowRight /></Link>
            ) : (
              <>
                {email && <button type="button" className="se-btn se-btn-secondary se-btn-full" onClick={resend} disabled={resendLoading}>{resendLoading ? 'Sending again...' : 'Resend verification email'}</button>}
                {resendDone && <div className="se-alert success" style={{ marginTop: 10 }}>A new verification email has been sent if the account is still pending verification.</div>}
                <Link to="/register" className="se-btn se-btn-primary se-btn-full" style={{ marginTop: 10 }}>Create another account <FaArrowRight /></Link>
              </>
            )}
          </div>

          <div className="auth-trust">
            <span><FaShieldAlt /> Only verified email addresses can sign in</span>
            <span><Link to="/login">Back to sign in</Link></span>
          </div>
        </div>
      </div>
    </div>
  )
}
