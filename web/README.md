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
| `/login`         | Sign in; returns to `?next=` afterwards                                     |
| `/register`      | Create an account (signs you in straight away)                              |
| `/`              | Repository dashboard: status counts, list newest first, polls while indexing |
| `/projects/new`  | Register a Git URL (HTTPS or `git@host:owner/repo`) and optional name       |
| `/projects/[id]` | Ingestion progress, error details, re-index, and the detected tech stack    |

## Layout

- `lib/api.ts`: typed client for `/api/auth` and `/api/projects` (types mirror the API DTOs); sends
  the bearer token and clears the session on `401`
- `lib/auth.ts`: session store (token + user in `localStorage`, synced across tabs) and `useSession()`
- `components/AuthGate.tsx`: client-side route protection; everything except `/login` and
  `/register` needs a session
- `lib/usePoll.ts`: fetch on mount and on window focus, then poll while a predicate holds
- `lib/format.ts`: relative times and short repo URLs
- `components/`: header (signed-in user, sign out), auth form, form field, status badge, error banner
- Theme colours are CSS variables in `app/globals.css` (light and dark), exposed to Tailwind as
  `bg`, `surface`, `fg`, `muted`, `line`, `hover` and `accent` in `tailwind.config.ts`.
