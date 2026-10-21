# Partner Video Introduction

SkillSwap includes a participant-scoped video introduction flow for strong reciprocal matches.

## Eligibility

A video introduction is available only when the deterministic compatibility engine finds:

- an overall match score of at least 60% (configurable with `VIDEO_ROOM_MIN_MATCH_SCORE`)
- at least one skill the partner can teach the current user
- at least one skill the current user can teach the partner

No AI or machine-learning model is used for this feature.

## Flow

1. User opens **Matches**.
2. The frontend shows **Video Intro** only for eligible matches.
3. The backend recalculates the match before creating a room.
4. A random, short-lived room identifier is persisted for the two participants.
5. Each participant must be authorized by the backend before entering the room.
6. The frontend opens a Jitsi meeting using the generated room name.
7. The room expires after 30 minutes and expired records are cleaned up automatically.

## Security note

The backend protects room creation and room-entry authorization. The current local implementation uses the public Jitsi service with an unguessable room name. For stronger production-level identity enforcement, the deployment can later move to a Jitsi deployment with JWT/Secure Domain or another authenticated video provider without changing the SkillSwap matching workflow.
