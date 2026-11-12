# CodeMind

AI-powered repository intelligence and software impact analysis.

Current checkpoint: **Week 1: repository ingestion + accounts**. Create an account, register a Git
repository, and CodeMind clones it into an isolated workspace and fingerprints its stack (languages,
manifests, build systems, package managers). Each account only sees its own repositories.

| Service     | Stack                       | Port | Docs                                     |
| ----------- | --------------------------- | ---- | ---------------------------------------- |
| `api/`      | Java 21, Spring Boot 3, JPA | 8080 | below                                    |
| `analysis/` | Python, FastAPI             | 8000 | [analysis/README.md](analysis/README.md) |
| `web/`      | Next.js 15, Tailwind        | 3000 | [web/README.md](web/README.md)           |

## Run locally

Start the three services in separate terminals. `git` must be on `PATH`.

```bash
# 1. analysis service
cd analysis
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
.venv/Scripts/python -m uvicorn app.main:app --port 8000

# 2. API (H2 file database in api/data/ by default)
cd api
mvnw.cmd spring-boot:run   # Windows wrapper; use mvn elsewhere

# 3. web
cd web
npm install
npm run dev
```

Open http://localhost:3000, create an account, and add a repository.

PostgreSQL + pgvector is optional at this stage: `docker compose up -d`, then run the API with
`-Dspring-boot.run.profiles=postgres`. Environment variables are listed in `.env.example`.

## Authentication

Email and password accounts with stateless JWT bearer tokens.

1. `POST /api/auth/register` or `POST /api/auth/login` with `{email, password}` returns
   `{token, expiresAt, user}`. Passwords are stored as BCrypt hashes; emails are case-insensitive.
2. Send the token on every other call: `Authorization: Bearer <token>`. Tokens are HS256-signed by
   the API, carry the user id as `sub`, and expire after `CODEMIND_JWT_TTL` (default `24h`).
3. Signing out is client-side: the web app drops the token.

The web app keeps the session in `localStorage`, attaches the token in `lib/api.ts`, and sends you
to `/login?next=<page>` when there's no session or the API answers `401`.

Set `CODEMIND_JWT_SECRET` (32+ bytes) outside local development; the default in
`application.yml` is for dev only. Repositories added before accounts existed have no owner and are
hidden.

## API

| Method | Path                        | Auth   | Description                                         |
| ------ | --------------------------- | ------ | --------------------------------------------------- |
| GET    | `/api/health`               | public | Liveness                                            |
| POST   | `/api/auth/register`        | public | `{email, password}` (8–72 chars) → `201` + token; `409` if taken |
| POST   | `/api/auth/login`           | public | `{email, password}` → `200` + token; `401` if wrong |
| GET    | `/api/auth/me`              | token  | The signed-in user `{id, email, createdAt}`         |
| GET    | `/api/projects`             | token  | Your projects, newest first                         |
| POST   | `/api/projects`             | token  | `{gitUrl, name?}` → `202`, status `QUEUED`          |
| GET    | `/api/projects/{id}`        | token  | One of your projects, including its fingerprint    |
| POST   | `/api/projects/{id}/ingest` | token  | Re-index (`409` while already indexing)             |

Status flow: `QUEUED → INDEXING → COMPLETED | FAILED`. Errors are returned as `{"error": "..."}`,
including `401` for a missing, invalid or expired token. Another user's project is a `404`, not a
`403`, so project ids can't be probed.

## Tests

```bash
cd api && mvnw.cmd test
cd analysis && .venv/Scripts/python -m pytest -q
cd web && npx tsc --noEmit && npm run lint && npm run build
```

GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs the same three checks on
every pull request and on every push to `main`.
