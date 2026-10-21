import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// Use the current browser origin. Vite proxies /ws to Spring Boot, so
// localhost and LAN access (for example 192.168.0.101:5173) use the same code.
const WS_BASE_URL =
  import.meta.env.VITE_WS_BASE_URL || `${window.location.origin}/ws`

let client = null
const connectListeners = new Set()

function notifyConnect(clientInstance) {
  for (const listener of [...connectListeners]) {
    try { listener(clientInstance) } catch (_) { /* one listener must not break the others */ }
  }
}

export function getSocketClient() {
  if (client) return client

  const token = localStorage.getItem('se_token')

  client = new Client({
    webSocketFactory: () => new SockJS(WS_BASE_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    onConnect: () => notifyConnect(client),
  })

  client.activate()
  return client
}

export function onSocketConnect(listener) {
  connectListeners.add(listener)
  const c = getSocketClient()
  if (c.connected) {
    queueMicrotask(() => {
      if (connectListeners.has(listener)) {
        try { listener(c) } catch (_) {}
      }
    })
  }
  return () => connectListeners.delete(listener)
}

export function disconnectSocketClient() {
  if (client) {
    client.deactivate()
    client = null
  }
  connectListeners.clear()
}

export function sendChatMessage(receiverId, content) {
  const c = getSocketClient()
  c.publish({ destination: '/app/chat.send', body: JSON.stringify({ receiverId, content }) })
}

export function sendTyping(receiverId, typing) {
  const c = getSocketClient()
  c.publish({ destination: '/app/chat.typing', body: JSON.stringify({ receiverId, typing }) })
}
