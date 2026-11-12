export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type ProjectStatus = "QUEUED" | "INDEXING" | "COMPLETED" | "FAILED";

export interface LanguageShare {
  name: string;
  files: number;
  share: number;
}

export interface Fingerprint {
  primaryLanguage: string | null;
  languages: LanguageShare[];
  manifests: string[];
  buildSystems: string[];
  packageManagers: string[];
  containerized: boolean;
  totalFiles: number;
  truncated: boolean;
}

export interface Project {
  id: string;
  name: string;
  gitUrl: string;
  localPath: string | null;
  status: ProjectStatus;
  /** `{ raw }` is the API's fallback when the stored fingerprint isn't valid JSON. */
  fingerprint: Fingerprint | { raw: string } | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      ...init,
      headers: { "Content-Type": "application/json", ...init?.headers },
      cache: "no-store",
    });
  } catch {
    throw new ApiError(`Can't reach the CodeMind API at ${API_URL}. Is it running?`, 0);
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (typeof body?.error === "string") message = body.error;
    } catch {
      // non-JSON error body; keep the generic message
    }
    throw new ApiError(message, res.status);
  }
  return res.json() as Promise<T>;
}

export const api = {
  listProjects: () => request<Project[]>("/api/projects"),
  getProject: (id: string) => request<Project>(`/api/projects/${encodeURIComponent(id)}`),
  createProject: (gitUrl: string, name?: string) =>
    request<Project>("/api/projects", {
      method: "POST",
      body: JSON.stringify({ gitUrl, name: name || undefined }),
    }),
  reingest: (id: string) =>
    request<Project>(`/api/projects/${encodeURIComponent(id)}/ingest`, { method: "POST" }),
};

/**
 * The API returns `{ raw: "..." }` when the stored fingerprint isn't valid JSON, so check the
 * shape before rendering it.
 */
export function isFingerprint(value: unknown): value is Fingerprint {
  if (typeof value !== "object" || value === null) return false;
  const fp = value as Partial<Fingerprint>;
  return (
    Array.isArray(fp.languages) &&
    Array.isArray(fp.manifests) &&
    Array.isArray(fp.buildSystems) &&
    Array.isArray(fp.packageManagers)
  );
}

export function isActive(status: ProjectStatus) {
  return status === "QUEUED" || status === "INDEXING";
}
