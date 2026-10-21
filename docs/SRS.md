# Software Requirements Specification (SRS)
## Skill Exchange & Knowledge Sharing Platform

### 1. Introduction

**1.1 Purpose**
This document specifies the functional and non-functional requirements for a
real-time platform where users teach and learn skills from one another,
mediated by an internal skill-credit economy instead of cash payments.

**1.2 Scope**
The system supports user registration/authentication, profile and skill
management, a compatibility-based matching engine, exchange-request
negotiation, session scheduling and completion, an atomic credit ledger,
real-time chat and notifications, ratings/reputation, moderation (reports),
and an administrative console.

**1.3 Intended Audience**
Developers extending the platform, and evaluators reviewing the reference
implementation.

### 2. Overall Description

**2.1 Product Perspective**
A three-tier web application: a React SPA, a Spring Boot REST + WebSocket
API, and a PostgreSQL relational database, deployable via Docker Compose.

**2.2 User Classes**
- **USER** — registers, manages a profile and skills, finds matches, sends/
  responds to exchange requests, chats, schedules and completes sessions,
  rates counterparts, and can file reports.
- **ADMIN** — everything a USER can do, plus user suspension, skill/category
  curation, report resolution, and platform-wide analytics.

**2.3 Operating Environment**
Backend: Java 21 / Spring Boot 3.x on any JVM-compatible host or container.
Frontend: any evergreen browser. Database: PostgreSQL 14+.

### 3. Functional Requirements (summary)

| ID | Requirement |
|----|-------------|
| FR-1 | Users register with a unique username/email and BCrypt-hashed password. |
| FR-2 | Users authenticate via JWT; tokens expire and are required on protected routes. |
| FR-3 | Users add skills they can TEACH and skills they want to LEARN, each with a level. |
| FR-4 | The system computes a 0–100% compatibility score between any two users across skill reciprocity, availability overlap, shared language, and reputation. |
| FR-5 | Users send exchange requests naming an offered skill, a requested skill, and a duration; requests can be accepted, rejected, or cancelled. |
| FR-6 | Accepted exchanges can be scheduled into one or more sessions with a date/time range; overlapping sessions for either participant are rejected. |
| FR-7 | Completing a session atomically transfers skill credits from learner to teacher, exactly once. |
| FR-8 | Only completed sessions may be rated, and only once per rater. |
| FR-9 | Users exchange real-time chat messages over WebSocket/STOMP, with delivery, read state, typing indicators, and online/offline presence. |
| FR-10 | The system raises real-time notifications for the events listed in the platform spec (request received/accepted/rejected, new message, session scheduled/completed, credits transferred, rating received, report status changed). |
| FR-11 | Users may report a user, message, session, or other content; admins triage and resolve reports. |
| FR-12 | Admins can suspend/unsuspend users, curate skills/categories, and view aggregate analytics; every administrative action is written to an audit log. |

### 4. Non-Functional Requirements

- **Security:** BCrypt password hashing, stateless JWT auth, role-based
  method/path authorization, input validation on every write endpoint,
  and a global exception handler that never leaks stack traces.
- **Data integrity:** All monetary-equivalent (credit) operations run inside
  a single DB transaction with pessimistic row locking and an idempotency
  key, so retries and concurrent completions cannot double-pay.
- **Consistency:** The relational schema enforces foreign keys, uniqueness
  (e.g. one rating per rater per session), and check constraints (e.g.
  `sender_id <> receiver_id`) at the database level, not just in application code.
- **Portability:** The whole stack runs via `docker compose up` given a
  populated `.env` file.

### 5. Out of Scope for This Reference Build
Payment processing (the platform is credit-only by design), production email
delivery for password resets, file/image upload storage (profile pictures
are URLs), and mobile native apps.
