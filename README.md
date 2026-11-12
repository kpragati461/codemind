# CodeMind

AI-powered repository intelligence and software impact analysis.

Current checkpoint: **repository ingestion**. Register a Git repository; CodeMind clones it into an
isolated workspace and fingerprints its stack (languages, manifests, build systems, package
managers).

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

Open http://localhost:3000 and add a repository.

PostgreSQL + pgvector is optional at this stage: `docker compose up -d`, then run the API with
`-Dspring-boot.run.profiles=postgres`. Environment variables are listed in `.env.example`.

## API

| Method | Path                       | Description                                  |
| ------ | -------------------------- | -------------------------------------------- |
| GET    | `/api/health`              | Liveness                                     |
| GET    | `/api/projects`            | All projects, newest first                   |
| POST   | `/api/projects`            | `{gitUrl, name?}` → `202`, status `QUEUED`   |
| GET    | `/api/projects/{id}`       | One project, including its fingerprint       |
| POST   | `/api/projects/{id}/ingest`| Re-index (`409` while already indexing)      |

Status flow: `QUEUED → INDEXING → COMPLETED | FAILED`. Errors are returned as `{"error": "..."}`.

## Tests

```bash
cd api && mvnw.cmd test
cd analysis && .venv/Scripts/python -m pytest -q
cd web && npx tsc --noEmit && npm run lint && npm run build
```
