import type { ProjectStatus } from "@/lib/api";

const styles: Record<ProjectStatus, { label: string; className: string; pulse: boolean }> = {
  QUEUED: { label: "Queued", className: "bg-slate-500/15 text-slate-600 dark:text-slate-300", pulse: true },
  INDEXING: { label: "Indexing", className: "bg-blue-500/15 text-blue-700 dark:text-blue-300", pulse: true },
  COMPLETED: { label: "Completed", className: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-300", pulse: false },
  FAILED: { label: "Failed", className: "bg-red-500/15 text-red-700 dark:text-red-300", pulse: false },
};

export function StatusBadge({ status }: { status: ProjectStatus }) {
  const s = styles[status];
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${s.className}`}
    >
      <span className="relative flex h-1.5 w-1.5">
        {s.pulse && (
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-60" />
        )}
        <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
      </span>
      {s.label}
    </span>
  );
}
