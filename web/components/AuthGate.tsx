"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getSession, saveSession, sessionEndedBy, useSession } from "@/lib/auth";

/** Pages a signed-out visitor may see. Everything else needs a session. */
const PUBLIC_PATHS = ["/login", "/register"];

/**
 * Client-side route protection. Signed-out visitors on a protected page go to /login?next=<page>;
 * signed-in users on /login or /register go to the dashboard. The API enforces access on its own;
 * this only keeps the UI from showing pages that would just fail with 401.
 */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = useSession();
  const [mounted, setMounted] = useState(false);
  const isPublic = PUBLIC_PATHS.includes(pathname);

  useEffect(() => setMounted(true), []);

  useEffect(() => {
    if (!mounted) return;
    // Read the store directly: on the first client render useSession() still reports the
    // server snapshot (signed out).
    const current = getSession();
    if (!isPublic && !current) {
      // After an explicit sign-out, don't carry the page along: the next person to sign in may
      // be a different user.
      const here = window.location.pathname + window.location.search;
      const keepPage = here !== "/" && sessionEndedBy() !== "signout";
      router.replace(keepPage ? `/login?next=${encodeURIComponent(here)}` : "/login");
    } else if (isPublic && current) {
      router.replace("/");
    }
  }, [mounted, isPublic, session, router]);

  // Confirm the stored token is still accepted and refresh the cached user. A 401 clears the
  // session inside api.request(), which re-runs the effect above and redirects to /login.
  const token = session?.token;
  useEffect(() => {
    if (!token) return;
    api
      .me()
      .then((user) => {
        const current = getSession();
        if (current?.token === token && current.user.email !== user.email) saveSession({ ...current, user });
      })
      .catch(() => {
        // Network errors leave the session alone; the page shows its own error.
      });
  }, [token]);

  if (isPublic) return <>{children}</>;
  if (!mounted || !session) return <PageSkeleton />;
  return <>{children}</>;
}

function PageSkeleton() {
  return (
    <div aria-busy="true" className="space-y-3">
      <div className="h-7 w-48 animate-pulse rounded bg-hover" />
      <div className="h-40 animate-pulse rounded-xl border border-line bg-surface" />
    </div>
  );
}
