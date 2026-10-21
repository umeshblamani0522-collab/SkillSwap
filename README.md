# SkillSwap — Real-Time Skill Exchange & Knowledge Sharing Platform

A full-stack platform where people teach what they know and learn what they
want, using an internal skill-credit economy instead of cash. Built with
Java 21 / Spring Boot 3, React 18 / Vite, and PostgreSQL.

> **Read this before you run it.** This is a real, from-scratch implementation
> (no mocked endpoints, no stubbed business logic) but it has **not been
> compiled/run end-to-end in the environment that produced it** — see
> [Verification Status](#verification-status) below for exactly what was and
> wasn't tested, so you know where to look first if something doesn't build.

---

## Table of Contents
- [Project Overview](#project-overview)
- [Problem Statement & Objectives](#problem-statement--objectives)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Verification Status](#verification-status)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Environment Configuration](#environment-configuration)
- [Running Locally (without Docker)](#running-locally-without-docker)
- [Running with Docker](#running-with-docker)
- [Database Setup](#database-setup)
- [Testing](#testing)
- [Development Login Credentials](#development-login-credentials)
- [API & WebSocket Documentation](#api--websocket-documentation)
- [Business Rules Implemented](#business-rules-implemented)
- [Known Limitations](#known-limitations)
- [Future Enhancements](#future-enhancements)

---

## Project Overview

SkillSwap lets a user list skills they can **teach** and skills they want to
**learn**. A matching engine scores every potential partner (0–100%) on
skill reciprocity, availability overlap, shared language, and reputation.
Matched users send exchange requests, chat in real time, schedule sessions,
and — on completion — credits move automatically from learner to teacher.
Ratings build reputation, which feeds back into future match scores.

## Problem Statement & Objectives

Traditional tutoring/mentoring marketplaces require cash and often gatekeep
by ability to pay. SkillSwap's objective is a **barter-based** knowledge
economy: your time teaching is directly worth someone else's time teaching
you, mediated by a credit ledger so exchanges don't need to be simultaneous
or perfectly 1:1.

## Features

See [docs/USE_CASES.md](docs/USE_CASES.md) for the full use-case breakdown.
In short: registration/auth, rich profiles, skill management with levels,
a weighted matching engine, exchange-request negotiation, session
scheduling with overlap prevention, atomic credit transfers, 1:1 real-time
chat with typing/presence, real-time notifications, ratings & reputation,
search/filter/pagination, a user dashboard, an admin console with analytics
and audit logs, and a report/moderation queue.

## Technology Stack

**Frontend:** React 18, Vite, React Router, Axios, Bootstrap 5, React
Context API, React Icons, `@stomp/stompjs` + `sockjs-client` for WebSocket
chat, Recharts for admin analytics charts.

**Backend:** Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA
(Hibernate), Spring Security, JJWT, Bean Validation, Spring WebSocket +
STOMP, Flyway (schema migrations), Maven.

**Database:** PostgreSQL 16.

**Dev tools assumed:** IntelliJ IDEA / VS Code, pgAdmin, Postman, Git.

**Deployment:** Docker, Docker Compose, environment-variable-driven config
(AWS-ready — no code changes needed to point `DB_HOST` at an RDS instance).

## Verification Status

Being transparent about exactly what was checked, so you can trust this
section more than a blanket "it works":

| Component | Status |
|---|---|
| **Frontend build** (`npm install && npm run build`) | ✅ **Actually run in the build environment. Succeeded with zero errors** (see `frontend/package.json` toolchain — Vite 5, React 18). |
| **Backend compile** (`mvn compile`) | ⚠️ **Not verified.** The sandbox that produced this code has network access to npm's registry but not to Maven Central, so `mvn` could resolve Maven itself via `apt` but could not download Spring Boot's parent POM/dependencies to actually compile the Java. The code was written and reviewed carefully by hand (consistent imports, matching method signatures, Lombok annotations checked for completeness), but you should run `mvn clean install` yourself as the first step — see [Testing](#testing) — before assuming it's error-free. |
| **Database migrations** (Flyway SQL) | ⚠️ Written and manually reviewed for valid PostgreSQL syntax, but not executed against a live PostgreSQL instance. |
| **Docker Compose end-to-end** | ⚠️ Not run — depends on the backend compiling first. |
| **JUnit tests** | ⚠️ Written (5 test classes covering the matching engine, credit ledger, session, and exchange-request business rules) but not executed, for the same Maven Central reason above. |

**If `mvn compile` fails**, the most likely causes are a typo in an import or
a Lombok-generated method name mismatch — both are mechanical fixes. Start
with `mvn compile` (not `package`) for the fastest feedback loop.

## Project Structure

```
SKILL_EXCHANGE_PLATFORM/
├── frontend/           React + Vite SPA
├── backend/             Spring Boot API (Maven)
│   └── src/main/resources/db/migration/   Flyway SQL (schema + seed data)
├── database/           (schema also lives in backend/src/.../db/migration; see below)
├── docs/                SRS, ER diagram, use cases, architecture, class/sequence diagrams, test cases, API docs
├── docker/              (docker-compose.yml and per-service Dockerfiles are at their respective roots — see below)
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md            (this file)
```

Note on `database/` and `docker/`: the actual SQL migrations live under
`backend/src/main/resources/db/migration/` (the conventional Flyway
location, so the backend can run them automatically on startup), and the
actual Dockerfiles live inside `backend/` and `frontend/` respectively (so
each image's build context only contains what it needs). The top-level
`database/` and `docker/` folders are kept as pointers for discoverability
per the requested structure.

## Prerequisites

- Java 21 (JDK)
- Maven 3.8+
- Node.js 20+ and npm
- PostgreSQL 14+ (or Docker, to avoid installing it locally)
- Docker + Docker Compose (for the containerized path)

## Environment Configuration

1. Copy the template: `cp .env.example .env`
2. Fill in real values — **at minimum, set `JWT_SECRET` to a long random
   string** (e.g. `openssl rand -base64 48`) and a real `DB_PASSWORD`.
3. Never commit `.env` — it's already in `.gitignore`.

## Running Locally (without Docker)

**1. Start PostgreSQL** and create a database matching your `.env`:
```sql
CREATE DATABASE skillexchange;
CREATE USER skillexchange_user WITH PASSWORD 'changeme_use_a_strong_password';
GRANT ALL PRIVILEGES ON DATABASE skillexchange TO skillexchange_user;
```

**2. Run the backend** (Flyway applies the schema and reference skill data automatically.)
on startup):
```bash
cd backend
export $(cat ../.env | xargs)   # or set the same vars in your IDE run config
mvn spring-boot:run
```
The API comes up on `http://localhost:8080`.

**3. Run the frontend:**
```bash
cd frontend
cp .env.example .env   # adjust VITE_API_BASE_URL / VITE_WS_BASE_URL if needed
npm install
npm run dev
```
The app comes up on `http://localhost:5173` and proxies `/api` and `/ws` to
the backend during dev (see `vite.config.js`).

## Running with Docker

```bash
cp .env.example .env   # fill in JWT_SECRET and DB_PASSWORD at minimum
docker compose up --build
```
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (exposed for pgAdmin/psql access)

Stop with `docker compose down` (add `-v` to also drop the database volume).

## Database Setup

The schema is entirely managed by Flyway migrations in
`backend/src/main/resources/db/migration/`:
- `V1__init_schema.sql` — all 16 tables, foreign keys, unique constraints,
  check constraints, and indexes.
- `V2__seed_data.sql` — roles, 18 skill categories, and 18 skills
  users (1 admin + 4 regular users with realistic teach/learn skill sets),
  their credit accounts, sample availability, and one sample pending
  exchange request — enough to exercise the full workflow immediately after
  first boot.

`spring.jpa.hibernate.ddl-auto` is set to `validate`, not `update` or
`create` — Hibernate will refuse to start if the migrations and entity
mappings disagree, which is a deliberate safety net against schema drift.

## Testing

```bash
cd backend
mvn test
```
5 test classes exercise the matching engine's scoring math and the
business rules that are easiest to silently get wrong: double credit
transfer, double session completion, self-exchange-requests, and
suspended-user blocks. See [docs/TEST_CASES.md](docs/TEST_CASES.md) for the
full list plus a manual/Postman QA checklist for everything not yet
automated (chat isolation, message deletion, admin audit logging, etc.).

## Account & Email Verification

No user accounts are seeded. Every new account must use an email address that the user can actually access. Registration sends a one-time verification link through the configured SMTP server, and an account cannot sign in until the link is successfully used.

Password recovery follows the same verified-email rule: reset links are sent only to verified accounts, are time-limited, single-use, and are backed by hashed tokens.

For reviewer testing, create two accounts with real email addresses and verify both before testing matching, chat, exchanges, sessions, credits, and ratings.

## API & WebSocket Documentation

Full endpoint reference: [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)

## Business Rules Implemented

(Full list of 13 in the original spec — all implemented in the service
layer, several also enforced redundantly at the database level via check
constraints):

1. A user cannot send an exchange request to themselves (service + DB check constraint).
2. Suspended users cannot create exchange requests.
3. Users cannot create overlapping sessions (checked per participant per date).
4. Credits transfer only after successful session completion.
5. Credit transfers are atomic (`@Transactional`, row-locked).
6. Users cannot spend more credits than their balance (checked pre-transfer; DB `CHECK (balance >= 0)` as a last line of defense).
7. Only conversation participants can access private messages.
8. Only completed sessions can be rated.
9. A session cannot be completed twice (idempotency key on the resulting credit transaction).
10. A session cannot be rated twice by the same rater (`UNIQUE (session_id, rater_id)`).
11. Admin APIs require the ADMIN role (path-matcher + method-level `@PreAuthorize`).
12. Inactive skills are excluded from search results and from the matching engine's candidate gathering.
13. Administrative actions (suspend/unsuspend) are recorded in `audit_logs`.

## Known Limitations

Being explicit about gaps rather than glossing over them:

- **Compile/run not verified**, as detailed above — treat this as a strong
  first draft, not a guaranteed-green build.
- **Profile pictures are URLs only** — there's no file upload / S3
  integration, matching the "no file uploads" constraint of this build
  environment.
- **No rate limiting** on auth endpoints.
- **JUnit coverage is representative, not exhaustive** — see
  `docs/TEST_CASES.md` for the manual checklist covering what isn't yet
  automated.
- **OpenAPI/Swagger UI is not included** to keep the dependency footprint
  minimal; `docs/API_DOCUMENTATION.md` is the source of truth instead.

## Future Enhancements

- Real email delivery for password reset and session reminders.
- File/image upload for profile pictures (S3-backed, given AWS-ready config).
- Rate limiting and account lockout after repeated failed logins.
- OpenAPI/Swagger UI generation.
- Push notifications (web push) in addition to in-app + WebSocket.
- A recommendation model that learns from accepted/rejected exchange
  history instead of only the static weighted formula.

## Runtime configuration profiles

Local Maven development uses Spring's `local` profile by default. Containerized deployment uses the `prod` profile through Docker Compose. Production requires `JWT_SECRET` to be supplied explicitly; the development fallback is never used by the production profile.

Configure `CORS_ALLOWED_ORIGINS` with the exact frontend origins that are allowed to call the API. WebSocket endpoints use the same origin allow-list.

Profile image uploads are restricted to JPEG/PNG files up to 5 MB and are stored using generated filenames. Set `UPLOADS_DIR` and `BACKEND_URL` when changing the storage location or public backend URL.

## Engineering-grade notes

SkillSwap includes email verification, password reset, JWT authentication, role-based administration, private chat with WebSocket delivery, sender-only message deletion, exchange lifecycle validation, session scheduling, atomic credit transfers, reporting/moderation, availability management, Flyway migrations and Docker support.

### Local user flow

1. Start PostgreSQL with `start-database.ps1` or `docker start skillexchange-pg`.
2. Start the backend from `backend` with `mvn spring-boot:run`.
3. Start the frontend from `frontend` with `npm run dev`.
4. Register using a real email address and complete verification before signing in.
5. Add teaching/learning skills and availability before using Matches.
6. Exchange requests become the authorization point for private chat; accepting an exchange creates the accepted chat connection.
7. Complete sessions only after their scheduled end time; credits are transferred once and the exchange closes when no active sessions remain.

### Production configuration

Set `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, `BACKEND_URL`, database variables and SMTP variables through the deployment environment. Never commit `.env`, credentials or provider secrets.

## Partner Video Introduction

Strong reciprocal matches can start a short-lived video introduction directly from the Matches page. The feature uses deterministic compatibility rules (no AI) and Jitsi for the call. The backend issues a participant-scoped room that expires automatically.

### Local email testing

The default `local` profile uses `APP_MAIL_MODE=console`, so email verification and password-reset messages are printed to the Spring Boot console instead of contacting an SMTP server. Production uses `APP_MAIL_MODE=smtp` and the configured SMTP credentials.


## Real email and LAN access
New registrations require real email ownership verification before sign-in. Configure SMTP credentials in the root `.env`; console-only verification is disabled. The Vite frontend is LAN-friendly and proxies `/api`, `/ws`, and `/uploads` to the local Spring Boot backend. Start Vite with `npm run dev -- --host 0.0.0.0` and use the host PC's LAN URL such as `http://192.168.0.101:5173` from another PC on the same network.


## Real email verification and LAN access
Registration requires ownership verification of the submitted email address. Console-only verification is disabled; configure SMTP in `.env`. The frontend uses same-origin `/api`, `/ws` and `/uploads` routes so the same build can be opened from another computer on the same LAN.
