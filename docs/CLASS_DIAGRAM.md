# Class Diagram Description

This describes the key backend classes and their relationships in prose +
Mermaid, since a full UML class diagram for ~50 classes doesn't render
legibly as a single image. Grouped by concern.

## Matching Engine

```mermaid
classDiagram
    class MatchingService {
        -CompatibilityCalculator calculator
        +findMatchesForUser(userId, limit) List~MatchResultDto~
        +calculateMatchBetween(userId, candidateId) MatchResultDto
    }
    class CompatibilityCalculator {
        +calculateSkillCompatibility(...) double
        +calculateAvailabilityCompatibility(me, candidate) double
        +calculateLanguageCompatibility(me, candidate) double
        +calculateReputationCompatibility(candidate) double
    }
    class MatchResult {
        +User candidate
        +double skillCompatibility
        +double availabilityCompatibility
        +double languageCompatibility
        +double reputationCompatibility
        +double overallMatchPercentage
        +List~String~ theyCanTeachYouWant
        +List~String~ youCanTeachTheyWant
    }
    MatchingService --> CompatibilityCalculator : delegates scoring
    MatchingService --> MatchResult : builds
    MatchResult --> MatchResultDto : mapped to (controller-facing)
```

## Core Domain Services

```mermaid
classDiagram
    class ExchangeRequestService {
        +create(senderId, dto) ExchangeRequestDto
        +respond(userId, requestId, accept) ExchangeRequestDto
        +cancel(userId, requestId) ExchangeRequestDto
    }
    class SessionService {
        +schedule(userId, dto) SessionDto
        +complete(userId, sessionId) SessionDto
        +cancel(userId, sessionId) SessionDto
        +markNoShow(userId, sessionId) SessionDto
    }
    class CreditService {
        +transferForCompletedSession(teacher, learner, amount, session)
        +adminAdjust(userId, amount, reason)
    }
    class RatingService {
        +rate(raterId, dto) RatingDto
    }
    class NotificationService {
        +notify(userId, type, content, linkUrl) NotificationDto
    }
    ExchangeRequestService --> MatchingService : stamps match % on creation
    ExchangeRequestService --> NotificationService : notifies receiver
    SessionService --> CreditService : triggers transfer on completion
    SessionService --> NotificationService : notifies both parties
    RatingService --> NotificationService : notifies rated user
```

## Security

```mermaid
classDiagram
    class JwtUtil {
        +generateToken(userId, username, roles) String
        +extractUsername(token) String
        +isTokenValid(token, username) boolean
    }
    class JwtAuthenticationFilter {
        +doFilterInternal(request, response, chain)
    }
    class CustomUserDetailsService {
        +loadUserByUsername(usernameOrEmail) UserDetails
    }
    class UserPrincipal {
        +Long id
        +getAuthorities() Collection~GrantedAuthority~
    }
    JwtAuthenticationFilter --> JwtUtil : validates bearer token
    JwtAuthenticationFilter --> CustomUserDetailsService : loads principal
    CustomUserDetailsService --> UserPrincipal : wraps User entity
```

## Persistence (see `docs/ER_DIAGRAM.md` for the full schema)

Every entity in `entity/` has a matching Spring Data JPA repository in
`repository/` (e.g. `User` ↔ `UserRepository`), and every entity has a
hand-written mapping method in the single `EntityMapper` component rather
than a reflection-based mapper, so the entity-to-DTO boundary is explicit
and easy to audit for accidental data leakage.
