# CodeMind — Day 1 Setup

Goal for today: Spring Boot skeleton + React (Vite) skeleton + Postgres (with pgvector) running locally, all talking to each other.

## Prerequisites
- Java 21 (`java -version`)
- Node.js 18+ (`node -v`)
- Docker Desktop running

## 1. Start Postgres (with pgvector)
From the `codemind/` root folder:
```bash
docker compose up -d
```
This starts Postgres on `localhost:5432` (db: `codemind`, user/pass: `codemind`/`codemind`) and enables the `vector` extension automatically via `init.sql`.

Verify it's up:
```bash
docker ps
```
You should see `codemind-postgres` running.

## 2. Start the backend
```bash
cd backend
./mvnw spring-boot:run
```
(If `mvnw` isn't present yet, open the `backend/` folder in IntelliJ — it'll offer to set up the Maven wrapper, or just run `BackendApplication.java` directly from the IDE.)

Check it's working:
```bash
curl http://localhost:8080/api/health
```
Expected:
```json
{"status":"UP","service":"codemind-backend","timestamp":"..."}
```

## 3. Start the frontend
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` — you should see "CodeMind" with **Backend status: UP — codemind-backend**.

If it says "backend unreachable," check the backend is running on port 8080 and Postgres is up.

## What "done" looks like today
- [ ] `docker compose up -d` runs Postgres with pgvector enabled, no errors
- [ ] Backend starts cleanly on port 8080, `/api/health` returns UP
- [ ] Frontend loads on port 5173 and shows "Backend status: UP"
- [ ] No `create-drop` anywhere in `application.yml` (data-loss risk — keep `ddl-auto: update` during dev)

## Tomorrow (Day 2)
GitHub REST API integration — clone/download a public repo by URL into a temp analysis directory using JGit (already added as a dependency in `pom.xml`).
