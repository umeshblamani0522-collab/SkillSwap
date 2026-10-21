# API Documentation

Base URL (local dev): `http://localhost:8080/api`
All endpoints except `/auth/**` and `GET /skills`, `GET /skill-categories`
require header: `Authorization: Bearer <jwt>`

Errors follow a consistent shape:
```json
{
  "timestamp": "2026-08-12T10:15:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "You cannot send an exchange request to yourself",
  "path": "/api/exchange-requests"
}
```

## Auth
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{username, email, password, fullName}` | Returns JWT + starter 5 credits |
| POST | `/auth/login` | `{usernameOrEmail, password}` | Returns JWT |

## Users
| Method | Path | Notes |
|---|---|---|
| GET | `/users/me` | Current user's full profile (skills, credits, rating) |
| PUT | `/users/me` | Update bio/location/languages/etc. |
| GET | `/users/{id}` | Public profile view |
| GET | `/users?q=&page=&size=` | Search/browse users |

## Skills
| Method | Path | Notes |
|---|---|---|
| GET | `/skill-categories` | Public |
| GET | `/skills?q=&page=&size=` | Public |
| POST | `/users/me/skills` | `{skillId, skillType, level, experienceDescription}` |
| PUT | `/users/me/skills/{id}` | Update level/notes |
| DELETE | `/users/me/skills/{id}` | Remove |

## Matching
| Method | Path | Notes |
|---|---|---|
| GET | `/matches?limit=20` | Ranked candidates for the current user |
| GET | `/matches/{candidateId}` | Score against one specific user |

## Exchange Requests
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/exchange-requests` | `{receiverId, offeredSkillId, requestedSkillId, durationHours, message}` | |
| GET | `/exchange-requests?page=&size=` | | All requests you sent or received |
| GET | `/exchange-requests/{id}` | | Participants only |
| POST | `/exchange-requests/{id}/accept` | | Receiver only |
| POST | `/exchange-requests/{id}/reject` | | Receiver only |
| POST | `/exchange-requests/{id}/cancel` | | Either participant |

## Sessions
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/sessions` | `{exchangeRequestId, sessionDate, startTime, endTime, meetingInfo, notes}` | Requires ACCEPTED exchange |
| GET | `/sessions` | | All sessions you teach or learn in |
| GET | `/sessions/{id}` | | Participants only |
| POST | `/sessions/{id}/complete` | | Transfers credits atomically |
| POST | `/sessions/{id}/cancel` | | |
| POST | `/sessions/{id}/no-show` | | |

## Credits
| Method | Path | Notes |
|---|---|---|
| GET | `/credits/wallet` | Balance, total earned, total spent |
| GET | `/credits/transactions?page=&size=` | Full ledger |

## Chat (REST)
| Method | Path | Notes |
|---|---|---|
| POST | `/chat/messages` | REST fallback; primary path is WebSocket |
| DELETE | `/chat/messages/{messageId}` | Deletes only a message sent by the authenticated user |
| GET | `/chat/conversations/{otherUserId}?page=&size=` | Full thread with one user |
| POST | `/chat/conversations/{otherUserId}/read` | Marks their messages to you as read |
| GET | `/chat/unread-count` | |

## Chat (WebSocket / STOMP)
Endpoint: `ws(s)://<host>/ws` (SockJS) — send `Authorization: Bearer <jwt>` as
a STOMP native header on `CONNECT`.

| Destination | Direction | Payload |
|---|---|---|
| `/app/chat.send` | Client → Server | `{receiverId, content}` |
| `/app/chat.typing` | Client → Server | `{receiverId, typing: true|false}` |
| `/user/queue/messages` | Server → Client | `MessageDto` for new messages; `{type:"MESSAGE_DELETED", messageId}` for deletions |
| `/user/queue/typing` | Server → Client | `{fromUserId, fromUsername, typing}` |
| `/user/queue/notifications` | Server → Client | `NotificationDto` |
| `/topic/presence` | Server → Client (broadcast) | `{username, online}` |

## Notifications
| Method | Path | Notes |
|---|---|---|
| GET | `/notifications?page=&size=` | |
| GET | `/notifications/unread-count` | |
| POST | `/notifications/{id}/read` | |

## Ratings
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/ratings` | `{sessionId, stars, review}` | Session must be COMPLETED |
| GET | `/ratings/user/{userId}?page=&size=` | | |
| GET | `/ratings/user/{userId}/average` | | |

## Reports
| Method | Path | Body | Auth |
|---|---|---|---|
| POST | `/reports` | `{targetType, targetId, reason}` | Any authenticated user |
| GET | `/reports?status=OPEN&page=&size=` | | ADMIN |
| POST | `/reports/{id}/resolve` | `{status, resolutionNotes}` | ADMIN |

## Admin
All under `/admin/**`, ADMIN role required (enforced both at the security
path-matcher level and `@PreAuthorize` on the controller).

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/dashboard` | Aggregate platform stats |
| GET | `/admin/users?page=&size=` | |
| POST | `/admin/users/{id}/suspend` | `{reason}` |
| POST | `/admin/users/{id}/unsuspend` | `{reason}` |
| POST | `/admin/skills` | `{name, categoryId}` |
| POST | `/admin/skills/{id}/deactivate` | |
| POST | `/admin/skills/{id}/activate` | |
| POST | `/admin/skill-categories` | `{name, description}` |
| POST | `/admin/credits/{userId}/adjust` | `{amount, reason}` |

## Postman
There is no bundled `.postman_collection.json` in this build — every endpoint
above maps directly to a Postman request (method + path + JSON body), so a
collection can be assembled quickly by importing this table, or by pointing
Postman's OpenAPI import at `springdoc` if you add
`springdoc-openapi-starter-webmvc-ui` as a further enhancement (not included
by default to keep the dependency footprint minimal).


### Profile image upload

`POST /api/upload` accepts an authenticated multipart request with a `file` field.
Only JPEG and PNG images are accepted, the maximum size is 5 MB, and the backend validates the actual image bytes before storing a server-generated filename.
The public file URL is derived from the configured `BACKEND_URL`, not hard-coded to a local development address.

## Partner Video Introduction APIs

### Create or reuse a partner video room
`POST /api/partner-video/rooms/partner/{partnerId}`

Creates or reuses a short-lived Jitsi room for the authenticated user and a strong reciprocal match. The backend recalculates compatibility before issuing the room and does not use AI or machine learning.

### Authorize a partner video room
`GET /api/partner-video/rooms/{roomId}`

Authorizes the authenticated user to enter a previously issued room. Only the two persisted participants can use the room, and expired rooms are rejected.
