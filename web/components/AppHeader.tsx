import Link from "next/link";

export function AppHeader() {
  return (
    <header className="border-b border-line bg-surface/80 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2 font-semibold tracking-tight">
          <span className="grid h-7 w-7 place-items-center rounded-md bg-accent font-mono text-sm text-white">
            {"{}"}
          </span>
          CodeMind
        </Link>
        <nav className="flex items-center gap-1 text-sm">
          <Link href="/" className="rounded-md px-3 py-1.5 text-muted hover:bg-hover hover:text-fg">
            Repositories
          </Link>
          <Link
            href="/projects/new"
            className="rounded-md bg-accent px-3 py-1.5 font-medium text-white hover:bg-accent-strong"
          >
            Add repository
          </Link>
        </nav>
      </div>
    </header>
  );
}
