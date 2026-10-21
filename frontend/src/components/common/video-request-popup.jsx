import React, { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { FaVideo, FaTimes } from 'react-icons/fa'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/auth'
import { videoRequestService } from '../../services/video-requests'
import { partnerVideoService } from '../../services/partner-video'
import { getSocketClient, onSocketConnect } from '../../websocket/socket'
import { useToast } from './toast'

function requestIdFromLink(linkUrl) {
  const match = String(linkUrl || '').match(/\/video-requests\/(\d+)/)
  return match ? Number(match[1]) : null
}

function roomIdFromLink(linkUrl) {
  const match = String(linkUrl || '').match(/\/partner-video\/([^/?]+)/)
  return match ? match[1] : null
}

function autoJoinGuardKey(roomId) {
  return `sk_auto_joined_video_room_${roomId}`
}

function hasRecentAutoJoinGuard(roomId) {
  const value = sessionStorage.getItem(autoJoinGuardKey(roomId))
  if (!value) return false
  const timestamp = Number(value)
  if (!Number.isFinite(timestamp)) {
    sessionStorage.removeItem(autoJoinGuardKey(roomId))
    return false
  }
  // Prevent duplicate websocket + polling navigation, but allow the same
  // room id to be used again for a later video call.
  if (Date.now() - timestamp < 30_000) return true
  sessionStorage.removeItem(autoJoinGuardKey(roomId))
  return false
}

export default function VideoRequestPopup() {
  const [request, setRequest] = useState(null)
  const [busy, setBusy] = useState(false)
  const [dismissedIds, setDismissedIds] = useState(() => new Set())
  const navigate = useNavigate()
  const { push } = useToast()
  const { user } = useAuth()

  const openRoomOnce = (roomId, message) => {
    const roomKey = String(roomId || '')
    if (!roomKey || window.location.pathname === `/partner-video/${roomKey}`) return false
    const guard = autoJoinGuardKey(roomKey)
    if (hasRecentAutoJoinGuard(roomKey)) return false
    sessionStorage.setItem(guard, String(Date.now()))
    push(message, 'success')
    navigate(`/partner-video/${roomKey}`)
    return true
  }

  useEffect(() => {
    const client = getSocketClient()
    let notificationSubscription = null
    let requestSubscription = null
    let roomSubscription = null

    const subscribe = () => {
      notificationSubscription?.unsubscribe()
      requestSubscription?.unsubscribe()
      roomSubscription?.unsubscribe()
      if (!client.connected) return

      // Dedicated real-time video request channel. This message is sent by the
      // backend only to the intended receiver, so it can safely open the modal
      // without relying on the general notification stream.
      requestSubscription = client.subscribe('/user/queue/video-request', (frame) => {
        try {
          const incoming = JSON.parse(frame.body || '{}')
          const receiverId = Number(incoming?.receiverId)
          if (incoming?.status !== 'PENDING' || !incoming?.id ||
              (Number.isFinite(receiverId) && Number(user?.id) !== receiverId) ||
              dismissedIds.has(Number(incoming.id))) return
          setRequest({
            id: incoming.id,
            type: 'VIDEO_CALL_REQUEST',
            content: `${incoming.senderUsername || incoming.senderFullName || 'Your partner'} wants to start a video introduction with you`,
            linkUrl: `/video-requests/${incoming.id}`,
          })
        } catch (_) {}
      })

      notificationSubscription = client.subscribe('/user/queue/notifications', (frame) => {
        try {
          const notification = JSON.parse(frame.body || '{}')
          if (notification?.type === 'VIDEO_CALL_REQUEST' && notification?.linkUrl) {
            const id = requestIdFromLink(notification.linkUrl)
            if (!id || dismissedIds.has(id)) return
            // Never trust a notification alone to display the accept/reject modal.
            // Validate that this authenticated user is actually the receiver of
            // the still-pending request. This prevents old/read notifications
            // from making the modal appear on both sides of a video exchange.
            videoRequestService.incoming(0, 50).then((res) => {
              const items = Array.isArray(res?.content) ? res.content : []
              const pending = items.find((item) => Number(item?.id) === id && item?.status === 'PENDING')
              if (pending && !dismissedIds.has(id)) {
                setRequest({
                  id: pending.id,
                  type: 'VIDEO_CALL_REQUEST',
                  content: `${pending.senderUsername || pending.senderFullName || 'Your partner'} wants to start a video introduction with you`,
                  linkUrl: `/video-requests/${pending.id}`,
                })
              }
            }).catch(() => {})
            return
          }
          if (notification?.type === 'VIDEO_CALL_REQUEST_ACCEPTED' && notification?.linkUrl) {
            const roomKey = roomIdFromLink(notification.linkUrl)
            if (roomKey) openRoomOnce(roomKey, 'Video request accepted. Opening the private room…')
          }
        } catch (_) {}
      })

      // Primary realtime signal: the backend tells the partner that someone
      // actually entered the room.
      roomSubscription = client.subscribe('/user/queue/video-room', (frame) => {
        try {
          const event = JSON.parse(frame.body || '{}')
          if (event?.type === 'VIDEO_ROOM_PARTICIPANT_READY' && event?.roomId) {
            openRoomOnce(event.roomId, 'Your partner joined the SkillSwap video room. Opening it now…')
          }
        } catch (_) {}
      })
    }

    const remove = onSocketConnect(subscribe)
    if (client.connected) subscribe()
    return () => {
      remove()
      notificationSubscription?.unsubscribe()
      requestSubscription?.unsubscribe()
      roomSubscription?.unsubscribe()
    }
  }, [navigate, push, dismissedIds, user?.id])

  // Reliable fallback: if a browser missed the STOMP event, the backend keeps
  // a tiny presence marker on the room. Poll it until the partner has joined.
  useEffect(() => {
    let active = true
    const checkReadyRooms = async () => {
      try {
        const roomIds = await partnerVideoService.readyRooms()
        if (!active || !Array.isArray(roomIds)) return
        const candidate = roomIds.find((roomId) => window.location.pathname !== `/partner-video/${roomId}`)
        if (candidate) openRoomOnce(candidate, 'Your SkillSwap video room is ready. Opening it now…')
      } catch (_) {}
    }
    checkReadyRooms()
    const interval = setInterval(checkReadyRooms, 2000)
    return () => { active = false; clearInterval(interval) }
  }, [navigate, push])

  // Reliable incoming-request watcher. The database-backed pending request is
  // the source of truth, with notifications used as a secondary recovery path.
  useEffect(() => {
    let active = true

    const showRequest = (nextRequest) => {
      if (!active || !nextRequest) return
      const id = Number(nextRequest.id)
      if (!Number.isFinite(id) || dismissedIds.has(id)) return
      setRequest((current) => current?.id ? current : nextRequest)
    }

    const loadIncoming = async () => {
      // First check the actual pending-request table through the API.
      try {
        const res = await videoRequestService.incoming(0, 50)
        const items = Array.isArray(res?.content) ? res.content : []
        const pending = items
          .filter((item) => item?.status === 'PENDING' && !dismissedIds.has(item.id))
          .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))[0]
        if (pending) {
          showRequest({
            id: pending.id,
            type: 'VIDEO_CALL_REQUEST',
            content: `${pending.senderUsername || pending.senderFullName || 'Your partner'} wants to start a video introduction with you`,
            linkUrl: `/video-requests/${pending.id}`,
          })
          return
        }
      } catch (_) {
        // Continue to notification recovery below.
      }

    }

    loadIncoming()
    const interval = setInterval(loadIncoming, 1000)
    const onFocus = () => loadIncoming()
    window.addEventListener('focus', onFocus)
    document.addEventListener('visibilitychange', onFocus)
    return () => {
      active = false
      clearInterval(interval)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('visibilitychange', onFocus)
    }
  }, [dismissedIds])

  // Requester fallback: poll the request status when the acceptance websocket
  // was missed.
  useEffect(() => {
    let active = true
    const checkAcceptedOutgoing = async () => {
      try {
        const keys = Object.keys(localStorage).filter((key) => key.startsWith('sk_session_video_request_') || key.startsWith('sk_partner_video_request_'))
        if (!keys.length) return
        const ids = keys.map((key) => Number(key.split('_').pop())).filter(Number.isFinite)
        if (!ids.length) return
        const res = await videoRequestService.mine(0, 100)
        const accepted = (res.content || []).find((item) => ids.includes(item.id) && item.status === 'ACCEPTED' && item.roomId)
        if (!active || !accepted) return
        if (openRoomOnce(accepted.roomId, 'Video request accepted. Opening the private room…')) {
          keys.forEach((key) => localStorage.removeItem(key))
        }
      } catch (_) {}
    }
    checkAcceptedOutgoing()
    const interval = setInterval(checkAcceptedOutgoing, 2000)
    return () => { active = false; clearInterval(interval) }
  }, [navigate, push])

  const close = () => {
    if (request?.id) setDismissedIds((previous) => new Set(previous).add(request.id))
    setRequest(null)
  }

  const respond = async (accepted) => {
    const requestId = requestIdFromLink(request?.linkUrl)
    if (!requestId) return
    setBusy(true)
    try {
      const result = accepted ? await videoRequestService.accept(requestId) : await videoRequestService.reject(requestId)
      close()
      if (accepted && result.roomId) {
        openRoomOnce(result.roomId, 'Accepted. Opening the private SkillSwap video room…')
      } else {
        push(accepted ? 'Video introduction accepted.' : 'Video introduction declined.', accepted ? 'success' : 'secondary')
      }
    } catch (error) {
      push(error.response?.data?.message || error.response?.data?.error || 'Could not process the video request', 'danger')
    } finally {
      setBusy(false)
    }
  }

  if (!request) return null

  const modal = (
    <div className="video-request-overlay" role="dialog" aria-modal="true" aria-labelledby="video-request-title">
      <div className="video-request-modal">
        <button className="video-request-close" onClick={close} disabled={busy} aria-label="Close"><FaTimes /></button>
        <div className="video-request-icon"><FaVideo /></div>
        <span className="video-request-kicker">Video introduction request</span>
        <h3 id="video-request-title">Someone wants to meet you</h3>
        <p>{request.content}</p>
        <div className="video-request-note">Only the two participants in this SkillSwap exchange can enter the private room.</div>
        <div className="video-request-actions">
          <button className="btn btn-light border" onClick={() => respond(false)} disabled={busy}>Decline</button>
          <button className="btn btn-primary se-btn-primary" onClick={() => respond(true)} disabled={busy}>{busy ? 'Opening…' : 'Accept & Join'}</button>
        </div>
      </div>
    </div>
  )

  return typeof document === 'undefined' ? null : createPortal(modal, document.body)
}
