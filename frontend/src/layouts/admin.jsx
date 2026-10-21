import React from 'react'
import { NavLink, Outlet, Link, useNavigate } from 'react-router-dom'
import {
  FaChartLine, FaUsers, FaGraduationCap, FaFlag,
  FaShieldAlt, FaSignOutAlt, FaBars, FaTimes, FaGlobe,
} from 'react-icons/fa'
import { useAuth } from '../context/auth'

const adminNav = [
  { to: '/admin', label: 'Overview', icon: <FaChartLine />, end: true },
  { to: '/admin/users', label: 'Users', icon: <FaUsers /> },
  { to: '/admin/skills', label: 'Skills', icon: <FaGraduationCap /> },
  { to: '/admin/reports', label: 'Reports', icon: <FaFlag /> },
]

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = React.useState(false)

  const displayName = user?.fullName || user?.username || 'Administrator'
  const initials = displayName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell admin-shell">
      <div className={`mobile-overlay ${mobileOpen ? 'show' : ''}`} onClick={() => setMobileOpen(false)} />

      <aside className={`app-sidebar admin-sidebar ${mobileOpen ? 'open' : ''}`}>
        <div className="sidebar-brand admin-brand">
          <Link to="/admin" className="brand-lockup">
            <span className="brand-mark"><FaShieldAlt /></span>
            <span>
              <strong>SkillSwap</strong>
              <small>Administration</small>
            </span>
          </Link>
          <button className="mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close admin navigation">
            <FaTimes />
          </button>
        </div>

        <div className="sidebar-scroll">
          <div className="admin-nav-intro">
            <span className="admin-nav-badge">ADMIN</span>
            <p>Manage the platform, users and moderation from one workspace.</p>
          </div>

          <div className="sidebar-section-label">Administration</div>
          <nav className="se-nav admin-nav">
            {adminNav.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className="se-nav-item">
                <span className="se-nav-item-left">
                  <span className="se-nav-icon">{item.icon}</span>
                  <span>{item.label}</span>
                </span>
              </NavLink>
            ))}
          </nav>

        </div>

        <div className="sidebar-footer">
          <Link to="/" className="sidebar-back">
            <FaGlobe />
            <span>View public site</span>
          </Link>
          <div className="sidebar-profile admin-profile">
            <span className="avatar-fallback">{initials || 'A'}</span>
            <span className="sidebar-profile-copy">
              <strong>{displayName}</strong>
              <small>Platform administrator</small>
            </span>
          </div>
          <button className="sidebar-logout" onClick={handleLogout}>
            <FaSignOutAlt />
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <main className="app-main admin-main">
        <header className="workspace-header admin-header">
          <div className="workspace-header-left">
            <button className="mobile-menu" onClick={() => setMobileOpen(true)} aria-label="Open admin navigation">
              <FaBars />
            </button>
            <div className="workspace-context">
              <span>SkillSwap administration</span>
              <strong>Control center</strong>
            </div>
          </div>
          <div className="workspace-header-right">
            <Link to="/" className="se-btn se-btn-ghost se-btn-small">Public site</Link>
            <div className="header-user">
              <span className="avatar-fallback">{initials || 'A'}</span>
              <div>
                <strong>{displayName}</strong>
                <small>Administrator</small>
              </div>
            </div>
          </div>
        </header>
        <div className="page-container admin-page-container">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
