# Test Cases

## Automated (JUnit 5 + Mockito) — included in `backend/src/test/java`

| Class | Test | Type |
|---|---|---|
| `CompatibilityCalculatorTest` | Reciprocal skill match (I want to learn X, they teach X) scores above neutral | Positive |
| | No skill overlap at all scores 0 | Negative |
| | Both sides empty returns 0 (no division-by-zero) | Edge |
| | Missing language data on either side returns neutral 50% | Edge |
| | Partial language overlap scores proportionally | Positive |
| | New user with no ratings gets neutral-positive default reputation (60%) | Edge |
| | Reputation scales linearly with average stars (5★→100%, 2.5★→50%) | Positive |
| `CreditServiceTest` | Learner with insufficient balance → `BusinessRuleException`, no accounts mutated | Negative |
| | Successful transfer moves the exact amount both directions, 2 transaction rows written | Positive |
| | A session already carrying a debit transaction is a no-op on retry (idempotency) | Negative/Edge |
| `SessionServiceTest` | Completing an already-COMPLETED session throws, credit transfer never called | Negative |
| | Completing a SCHEDULED session transitions status and calls credit transfer exactly once | Positive |
| | Scheduling against a non-ACCEPTED exchange request is rejected | Negative |
| | Start time ≥ end time is rejected | Negative |
| `ExchangeRequestServiceTest` | Sending a request to yourself is rejected before touching the repository | Negative |
| | A suspended sender cannot create a request | Negative |
| | A valid request is persisted with the computed match percentage and the receiver is notified | Positive |

Run them with:
```
cd backend
mvn test
```

## Manual / Postman — recommended additional coverage

These are documented here as a checklist for manual QA or for extending the
automated suite; they are not all encoded as JUnit tests in this reference
build.

### Authentication
- [ ] Register with a duplicate username → 409 Conflict
- [ ] Register with a duplicate email → 409 Conflict
- [ ] Register with a password under 8 characters → 400 with field error
- [ ] Login with wrong password → 401 Unauthorized
- [ ] Access a protected endpoint with no token → 401 Unauthorized
- [ ] Access a protected endpoint with an expired token → 401 Unauthorized
- [ ] Access `/api/admin/**` as a USER-role token → 403 Forbidden

### Skills
- [ ] Add the same skill as TEACH twice → 409 Conflict
- [ ] Add a skill as both TEACH and LEARN → allowed (different `skill_type`)
- [ ] Edit another user's `user_skill` by id → 403 Forbidden

### Matching
- [ ] Two users with fully reciprocal skills score highly (>70%)
- [ ] Two users with no skill overlap score 0% on the skill dimension
- [ ] A user with no availability rows set gets the neutral 50% availability score

### Exchange Requests
- [ ] Sending to a suspended receiver is rejected
- [ ] Accepting an already-ACCEPTED request is rejected (state machine guard)
- [ ] A non-participant cannot GET a specific exchange request by id → 403

### Sessions
- [ ] Two sessions for the same teacher at overlapping times on the same day → rejected
- [ ] Completing a CANCELLED session → rejected
- [ ] Marking NO_SHOW after COMPLETED → rejected

### Credits
- [ ] Wallet balance never goes negative, even under two rapid completion requests for
      the same session (idempotency key test — see `CreditServiceTest`)
- [ ] Admin credit adjustment that would push balance negative is rejected

### Ratings
- [ ] Rating a SCHEDULED (not yet completed) session → rejected
- [ ] Rating the same session twice by the same rater → rejected
- [ ] A non-participant cannot rate a session → 403

### Chat
- [ ] User A cannot read User B/C's conversation by guessing a conversation URL — only
      the two participants' own messages are ever returned for a given pair of ids
- [ ] A message sent over WebSocket appears via the REST conversation-history endpoint
      (both paths write to the same `messages` table)

### Admin
- [ ] Suspending a user writes an `audit_logs` row with the admin's id
- [ ] Dashboard stats update after completing a session (completedSessions increments)
