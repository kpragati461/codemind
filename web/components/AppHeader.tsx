"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { clearSession, useSession } from "@/lib/auth";

export function AppHeader() {
  const pathname = usePathname();
  const session = useSession();
  // On protected pages, show no links until the session is known rather than flashing "Sign in".
  const onAuthPage = pathname === "/login" || pathname === "/register";

  // <AuthGate> notices the cleared session and moves to /login.
  const signOut = () => clearSession("signout");

  return (
    <header className="border-b border-line bg-surface/80 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-4 px-4">
        <Link href="/" className="flex items-center gap-2 font-semibold tracking-tight">
          <span className="grid h-7 w-7 place-items-center rounded-md bg-accent font-mono text-sm text-white">
            {"{}"}
          </span>
          CodeMind
        </Link>
        {session ? (
          <nav className="flex min-w-0 items-center gap-1 text-sm">
            <Link href="/" className="rounded-md px-3 py-1.5 text-muted hover:bg-hover hover:text-fg">
              Repositories
            </Link>
            <Link
              href="/projects/new"
              className="rounded-md bg-accent px-3 py-1.5 font-medium text-white hover:bg-accent-strong"
            >
              Add repository
            </Link>
            <span className="ml-3 hidden max-w-48 truncate text-muted sm:inline" title={session.user.email}>
              {session.user.email}
            </span>
            <button
              type="button"
              onClick={signOut}
              className="rounded-md px-3 py-1.5 text-muted hover:bg-hover hover:text-fg"
            >
              Sign out
            </button>
          </nav>
        ) : onAuthPage ? (
          <nav className="flex items-center gap-1 text-sm">
            <Link href="/login" className="rounded-md px-3 py-1.5 text-muted hover:bg-hover hover:text-fg">
              Sign in
            </Link>
            <Link
              href="/register"
              className="rounded-md bg-accent px-3 py-1.5 font-medium text-white hover:bg-accent-strong"
            >
              Create account
            </Link>
          </nav>
        ) : null}
      </div>
    </header>
  );
}
