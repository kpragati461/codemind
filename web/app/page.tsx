"use client";

import Link from "next/link";
import { api, isActive, isFingerprint, type Project } from "@/lib/api";
import { shortRepoUrl, timeAgo } from "@/lib/format";
import { usePoll } from "@/lib/usePoll";
import { StatusBadge } from "@/components/StatusBadge";
import { ErrorBanner } from "@/components/ErrorBanner";

export default function DashboardPage() {
  const { data: projects, error, loading } = usePoll(
    api.listProjects,
    2500,
    (list) => list.some((p) => isActive(p.status)),
  );

  const counts = summarize(projects ?? []);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Repositories</h1>
          <p className="mt-1 text-sm text-muted">Connect a repository to fingerprint its stack and start analysis.</p>
        </div>
        {projects && projects.length > 0 && (
          <dl className="flex gap-6 text-sm">
            <Stat label="Total" value={projects.length} />
            <Stat label="Indexing" value={counts.active} />
            <Stat label="Completed" value={counts.completed} />
            <Stat label="Failed" value={counts.failed} />
          </dl>
        )}
      </div>

      {error && <ErrorBanner message={error.message} />}

      {loading && !projects ? (
        <ListSkeleton />
      ) : projects && projects.length === 0 ? (
        <EmptyState />
      ) : projects ? (
        <ul className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface">
          {projects.map((p) => (
            <ProjectRow key={p.id} project={p} />
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function ProjectRow({ project: p }: { project: Project }) {
  const fp = isFingerprint(p.fingerprint) ? p.fingerprint : null;
  return (
    <li>
      <Link
        href={`/projects/${p.id}`}
        className="flex flex-col gap-2 px-5 py-4 hover:bg-hover sm:flex-row sm:items-center sm:justify-between"
      >
        <div className="min-w-0">
          <div className="flex items-center gap-3">
            <span className="truncate font-medium">{p.name}</span>
            <StatusBadge status={p.status} />
          </div>
          <div className="mt-1 truncate font-mono text-xs text-muted">{shortRepoUrl(p.gitUrl)}</div>
          {p.status === "FAILED" && p.errorMessage && (
            <div className="mt-1 line-clamp-1 text-xs text-red-600 dark:text-red-400">{p.errorMessage}</div>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-4 text-xs text-muted">
          {fp?.primaryLanguage && (
            <span className="rounded-md border border-line px-2 py-0.5 text-fg">{fp.primaryLanguage}</span>
          )}
          {fp && fp.buildSystems.length > 0 && <span>{fp.buildSystems.join(", ")}</span>}
          <span title={new Date(p.updatedAt).toLocaleString()}>{timeAgo(p.updatedAt)}</span>
        </div>
      </Link>
    </li>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="text-lg font-semibold tabular-nums">{value}</dd>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="rounded-xl border border-dashed border-line bg-surface px-6 py-16 text-center">
      <h2 className="font-medium">No repositories yet</h2>
      <p className="mx-auto mt-1 max-w-sm text-sm text-muted">
        Add a GitHub repository URL and CodeMind will clone it and detect its technology stack.
      </p>
      <Link
        href="/projects/new"
        className="mt-5 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-strong"
      >
        Add your first repository
      </Link>
    </div>
  );
}

function ListSkeleton() {
  return (
    <ul className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface">
      {[0, 1, 2].map((i) => (
        <li key={i} className="space-y-2 px-5 py-4">
          <div className="h-4 w-40 animate-pulse rounded bg-hover" />
          <div className="h-3 w-64 animate-pulse rounded bg-hover" />
        </li>
      ))}
    </ul>
  );
}

function summarize(projects: Project[]) {
  return {
    active: projects.filter((p) => isActive(p.status)).length,
    completed: projects.filter((p) => p.status === "COMPLETED").length,
    failed: projects.filter((p) => p.status === "FAILED").length,
  };
}
