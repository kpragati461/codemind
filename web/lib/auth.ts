"use client";

import { useSyncExternalStore } from "react";
import type { AuthResponse } from "@/lib/api";

/**
 * Client-side session store. The API is stateless, so the session is just the bearer token from
 * login/register plus the user it belongs to, kept in localStorage so it survives reloads.
 */
export type Session = AuthResponse;

const STORAGE_KEY = "codemind.session";

let cached: Session | null | undefined;
let endedBy: "signout" | "expired" | null = null;
const listeners = new Set<() => void>();

function load(): Session | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    if (typeof session?.token !== "string") return null;
    if (session.expiresAt && Date.parse(session.expiresAt) <= Date.now()) return null;
    return session;
  } catch {
    return null; // storage blocked or corrupted: treat as signed out
  }
}

export function getSession(): Session | null {
  if (typeof window === "undefined") return null;
  if (cached === undefined) cached = load();
  return cached;
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

function set(session: Session | null) {
  cached = session;
  try {
    if (session) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // storage unavailable: the session still lives in memory for this tab
  }
  listeners.forEach((l) => l());
}

export function saveSession(session: Session) {
  endedBy = null;
  set(session);
}

/**
 * Ends the session. `"signout"` is the user pressing Sign out; `"expired"` is the API rejecting the
 * token, after which the user is sent back to the page they were on once they sign in again.
 */
export function clearSession(reason: "signout" | "expired") {
  if (getSession() === null) return;
  endedBy = reason;
  set(null);
}

/** How the last session in this tab ended, or `null` if it never had one. */
export function sessionEndedBy() {
  return endedBy;
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  // Keep tabs in sync: signing out in one tab signs out the others.
  const onStorage = (e: StorageEvent) => {
    if (e.key !== STORAGE_KEY) return;
    cached = undefined;
    listener();
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", onStorage);
  };
}

/** The current session, or `null` when signed out (and always `null` during server rendering). */
export function useSession(): Session | null {
  return useSyncExternalStore(subscribe, getSession, () => null);
}

/** Only allow same-origin relative paths as a post-login destination. */
export function safeNext(next: string | null): string {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/";
}
