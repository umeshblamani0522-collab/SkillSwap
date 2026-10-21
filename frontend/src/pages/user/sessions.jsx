import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { FaVideo, FaCalendarAlt, FaTrash, FaCheckCircle, FaExclamationTriangle } from 'react-icons/fa'
import { sessionService } from '../../services/sessions'
import { exchangeService } from '../../services/exchanges'
import { videoRequestService } from '../../services/video-requests'
import { partnerVideoService } from '../../services/partner-video'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

const STATUS_COLORS = {
  SCHEDULED: 'bg-primary', IN_PROGRESS: 'bg-info', COMPLETED: 'bg-success',
  CANCELLED: 'bg-secondary', NO_SHOW: 'bg-danger',
}

const MEETING_PROVIDERS = {
  jitsi: {
    label: 'SkillSwap Video',
    helper: 'Built-in video room — no external link required.',
  },
  google: {
    label: 'Google Meet',
    helper: 'Paste the Google Meet link shared with your exchange partner.',
    hosts: ['meet.google.com'],
  },
  zoom: {
    label: 'Zoom',
    helper: 'Paste the Zoom meeting link shared with your exchange partner.',
    hosts: ['zoom.us', 'zoom.com'],
  },
  teams: {
    label: 'Microsoft Teams',
    helper: 'Paste the Microsoft Teams meeting link shared with your exchange partner.',
    hosts: ['teams.microsoft.com'],
  },
  other: {
    label: 'Other link',
    helper: 'Use another online meeting URL if your partner prefers a different service.',
  },
}

function detectProvider(link = '') {
  if (!link) return 'jitsi'
  try {
    const host = new URL(link).hostname.toLowerCase().replace(/^www\./, '')
    if (host === 'meet.google.com') return 'google'
    if (host === 'zoom.us' || host.endsWith('.zoom.us') || host === 'zoom.com') return 'zoom'
    if (host === 'teams.microsoft.com') return 'teams'
  } catch (_) {
    return 'other'
  }
  return 'other'
}

function providerForSession(link = '') {
  return detectProvider(link)
}

function isValidHttpUrl(value) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch (_) {
    return false
  }
}

function meetingLabel(provider) {
  return MEETING_PROVIDERS[provider]?.label || 'Online meeting'
}

export default function Sessions() {
  const { push } = useToast()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [sessions, setSessions] = useState([])
  const [acceptedRequests, setAcceptedRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(!!searchParams.get('exchangeRequestId'))
  const [meetingProvider, setMeetingProvider] = useState('jitsi')
  const [form, setForm] = useState({
    exchangeRequestId: searchParams.get('exchangeRequestId') || '',
    sessionDate: '', startTime: '', endTime: '', meetingInfo: '', notes: '',
  })
  const [joiningMeeting, setJoiningMeeting] = useState(false)
  const [videoRequests, setVideoRequests] = useState([])
  const [requestingVideoForSession, setRequestingVideoForSession] = useState(null)

  const currentProvider = useMemo(() => MEETING_PROVIDERS[meetingProvider], [meetingProvider])


  const load = async (silent = false) => {
    if (!silent) setLoading(true)

    try {
      const data = await sessionService.mine()
      setSessions(Array.isArray(data) ? data : [])

      const requestsData = await exchangeService.mine(0, 100)
      setAcceptedRequests(
        (requestsData?.content || []).filter(
          (r) => r.status === 'ACCEPTED'
        )
      )

      const videoData = await videoRequestService.mine(0, 100)
      setVideoRequests(videoData?.content || [])
    } catch (e) {
      // Avoid repeated error toasts during background refreshes.
      if (!silent) {
        push(
          e.response?.data?.message || 'Could not load sessions',
          'danger'
        )
      }
    } finally {
      if (!silent) setLoading(false)
    }
  }


useEffect(() => {
  load()

  const interval = window.setInterval(() => {
    load(true)
  }, 5000)

  const handleFocus = () => load(true)
  window.addEventListener('focus', handleFocus)

  return () => {
    window.clearInterval(interval)
    window.removeEventListener('focus', handleFocus)
  }
  }, [])

  const handleProviderChange = (value) => {
    setMeetingProvider(value)
    if (value === 'jitsi') setForm(prev => ({ ...prev, meetingInfo: '' }))
  }

  const handleSchedule = async (e) => {
    e.preventDefault()

    if (meetingProvider !== 'jitsi' && !isValidHttpUrl(form.meetingInfo)) {
      push(`Please enter a valid ${currentProvider.label} URL`, 'danger')
      return
    }

    if (meetingProvider !== 'jitsi' && currentProvider.hosts) {
      try {
        const hostname = new URL(form.meetingInfo).hostname.toLowerCase().replace(/^www\./, '')
        const allowed = currentProvider.hosts.some(host => hostname === host || hostname.endsWith(`.${host}`))
        if (!allowed) {
          push(`Please enter a valid ${currentProvider.label} link`, 'danger')
          return
        }
      } catch (_) {
        push('Please enter a valid meeting URL', 'danger')
        return
      }
    }

    try {
      await sessionService.schedule({ ...form, exchangeRequestId: Number(form.exchangeRequestId) })
      push('Session scheduled successfully!')
      setShowForm(false)
      setForm(prev => ({ ...prev, meetingInfo: '' }))
      setMeetingProvider('jitsi')
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not schedule session', 'danger')
    }
  }

  const handleComplete = async (id) => {
    try {
      await sessionService.complete(id)
      push('Session marked complete — credits transferred!')
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not complete session', 'danger')
    }
  }

  const handleCancel = async (id) => {
    try {
      await sessionService.cancel(id)
      push('Session cancelled')
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not cancel session', 'danger')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this session? This cannot be undone.')) return
    try {
      await sessionService.remove(id)
      setSessions((prev) => prev.filter((session) => session.id !== id))
      setVideoRequests((prev) => prev.filter((request) => request.sessionId !== id))
      push('Session deleted')
    } catch (err) {
      push(err.response?.data?.message || 'Could not delete session', 'danger')
    }
  }

  const handleNoShow = async (id) => {
    try {
      await sessionService.noShow(id)
      push('Session marked as no-show')
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not update session', 'danger')
    }
  }

  const copyMeetingLink = async (link) => {
    try {
      await navigator.clipboard.writeText(link)
      push('Meeting link copied')
    } catch (_) {
      push('Could not copy meeting link', 'danger')
    }
  }

  const requestSessionVideo = async (session) => {
    setRequestingVideoForSession(session.id)
    try {
      const result = await videoRequestService.createForSession(session.id)
      localStorage.setItem(`sk_session_video_request_${result.id}`, JSON.stringify({ sessionId: session.id }))
      setVideoRequests((prev) => [result, ...prev])
      push('Video request sent. Your partner will receive an Accept / Decline prompt.', 'success')
    } catch (err) {
      push(err.response?.data?.message || 'Could not send video request', 'danger')
    } finally {
      setRequestingVideoForSession(null)
    }
  }

  const joinSkillSwapVideo = async (sessionId) => {
    setJoiningMeeting(true)
    try {
      // The Session page stores an accepted request room id, which can become
      // stale after the short-lived room expires. Ask the backend to resolve
      // the current room from the session before navigating.
      const result = await partnerVideoService.prepareSessionRoom(sessionId)
      if (!result?.roomId) throw new Error('No video room was returned')
      navigate(`/partner-video/${result.roomId}`)
    } catch (err) {
      push(err.response?.data?.message || 'Could not prepare the SkillSwap video room', 'danger')
    } finally {
      setJoiningMeeting(false)
    }
  }

  return (
    <div className="sessions-page">
      <div className="sessions-page-header d-flex justify-content-between align-items-center mb-4">
        <div className="d-flex align-items-center gap-3">
          <div className="sessions-title-icon"><FaVideo /></div>
          <div>
            <h3 className="mb-1 fw-bold">My Sessions</h3>
            <p className="text-muted mb-0 small">View and manage your learning sessions</p>
          </div>
        </div>
        <button className="btn btn-primary se-btn-primary sessions-new-btn" onClick={() => setShowForm(!showForm)}>
          <span className="me-1">+</span> {showForm ? 'Close' : 'Schedule New Session'}
        </button>
      </div>

      {showForm && (
        <div className="se-card p-4 mb-4 sessions-form-card">
          <div className="d-flex justify-content-between align-items-start mb-3">
            <div>
              <h5 className="mb-1">Schedule a Session</h5>
              <div className="text-muted small">Choose a time and decide how you and your partner will meet.</div>
            </div>
            <span className="badge bg-light text-primary border">Accepted exchange required</span>
          </div>
          <form onSubmit={handleSchedule}>
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Accepted exchange</label>
                <select className="form-select" required value={form.exchangeRequestId}
                  onChange={(e) => setForm({ ...form, exchangeRequestId: e.target.value })}>
                  <option value="">Select an accepted exchange</option>
                  {acceptedRequests.map(req => (
                    <option key={req.id} value={req.id}>
                      #{req.id} — {req.offeredSkillName} with {req.receiverUsername}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-md-3">
                <label className="form-label">Date</label>
                <input className="form-control" required type="date" value={form.sessionDate}
                  onChange={(e) => setForm({ ...form, sessionDate: e.target.value })} />
              </div>
              <div className="col-md-3">
                <label className="form-label">Start time</label>
                <input className="form-control" required type="time" value={form.startTime}
                  onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
              </div>
              <div className="col-md-3">
                <label className="form-label">End time</label>
                <input className="form-control" required type="time" value={form.endTime}
                  onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
              </div>
              <div className="col-md-5">
                <label className="form-label">Meeting platform</label>
                <select className="form-select" value={meetingProvider} onChange={(e) => handleProviderChange(e.target.value)}>
                  {Object.entries(MEETING_PROVIDERS).map(([key, provider]) => (
                    <option key={key} value={key}>{provider.label}</option>
                  ))}
                </select>
                <div className="form-text">{currentProvider.helper}</div>
              </div>
              <div className="col-md-7">
                <label className="form-label">Online meeting link</label>
                <input className="form-control" disabled={meetingProvider === 'jitsi'} required={meetingProvider !== 'jitsi'}
                  type="url" placeholder={meetingProvider === 'jitsi' ? 'Not required — SkillSwap creates the video room' : 'https://meet.google.com/...'}
                  value={form.meetingInfo} onChange={(e) => setForm({ ...form, meetingInfo: e.target.value })} />
              </div>
              <div className="col-12">
                <label className="form-label">Session notes <span className="text-muted fw-normal">(optional)</span></label>
                <textarea className="form-control" rows={2} value={form.notes}
                  placeholder="Example: We will cover REST APIs and JWT authentication."
                  onChange={(e) => setForm({ ...form, notes: e.target.value })} />
              </div>
            </div>
            <div className="d-flex align-items-center justify-content-between mt-3 gap-3 flex-wrap">
              <div className="small text-muted">The session is created only after the exchange request has been accepted.</div>
              <button className="btn btn-primary se-btn-primary">Schedule Session</button>
            </div>
          </form>
        </div>
      )}

      {loading ? <LoadingSpinner /> : sessions.length === 0 ? (
        <EmptyState title="No sessions yet" subtitle="Accept an exchange request, then schedule your first learning session." />
      ) : (
        <div className="sessions-stack">
          {sessions.map((s) => {
            const provider = providerForSession(s.meetingInfo)
            const externalMeeting = Boolean(s.meetingInfo && isValidHttpUrl(s.meetingInfo))
            const sessionRequests = videoRequests.filter((r) => r.sessionId === s.id)
            const accepted = sessionRequests.find((r) => r.status === 'ACCEPTED' && r.roomId)
            const pending = sessionRequests.some((r) => r.status === 'PENDING')
            return (
              <article className="se-card session-reference-card" key={s.id}>
                <div className="session-reference-head">
                  <div className="session-reference-title">
                    <h5>{s.skillName}</h5>
                    <div>{s.teacherUsername} teaching {s.learnerUsername}</div>
                    <div className="session-date-line"><FaCalendarAlt /> {s.sessionDate} · {s.startTime}–{s.endTime}</div>
                  </div>
                  <div className="session-reference-status">
                    <span className={`badge ${STATUS_COLORS[s.status]}`}>{s.status}</span>
                    <button className="session-delete-btn" onClick={() => handleDelete(s.id)} title="Delete session" aria-label={`Delete ${s.skillName} session`}>
                      <FaTrash />
                    </button>
                  </div>
                </div>

                <div className="session-online-box">
                  <FaVideo className="session-online-icon" />
                  <div className="session-online-copy">
                    <div className="session-online-label">ONLINE SESSION</div>
                    <div className="fw-bold">{meetingLabel(provider)}</div>
                  </div>
                  <div className="session-online-action">
                    {externalMeeting ? (
                      <div className="d-flex gap-2 flex-wrap">
                        <a href={s.meetingInfo} target="_blank" rel="noreferrer" className="btn btn-sm btn-primary se-btn-primary">
                          <FaVideo className="me-1" /> Join {meetingLabel(provider)}
                        </a>
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => copyMeetingLink(s.meetingInfo)}>Copy link</button>
                      </div>
                    ) : (s.status === 'SCHEDULED' || s.status === 'IN_PROGRESS') ? (
                      accepted ? (
                        <button className="btn btn-sm btn-primary se-btn-primary" onClick={() => joinSkillSwapVideo(s.id)} disabled={joiningMeeting}>
                          <FaVideo className="me-1" /> {joiningMeeting ? 'Opening…' : 'Join SkillSwap Video'}
                        </button>
                      ) : pending ? (
                        <button className="btn btn-sm btn-outline-secondary" disabled>Video request sent</button>
                      ) : (
                        <button className="btn btn-sm btn-primary se-btn-primary" onClick={() => requestSessionVideo(s)} disabled={requestingVideoForSession === s.id}>
                          <FaVideo className="me-1" /> {requestingVideoForSession === s.id ? 'Sending…' : 'Request to join video'}
                        </button>
                      )
                    ) : (
                      <span className="text-muted small">Video access is unavailable after this session is {s.status.toLowerCase().replace('_', ' ')}.</span>
                    )}
                  </div>
                </div>

                {s.notes && <div className="small text-muted mt-3"><strong>Notes:</strong> {s.notes}</div>}

                {s.status === 'SCHEDULED' && (
                  <div className="session-reference-actions">
                    <button className="btn btn-sm btn-success" onClick={() => handleComplete(s.id)}><FaCheckCircle className="me-1" /> Complete Session</button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => handleNoShow(s.id)}><FaExclamationTriangle className="me-1" /> Report Problem</button>
                    <button className="btn btn-sm btn-outline-secondary" onClick={() => handleCancel(s.id)}>Cancel</button>
                  </div>
                )}
                {s.status === 'COMPLETED' && (
                  <div className="session-success-message"><FaCheckCircle /> <span>Session completed successfully.</span></div>
                )}
                {s.creditsTransferred && <div className="small text-success mt-2 fw-semibold">✓ Credits transferred</div>}
              </article>
            )
          })}
        </div>
      )}
    </div>
  )
}
