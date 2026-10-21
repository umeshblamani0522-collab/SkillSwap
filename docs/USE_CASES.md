# Use Case Documentation

## Actors
- **Visitor** — unauthenticated
- **User** — authenticated, role USER
- **Admin** — authenticated, role ADMIN (also has all USER capabilities)

## UC-1: Register
**Actor:** Visitor
**Preconditions:** Username and email not already in use.
**Flow:** Visitor submits username, email, password, full name → system hashes
the password with BCrypt, creates the user with a starter credit balance, and
returns a JWT.
**Postconditions:** A new `users` row, a `user_roles` row (USER), and a
`credit_accounts` row exist.

## UC-2: Login
**Actor:** User/Admin
**Flow:** Submit username-or-email + password → Spring Security's
`AuthenticationManager` validates against the BCrypt hash → a signed JWT
(24h default expiry) is returned containing the user id, username, and roles.

## UC-3: Manage Skills
**Actor:** User
**Flow:** Add a skill as TEACH or LEARN with a level and optional notes; edit
level/notes; remove a skill. Duplicate (same user, skill, type) entries are
rejected.

## UC-4: Find Compatible Users
**Actor:** User
**Flow:** `MatchingService` gathers all users who TEACH a skill the current
user wants to LEARN, computes four sub-scores (skill reciprocity,
availability overlap, language overlap, reputation) per candidate via
`CompatibilityCalculator`, combines them with fixed weights into an overall
percentage, and returns the ranked list.
**Alternate flow:** Calculate the score against one specific candidate
(`GET /api/matches/{candidateId}`), used before creating an exchange request
so the request itself can be stamped with the match percentage at the time
of sending.

## UC-5: Send / Respond to an Exchange Request
**Actor:** User (sender), User (receiver)
**Preconditions:** Sender ≠ receiver; neither party suspended.
**Flow:** Sender names an offered skill (their TEACH), a requested skill
(their LEARN), a duration, and a message. Receiver accepts or rejects. Either
party may cancel before completion.
**Business rules enforced:** no self-requests; suspended users blocked;
status transitions restricted to `PENDING → {ACCEPTED, REJECTED, CANCELLED}`.

## UC-6: Schedule a Session
**Actor:** User (either party of an accepted exchange)
**Preconditions:** The parent exchange request is ACCEPTED.
**Flow:** Choose a date and start/end time; the system rejects the booking if
it overlaps an existing session for either the teacher or the learner.

## UC-7: Complete a Session → Transfer Credits
**Actor:** User (either party)
**Preconditions:** Session is SCHEDULED (not already COMPLETED/CANCELLED).
**Flow:** Either participant marks the session complete. `CreditService`
locks both accounts, verifies the learner has sufficient balance, moves
credits (duration in hours, rounded) from learner to teacher, and records two
linked `credit_transactions` rows under a shared idempotency key so retries
are no-ops.
**Postconditions:** Session status = COMPLETED; both parties are notified;
the session becomes ratable.

## UC-8: Rate a Session
**Actor:** User (either party of a COMPLETED session)
**Preconditions:** Session is COMPLETED; this rater has not already rated
this session.
**Flow:** Submit 1–5 stars and an optional review for the other participant.

## UC-9: Real-Time Chat
**Actor:** User
**Flow:** Client connects to `/ws` (SockJS/STOMP) with a JWT in the STOMP
CONNECT header, subscribes to `/user/queue/messages` and
`/user/queue/typing`, and publishes to `/app/chat.send` /
`/app/chat.typing`. The server persists every message and pushes it to the
recipient's private queue instantly; a REST fallback
(`POST /api/chat/messages`) exists for non-WebSocket clients.

## UC-10: Report Content
**Actor:** User
**Flow:** Submit a target type (USER/MESSAGE/SESSION/CONTENT), a target id,
and a reason. Report starts as OPEN.

## UC-11: Moderate Reports (Admin)
**Actor:** Admin
**Flow:** List reports by status; transition a report to UNDER_REVIEW,
RESOLVED, or REJECTED with resolution notes. The reporter is notified of the
status change.

## UC-12: Suspend / Unsuspend a User (Admin)
**Actor:** Admin
**Flow:** Toggle a user's `is_suspended` flag with a reason; the action is
written to `audit_logs`. Suspended users cannot create exchange requests and
cannot receive new ones.

## UC-13: View Platform Analytics (Admin)
**Actor:** Admin
**Flow:** `GET /api/admin/dashboard` aggregates user counts, exchange/session
funnels, average rating, open-report count, most popular skills, and most
active users for the admin dashboard's charts and cards.
