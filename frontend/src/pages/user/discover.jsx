import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FaSearch, FaMapMarkerAlt, FaUsers, FaLanguage } from 'react-icons/fa'
import { userService } from '../../services/users'
import { matchService } from '../../services/matches'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'
import MatchBadge from '../../components/common/match-badge'

export default function FindPeople() {
  const [query, setQuery] = useState('')
  const [people, setPeople] = useState([])
  const [matchScores, setMatchScores] = useState({})
  const [loading, setLoading] = useState(true)

  const search = async (q) => {
    setLoading(true)
    const res = await userService.search(q, 0, 20)
    setPeople(res.content)
    setLoading(false)
  }

  useEffect(() => { search('') }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    search(query)
  }

  const loadMatchScore = async (userId) => {
    if (matchScores[userId] !== undefined) return
    try {
      const result = await matchService.matchWith(userId)
      setMatchScores((prev) => ({ ...prev, [userId]: result.overallMatchPercentage }))
    } catch {
      setMatchScores((prev) => ({ ...prev, [userId]: null }))
    }
  }

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
          <h2 className="mb-1 fw-bold">Find <span className="se-gradient-text">People</span></h2>
          <p className="text-muted mb-0">Discover skilled individuals to learn from and share with.</p>
        </div>
        <div className="se-icon-box primary rounded-circle d-none d-sm-flex" style={{ width: '64px', height: '64px', fontSize: '2rem' }}>
          <FaUsers />
        </div>
      </div>

      <div className="se-card p-2 mb-5 animate-slide-up delay-100 border-0 shadow-sm rounded-pill bg-white">
        <form className="d-flex align-items-center m-0" onSubmit={handleSearch}>
          <div className="ps-4 text-muted fs-5">
            <FaSearch />
          </div>
          <input
            className="form-control border-0 shadow-none bg-transparent py-3 px-3 fs-5"
            placeholder="Search by name, username, or location..."
            value={query} onChange={(e) => setQuery(e.target.value)}
          />
          <button className="btn se-btn-primary rounded-pill px-5 py-3 fw-semibold m-1">Search</button>
        </form>
      </div>

      {loading ? (
        <div className="py-5"><LoadingSpinner /></div>
      ) : people.length === 0 ? (
        <div className="animate-slide-up delay-200">
           <EmptyState title="No people found" subtitle="Try a different search term." />
        </div>
      ) : (
        <div className="row g-4">
          {people.map((p, index) => (
            <div className="col-md-6 col-lg-4" key={p.id} onMouseEnter={() => loadMatchScore(p.id)}>
              <div className={`se-card se-card-hover p-4 h-100 d-flex flex-column animate-slide-up delay-${Math.min(300, (index % 3 + 1) * 100)} border-0 shadow-sm`}>
                <div className="d-flex align-items-center gap-3 mb-3">
                  <img
                    src={p.profilePictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(p.username)}&size=64&background=4f46e5&color=fff`}
                    alt="" className="rounded-circle shadow-sm" width={64} height={64}
                  />
                  <div>
                    <div className="fw-bold fs-5 text-dark mb-1">{p.fullName || p.username}</div>
                    <div className="text-muted small">@{p.username}</div>
                  </div>
                </div>

                {p.location && (
                  <div className="text-muted small mb-2 d-flex align-items-center gap-2">
                    <FaMapMarkerAlt className="text-primary" /> {p.location}
                  </div>
                )}
                {p.communicationLanguage && (
                  <div className="text-muted small mb-3 d-flex align-items-center gap-2">
                    <FaLanguage className="text-primary" /> Communication: {p.communicationLanguage}
                  </div>
                )}

                <div className="d-flex flex-wrap gap-2 mb-4">
                  {(p.teachSkills || []).slice(0, 3).map((s) => (
                    <span key={s.id} className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 rounded-pill px-3 py-2 fw-medium">
                      {s.skillName}
                    </span>
                  ))}
                  {(p.teachSkills || []).length > 3 && (
                     <span className="badge bg-light text-muted border rounded-pill px-3 py-2 fw-medium">
                       +{p.teachSkills.length - 3} more
                     </span>
                  )}
                </div>

                <div className="mt-auto pt-3 border-top d-flex justify-content-between align-items-center">
                  <div>
                    {matchScores[p.id] != null ? (
                      <MatchBadge percentage={matchScores[p.id]} />
                    ) : (
                      <span className="badge bg-light text-muted border border-light rounded-pill px-3 py-2 small fw-normal" style={{ cursor: 'help' }}>Hover for Match %</span>
                    )}
                  </div>
                  <Link to={`/profile/${p.id}`} className="btn btn-sm btn-light text-primary fw-semibold rounded-pill px-4 py-2 hover-bg-primary hover-text-white transition-all">View Profile</Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
