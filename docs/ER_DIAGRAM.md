# Entity-Relationship Documentation

## Diagram (Mermaid ER syntax)

Paste this block into any Mermaid renderer (e.g. https://mermaid.live) to view it visually.

```mermaid
erDiagram
    USERS ||--o{ USER_SKILLS : has
    USERS ||--o{ AVAILABILITY : sets
    USERS ||--|| CREDIT_ACCOUNTS : owns
    USERS ||--o{ EXCHANGE_REQUESTS : sends
    USERS ||--o{ EXCHANGE_REQUESTS : receives
    USERS ||--o{ SESSIONS : teaches
    USERS ||--o{ SESSIONS : learns
    USERS ||--o{ MESSAGES : sends
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ RATINGS : gives
    USERS ||--o{ RATINGS : receives
    USERS ||--o{ REPORTS : files
    USERS }o--o{ ROLES : "user_roles"
    SKILL_CATEGORIES ||--o{ SKILLS : groups
    SKILLS ||--o{ USER_SKILLS : "taught/wanted"
    EXCHANGE_REQUESTS ||--o{ SESSIONS : "scheduled as"
    SESSIONS ||--o{ RATINGS : "rated after"
    SESSIONS ||--o{ CREDIT_TRANSACTIONS : "triggers"
    CREDIT_ACCOUNTS ||--o{ CREDIT_TRANSACTIONS : records

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        varchar full_name
        boolean is_suspended
        boolean is_enabled
    }
    ROLES {
        bigint id PK
        varchar name UK
    }
    SKILL_CATEGORIES {
        bigint id PK
        varchar name UK
    }
    SKILLS {
        bigint id PK
        varchar name UK
        bigint category_id FK
        boolean is_active
    }
    USER_SKILLS {
        bigint id PK
        bigint user_id FK
        bigint skill_id FK
        varchar skill_type "TEACH|LEARN"
        varchar skill_level
    }
    AVAILABILITY {
        bigint id PK
        bigint user_id FK
        varchar day_of_week
        time start_time
        time end_time
    }
    EXCHANGE_REQUESTS {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        bigint offered_skill_id FK
        bigint requested_skill_id FK
        varchar status
        double match_percentage
    }
    SESSIONS {
        bigint id PK
        bigint exchange_request_id FK
        bigint teacher_id FK
        bigint learner_id FK
        bigint skill_id FK
        date session_date
        varchar status
        boolean credits_transferred
    }
    MESSAGES {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        varchar content
        boolean is_read
    }
    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar type
        boolean is_read
    }
    CREDIT_ACCOUNTS {
        bigint id PK
        bigint user_id FK UK
        numeric balance
    }
    CREDIT_TRANSACTIONS {
        bigint id PK
        bigint account_id FK
        varchar type
        numeric amount
        varchar idempotency_key UK
    }
    RATINGS {
        bigint id PK
        bigint session_id FK
        bigint rater_id FK
        bigint rated_user_id FK
        int stars
    }
    REPORTS {
        bigint id PK
        bigint reporter_id FK
        varchar target_type
        bigint target_id
        varchar status
    }
    AUDIT_LOGS {
        bigint id PK
        bigint admin_id FK
        varchar action
    }
```

## Key design decisions

- **`user_skills` is a single table with a `skill_type` discriminator** (`TEACH`
  / `LEARN`) rather than two separate tables, so the matching engine can query
  "what does user X want to learn" and "what does user Y teach" with the same
  shape of query and a single unique constraint
  (`user_id, skill_id, skill_type`) prevents duplicate entries.
- **`credit_accounts` is one-to-one with `users`**, versioned with an optimistic
  `version` column and additionally locked pessimistically
  (`SELECT ... FOR UPDATE`) during transfers, because balance correctness
  matters more than throughput for this table.
- **`credit_transactions.idempotency_key` is unique**, keyed off
  `session-complete-{sessionId}-{debit|credit}`, which is what makes
  "a session cannot pay out twice" a database-enforced guarantee, not just an
  application-level check.
- **`sessions` references `exchange_requests`** rather than duplicating
  sender/receiver/skill data, but denormalizes `teacher_id` / `learner_id` /
  `skill_id` onto the session itself because a single exchange request could
  in principle spawn multiple sessions over time.
- **Check constraints at the DB layer** (`sender_id <> receiver_id`,
  `start_time < end_time`, `stars BETWEEN 1 AND 5`, `balance >= 0`) enforce
  the platform's core business rules even if application code is bypassed.
