import React, { useEffect, useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { FaStar, FaMapMarkerAlt, FaLanguage, FaEdit, FaCheckCircle, FaCoins, FaArrowRight, FaExchangeAlt, FaShieldAlt } from 'react-icons/fa'
import { useAuth } from '../../context/auth'
import { userService } from '../../services/users'
import LoadingSpinner from '../../components/common/loading-spinner'

function initials(value = 'User') {
  return value.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U'
}

export default function Profile() {
  const { id } = useParams()
  const { user: currentUser } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const targetId = id || currentUser?.id
        if (targetId) setProfile(await userService.getById(targetId))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id, currentUser])

  const completion = useMemo(() => {
    if (!profile) return 0
    const signals = [profile.fullName, profile.bio, profile.location, profile.languages, profile.communicationLanguage, profile.teachSkills?.length, profile.learnSkills?.length]
    return Math.round((signals.filter(Boolean).length / signals.length) * 100)
  }, [profile])

  if (loading || !profile) return <LoadingSpinner />
  const ownProfile = !id || String(id) === String(currentUser?.id)

  return (
    <div className="professional-profile-page">
      <section className="profile-hero-card">
        <div className="profile-cover" />
        <div className="profile-hero-content">
          <div className="profile-primary">
            {profile.profilePictureUrl ? (
              <img
                src={profile.profilePictureUrl}
                alt="Profile"
                className="profile-avatar-xxl"
              />
            ) : (
              <div className="profile-avatar-xxl profile-avatar-placeholder">
                {initials(profile.fullName || profile.username)}
              </div>
            )}
            <div className="profile-primary-copy"><div className="profile-name-line"><h1>{profile.fullName || profile.username}</h1><span className="verified-pill"><FaShieldAlt /> Verified member</span></div><p>@{profile.username}</p><div className="profile-meta-line">{profile.location && <span><FaMapMarkerAlt /> {profile.location}</span>}{profile.languages && (
  <span>
    <FaLanguage /> Languages: {profile.languages}
  </span>
)}

{profile.communicationLanguage && (
  <span>
    <FaLanguage /> Preferred chat: {profile.communicationLanguage}
  </span>
)}<span><FaStar /> {profile.averageRating ? profile.averageRating.toFixed(1) : 'New'} rating</span></div></div>
          </div>
          <div className="profile-hero-actions">{ownProfile ? <Link to="/profile/edit" className="se-btn se-btn-secondary"><FaEdit /> Edit profile</Link> : <Link to={`/exchange-requests/new?receiverId=${profile.id}`} className="se-btn se-btn-primary">Start exchange <FaArrowRight /></Link>}</div>
        </div>
      </section>

      <div className="profile-stat-grid">
        <div><span><FaCoins /></span><strong>{profile.skillCredits ?? 0}</strong><small>Skill credits</small></div>
        <div><span><FaExchangeAlt /></span><strong>{profile.completedExchanges ?? 0}</strong><small>Completed exchanges</small></div>
        <div><span><FaStar /></span><strong>{profile.averageRating ? profile.averageRating.toFixed(1) : '—'}</strong><small>Average rating</small></div>
        <div><span><FaCheckCircle /></span><strong>{completion}%</strong><small>Profile readiness</small></div>
      </div>

      <div className="profile-content-grid">
        <section className="product-card">
          <div className="card-heading"><div><span className="card-kicker">About the member</span><h2>{ownProfile ? 'Your professional profile' : 'About this member'}</h2></div></div>
          <p className="profile-bio">{profile.bio || 'This member has not added a bio yet. A stronger profile helps SkillSwap calculate better reciprocal matches.'}</p>
        </section>

        <aside className="product-card profile-readiness-card">
          <div className="card-heading"><div><span className="card-kicker">Profile quality</span><h2>Match readiness</h2></div></div>
          <div className="readiness-score"><strong>{completion}%</strong><span>ready</span></div>
          <div className="readiness-track"><span style={{ width: `${completion}%` }} /></div>
          <small>Skills, goals, availability and profile context improve matching quality.</small>
        </aside>

        <section className="product-card skills-profile-card">
          <div className="card-heading"><div><span className="card-kicker">Exchange inventory</span><h2>Can teach</h2></div></div>
          {profile.teachSkills?.length ? <div className="profile-skill-grid">{profile.teachSkills.map((s) => <div className="profile-skill-chip teach" key={s.id}><strong>{s.skillName}</strong><span>{s.level}</span></div>)}</div> : <EmptySkillState text="No teaching skills added yet." />}
        </section>

        <section className="product-card skills-profile-card">
          <div className="card-heading"><div><span className="card-kicker">Learning goals</span><h2>Wants to learn</h2></div></div>
          {profile.learnSkills?.length ? <div className="profile-skill-grid">{profile.learnSkills.map((s) => <div className="profile-skill-chip learn" key={s.id}><strong>{s.skillName}</strong><span>Goal</span></div>)}</div> : <EmptySkillState text="No learning interests added yet." />}
        </section>
      </div>
    </div>
  )
}

function EmptySkillState({ text }) {
  return <div className="empty-skill-state"><span><FaExchangeAlt /></span><p>{text}</p></div>
}
