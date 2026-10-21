import React, { useEffect, useMemo, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { FaArrowRight, FaCheckCircle, FaClock, FaExchangeAlt, FaLanguage, FaMapMarkerAlt, FaPaperPlane, FaUser, FaUsers } from 'react-icons/fa'
import { exchangeService } from '../../services/exchanges'
import { userService } from '../../services/users'
import { useAuth } from '../../context/auth'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

const STATUS_COLORS = {
  PENDING: 'bg-warning text-dark', ACCEPTED: 'bg-success', REJECTED: 'bg-danger',
  CANCELLED: 'bg-secondary', EXPIRED: 'bg-secondary', COMPLETED: 'bg-primary',
}

const DURATION_OPTIONS = [0.5, 1, 1.5, 2]

function initials(value = 'User') {
  return value.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U'
}

export default function ExchangeRequests() {
  const { user } = useAuth()
  const { push } = useToast()
  const [searchParams] = useSearchParams()
  const receiverId = searchParams.get('receiverId')
  const [requests, setRequests] = useState([])
  const [partner, setPartner] = useState(null)
  const [partnerLoading, setPartnerLoading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(Boolean(receiverId))
  const [form, setForm] = useState({
    receiverId: receiverId || '', offeredSkillId: '', requestedSkillId: '', durationHours: 1, message: '',
  })
  const [mySkills, setMySkills] = useState({ teach: [], learn: [] })

  const load = async () => {
    setLoading(true)
    try {
      const res = await exchangeService.mine(0, 50)
      setRequests(res.content || [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    if (user) setMySkills({ teach: user.teachSkills || [], learn: user.learnSkills || [] })
  }, [user])

  useEffect(() => {
    if (!receiverId) {
      setPartner(null)
      return
    }
    setForm((current) => ({ ...current, receiverId }))
    const loadPartner = async () => {
      setPartnerLoading(true)
      try {
        setPartner(await userService.getById(receiverId))
      } catch (err) {
        setPartner(null)
        push('Could not load the selected partner.', 'danger')
      } finally {
        setPartnerLoading(false)
      }
    }
    loadPartner()
  }, [receiverId])

  const stats = useMemo(() => ({
    total: requests.length,
    pending: requests.filter((r) => r.status === 'PENDING').length,
    accepted: requests.filter((r) => r.status === 'ACCEPTED').length,
    completed: requests.filter((r) => r.status === 'COMPLETED').length,
  }), [requests])

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!form.receiverId) {
      push('Select a partner from Matches or Find People first.', 'danger')
      return
    }
    try {
      await exchangeService.create({
        ...form,
        receiverId: Number(form.receiverId),
        offeredSkillId: Number(form.offeredSkillId),
        requestedSkillId: Number(form.requestedSkillId),
        durationHours: Number(form.durationHours),
      })
      push('Exchange request sent!')
      setShowForm(false)
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not send request', 'danger')
    }
  }

  const handleAction = async (id, action) => {
    try {
      if (action === 'accept') await exchangeService.accept(id)
      if (action === 'reject') await exchangeService.reject(id)
      if (action === 'cancel') await exchangeService.cancel(id)
      push(`Request ${action}ed`)
      load()
    } catch (err) {
      push(err.response?.data?.message || `Could not ${action} request`, 'danger')
    }
  }

  const partnerName = partner?.fullName || partner?.username || 'Selected partner'

  return (
    <div className="exchange-requests-page">
      <div className="exchange-page-header">
        <div>
          <span className="card-kicker">SKILLSWAP WORKSPACE</span>
          <h1>Exchange requests</h1>
          <p>Connect with another learner and agree on a clear skill exchange.</p>
        </div>
        <Link to="/matches" className="se-btn se-btn-secondary"><FaUsers /> Find a partner</Link>
      </div>

      <div className="exchange-stats-grid">
        <div className="exchange-stat-card"><span><FaExchangeAlt /></span><div><small>Total requests</small><strong>{stats.total}</strong></div></div>
        <div className="exchange-stat-card"><span><FaClock /></span><div><small>Awaiting response</small><strong>{stats.pending}</strong></div></div>
        <div className="exchange-stat-card"><span><FaCheckCircle /></span><div><small>Accepted</small><strong>{stats.accepted}</strong></div></div>
        <div className="exchange-stat-card"><span><FaCheckCircle /></span><div><small>Completed</small><strong>{stats.completed}</strong></div></div>
      </div>

      {showForm && (
        <div className="exchange-compose-layout">
          <section className="product-card exchange-compose-card">
            <div className="exchange-section-heading">
              <div className="exchange-heading-icon"><FaExchangeAlt /></div>
              <div><span className="card-kicker">NEW EXCHANGE</span><h2>Start a skill exchange</h2><p>Choose what you can teach, what you want to learn and how long you expect to meet.</p></div>
            </div>

            {partnerLoading ? <LoadingSpinner /> : partner ? (
              <div className="selected-partner-mobile">
                <div className="exchange-avatar">{initials(partnerName)}</div>
                <div><strong>{partnerName}</strong><small>@{partner.username}</small></div>
                <span className="selected-pill">Selected</span>
              </div>
            ) : (
              <div className="exchange-no-partner"><FaUsers /><div><strong>No partner selected</strong><p>Open Matches and choose <b>Request exchange</b> to start with a specific person.</p></div><Link to="/matches" className="se-btn se-btn-secondary">Find people <FaArrowRight /></Link></div>
            )}

            <form onSubmit={handleCreate} className="exchange-form">
              <div className="exchange-form-grid">
                <div className="exchange-field">
                  <label>I will teach <em>*</em></label>
                  <select className="form-select" required value={form.offeredSkillId} onChange={(e) => setForm({ ...form, offeredSkillId: e.target.value })}>
                    <option value="">Choose a skill</option>
                    {mySkills.teach.map((s) => <option key={s.skillId} value={s.skillId}>{s.skillName}</option>)}
                  </select>
                </div>
                <div className="exchange-field">
                  <label>I want to learn <em>*</em></label>
                  <select className="form-select" required value={form.requestedSkillId} onChange={(e) => setForm({ ...form, requestedSkillId: e.target.value })}>
                    <option value="">Choose a skill</option>
                    {mySkills.learn.map((s) => <option key={s.skillId} value={s.skillId}>{s.skillName}</option>)}
                  </select>
                </div>
              </div>

              <div className="exchange-field">
                <label>Session duration <em>*</em></label>
                <div className="duration-options">
                  {DURATION_OPTIONS.map((duration) => <button type="button" key={duration} className={Number(form.durationHours) === duration ? 'duration-option active' : 'duration-option'} onClick={() => setForm({ ...form, durationHours: duration })}>{duration < 1 ? '30 min' : `${duration} hour${duration > 1 ? 's' : ''}`}</button>)}
                </div>
              </div>

              <div className="exchange-field">
                <div className="message-label-row"><label>Message <span>(optional)</span></label><small>{form.message.length}/500</small></div>
                <textarea className="form-control" rows={4} maxLength={500} placeholder="Introduce yourself and briefly explain what you would like to exchange..." value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} />
              </div>

              <div className="exchange-submit-row">
                <div><FaPaperPlane /><span>{partner ? <>This request will be sent to <strong>{partnerName}</strong>.</> : 'Select a partner before sending this request.'}</span></div>
                <button className="se-btn se-btn-primary" disabled={!partner || partnerLoading}>Send request <FaArrowRight /></button>
              </div>
            </form>
          </section>

          {partner && <aside className="product-card selected-partner-card">
            <span className="card-kicker">SELECTED PARTNER</span>
            <div className="selected-partner-top"><div className="exchange-avatar large">{initials(partnerName)}</div><div><h3>{partnerName}</h3><p>@{partner.username}</p></div></div>
            {partner.communicationLanguage && <div className="partner-detail"><FaLanguage /><div><small>Communication language</small><strong>{partner.communicationLanguage}</strong></div></div>}
            {partner.location && <div className="partner-detail"><FaMapMarkerAlt /><div><small>Location</small><strong>{partner.location}</strong></div></div>}
            <div className="partner-skill-block"><small>Can teach</small><div>{partner.teachSkills?.length ? partner.teachSkills.slice(0, 5).map((s) => <span key={s.id || s.skillId}>{s.skillName}</span>) : <i>No skills listed</i>}</div></div>
            <div className="partner-skill-block"><small>Wants to learn</small><div>{partner.learnSkills?.length ? partner.learnSkills.slice(0, 5).map((s) => <span key={s.id || s.skillId}>{s.skillName}</span>) : <i>No learning goals listed</i>}</div></div>
            <Link to={`/profile/${partner.id}`} className="se-btn se-btn-secondary w-100">View full profile <FaArrowRight /></Link>
          </aside>}
        </div>
      )}

      {!showForm && <div className="exchange-list-intro"><div><span className="card-kicker">EXCHANGE ACTIVITY</span><h2>Your requests</h2><p>Review incoming and outgoing skill exchanges from one place.</p></div><Link to="/matches" className="se-btn se-btn-primary">Start an exchange <FaArrowRight /></Link></div>}

      {loading ? <LoadingSpinner /> : requests.length === 0 ? (
        <div className="product-card"><EmptyState title="No exchange requests yet" subtitle="Find a compatible person and start your first skill exchange." /></div>
      ) : (
        <div className="product-card exchange-table-card">
          <div className="exchange-table-wrap">
            <table className="table mb-0 align-middle exchange-table">
              <thead><tr><th>Exchange</th><th>Skills</th><th>Duration</th><th>Status</th><th>Match</th><th className="text-end">Action</th></tr></thead>
              <tbody>{requests.map((r) => (
                <tr key={r.id}>
                  <td><div className="request-person"><div className="exchange-avatar small">{initials(r.receiverUsername === user?.username ? r.senderUsername : r.receiverUsername)}</div><div><strong>{r.senderUsername === user?.username ? `To ${r.receiverUsername}` : `From ${r.senderUsername}`}</strong><small>Request #{r.id}</small></div></div></td>
                  <td><div className="request-skills"><strong>{r.offeredSkillName}</strong><FaExchangeAlt /><strong>{r.requestedSkillName}</strong></div></td>
                  <td>{r.durationHours}h</td>
                  <td><span className={`status-pill ${r.status.toLowerCase()}`}><span />{r.status}</span></td>
                  <td><strong className="match-value">{r.matchPercentage ? `${Math.round(r.matchPercentage)}%` : '—'}</strong></td>
                  <td className="text-end">
                    {r.status === 'PENDING' && r.receiverUsername === user?.username && <><button className="btn btn-sm btn-success me-1" onClick={() => handleAction(r.id, 'accept')}>Accept</button><button className="btn btn-sm btn-outline-danger" onClick={() => handleAction(r.id, 'reject')}>Reject</button></>}
                    {r.status === 'PENDING' && r.senderUsername === user?.username && <button className="btn btn-sm btn-outline-secondary" onClick={() => handleAction(r.id, 'cancel')}>Cancel</button>}
                    {r.status === 'ACCEPTED' && <Link to={`/sessions?exchangeRequestId=${r.id}`} className="btn btn-sm btn-primary se-btn-primary">Schedule session</Link>}
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
