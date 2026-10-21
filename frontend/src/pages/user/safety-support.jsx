import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { FaExclamationTriangle, FaShieldAlt, FaUserEdit, FaLifeRing } from 'react-icons/fa'
import { reportService } from '../../services/reports'
import { useToast } from '../../components/common/toast'
import { useAuth } from '../../context/auth'

const INITIAL = { targetType: 'USER', targetId: '', reason: '' }

export default function SafetySupport() {
  const { user } = useAuth()
  const { push } = useToast()
  const [report, setReport] = useState(INITIAL)
  const [submitting, setSubmitting] = useState(false)

  const handleReport = async (e) => {
    e.preventDefault()
    if (Number(report.targetId) === Number(user?.id)) {
      push('You cannot report your own account.', 'danger')
      return
    }
    setSubmitting(true)
    try {
      await reportService.create({ ...report, targetId: Number(report.targetId) })
      push('Report submitted. The moderation team will review it.')
      setReport(INITIAL)
    } catch (err) {
      push(err.response?.data?.message || 'Could not submit report', 'danger')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="container-fluid p-0">
      <div className="page-hero dashboard-hero mb-4">
        <div>
          <span className="eyebrow muted">Account & platform safety</span>
          <h1>Safety & Support</h1>
          <p>Manage your public profile and report users, messages, sessions or other content that needs moderator attention.</p>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <Link to="/profile" className="se-btn se-btn-secondary"><FaUserEdit /> View profile</Link>
          <Link to="/profile/edit" className="se-btn se-btn-primary"><FaUserEdit /> Edit profile</Link>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-5">
          <div className="content-card h-100 p-4">
            <div className="d-flex align-items-center gap-3 mb-3">
              <div className="se-icon-box info rounded-circle"><FaShieldAlt /></div>
              <div>
                <h2 className="h5 mb-1">Account information</h2>
                <p className="text-muted small mb-0">Your authenticated account identity.</p>
              </div>
            </div>
            <div className="bg-light rounded-4 p-3">
              <div className="mb-3"><div className="text-muted small text-uppercase fw-semibold">Username</div><div className="fw-semibold">@{user?.username}</div></div>
              <div><div className="text-muted small text-uppercase fw-semibold">Email</div><div className="fw-semibold">{user?.email}</div></div>
            </div>
            <div className="alert alert-light border mt-3 mb-0 small">
              Password recovery uses a one-time, expiring email reset link. Never share a reset token with another person.
            </div>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="content-card p-4">
            <div className="d-flex align-items-center gap-3 mb-3">
              <div className="se-icon-box danger rounded-circle"><FaExclamationTriangle /></div>
              <div>
                <h2 className="h5 mb-1">Report a problem</h2>
                <p className="text-muted small mb-0">Reports are persisted for admin moderation.</p>
              </div>
            </div>
            <form onSubmit={handleReport}>
              <div className="row g-3">
                <div className="col-sm-5">
                  <label className="form-label">Report type</label>
                  <select className="form-select" value={report.targetType} onChange={(e) => setReport({ ...report, targetType: e.target.value })}>
                    <option value="USER">User</option>
                    <option value="MESSAGE">Message</option>
                    <option value="SESSION">Session</option>
                    <option value="CONTENT">Content</option>
                  </select>
                </div>
                <div className="col-sm-7">
                  <label className="form-label">Target ID</label>
                  <input className="form-control" required min="1" type="number" value={report.targetId} onChange={(e) => setReport({ ...report, targetId: e.target.value })} placeholder="Enter the reported record ID" />
                </div>
                <div className="col-12">
                  <label className="form-label">Reason</label>
                  <textarea className="form-control" required minLength={10} rows={5} value={report.reason} onChange={(e) => setReport({ ...report, reason: e.target.value })} placeholder="Describe what happened and include useful context." />
                </div>
              </div>
              <button className="se-btn se-btn-primary mt-3" type="submit" disabled={submitting}>
                <FaLifeRing /> {submitting ? 'Submitting…' : 'Submit report'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
