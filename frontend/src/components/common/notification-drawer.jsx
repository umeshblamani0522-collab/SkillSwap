import React, { useEffect, useState } from 'react'
import { FaBell, FaCheck, FaExternalLinkAlt, FaTrash, FaTimes } from 'react-icons/fa'
import { notificationService } from '../../services/notifications'

export default function NotificationDrawer({ open, onClose, onUnreadChange }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [busyId, setBusyId] = useState(null)

  useEffect(() => {
    if (!open) return
    let active = true
    setLoading(true)
    notificationService.list(0, 12)
      .then((data) => { if (active) setItems(data.content || []) })
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [open])

  const markRead = async (id) => {
    setBusyId(id)
    try {
      await notificationService.markRead(id)
      setItems((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n))
      onUnreadChange?.()
    } finally {
      setBusyId(null)
    }
  }

  const remove = async (id) => {
    if (!window.confirm('Delete this notification?')) return
    setBusyId(id)
    try {
      await notificationService.remove(id)
      setItems((prev) => prev.filter((n) => n.id !== id))
      onUnreadChange?.()
    } finally {
      setBusyId(null)
    }
  }

  if (!open) return null

  return (
    <>
      <div className="notification-drawer-backdrop" onClick={onClose} />
      <aside className="notification-drawer" aria-label="Notification centre">
        <div className="notification-drawer-head">
          <div>
            <span className="drawer-kicker"><FaBell /> Notifications</span>
            <h3>Notification centre</h3>
            <p>Stay on top of your exchanges and sessions.</p>
          </div>
          <button className="drawer-close" onClick={onClose} aria-label="Close notifications"><FaTimes /></button>
        </div>

        <div className="notification-drawer-list">
          {loading ? (
            <div className="drawer-empty">Loading notifications…</div>
          ) : items.length === 0 ? (
            <div className="drawer-empty">
              <FaBell />
              <strong>You're all caught up</strong>
              <span>New activity will appear here.</span>
            </div>
          ) : items.map((n) => (
            <div key={n.id} className={`drawer-notification ${n.read ? 'is-read' : 'is-unread'}`}>
              <div className="drawer-notification-main">
                <div className="drawer-notification-icon"><FaBell /></div>
                <div className="drawer-notification-copy">
                  <strong>{n.content}</strong>
                  {n.type === 'VIDEO_CALL_REQUEST' && <span className="drawer-notification-meta">Video introduction request</span>}
                  <time>{new Date(n.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</time>
                </div>
              </div>
              <div className="drawer-notification-actions">
                {!n.read && (
                  <button onClick={() => markRead(n.id)} disabled={busyId === n.id} title="Mark as read" aria-label="Mark as read">
                    <FaCheck />
                  </button>
                )}
                {n.linkUrl && (
                  <a href={n.linkUrl} onClick={() => { if (!n.read) markRead(n.id); onClose() }} title="Open" aria-label="Open notification">
                    <FaExternalLinkAlt />
                  </a>
                )}
                <button className="drawer-delete" onClick={() => remove(n.id)} disabled={busyId === n.id} title="Delete" aria-label="Delete notification">
                  <FaTrash />
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="notification-drawer-foot">
          <a href="/notifications" onClick={onClose}>View all notifications</a>
        </div>
      </aside>
    </>
  )
}
