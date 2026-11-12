"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useState } from "react";
import { api, isActive, isFingerprint, type Fingerprint, type Project } from "@/lib/api";
import { shortRepoUrl, timeAgo } from "@/lib/format";
import { usePoll } from "@/lib/usePoll";
import { StatusBadge } from "@/components/StatusBadge";
import { ErrorBanner } from "@/components/ErrorBanner";

export default function ProjectPage() {
  const { id } = useParams<{ id: string }>();
  const fetchProject = useCallback(() => api.getProject(id), [id]);
  const { data: project, error, loading, refresh } = usePoll(fetchProject, 2000, (p) => isActive(p.status));

  if (loading && !project) {
    return <div className="h-40 animate-pulse rounded-xl border border-line bg-surface" />;
  }
  if (!project) {
    return (
      <div className="space-y-4">
        <BackLink />
        <ErrorBanner message={error?.message ?? "Repository not found"} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <BackLink />
      <Header project={project} onReingested={refresh} />
      {error && <ErrorBanner message={error.message} />}
      <StatusPanel project={project} />
      {isFingerprint(project.fingerprint) ? (
        <FingerprintView fp={project.fingerprint} />
      ) : (
        project.fingerprint && (
          <ErrorBanner message="The stack fingerprint for this repository couldn't be read. Try re-indexing." />
        )
      )}
    </div>
  );
}

function BackLink() {
  return (
    <Link href="/" className="text-sm text-muted hover:text-fg">
      ← Repositories
    </Link>
  );
}

function Header({ project: p, onReingested }: { project: Project; onReingested: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function reingest() {
    setBusy(true);
    setError(undefined);
    try {
      await api.reingest(p.id);
      await onReingested();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Re-index failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-3">
            <h1 className="truncate text-2xl font-semibold tracking-tight">{p.name}</h1>
            <StatusBadge status={p.status} />
          </div>
          <a
            href={p.gitUrl.startsWith("http") ? p.gitUrl : undefined}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-1 block truncate font-mono text-xs text-muted hover:text-fg"
          >
            {shortRepoUrl(p.gitUrl)}
          </a>
        </div>
        <button
          onClick={reingest}
          disabled={busy || isActive(p.status)}
          className="rounded-md border border-line bg-surface px-3 py-1.5 text-sm hover:bg-hover disabled:cursor-not-allowed disabled:opacity-50"
        >
          {busy ? "Starting…" : "Re-index"}
        </button>
      </div>
      {error && <ErrorBanner message={error} />}
    </div>
  );
}

const STEPS = [
  { key: "QUEUED", label: "Queued" },
  { key: "INDEXING", label: "Clone & fingerprint" },
  { key: "COMPLETED", label: "Ready" },
] as const;

function StatusPanel({ project: p }: { project: Project }) {
  const failed = p.status === "FAILED";
  const current = failed ? -1 : STEPS.findIndex((s) => s.key === p.status);

  return (
    <section className="rounded-xl border border-line bg-surface p-5">
      <ol className="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm">
        {STEPS.map((step, i) => {
          const done = !failed && (i < current || p.status === "COMPLETED");
          const active = !failed && i === current && p.status !== "COMPLETED";
          return (
            <li key={step.key} className="flex items-center gap-3">
              {i > 0 && <span className="h-px w-6 bg-line" aria-hidden />}
              <span
                className={`flex items-center gap-2 ${done ? "text-fg" : active ? "font-medium text-accent" : "text-muted"}`}
              >
                <span
                  className={`grid h-5 w-5 place-items-center rounded-full border text-[10px] ${
                    done ? "border-emerald-500 bg-emerald-500 text-white" : active ? "border-accent" : "border-line"
                  }`}
                >
                  {done ? "✓" : i + 1}
                </span>
                {step.label}
              </span>
            </li>
          );
        })}
      </ol>

      {failed && (
        <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/10 p-3">
          <div className="text-sm font-medium text-red-700 dark:text-red-300">Ingestion failed</div>
          <pre className="mt-1 whitespace-pre-wrap break-words font-mono text-xs text-red-700/90 dark:text-red-300/90">
            {p.errorMessage ?? "Unknown error"}
          </pre>
        </div>
      )}

      <dl className="mt-4 grid grid-cols-1 gap-3 border-t border-line pt-4 text-xs sm:grid-cols-3">
        <Meta label="Added" value={timeAgo(p.createdAt)} title={new Date(p.createdAt).toLocaleString()} />
        <Meta label="Last update" value={timeAgo(p.updatedAt)} title={new Date(p.updatedAt).toLocaleString()} />
        <Meta label="Workspace" value={p.localPath ?? "—"} mono />
      </dl>
    </section>
  );
}

function Meta({ label, value, title, mono }: { label: string; value: string; title?: string; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="text-muted">{label}</dt>
      <dd className={`mt-0.5 truncate ${mono ? "font-mono" : ""}`} title={title ?? value}>
        {value}
      </dd>
    </div>
  );
}

const BAR_COLORS = ["bg-indigo-500", "bg-sky-500", "bg-emerald-500", "bg-amber-500", "bg-rose-500", "bg-violet-500"];

function FingerprintView({ fp }: { fp: Fingerprint }) {
  const shown = fp.languages.slice(0, 6);
  const otherShare = fp.languages.slice(6).reduce((sum, l) => sum + l.share, 0);

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold tracking-tight">Technology stack</h2>

      <div className="rounded-xl border border-line bg-surface p-5">
        <div className="flex items-baseline justify-between">
          <h3 className="text-sm font-medium">Languages</h3>
          <span className="text-xs text-muted">
            {fp.totalFiles.toLocaleString()} files scanned{fp.truncated && " (truncated)"}
          </span>
        </div>
        {shown.length === 0 ? (
          <p className="mt-3 text-sm text-muted">No recognised source files.</p>
        ) : (
          <>
            <div className="mt-3 flex h-2.5 overflow-hidden rounded-full bg-hover">
              {shown.map((l, i) => (
                <div key={l.name} className={BAR_COLORS[i]} style={{ width: `${l.share * 100}%` }} title={l.name} />
              ))}
              {otherShare > 0 && <div className="bg-slate-400" style={{ width: `${otherShare * 100}%` }} />}
            </div>
            <ul className="mt-3 flex flex-wrap gap-x-5 gap-y-1.5 text-sm">
              {shown.map((l, i) => (
                <li key={l.name} className="flex items-center gap-2">
                  <span className={`h-2 w-2 rounded-full ${BAR_COLORS[i]}`} />
                  <span>{l.name}</span>
                  <span className="tabular-nums text-muted">
                    {(l.share * 100).toFixed(1)}% · {l.files}
                  </span>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Chips title="Build systems" items={fp.buildSystems} />
        <Chips title="Package managers" items={fp.packageManagers} />
        <Chips title="Container" items={fp.containerized ? ["Docker"] : []} />
      </div>

      <div className="rounded-xl border border-line bg-surface p-5">
        <h3 className="text-sm font-medium">Manifests</h3>
        {fp.manifests.length === 0 ? (
          <p className="mt-2 text-sm text-muted">None found.</p>
        ) : (
          <ul className="mt-2 grid gap-1 font-mono text-xs sm:grid-cols-2">
            {fp.manifests.map((m) => (
              <li key={m} className="truncate text-muted" title={m}>
                {m}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

function Chips({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-5">
      <h3 className="text-sm font-medium">{title}</h3>
      {items.length === 0 ? (
        <p className="mt-2 text-sm text-muted">None detected</p>
      ) : (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {items.map((it) => (
            <span key={it} className="rounded-md border border-line bg-bg px-2 py-0.5 text-xs">
              {it}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
