# Database

The actual schema and seed-data SQL live under
`backend/src/main/resources/db/migration/` as Flyway migrations
(`V1__init_schema.sql`, `V2__seed_data.sql`), so the backend applies them
automatically on startup. They're kept there (the conventional Flyway
location) rather than here so migrations run without extra configuration.

See [`docs/ER_DIAGRAM.md`](../docs/ER_DIAGRAM.md) for the full schema
diagram and design rationale.
