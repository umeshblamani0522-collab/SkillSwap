# SkillSwap Product Workflow

SkillSwap is organised around one continuous product journey rather than isolated CRUD modules:

1. **Profile** — A member defines what they can teach, what they want to learn, languages, location and availability.
2. **Discover & Match** — The compatibility engine ranks reciprocal partners using deterministic signals. The UI exposes the score components so a recommendation is explainable without AI.
3. **Exchange Request** — A member proposes a concrete skill-for-skill exchange. The request follows a lifecycle from pending to accepted/rejected/cancelled/completed/expired.
4. **Communication** — Accepted participants can communicate through the platform's authorised real-time messaging flow.
5. **Video Introduction** — Strong reciprocal matches can request a private video introduction. The recipient receives a real-time approval prompt and can accept or decline.
6. **Session** — An accepted exchange can be turned into a scheduled learning session with an online meeting option.
7. **Completion & Credits** — Completing the exchange updates the participant's learning/teaching outcome and credit ledger.
8. **Reputation** — Participants rate completed exchanges and build reputation for future matching.
9. **Trust & Moderation** — Reporting, blocking, cancellation and admin moderation provide operational controls around the community.

## Design principles

- Business workflows are connected end-to-end.
- Matching is deterministic and explainable; no AI is required.
- Video access is permissioned by the backend rather than exposed as an arbitrary public meeting link.
- Database migrations are append-only. Existing Flyway migrations are not edited after application; new schema changes receive a new migration version.
- Dashboard screens focus on next actions and product context rather than presenting disconnected statistics.
