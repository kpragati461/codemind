# CodeMind Web

Next.js 15 (App Router) + Tailwind frontend for the CodeMind API.

## Run

```bash
npm install
npm run dev        # http://localhost:3000
```

The API must be running on `http://localhost:8080` (override with `NEXT_PUBLIC_API_URL`). The API only
allows CORS from `http://localhost:3000`, so keep the dev server on that port.

## Check

```bash
npx tsc --noEmit
npm run lint
npm run build
```

## Pages

| Route            | What it does                                                                |
| ---------------- | --------------------------------------------------------------------------- |
| `/`              | Repository dashboard: status counts, list newest first, polls while indexing |
| `/projects/new`  | Register a Git URL (HTTPS or `git@host:owner/repo`) and optional name       |
| `/projects/[id]` | Ingestion progress, error details, re-index, and the detected tech stack    |

## Layout

- `lib/api.ts`: typed client for `/api/projects` (types mirror `ProjectResponse` in the API)
- `lib/usePoll.ts`: fetch on mount and on window focus, then poll while a predicate holds
- `lib/format.ts`: relative times and short repo URLs
- `components/`: header, status badge, error banner
- Theme colours are CSS variables in `app/globals.css` (light and dark), exposed to Tailwind as
  `bg`, `surface`, `fg`, `muted`, `line`, `hover` and `accent` in `tailwind.config.ts`.
