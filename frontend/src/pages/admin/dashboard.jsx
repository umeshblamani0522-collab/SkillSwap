import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import {
  FaUsers, FaUserCheck, FaExchangeAlt, FaCalendarCheck, FaFlag,
  FaGraduationCap, FaArrowRight, FaChartLine,
} from 'react-icons/fa'
import { adminService } from '../../services/admin'
import LoadingSpinner from '../../components/common/loading-spinner'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    adminService.dashboard()
      .then((data) => setStats(data))
      .catch((err) => setError(err.response?.data?.message || 'Unable to load administration metrics.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner label="Loading administration dashboard..." />
  if (!stats) return <div className="se-alert danger">{error || 'Administration data is unavailable.'}</div>

  const metrics = [
    { label: 'Total users', value: stats.totalUsers ?? 0, note: 'All registered members', icon: <FaUsers />, tone: 'blue' },
    { label: 'Active users', value: stats.activeUsers ?? 0, note: 'Currently active accounts', icon: <FaUserCheck />, tone: 'green' },
    { label: 'Exchanges', value: stats.totalExchanges ?? 0, note: 'All exchange requests', icon: <FaExchangeAlt />, tone: 'violet' },
    { label: 'Completed sessions', value: stats.completedSessions ?? 0, note: 'Finished learning sessions', icon: <FaCalendarCheck />, tone: 'gold' },
  ]

  const moderation = [
    { label: 'Open reports', value: stats.openReports ?? 0, href: '/admin/reports', icon: <FaFlag />, tone: 'danger', action: true },
    { label: 'Successful exchanges', value: stats.successfulExchanges ?? 0, icon: <FaExchangeAlt />, tone: 'green' },
    { label: 'Cancelled exchanges', value: stats.cancelledExchanges ?? 0, icon: <FaChartLine />, tone: 'muted' },
    { label: 'Average rating', value: Number.isFinite(stats.averageRating) ? stats.averageRating.toFixed(2) : '—', icon: <FaGraduationCap />, tone: 'blue' },
  ]

  return (
    <div className="admin-dashboard-page">
      <section className="admin-welcome">
        <div>
          <span className="eyebrow muted">Administration</span>
          <h1>Dashboard</h1>
          <p>Monitor growth, manage members, maintain the skill catalogue and review moderation activity.</p>
        </div>
        <div className="admin-welcome-actions">
          <Link to="/admin/users" className="se-btn se-btn-secondary">Manage users <FaArrowRight /></Link>
          <Link to="/admin/reports" className="se-btn se-btn-primary">Review reports <FaArrowRight /></Link>
        </div>
      </section>

      <div className="admin-metric-grid">
        {metrics.map((metric) => (
          <div className="admin-metric-card" key={metric.label}>
            <div className={`admin-metric-icon ${metric.tone}`}>{metric.icon}</div>
            <div>
              <span>{metric.label}</span>
              <strong>{metric.value}</strong>
              <small>{metric.note}</small>
            </div>
          </div>
        ))}
      </div>

      <div className="admin-content-grid">
        <section className="content-card admin-chart-card">
          <div className="card-heading">
            <div>
              <span className="card-kicker">Platform insight</span>
              <h2>Popular skills</h2>
            </div>
            <Link to="/admin/skills" className="subtle-link">Manage catalogue <FaArrowRight /></Link>
          </div>
          {stats.popularSkills?.length ? (
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={stats.popularSkills} margin={{ top: 10, right: 12, left: -18, bottom: 35 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="skill" tick={{ fontSize: 11 }} angle={-20} textAnchor="end" height={65} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#2563eb" radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="dashboard-empty-state compact"><strong>No skill activity yet</strong><p>Skill usage will appear here as members build their profiles.</p></div>
          )}
        </section>

        <section className="content-card admin-activity-card">
          <div className="card-heading">
            <div>
              <span className="card-kicker">Operational snapshot</span>
              <h2>Platform health</h2>
            </div>
          </div>
          <div className="admin-snapshot-list">
            {moderation.map((item) => {
              const content = (
                <>
                  <span className={`admin-snapshot-icon ${item.tone}`}>{item.icon}</span>
                  <span>
                    <strong>{item.label}</strong>
                    <small>{item.action ? 'Review moderation queue' : 'Current platform metric'}</small>
                  </span>
                  <b>{item.value}</b>
                </>
              )
              return item.action ? (
                <Link key={item.label} to={item.href} className="admin-snapshot-row">{content}</Link>
              ) : (
                <div key={item.label} className="admin-snapshot-row">{content}</div>
              )
            })}
          </div>
          <div className="admin-callout">
            <span><FaUsers /></span>
            <div>
              <strong>{stats.newUsersLast30Days ?? 0} new users</strong>
              <p>Joined in the last 30 days.</p>
            </div>
          </div>
        </section>
      </div>

      <section className="content-card admin-active-card">
        <div className="card-heading">
          <div>
            <span className="card-kicker">Engagement</span>
            <h2>Most active members</h2>
          </div>
          <Link to="/admin/users" className="subtle-link">View users <FaArrowRight /></Link>
        </div>
        {stats.mostActiveUsers?.length ? (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stats.mostActiveUsers} margin={{ top: 10, right: 12, left: -18, bottom: 35 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="username" tick={{ fontSize: 11 }} angle={-20} textAnchor="end" height={65} />
              <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
              <Tooltip />
              <Bar dataKey="sessions" fill="#14b8a6" radius={[5, 5, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="dashboard-empty-state compact"><strong>No session activity yet</strong><p>Active member data will appear after sessions are completed.</p></div>
        )}
      </section>
    </div>
  )
}
