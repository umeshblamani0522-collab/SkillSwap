import React, { useEffect, useState } from 'react'
import { NavLink, Outlet, Link, useNavigate, useLocation } from 'react-router-dom'
import {
  FaHome, FaUser, FaSearch, FaExchangeAlt, FaComments, FaCalendarAlt,
  FaWallet, FaStar, FaSignOutAlt, FaBell, FaBrain, FaGraduationCap, FaLifeRing,
  FaBars, FaTimes, FaChevronRight,
} from 'react-icons/fa'
import { useAuth } from '../context/auth'
import { notificationService } from '../services/notifications'
import { chatService } from '../services/chat'
import VideoRequestPopup from '../components/common/video-request-popup'
import NotificationDrawer from '../components/common/notification-drawer'

const mainNav = [
  { to: '/dashboard', label: 'Overview', icon: <FaHome /> },
  { to: '/my-skills', label: 'My Skills', icon: <FaGraduationCap /> },
  { to: '/availability', label: 'Availability', icon: <FaCalendarAlt /> },
  { to: '/find-people', label: 'Discover', icon: <FaSearch /> },
  { to: '/matches', label: 'Matches', icon: <FaBrain /> },
  { to: '/exchange-requests', label: 'Exchanges', icon: <FaExchangeAlt /> },
  { to: '/sessions', label: 'Sessions', icon: <FaCalendarAlt /> },
]

const communicationNav = [
  { to: '/chat', label: 'Messages', icon: <FaComments />, badgeKey: 'messages' },
  { to: '/notifications', label: 'Notifications', icon: <FaBell />, badgeKey: 'notifications' },
]

const activityNav = [
  { to: '/wallet', label: 'Credits', icon: <FaWallet /> },
  { to: '/ratings', label: 'Ratings', icon: <FaStar /> },
  { to: '/support', label: 'Safety & Support', icon: <FaLifeRing /> },
]

export default function UserLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [unreadNotifications, setUnreadNotifications] = useState(0)
  const [unreadMessages, setUnreadMessages] = useState(0)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationDrawerOpen, setNotificationDrawerOpen] = useState(false)

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const [n, m] = await Promise.all([notificationService.unreadCount(), chatService.unreadCount()])
        setUnreadNotifications(n)
        setUnreadMessages(m)
      } catch {
        // Badge counts are non-critical.
      }
    }
    fetchCounts()
    const interval = setInterval(fetchCounts, 20000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  const refreshNotificationCount = async () => {
    try { setUnreadNotifications(await notificationService.unreadCount()) } catch {}
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const badgeFor = (item) => {
    if (item.badgeKey === 'messages') return unreadMessages
    if (item.badgeKey === 'notifications') return unreadNotifications
    return 0
  }

  const displayName = user?.fullName || user?.username || 'Member'
  const initials = displayName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()

  const renderItem = (item) => {
    const badge = badgeFor(item)
    return (
      <NavLink key={item.to} to={item.to} className="se-nav-item">
        <span className="se-nav-item-left">
          <span className="se-nav-icon">{item.icon}</span>
          <span>{item.label}</span>
        </span>
        {badge > 0 && <span className="se-nav-badge">{badge > 9 ? '9+' : badge}</span>}
      </NavLink>
    )
  }

  return (
    <div className="app-shell">
      <div className={`mobile-overlay ${mobileOpen ? 'show' : ''}`} onClick={() => setMobileOpen(false)} />

      <aside className={`app-sidebar ${mobileOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <Link to="/dashboard" className="brand-lockup">
            <span className="brand-mark"><FaGraduationCap /></span>
            <span>
              <strong>SkillSwap</strong>
              <small>Learning workspace</small>
            </span>
          </Link>
          <button className="mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close navigation">
            <FaTimes />
          </button>
        </div>

        <div className="sidebar-scroll">
          <div className="sidebar-section-label">Workspace</div>
          <nav className="se-nav">{mainNav.map(renderItem)}</nav>

          <div className="sidebar-section-label">Communication</div>
          <nav className="se-nav">{communicationNav.map(renderItem)}</nav>

          <div className="sidebar-section-label">Activity</div>
          <nav className="se-nav">{activityNav.map(renderItem)}</nav>

        </div>

        <div className="sidebar-footer">
          <Link to="/profile" className="sidebar-profile">
            {user?.profilePictureUrl ? (
              <img src={user.profilePictureUrl} alt="Profile" />
            ) : (
              <span className="avatar-fallback">{initials || 'U'}</span>
            )}
            <span className="sidebar-profile-copy">
              <strong>{displayName}</strong>
              <small>View your profile</small>
            </span>
            <FaChevronRight className="nav-chevron" />
          </Link>
          <button className="sidebar-logout" onClick={handleLogout}>
            <FaSignOutAlt />
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <main className="app-main">
        <header className="workspace-header">
          <div className="workspace-header-left">
            <button className="mobile-menu" onClick={() => setMobileOpen(true)} aria-label="Open navigation">
              <FaBars />
            </button>
            <div className="workspace-context">
              <span>Skill exchange workspace</span>
              <strong>{location.pathname === '/dashboard' ? 'Overview' : 'Workspace'}</strong>
            </div>
          </div>
          <div className="workspace-header-right">
            <button className="header-icon-button" aria-label="Open notifications" onClick={() => setNotificationDrawerOpen(true)}>
              <FaBell />
              {unreadNotifications > 0 && <span className="header-dot" />}
            </button>
            <NavLink to="/chat" className="header-icon-button" aria-label="Messages">
              <FaComments />
              {unreadMessages > 0 && <span className="header-dot" />}
            </NavLink>
            <div className="header-user">
              {user?.profilePictureUrl ? (
                <img src={user.profilePictureUrl} alt="Avatar" />
              ) : (
                <span className="avatar-fallback">{initials || 'U'}</span>
              )}
              <div>
                <strong>{displayName}</strong>
                <small>Learner & mentor</small>
              </div>
            </div>
          </div>
        </header>
        <NotificationDrawer open={notificationDrawerOpen} onClose={() => setNotificationDrawerOpen(false)} onUnreadChange={refreshNotificationCount} />
        <div className="page-container">
          <Outlet />
        </div>
        <VideoRequestPopup />
      </main>
    </div>
  )
}
