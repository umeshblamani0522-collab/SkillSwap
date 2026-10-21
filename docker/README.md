# Docker

The actual Dockerfiles live next to the code they build:
- `../backend/Dockerfile` — multi-stage Maven build → JRE runtime
- `../frontend/Dockerfile` — multi-stage Vite build → nginx runtime

Orchestration for all three services (PostgreSQL, backend, frontend) is at
the project root: [`../docker-compose.yml`](../docker-compose.yml).

Run everything with:
```bash
cp ../.env.example ../.env   # fill in JWT_SECRET and DB_PASSWORD
docker compose -f ../docker-compose.yml up --build
```
