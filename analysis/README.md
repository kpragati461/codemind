# CodeMind Analysis Service

Python/FastAPI service the Spring Boot API calls during ingestion (`CODEMIND_ANALYSIS_URL`, default `http://localhost:8000`).

## Run

```bash
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt   # Windows; use .venv/bin/python elsewhere
.venv/Scripts/python -m uvicorn app.main:app --port 8000
```

## Test

```bash
.venv/Scripts/python -m pytest -q
```

## Endpoints

- `GET /health`
- `POST /v1/fingerprint` `{projectId, path}` — languages, manifests, build systems, package managers. `path` must be inside `CODEMIND_WORKSPACE` (default `~/.codemind`).
