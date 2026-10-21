import React, { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { chatService } from '../../services/chat'
import { userService } from '../../services/users'
import { useAuth } from '../../context/auth'
import { getSocketClient, onSocketConnect, sendChatMessage, sendTyping } from '../../websocket/socket'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmojiPicker from 'emoji-picker-react'
import { FaPaperPlane, FaSmile, FaBan, FaCheck, FaTimes, FaTrashAlt } from 'react-icons/fa'

export default function Chat() {
  const { userId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [conversations, setConversations] = useState([])
  const [activeUser, setActiveUser] = useState(null)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [loading, setLoading] = useState(true)
  const [otherTyping, setOtherTyping] = useState(false)
  const [showEmojiPicker, setShowEmojiPicker] = useState(false)
  const [connectionStatus, setConnectionStatus] = useState(null)
  const [actionUserId, setActionUserId] = useState(null)
  const [deletingMessageId, setDeletingMessageId] = useState(null)
  const bottomRef = useRef(null)
  const typingTimeout = useRef(null)

  useEffect(() => {
    const load = async () => {
      const res = await chatService.conversations()
      setConversations(res.filter((u) => u.id !== user?.id))
      setLoading(false)
    }
    load()
  }, [user])

  useEffect(() => {
    if (!userId) return
    const loadConversation = async () => {
      const profile = await userService.getById(userId)
      setActiveUser(profile)
      const res = await chatService.conversation(userId, 0, 100)
      setMessages(res.content)
      await chatService.markRead(userId)

      const statusRes = await chatService.getStatus(userId)
      setConnectionStatus(statusRes.status)
      setActionUserId(statusRes.actionUserId)
    }
    loadConversation()
    setShowEmojiPicker(false)
  }, [userId])

  useEffect(() => {
    const client = getSocketClient()
    let messageSubscription = null
    let typingSubscription = null

    const subscribe = () => {
      messageSubscription?.unsubscribe()
      typingSubscription?.unsubscribe()
      if (!client.connected) return

      messageSubscription = client.subscribe('/user/queue/messages', (frame) => {
        const msg = JSON.parse(frame.body)
        if (msg?.type === 'MESSAGE_DELETED') {
          setMessages((prev) => prev.filter((item) => String(item.id) !== String(msg.messageId)))
          return
        }
        if (msg?.id != null && (String(msg.senderId) === userId || String(msg.receiverId) === userId)) {
          setMessages((prev) => prev.some((item) => String(item.id) === String(msg.id)) ? prev : [...prev, msg])
        }
      })
      typingSubscription = client.subscribe('/user/queue/typing', (frame) => {
        const payload = JSON.parse(frame.body)
        if (String(payload.fromUserId) === userId) {
          setOtherTyping(!!payload.typing)
          if (payload.typing) setTimeout(() => setOtherTyping(false), 3000)
        }
      })
    }

    const removeConnectListener = onSocketConnect(subscribe)
    if (client.connected) subscribe()

    return () => {
      removeConnectListener()
      messageSubscription?.unsubscribe()
      typingSubscription?.unsubscribe()
    }
  }, [userId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = (e) => {
    e.preventDefault()
    const content = draft.trim()
    if (!content || !userId) return
    sendChatMessage(Number(userId), content)
    setDraft('')
    setShowEmojiPicker(false)
  }

  const handleDeleteMessage = async (messageId) => {
    if (!messageId || deletingMessageId) return
    const confirmed = window.confirm('Delete this message? This action cannot be undone.')
    if (!confirmed) return

    try {
      setDeletingMessageId(messageId)
      await chatService.deleteMessage(messageId)
      setMessages((prev) => prev.filter((item) => String(item.id) !== String(messageId)))
    } catch (error) {
      window.alert(error?.response?.data?.message || 'Unable to delete the message.')
    } finally {
      setDeletingMessageId(null)
    }
  }

  const handleTyping = (value) => {
    setDraft(value)
    if (!userId) return
    sendTyping(Number(userId), true)
    clearTimeout(typingTimeout.current)
    typingTimeout.current = setTimeout(() => sendTyping(Number(userId), false), 1500)
  }

  const onEmojiClick = (emojiObject) => {
    setDraft(prev => prev + emojiObject.emoji)
  }

  const handleAccept = async () => {
    await chatService.accept(userId)
    setConnectionStatus('ACCEPTED')
  }

  const handleReject = async () => {
    await chatService.reject(userId)
    setConnectionStatus('BLOCKED')
  }

  const handleBlock = async () => {
    await chatService.block(userId)
    setConnectionStatus('BLOCKED')
    setActionUserId(user.id)
  }

  const handleUnblock = async () => {
    await chatService.unblock(userId)
    setConnectionStatus('ACCEPTED')
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="container-fluid p-0">
      <div className="se-card animate-slide-up border-0 shadow-sm d-flex overflow-hidden" style={{ height: '82vh', borderRadius: '24px' }}>

        {/* Sidebar - Chat List */}
        <div style={{ width: 320, backgroundColor: 'var(--se-surface)' }} className="border-end d-flex flex-column flex-shrink-0 z-2 shadow-sm">
          <div className="p-4 bg-white border-bottom">
            <h5 className="mb-0 fw-bold">Messages</h5>
          </div>
          <div className="flex-grow-1 overflow-auto bg-light">
            {conversations.map((c) => (
              <div key={c.id}
                className={`p-3 d-flex align-items-center gap-3 border-bottom border-light ${String(c.id) === userId ? 'bg-white' : 'bg-transparent'}`}
                style={{
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  borderLeft: String(c.id) === userId ? '4px solid var(--se-primary)' : '4px solid transparent'
                }}
                onClick={() => navigate(`/chat/${c.id}`)}>
                <img src={c.profilePictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(c.username)}&size=48&background=4f46e5&color=fff`}
                  width={48} height={48} className="rounded-circle shadow-sm" alt="" />
                <div className="flex-grow-1 min-w-0">
                  <div className={`fw-semibold text-truncate ${String(c.id) === userId ? 'text-primary' : 'text-dark'}`}>
                    {c.fullName || c.username}
                  </div>
                  <div className="text-muted small text-truncate">@{c.username}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Chat Area */}
        <div className="flex-grow-1 d-flex flex-column position-relative z-1" style={{ backgroundColor: '#f8fafc' }}>
          {!activeUser ? (
            <div className="flex-grow-1 d-flex align-items-center justify-content-center">
              <div className="text-center p-5 animate-slide-up delay-100">
                <div className="se-icon-box primary mx-auto mb-3 rounded-circle" style={{ width: '80px', height: '80px', fontSize: '2.5rem' }}>
                  <FaPaperPlane />
                </div>
                <h4 className="fw-bold mb-2">Welcome to SkillSwap Chat</h4>
                <p className="text-muted">Select a conversation from the left to start messaging.</p>
              </div>
            </div>
          ) : (
            <>
              {/* Chat Header */}
              <div className="bg-white px-4 py-3 d-flex align-items-center justify-content-between border-bottom shadow-sm z-1">
                <div className="d-flex align-items-center gap-3">
                  <img src={activeUser.profilePictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(activeUser.username)}&size=48&background=4f46e5&color=fff`}
                    width={48} height={48} className="rounded-circle shadow-sm" alt="" />
                  <div>
                    <div className="fw-bold fs-5 text-dark">{activeUser.fullName || activeUser.username}</div>
                    <div className="text-muted small fw-medium" style={{ height: '20px' }}>
                      {otherTyping ? <span className="text-primary animate-pulse">typing...</span> : `@${activeUser.username}`}
                    </div>
                  </div>
                </div>
                <div>
                   {connectionStatus !== 'BLOCKED' && (
                     <button className="btn btn-sm btn-light text-danger fw-semibold rounded-pill px-3 py-2 border hover-bg-danger hover-text-white transition-all d-flex align-items-center gap-2" onClick={handleBlock}>
                       <FaBan /> Block
                     </button>
                   )}
                </div>
              </div>

              {/* Messages Container */}
              <div className="flex-grow-1 overflow-auto p-4 d-flex flex-column gap-3" style={{ backgroundImage: 'radial-gradient(#e2e8f0 1px, transparent 1px)', backgroundSize: '20px 20px' }}>
                {messages.map((m) => {
                  const isMe = m.senderId === user.id
                  return (
                    <div key={m.id} className={`d-flex ${isMe ? 'justify-content-end' : 'justify-content-start'} align-items-end gap-2`}>
                      {isMe && (
                        <button
                          type="button"
                          className="btn btn-sm btn-light border shadow-sm rounded-circle text-danger d-flex align-items-center justify-content-center"
                          style={{ width: 34, height: 34, flex: '0 0 auto' }}
                          title="Delete message"
                          aria-label="Delete message"
                          disabled={deletingMessageId === m.id}
                          onClick={() => handleDeleteMessage(m.id)}
                        >
                          <FaTrashAlt size={13} />
                        </button>
                      )}
                      <div className={`p-3 rounded-4 shadow-sm position-relative animate-slide-up`}
                        style={{
                          maxWidth: '75%',
                          backgroundColor: isMe ? 'var(--se-primary)' : '#ffffff',
                          color: isMe ? '#ffffff' : 'var(--se-text)',
                          borderBottomRightRadius: isMe ? '4px' : '1rem',
                          borderBottomLeftRadius: !isMe ? '4px' : '1rem',
                          animationDuration: '0.3s'
                        }}>
                        <div style={{ wordBreak: 'break-word', fontSize: '0.95rem', lineHeight: '1.5' }}>{m.content}</div>
                        <div className="d-flex justify-content-end align-items-center mt-2 gap-1" style={{ fontSize: '0.7rem', color: isMe ? 'rgba(255,255,255,0.7)' : '#94a3b8' }}>
                          <span>{new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        </div>
                      </div>
                    </div>
                  )
                })}
                <div ref={bottomRef} />
              </div>

              {/* Status & Input Area */}
              {connectionStatus === 'BLOCKED' ? (
                 <div className="p-3 bg-white text-center text-muted border-top shadow-sm z-2">
                   {Number(actionUserId) === Number(user?.id) ? (
                     <div className="d-flex align-items-center justify-content-center gap-3">
                       <span className="fw-medium">You blocked this user.</span>
                       <button className="btn btn-sm se-btn-primary rounded-pill px-4" onClick={handleUnblock}>Unblock</button>
                     </div>
                   ) : <span className="fw-medium text-danger">You cannot send messages to this user.</span>}
                 </div>
              ) : connectionStatus === 'PENDING' && Number(actionUserId) !== Number(user?.id) ? (
                <div className="p-4 bg-white border-top shadow-lg text-center z-2">
                  <h6 className="mb-3 fw-bold text-dark">@{activeUser.username} wants to connect with you.</h6>
                  <div className="d-flex justify-content-center gap-3">
                    <button className="btn btn-light text-danger border rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2" onClick={handleReject}><FaTimes /> Reject</button>
                    <button className="btn btn-success rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2 shadow-sm" onClick={handleAccept}><FaCheck /> Accept</button>
                  </div>
                </div>
              ) : connectionStatus === 'NONE' ? (
                <div className="p-4 bg-white border-top text-center text-muted">
                  <div className="fw-semibold text-dark mb-1">Messaging is locked</div>
                  <div className="small">Accepting an exchange request creates the connection needed for private chat.</div>
                </div>
              ) : (
                <div className="bg-white p-3 border-top shadow-sm z-2 position-relative">
                  {showEmojiPicker && (
                    <div className="position-absolute shadow-lg rounded-4 overflow-hidden" style={{ bottom: '100%', left: 20, zIndex: 1000, marginBottom: '15px' }}>
                      <EmojiPicker onEmojiClick={onEmojiClick} searchDisabled skinTonesDisabled />
                    </div>
                  )}
                  <form className="d-flex gap-3 align-items-center bg-light rounded-pill p-2 border" onSubmit={handleSend}>
                    <button type="button" className="btn btn-light rounded-circle text-muted d-flex align-items-center justify-content-center border-0 shadow-none hover-text-primary transition-all"
                      onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                      style={{ width: 44, height: 44, fontSize: '1.2rem' }}>
                      <FaSmile />
                    </button>
                    <input className="form-control rounded-pill px-2 py-2 bg-transparent border-0 shadow-none fs-6"
                      placeholder="Type your message..."
                      value={draft}
                      onChange={(e) => handleTyping(e.target.value)} />
                    <button type="submit" className="btn se-btn-primary rounded-circle d-flex align-items-center justify-content-center flex-shrink-0 shadow-sm transition-all"
                      style={{ width: 44, height: 44, fontSize: '1.1rem' }}
                      disabled={!draft.trim()}>
                      <FaPaperPlane style={{ marginLeft: '-2px' }} />
                    </button>
                  </form>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
