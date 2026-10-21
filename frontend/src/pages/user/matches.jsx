import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { FaArrowRight, FaHandshake, FaVideo, FaCheckCircle, FaClock, FaLanguage, FaStar, FaExchangeAlt } from 'react-icons/fa'
import { matchService } from '../../services/matches'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'
import MatchBadge from '../../components/common/match-badge'
import { videoRequestService } from '../../services/video-requests'
import { useToast } from '../../components/common/toast'

function initials(value = 'User') {
  return value.trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase() || 'U'
}

export default function Matches() {
  const [matches, setMatches] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [callingUserId, setCallingUserId] = useState(null)
  const [requestedUserIds, setRequestedUserIds] = useState(() => new Set())
  const { push } = useToast()

  useEffect(() => {
    let mounted = true
    matchService.myMatches(30)
      .then((data) => { if (mounted) setMatches(data || []) })
      .catch((error) => {
        if (mounted) {
          const message = error.response?.data?.message || 'Could not load your matches'
          setLoadError(message)
          push(message, 'danger')
        }
      })
      .finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [])

  const startPartnerCall = async (partnerId) => {
    setCallingUserId(partnerId)
    try {
      const result = await videoRequestService.create(partnerId)
      localStorage.setItem(`sk_partner_video_request_${result.id}`, JSON.stringify({ partnerId }))
      setRequestedUserIds((previous) => new Set(previous).add(partnerId))
      push('Video introduction request sent. Your partner will receive an approval prompt.')
    } catch (error) {
      push(error.response?.data?.message || 'Could not send video introduction request', 'danger')
    } finally {
      setCallingUserId(null)
    }
  }

  const strongest = useMemo(() => matches[0], [matches])

  if (loading) return <LoadingSpinner label="Calculating your best matches..." />
  if (loadError && matches.length === 0) return <div className="product-card p-5 text-center"><h4>We could not calculate your matches</h4><p className="text-muted mb-0">{loadError}</p></div>

  return (
    <div className="matches-product-page">
      <section className="matches-intro">
        <div>
          <span className="eyebrow muted">Compatibility workspace</span>
          <h1>Find the right person to <span>exchange skills with.</span></h1>
          <p>SkillSwap ranks reciprocal matches using deterministic signals. Every score is visible, explainable and independent of AI.</p>
        </div>
        <div className="matches-intro-stat"><strong>{matches.length}</strong><span>candidate matches</span><small>ranked for your current goals</small></div>
      </section>

      <section className="match-method-card">
        <div className="method-title"><span className="method-icon"><FaHandshake /></span><div><strong>How compatibility works</strong><small>We score the fit that matters to an actual exchange.</small></div></div>
        <div className="method-grid">
          {[
            [FaCheckCircle, 'Skills', 'Can both people teach what the other needs?'],
            [FaClock, 'Availability', 'Do the available time windows overlap?'],
            [FaLanguage, 'Language', 'Can both users communicate comfortably?'],
            [FaStar, 'Reputation', 'How reliable and positively rated is the profile?'],
          ].map(([Icon, title, copy]) => <div className="method-point" key={title}><Icon /><div><strong>{title}</strong><small>{copy}</small></div></div>)}
        </div>
      </section>

      {matches.length === 0 ? (
        <div className="py-5"><EmptyState title="No matches yet" subtitle="Add skills you want to learn and skills you can teach to unlock reciprocal recommendations." /></div>
      ) : (
        <div className="match-result-list">
          {matches.map((m, index) => (
            <article className={`match-result-card ${index === 0 ? 'is-featured' : ''}`} key={m.userId}>
              <div className="match-result-main">
                <div className="match-result-identity">
                  <div className="profile-avatar-xl">{initials(m.fullName || m.username)}</div>
                  <div><strong>{m.fullName || m.username}</strong><small>@{m.username}</small><div className="match-context">{index === 0 ? 'Best current reciprocal fit' : `Ranked #${index + 1} for your goals`}</div></div>
                </div>
                <div className="match-score-block"><span>Compatibility</span><MatchBadge percentage={m.overallMatchPercentage} /><small>out of 100</small></div>
              </div>

              <div className="match-exchange-band">
                <div><span>They can teach you</span><strong>{m.theyCanTeachYouWant?.join(', ') || 'No direct skill overlap'}</strong></div>
                <FaExchangeAlt />
                <div><span>You can teach them</span><strong>{m.youCanTeachTheyWant?.join(', ') || 'No direct skill overlap'}</strong></div>
              </div>

              <div className="match-breakdown-grid">
                {[
                  ['Skills', m.skillCompatibility, FaCheckCircle],
                  ['Availability', m.availabilityCompatibility, FaClock],
                  ['Language', m.languageCompatibility, FaLanguage],
                  ['Reputation', m.reputationCompatibility, FaStar],
                ].map(([label, value, Icon]) => (
                  <div className="match-breakdown" key={label}>
                    <div><span><Icon /> {label}</span><strong>{Math.round(value || 0)}%</strong></div>
                    <div className="compatibility-track"><span style={{ width: `${Math.min(100, Math.max(0, value || 0))}%` }} /></div>
                  </div>
                ))}
              </div>

              <div className="match-result-actions">
                <Link to={`/profile/${m.userId}`} className="se-btn se-btn-secondary">View full profile</Link>
                <Link to={`/exchange-requests/new?receiverId=${m.userId}`} className="se-btn se-btn-primary">Request exchange <FaArrowRight /></Link>
                <button type="button" className="video-intro-action" disabled={!m.videoCallEligible || callingUserId === m.userId || requestedUserIds.has(m.userId)} onClick={() => startPartnerCall(m.userId)} title={m.videoCallEligibilityMessage}>
                  <FaVideo /> {callingUserId === m.userId ? 'Sending...' : requestedUserIds.has(m.userId) ? 'Request sent' : 'Video introduction'}
                </button>
              </div>
              {!m.videoCallEligible && <div className="eligibility-note">Video introductions open after a strong reciprocal match is established.</div>}
              {m.videoCallEligible && index === 0 && <div className="eligibility-note positive"><FaVideo /> This partner is eligible for a private video introduction request.</div>}
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
