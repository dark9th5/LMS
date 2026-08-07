const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/** Reject browser cross-origin mutations before cookie-backed BFF routes run. */
export function isSameOriginMutation(request: Request): boolean {
  if (SAFE_METHODS.has(request.method.toUpperCase())) return true;
  const fetchSite = request.headers.get("sec-fetch-site")?.toLowerCase();
  if (fetchSite === "cross-site") return false;
  const origin = request.headers.get("origin");
  if (!origin) return true;

  try {
    const supplied = new URL(origin);
    const hostHeader = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
    let expectedHost = new URL(request.url).host;
    if (hostHeader) expectedHost = hostHeader.split(",")[0].trim();
    const expectedHostname = expectedHost.split(":")[0];
    const isSuppliedLocal = supplied.hostname === "localhost" || supplied.hostname === "127.0.0.1";
    const isExpectedLocal = expectedHostname === "localhost" || expectedHostname === "127.0.0.1";
    if (isSuppliedLocal && isExpectedLocal) return true;
    if (supplied.hostname === expectedHostname) return true;
    return supplied.host === expectedHost;
  } catch {
    return false;
  }
}
