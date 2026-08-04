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

    // 1. Direct match on request URL
    if (supplied.protocol === expected.protocol && supplied.host === expected.host) {
      return true;
    }

    // 2. Check against Host / X-Forwarded-Host headers
    const hostHeader = request.headers.get("x-forwarded-host") || request.headers.get("host");
    if (hostHeader) {
      const cleanHost = hostHeader.split(",")[0].trim().toLowerCase();
      if (supplied.host.toLowerCase() === cleanHost) {
        return true;
      }
    }

    // 3. Check against LMSPILOT_PUBLIC_URL if configured
    const publicUrl = process.env.LMSPILOT_PUBLIC_URL;
    if (publicUrl) {
      const pub = new URL(publicUrl);
      if (supplied.host.toLowerCase() === pub.host.toLowerCase()) {
        return true;
      }
    }

    // 4. Development / Docker local access (localhost vs 127.0.0.1 vs container host)
    const suppliedHostname = supplied.hostname.toLowerCase();
    if (suppliedHostname === "localhost" || suppliedHostname === "127.0.0.1") {
      return true;
    }

    return false;
  } catch {
    return false;
  }
}

