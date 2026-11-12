"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/api";
import { ErrorBanner } from "@/components/ErrorBanner";

const EXAMPLES = [
  "https://github.com/spring-projects/spring-petclinic",
  "https://github.com/gothinkster/realworld",
];

export default function NewProjectPage() {
  const router = useRouter();
  const [gitUrl, setGitUrl] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(undefined);
    setSubmitting(true);
    try {
      const project = await api.createProject(gitUrl.trim(), name.trim());
      router.push(`/projects/${project.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <Link href="/" className="text-sm text-muted hover:text-fg">
          ← Repositories
        </Link>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">Add repository</h1>
        <p className="mt-1 text-sm text-muted">
          CodeMind clones the repository into an isolated workspace and fingerprints its stack.
        </p>
      </div>

      <form onSubmit={onSubmit} className="space-y-5 rounded-xl border border-line bg-surface p-6">
        <Field label="Git URL" htmlFor="gitUrl" hint="HTTPS (https://github.com/owner/repo) or SSH (git@github.com:owner/repo.git)">
          <input
            id="gitUrl"
            type="text"
            required
            autoFocus
            spellCheck={false}
            value={gitUrl}
            onChange={(e) => setGitUrl(e.target.value)}
            placeholder="https://github.com/owner/repo"
            className={inputClass}
          />
        </Field>

        <Field label="Display name" htmlFor="name" hint="Optional. Defaults to the repository name.">
          <input
            id="name"
            type="text"
            maxLength={200}
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
        </Field>

        {error && <ErrorBanner message={error} />}

        <div className="flex items-center justify-between gap-4">
          <div className="text-xs text-muted">
            Try:{" "}
            {EXAMPLES.map((url, i) => (
              <span key={url}>
                {i > 0 && " · "}
                <button type="button" onClick={() => setGitUrl(url)} className="underline hover:text-fg">
                  {url.split("/").pop()}
                </button>
              </span>
            ))}
          </div>
          <button
            type="submit"
            disabled={submitting || !gitUrl.trim()}
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-strong disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? "Adding…" : "Add repository"}
          </button>
        </div>
      </form>
    </div>
  );
}

const inputClass =
  "w-full rounded-md border border-line bg-bg px-3 py-2 font-mono text-sm outline-none placeholder:text-muted/70 focus:border-accent focus:ring-2 focus:ring-accent/30";

function Field({
  label,
  htmlFor,
  hint,
  children,
}: {
  label: string;
  htmlFor: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium">
        {label}
      </label>
      {children}
      {hint && <p className="text-xs text-muted">{hint}</p>}
    </div>
  );
}
