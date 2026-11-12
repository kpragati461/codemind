"use client";

import { useCallback, useEffect, useRef, useState } from "react";

interface PollState<T> {
  data: T | undefined;
  error: Error | undefined;
  loading: boolean;
  refresh: () => Promise<void>;
}

/**
 * Fetches once on mount, then re-fetches every `intervalMs` while `keepPolling(data)` is true.
 * Also refreshes when the tab regains focus.
 */
export function usePoll<T>(
  fetcher: () => Promise<T>,
  intervalMs: number,
  keepPolling: (data: T) => boolean,
): PollState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<Error>();
  const [loading, setLoading] = useState(true);

  // Keep the latest callbacks without restarting the effect on every render.
  const fetcherRef = useRef(fetcher);
  const keepRef = useRef(keepPolling);
  fetcherRef.current = fetcher;
  keepRef.current = keepPolling;

  const refresh = useCallback(async () => {
    try {
      const next = await fetcherRef.current();
      setData(next);
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
    const onFocus = () => void refresh();
    window.addEventListener("focus", onFocus);
    return () => window.removeEventListener("focus", onFocus);
  }, [refresh]);

  const shouldPoll = data !== undefined && keepRef.current(data);
  useEffect(() => {
    if (!shouldPoll) return;
    const id = window.setInterval(() => void refresh(), intervalMs);
    return () => window.clearInterval(id);
  }, [shouldPoll, intervalMs, refresh]);

  return { data, error, loading, refresh };
}
