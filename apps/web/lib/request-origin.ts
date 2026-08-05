const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/**
 * Reject browser cross-origin mutations before cookie-backed BFF routes run.
 * Non-browser clients without Origin/Sec-Fetch-Site remain supported.
 */
export function isSameOriginMutation(request: Request): boolean {
  if (SAFE_METHODS.has(request.method.toUpperCase())) return true;

  const origin = request.headers.get("origin");
  if (!origin) return true;

  try {
    const supplied = new URL(origin);
    const expectedUrl = new URL(request.url);
    const hostHeader = request.headers.get("x-forwarded-host") || request.headers.get("host") || expectedUrl.host;

    if (supplied.host === hostHeader || supplied.host === expectedUrl.host) {
      return true;
    }

    const suppliedHostname = supplied.hostname.toLowerCase();
    const expectedHostname = expectedUrl.hostname.toLowerCase();
    const isLocal =
      (suppliedHostname === "localhost" || suppliedHostname === "127.0.0.1") &&
      (expectedHostname === "localhost" || expectedHostname === "127.0.0.1" || expectedHostname === "web");
    if (isLocal) return true;

    return supplied.host === hostHeader;
  } catch {
    return true;
  }
}

