import React, { useEffect, useState } from 'react'
import { FaBell, FaCheck, FaRegBell, FaTrash } from 'react-icons/fa'
import { notificationService } from '../../services/notifications'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

export default function Notifications() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)

  const load = async () => {
    const res = await notificationService.list(0, 50)
    setNotifications(res.content)
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const handleRead = async (id) => {
    await notificationService.markRead(id)
    load()
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this notification?')) return
    try {
      await notificationService.remove(id)
      setNotifications((prev) => prev.filter((n) => n.id !== id))
    } catch (_) {}
  }

  const markAllRead = async () => {
    const unread = notifications.filter(n => !n.read)
    for (const n of unread) {
      await notificationService.markRead(n.id)
    }
    load()
  }

  if (loading) return <LoadingSpinner />

  const unreadCount = notifications.filter(n => !n.read).length

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
          <h2 className="mb-1 fw-bold"> <span className="se-gradient-text">Notifications</span></h2>
          <p className="text-muted mb-0">Stay updated on your exchanges, messages, and account activity.</p>
        </div>
        <div className="d-flex align-items-center gap-4">
          {unreadCount > 0 && (
            <button className="btn btn-sm btn-outline-primary rounded-pill px-4 fw-semibold" onClick={markAllRead}>
              Mark all as read
            </button>
          )}
          <div className="se-icon-box primary rounded-circle position-relative d-none d-sm-flex" style={{ width: '64px', height: '64px', fontSize: '2rem' }}>
            <FaBell />
            {unreadCount > 0 && (
              <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-white border-3">
                {unreadCount}
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="se-card p-4 animate-slide-up delay-100 border-0 shadow-sm" style={{ minHeight: '50vh' }}>
        {notifications.length === 0 ? (
          <div className="py-5">
            <EmptyState title="You're all caught up" subtitle="New notifications will appear here." />
          </div>
        ) : (
          <div className="d-flex flex-column gap-3">
            {notifications.map((n, i) => (
              <div
                key={n.id}
                className={`se-list-item d-flex justify-content-between align-items-center mb-0 rounded-4 border-0 shadow-sm animate-slide-up delay-${Math.min(300, (i % 3 + 1) * 100)} ${!n.read ? 'bg-primary bg-opacity-10' : 'bg-light'}`}
                style={{ transition: 'all 0.2s ease', borderLeft: !n.read ? '4px solid var(--se-primary)' : '4px solid transparent' }}
              >
                <div className="d-flex align-items-start gap-3">
                  <div className={`mt-1 flex-shrink-0 ${!n.read ? 'text-primary' : 'text-muted'}`}>
                    {!n.read ? <FaBell /> : <FaRegBell />}
                  </div>
                  <div>
                    <div className={`fs-5 mb-1 ${!n.read ? 'fw-bold text-dark' : 'fw-medium text-secondary'}`}>
                      {n.content}
                    </div>
                    {n.type === 'VIDEO_CALL_REQUEST' && (
                      <div className="small text-primary fw-semibold mt-1">
                        A private video request is waiting in your workspace.
                      </div>
                    )}
                    <div className="text-muted small fw-medium">
                      {new Date(n.createdAt).toLocaleString(undefined, {
                        weekday: 'short', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                      })}
                    </div>
                  </div>
                </div>
                <div className="d-flex align-items-center gap-2 flex-shrink-0">
                  {!n.read && (
                    <button
                      className="btn btn-sm btn-light text-primary border border-primary border-opacity-25 rounded-circle shadow-sm d-flex align-items-center justify-content-center transition-all"
                      style={{ width: '40px', height: '40px' }}
                      onClick={() => handleRead(n.id)}
                      title="Mark as read"
                      aria-label="Mark as read"
                    >
                      <FaCheck />
                    </button>
                  )}
                  <button
                    className="btn btn-sm btn-light text-danger border border-danger border-opacity-25 rounded-circle shadow-sm d-flex align-items-center justify-content-center transition-all"
                    style={{ width: '40px', height: '40px' }}
                    onClick={() => handleDelete(n.id)}
                    title="Delete notification"
                    aria-label="Delete notification"
                  >
                    <FaTrash />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
