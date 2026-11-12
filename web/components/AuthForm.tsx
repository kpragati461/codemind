"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { api } from "@/lib/api";
import { safeNext, saveSession } from "@/lib/auth";
import { ErrorBanner } from "@/components/ErrorBanner";
import { Field, inputClass } from "@/components/Field";

const MIN_PASSWORD = 8;

const COPY = {
  login: {
    title: "Sign in",
    subtitle: "Sign in to see your repositories.",
    submit: "Sign in",
    busy: "Signing in…",
    switchText: "New to CodeMind?",
    switchLink: "Create an account",
    switchHref: "/register",
  },
  register: {
    title: "Create an account",
    subtitle: "Your repositories are private to your account.",
    submit: "Create account",
    busy: "Creating account…",
    switchText: "Already have an account?",
    switchLink: "Sign in",
    switchHref: "/login",
  },
} as const;

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const copy = COPY[mode];
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(undefined);
    if (mode === "register") {
      if (password.length < MIN_PASSWORD) {
        setError(`Password must be at least ${MIN_PASSWORD} characters.`);
        return;
      }
      if (password !== confirm) {
        setError("Passwords don't match.");
        return;
      }
    }
    setSubmitting(true);
    try {
      const session =
        mode === "login" ? await api.login(email.trim(), password) : await api.register(email.trim(), password);
      saveSession(session);
      // Read ?next= at submit time rather than via useSearchParams, which would force a Suspense boundary.
      router.replace(safeNext(new URLSearchParams(window.location.search).get("next")));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
      setSubmitting(false);
    }
  }

  // Carry ?next= across the login/register switch so the user still lands where they were going.
  // Set after mount: the server render has no query string, so reading it during render would
  // cause a hydration mismatch.
  const [switchHref, setSwitchHref] = useState<string>(copy.switchHref);
  useEffect(() => setSwitchHref(`${copy.switchHref}${window.location.search}`), [copy.switchHref]);

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{copy.title}</h1>
        <p className="mt-1 text-sm text-muted">{copy.subtitle}</p>
      </div>

      <form onSubmit={onSubmit} className="space-y-5 rounded-xl border border-line bg-surface p-6">
        <Field label="Email" htmlFor="email">
          <input
            id="email"
            type="email"
            required
            autoFocus
            autoComplete="email"
            spellCheck={false}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            className={inputClass}
          />
        </Field>

        <Field
          label="Password"
          htmlFor="password"
          hint={mode === "register" ? `At least ${MIN_PASSWORD} characters.` : undefined}
        >
          <input
            id="password"
            type="password"
            required
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            minLength={mode === "register" ? MIN_PASSWORD : undefined}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </Field>

        {mode === "register" && (
          <Field label="Confirm password" htmlFor="confirm">
            <input
              id="confirm"
              type="password"
              required
              autoComplete="new-password"
              maxLength={72}
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              className={inputClass}
            />
          </Field>
        )}

        {error && <ErrorBanner message={error} />}

        <button
          type="submit"
          disabled={submitting || !email.trim() || !password}
          className="w-full rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-strong disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? copy.busy : copy.submit}
        </button>
      </form>

      <p className="text-center text-sm text-muted">
        {copy.switchText}{" "}
        <Link href={switchHref} className="font-medium text-fg underline hover:text-accent">
          {copy.switchLink}
        </Link>
      </p>
    </div>
  );
}
