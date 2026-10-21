import React, { useEffect, useState } from 'react'
import { FaStar, FaRegStar, FaCommentDots, FaAward } from 'react-icons/fa'
import { useAuth } from '../../context/auth'
import { ratingService } from '../../services/ratings'
import { sessionService } from '../../services/sessions'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

export default function Ratings() {
  const { user } = useAuth()
  const { push } = useToast()
  const [ratings, setRatings] = useState([])
  const [completedSessions, setCompletedSessions] = useState([])
  const [form, setForm] = useState({ sessionId: '', stars: 5, review: '' })
  const [loading, setLoading] = useState(true)

  const load = async () => {
    if (!user) return
    try {
      const [r, sessions] = await Promise.all([ratingService.forUser(user.id, 0, 50), sessionService.mine()])
      setRatings(r.content)
      setCompletedSessions(sessions.filter((s) => s.status === 'COMPLETED'))
    } catch (e) {
      console.error("Failed to load ratings or sessions", e)
      push('Failed to load data', 'danger')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [user])

  const handleRate = async (e) => {
    e.preventDefault()
    try {
      await ratingService.rate({ ...form, sessionId: Number(form.sessionId), stars: Number(form.stars) })
      push('Rating submitted, thank you!')
      setForm({ sessionId: '', stars: 5, review: '' })
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not submit rating', 'danger')
    }
  }

  const renderStars = (rating) => {
    return (
      <div className="d-flex text-warning">
        {[...Array(5)].map((_, i) => (
          <span key={i} className="fs-5">
            {i < rating ? <FaStar /> : <FaRegStar className="text-muted opacity-25" />}
          </span>
        ))}
      </div>
    )
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
          <h2 className="mb-1 fw-bold">Ratings &amp; <span className="se-gradient-text">Reviews</span></h2>
          <p className="text-muted mb-0">Manage your feedback and see what others are saying about you.</p>
        </div>
        <div className="se-icon-box warning rounded-circle d-none d-sm-flex" style={{ width: '64px', height: '64px', fontSize: '2rem' }}>
          <FaStar />
        </div>
      </div>

      <div className="se-card se-card-hover p-4 mb-5 animate-slide-up delay-100 border-0 shadow-sm">
        <h5 className="mb-4 fw-bold d-flex align-items-center gap-2">
          <span className="se-icon-box info rounded-circle" style={{ width: '32px', height: '32px', fontSize: '1rem' }}><FaCommentDots /></span>
          Rate a Completed Session
        </h5>
        {completedSessions.length === 0 ? (
          <div className="bg-light rounded-4 border border-light py-4 text-center">
             <p className="text-muted mb-0 fw-medium">No completed sessions to rate right now.</p>
          </div>
        ) : (
          <form className="row g-3 align-items-end" onSubmit={handleRate}>
            <div className="col-md-5">
              <label className="form-label small fw-medium text-muted">Session</label>
              <select className="form-select bg-light border-0 shadow-none py-2" required value={form.sessionId}
                onChange={(e) => setForm({ ...form, sessionId: e.target.value })}>
                <option value="">Select a session...</option>
                {completedSessions.map((s) => (
                  <option key={s.id} value={s.id}>{s.skillName} — {s.sessionDate}</option>
                ))}
              </select>
            </div>
            <div className="col-md-2">
              <label className="form-label small fw-medium text-muted">Stars</label>
              <select className="form-select bg-light border-0 shadow-none py-2 fw-bold text-warning" value={form.stars} onChange={(e) => setForm({ ...form, stars: e.target.value })}>
                {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} ★</option>)}
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label small fw-medium text-muted">Review (optional)</label>
              <input className="form-control bg-light border-0 shadow-none py-2" placeholder="Leave a comment..." value={form.review} onChange={(e) => setForm({ ...form, review: e.target.value })} />
            </div>
            <div className="col-md-2">
              <button className="btn se-btn-primary w-100 py-2 rounded-3 shadow-sm fw-semibold">Submit</button>
            </div>
          </form>
        )}
      </div>

      <div className="se-card p-4 animate-slide-up delay-200 border-0 shadow-sm">
        <h5 className="mb-4 fw-bold d-flex align-items-center gap-2">
           <span className="se-icon-box success rounded-circle" style={{ width: '32px', height: '32px', fontSize: '1rem' }}><FaAward /></span>
           Ratings You've Received
        </h5>
        {ratings.length === 0 ? (
          <div className="bg-light rounded-4 border border-light py-5">
             <EmptyState title="No ratings yet" subtitle="Complete a session so your exchange partner can rate you." />
          </div>
        ) : (
          <div className="row g-3">
            {ratings.map((r, i) => (
              <div className={`col-md-6 animate-slide-up delay-${Math.min(300, (i % 3 + 1) * 100)}`} key={r.id}>
                <div className="se-list-item h-100 bg-light border-0 mb-0 rounded-4 shadow-sm p-4 d-flex flex-column">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <div className="d-flex align-items-center gap-3">
                      <div className="bg-white rounded-circle d-flex align-items-center justify-content-center shadow-sm text-primary fw-bold" style={{ width: '48px', height: '48px', fontSize: '1.2rem' }}>
                        {r.raterUsername.charAt(0).toUpperCase()}
                      </div>
                      <span className="fw-bold fs-5 text-dark">@{r.raterUsername}</span>
                    </div>
                    {renderStars(r.stars)}
                  </div>
                  {r.review ? (
                    <p className="text-muted fst-italic mb-0" style={{ lineHeight: '1.6' }}>"{r.review}"</p>
                  ) : (
                    <p className="text-muted opacity-50 fst-italic mb-0">No written review.</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
