import React from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { FaArrowRight, FaGraduationCap } from 'react-icons/fa'

export default function PublicLayout() {
  const location = useLocation()

  return (
    <div className="public-shell">
      <header className={`public-nav ${location.pathname === '/' ? 'home-public-nav' : ''}`}>
        <div className="se-container public-nav-inner">
          <Link className="brand-lockup" to="/" aria-label="SkillSwap home">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span>
              <strong>SkillSwap</strong>
              <small>Peer learning network</small>
            </span>
          </Link>

          <nav className="public-links" aria-label="Primary navigation">
            <Link className={location.pathname === '/' ? 'active' : ''} to="/">Home</Link>
          </nav>

          <div className="public-actions">
            <Link to="/login" className="text-link">Sign in</Link>
            <Link to="/register" className="se-btn se-btn-primary">
              Get started <FaArrowRight />
            </Link>
          </div>
        </div>
      </header>
      <Outlet />
    </div>
  )
}
