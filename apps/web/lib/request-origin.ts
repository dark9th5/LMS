const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/**
 * Reject browser cross-origin mutations before cookie-backed BFF routes run.
 * Non-browser clients without Origin/Sec-Fetch-Site remain supported.
 */
export function isSameOriginMutation(request: Request): boolean {
  if (SAFE_METHODS.has(request.method.toUpperCase())) return true;

  const fetchSite = request.headers.get("sec-fetch-site")?.toLowerCase();
  if (fetchSite === "cross-site") return false;

  const origin = request.headers.get("origin");
  if (!origin) return true;

  try {
    const supplied = new URL(origin);
    const expected = new URL(request.url);
    return supplied.protocol === expected.protocol && supplied.host === expected.host;
  } catch {
    return false;
  }
}
