import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  FaArrowRight,
  FaCalendarCheck,
  FaCheckCircle,
  FaCoins,
  FaExchangeAlt,
  FaInbox,
  FaSearch,
  FaStar,
  FaUserCircle,
  FaVideo,
  FaBolt,
} from 'react-icons/fa'
import { useAuth } from '../../context/auth'
import { creditService } from '../../services/credits'
import { sessionService } from '../../services/sessions'
import { exchangeService } from '../../services/exchanges'
import { matchService } from '../../services/matches'
import LoadingSpinner from '../../components/common/loading-spinner'
import MatchBadge from '../../components/common/match-badge'
import EmptyState from '../../components/common/empty-state'

function initialOf(value = 'User') {
  return value.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U'
}

function statusClass(status) {
  if (status === 'PENDING') return 'is-warning'
  if (status === 'ACCEPTED') return 'is-success'
  if (status === 'COMPLETED') return 'is-success'
  if (status === 'CANCELLED' || status === 'REJECTED' || status === 'EXPIRED') return 'is-muted'
  return 'is-info'
}

export default function Dashboard() {
  const { user } = useAuth()
  const [wallet, setWallet] = useState(null)
  const [sessions, setSessions] = useState([])
  const [requests, setRequests] = useState([])
  const [matches, setMatches] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    const load = async () => {
      try {
        const [walletData, sessionsData, requestsData, matchesData] = await Promise.all([
          creditService.wallet(),
          sessionService.mine(),
          exchangeService.mine(0, 10),
          matchService.myMatches(4),
        ])
        if (!mounted) return
        setWallet(walletData)
        setSessions(sessionsData || [])
        setRequests(requestsData?.content || [])
        setMatches(matchesData || [])
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => { mounted = false }
  }, [])

  const upcoming = useMemo(() => sessions.filter((s) => s.status === 'SCHEDULED').slice(0, 3), [sessions])
  const pendingRequests = useMemo(() => requests.filter((r) => r.status === 'PENDING'), [requests])
  const recentRequests = requests.slice(0, 4)
  const firstMatch = matches[0]
  const firstName = user?.fullName?.split(' ')[0] || user?.username || 'there'

  const profileSignals = [
    Boolean(user?.fullName),
    Boolean(user?.bio),
    Boolean(user?.location || user?.languages || user?.communicationLanguage),
    Array.isArray(user?.teachSkills) ? user.teachSkills.length > 0 : true,
    Array.isArray(user?.learnSkills) ? user.learnSkills.length > 0 : true,
  ]
  const profileCompletion = Math.round((profileSignals.filter(Boolean).length / profileSignals.length) * 100)

  if (loading) return <LoadingSpinner label="Preparing your learning workspace..." />

  return (
    <div className="product-dashboard">
      <section className="workspace-hero product-hero">
        <div className="product-hero-copy">
          <span className="eyebrow muted">Your SkillSwap workspace</span>
          <h1>Good to see you, {firstName}.</h1>
          <p>Turn a compatible skill match into a real exchange: connect, meet, learn, and build your reputation.</p>
          <div className="product-hero-actions">
            <Link to="/matches" className="se-btn se-btn-primary">Find my next match <FaArrowRight /></Link>
            <Link to="/exchange-requests" className="se-btn se-btn-secondary">Review exchanges</Link>
          </div>
        </div>
        <div className="hero-profile-health">
          <div className="hero-profile-avatar"><FaUserCircle /></div>
          <div className="hero-profile-health-copy">
            <span>Profile readiness</span>
            <strong>{profileCompletion}%</strong>
            <small>Better profiles produce better matches.</small>
          </div>
          <div className="profile-progress"><span style={{ width: `${profileCompletion}%` }} /></div>
          <Link to="/profile/edit" className="subtle-link">Complete profile <FaArrowRight /></Link>
        </div>
      </section>

      <section className="workflow-strip" aria-label="SkillSwap exchange journey">
        {[
          ['01', 'Discover', 'Find compatible skills'],
          ['02', 'Connect', 'Send an exchange request'],
          ['03', 'Meet', 'Use a private video intro'],
          ['04', 'Exchange', 'Learn, complete & rate'],
        ].map(([number, title, copy], index) => (
          <React.Fragment key={title}>
            <div className="workflow-step">
              <span>{number}</span>
              <div><strong>{title}</strong><small>{copy}</small></div>
            </div>
            {index < 3 && <div className="workflow-line" />}
          </React.Fragment>
        ))}
      </section>

      <div className="product-metrics">
        <div className="product-metric"><span className="metric-icon gold"><FaCoins /></span><div><small>Available credits</small><strong>{wallet?.balance ?? 0}</strong></div><Link to="/wallet"><FaArrowRight /></Link></div>
        <div className="product-metric"><span className="metric-icon blue"><FaCalendarCheck /></span><div><small>Upcoming sessions</small><strong>{upcoming.length}</strong></div><Link to="/sessions"><FaArrowRight /></Link></div>
        <div className="product-metric"><span className="metric-icon violet"><FaInbox /></span><div><small>Needs your attention</small><strong>{pendingRequests.length}</strong></div><Link to="/exchange-requests"><FaArrowRight /></Link></div>
        <div className="product-metric"><span className="metric-icon green"><FaCheckCircle /></span><div><small>Completed exchanges</small><strong>{user?.completedExchanges ?? 0}</strong></div><Link to="/ratings"><FaArrowRight /></Link></div>
      </div>

      <div className="product-grid">
        <section className="product-card featured-match-card">
          <div className="card-heading">
            <div><span className="card-kicker">Recommended now</span><h2>Your strongest reciprocal match</h2></div>
            <Link to="/matches" className="subtle-link">See all <FaArrowRight /></Link>
          </div>
          {!firstMatch ? (
            <EmptyState title="No strong match yet" subtitle="Add teaching and learning skills to unlock reciprocal matches." />
          ) : (
            <div className="featured-match">
              <div className="featured-match-top">
                <div className="identity-lockup">
                  <div className="profile-avatar-xl">{initialOf(firstMatch.fullName || firstMatch.username)}</div>
                  <div><strong>{firstMatch.fullName || firstMatch.username}</strong><small>@{firstMatch.username}</small></div>
                </div>
                <MatchBadge percentage={firstMatch.overallMatchPercentage} />
              </div>
              <div className="exchange-direction">
                <div><span>They can teach you</span><strong>{firstMatch.theyCanTeachYouWant?.slice(0, 3).join(', ') || 'Skills you want'}</strong></div>
                <FaExchangeAlt />
                <div><span>You can teach them</span><strong>{firstMatch.youCanTeachTheyWant?.slice(0, 3).join(', ') || 'Skills they want'}</strong></div>
              </div>
              <div className="compatibility-bars">
                {[
                  ['Skills', firstMatch.skillCompatibility],
                  ['Time', firstMatch.availabilityCompatibility],
                  ['Language', firstMatch.languageCompatibility],
                  ['Reputation', firstMatch.reputationCompatibility],
                ].map(([label, value]) => (
                  <div className="compatibility-row" key={label}>
                    <div><span>{label}</span><strong>{Math.round(value || 0)}%</strong></div>
                    <div className="compatibility-track"><span style={{ width: `${Math.min(100, Math.max(0, value || 0))}%` }} /></div>
                  </div>
                ))}
              </div>
              <div className="featured-match-actions">
                <Link to={`/profile/${firstMatch.userId}`} className="se-btn se-btn-secondary">View profile</Link>
                <Link to={`/exchange-requests/new?receiverId=${firstMatch.userId}`} className="se-btn se-btn-primary">Start exchange <FaArrowRight /></Link>
                {firstMatch.videoCallEligible && <Link to="/matches" className="video-action-link"><FaVideo /> Request video intro</Link>}
              </div>
            </div>
          )}
        </section>

        <aside className="product-card attention-card">
          <div className="card-heading"><div><span className="card-kicker">What matters now</span><h2>Next best actions</h2></div></div>
          <Link to="/matches" className="priority-action priority-primary"><span><FaSearch /></span><div><strong>Review strong matches</strong><small>See who can teach what you want.</small></div><FaArrowRight /></Link>
          <Link to="/exchange-requests" className={`priority-action ${pendingRequests.length ? 'priority-highlight' : ''}`}><span><FaInbox /></span><div><strong>{pendingRequests.length ? `${pendingRequests.length} request${pendingRequests.length > 1 ? 's' : ''} need review` : 'Review exchange activity'}</strong><small>{pendingRequests.length ? 'Accept, decline, or continue a request.' : 'Track the full exchange lifecycle.'}</small></div><FaArrowRight /></Link>
          <Link to="/sessions" className="priority-action"><span><FaCalendarCheck /></span><div><strong>Prepare for your next session</strong><small>{upcoming.length ? 'Join on time and complete the exchange.' : 'Schedule a learning session from an accepted exchange.'}</small></div><FaArrowRight /></Link>
          <div className="trust-note"><FaBolt /><div><strong>No AI in matching</strong><span>Scores are deterministic and explainable across skills, time, language and reputation.</span></div></div>
        </aside>

        <section className="product-card sessions-product-card">
          <div className="card-heading"><div><span className="card-kicker">Live schedule</span><h2>Upcoming sessions</h2></div><Link to="/sessions" className="subtle-link">View calendar <FaArrowRight /></Link></div>
          {upcoming.length === 0 ? (
            <div className="dashboard-empty-state compact-empty"><div className="dashboard-empty-icon"><FaCalendarCheck /></div><strong>No sessions scheduled</strong><p>Accepted exchanges become learning sessions here.</p><Link to="/exchange-requests" className="subtle-link">Open exchanges <FaArrowRight /></Link></div>
          ) : (
            <div className="session-list">
              {upcoming.map((s) => (
                <div key={s.id} className="session-row professional-session-row">
                  <div className="session-date"><strong>{String(s.sessionDate || '').slice(-2)}</strong><small>{s.sessionDate ? new Date(`${s.sessionDate}T00:00:00`).toLocaleString('en-US', { month: 'short' }) : 'Date'}</small></div>
                  <div className="session-copy"><strong>{s.skillName}</strong><span>{s.sessionDate} · {s.startTime}–{s.endTime}</span><small>{s.meetingPlatform || 'SkillSwap Video'}</small></div>
                  <Link to="/sessions" className="icon-action">Open <FaArrowRight /></Link>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="product-card recent-activity-card">
          <div className="card-heading"><div><span className="card-kicker">Exchange lifecycle</span><h2>Recent activity</h2></div><Link to="/exchange-requests" className="subtle-link">Full history <FaArrowRight /></Link></div>
          {recentRequests.length === 0 ? <EmptyState title="No exchange activity yet" subtitle="Your requests and completed exchanges will appear here." /> : (
            <div className="activity-list">
              {recentRequests.map((r) => (
                <div key={r.id} className="activity-item">
                  <div className={`activity-state ${statusClass(r.status)}`}><FaExchangeAlt /></div>
                  <div className="activity-copy"><strong>{r.senderUsername} <span>→</span> {r.receiverUsername}</strong><small>{r.offeredSkillName} for {r.requestedSkillName}</small></div>
                  <span className={`status-pill ${statusClass(r.status)}`}>{r.status}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
