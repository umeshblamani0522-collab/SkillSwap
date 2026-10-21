import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { JaaSMeeting } from '@jitsi/react-sdk'
import { FaArrowLeft, FaShieldAlt, FaVideo } from 'react-icons/fa'
import { partnerVideoService } from '../../services/partner-video'
import api from '../../services/api'
import { useAuth } from '../../context/auth'
import { onSocketConnect } from '../../websocket/socket'
import LoadingSpinner from '../../components/common/loading-spinner'
import { useToast } from '../../components/common/toast'

export default function PartnerVideo() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { push } = useToast()
  const [room, setRoom] = useState(null)
  const [loading, setLoading] = useState(true)
  const [ending, setEnding] = useState(false)
  const meetingApiRef = useRef(null)
  const handledRemoteEndRef = useRef(false)
  const endSentRef = useRef(false)

  useEffect(() => {
    let mounted = true
    let timer = null
    const wait = (ms) => new Promise((resolve) => { timer = setTimeout(resolve, ms) })

    const authorize = async () => {
      let lastError = null
      for (let attempt = 0; attempt < 3; attempt += 1) {
        try {
          const data = await partnerVideoService.authorizeRoom(roomId)
          if (!mounted) return
          if (data?.roomId && data.roomId !== roomId) {
            navigate(`/partner-video/${data.roomId}`, { replace: true })
            return
          }
          setRoom(data)
          return
        } catch (error) {
          lastError = error
          if (attempt < 2) await wait(700 * (attempt + 1))
        }
      }
      if (!mounted) return
      setLoading(false)
      push(lastError?.response?.data?.message || 'Could not open this SkillSwap video room. The session was not closed.', 'danger')
    }

    authorize().finally(() => mounted && setLoading(false))
    return () => { mounted = false; if (timer) clearTimeout(timer) }
  }, [roomId, navigate, push])

  useEffect(() => {
    if (!roomId) return undefined

    let active = true
    let unsubscribeConnect = null
    let subscription = null

    const subscribeToRoom = (client) => {
      if (!active || !client?.connected) return
      if (subscription) {
        try { subscription.unsubscribe() } catch (_) {}
        subscription = null
      }
      subscription = client.subscribe(`/topic/video-room/${roomId}`, (message) => {
        try {
          const event = JSON.parse(message.body || '{}')
          if (event.type !== 'VIDEO_ROOM_ENDED') return
          if (String(event.endedByUserId) === String(user?.id)) return
          handledRemoteEndRef.current = true
          try { meetingApiRef.current?.executeCommand?.('hangup') } catch (_) {}
          try { meetingApiRef.current?.dispose?.() } catch (_) {}
          navigate('/matches', { replace: true })
          push('The other participant ended the video session.', 'info')
        } catch (_) {
          // Ignore malformed websocket events.
        }
      })
    }

    unsubscribeConnect = onSocketConnect(subscribeToRoom)

    return () => {
      active = false
      if (subscription) {
        try { subscription.unsubscribe() } catch (_) {}
      }
      if (unsubscribeConnect) unsubscribeConnect()
    }
  }, [roomId, navigate, push, user?.id])

  const notifyRoomEnded = async () => {
    if (!roomId || endSentRef.current || handledRemoteEndRef.current) return
    endSentRef.current = true
    try {
      await partnerVideoService.endRoom(roomId)
    } catch (_) {
      // The local call can still close even if the notification request fails.
    }
  }

  const handleReadyToClose = async () => {
    if (handledRemoteEndRef.current) {
      navigate('/matches', { replace: true })
      return
    }
    setEnding(true)
    await notifyRoomEnded()
    navigate('/matches', { replace: true })
  }

  useEffect(() => {
    const onPageHide = () => {
      if (!roomId || handledRemoteEndRef.current || endSentRef.current) return
      endSentRef.current = true
      const base = api.defaults.baseURL || window.location.origin
      const target = new URL(`/partner-video/rooms/${roomId}/end`, base).toString()
      try {
        fetch(target, {
          method: 'POST',
          headers: { Authorization: `Bearer ${localStorage.getItem('se_token') || ''}` },
          keepalive: true,
        })
      } catch (_) {}
    }
    window.addEventListener('pagehide', onPageHide)
    return () => window.removeEventListener('pagehide', onPageHide)
  }, [roomId])

  if (loading) return <LoadingSpinner label="Authorizing partner video room..." />

  if (!room) return null

  const displayName = user?.fullName || user?.username || 'SkillSwap Member'

  return (
    <div className="partner-video-page">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
        <div>
          <button
            type="button"
            className="btn btn-link p-0 text-decoration-none small fw-semibold"
            onClick={async () => {
              await notifyRoomEnded()
              navigate('/matches', { replace: true })
            }}
          >
            <FaArrowLeft className="me-2" />Back to Matches
          </button>
          <div className="d-flex align-items-center gap-2 mt-2">
            <div className="se-icon-box primary rounded-3"><FaVideo /></div>
            <div>
              <h3 className="mb-0">Partner video introduction</h3>
              <div className="text-muted small">Private room with @{room.partnerUsername}</div>
            </div>
          </div>
        </div>
        <div className="small text-muted d-flex align-items-center gap-2">
          <FaShieldAlt /> Match eligibility verified · both participants enter automatically after approval
        </div>
      </div>

      <div className="se-card p-2 overflow-hidden" style={{ minHeight: '78vh' }}>
        <JaaSMeeting
          appId={room.appId}
          roomName={room.jitsiRoomName}
          jwt={room.jwt}
          useStaging={room.useStaging}
          configOverwrite={{
            startWithAudioMuted: false,
            startWithVideoMuted: false,
            prejoinPageEnabled: false,
            prejoinConfig: { enabled: false },
            disableAP: true,
            enableLobbyChat: false,
          }}
          interfaceConfigOverwrite={{
            DISABLE_JOIN_LEAVE_NOTIFICATIONS: true,
            MOBILE_APP_PROMO: false,
          }}
          userInfo={{ displayName }}
          onApiReady={(api) => { meetingApiRef.current = api }}
          onReadyToClose={handleReadyToClose}
          getIFrameRef={(iframeRef) => {
            iframeRef.style.height = '78vh'
            iframeRef.style.width = '100%'
            iframeRef.style.border = '0'
            iframeRef.style.borderRadius = '0.75rem'
          }}
        />
      </div>

      {ending && (
        <div className="small text-muted mt-2">Ending video session for both participants...</div>
      )}
    </div>
  )
}
