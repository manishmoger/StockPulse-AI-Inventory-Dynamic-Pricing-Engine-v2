import { useEffect, useRef } from 'react';

/**
 * Calls `fn` immediately and then every `intervalMs` milliseconds.
 * Stops polling when the component unmounts.
 */
export function usePolling(fn: () => void, intervalMs: number) {
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    fnRef.current();
    const id = setInterval(() => fnRef.current(), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
}
