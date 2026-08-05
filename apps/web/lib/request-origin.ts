const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

function isLocalhost(hostname: string): boolean {
  return (
    hostname === "localhost" ||
    hostname === "127.0.0.1" ||
    hostname === "0.0.0.0" ||
    hostname === "::1" ||
    hostname === "host.docker.internal" ||
    hostname.endsWith(".localhost")
  );
}

/**
 * Reject browser cross-origin mutations before cookie-backed BFF routes run.
 * Non-browser clients without Origin/Sec-Fetch-Site remain supported.
 */
export function isSameOriginMutation(request: Request): boolean {
  if (SAFE_METHODS.has(request.method.toUpperCase())) return true;

  const fetchSite = request.headers.get("sec-fetch-site")?.toLowerCase();
  if (fetchSite === "cross-site") return false;
  if (fetchSite === "same-origin" || fetchSite === "same-site") return true;

  const origin = request.headers.get("origin");
  if (!origin) return true;

  try {
    const supplied = new URL(origin);
    const expected = new URL(request.url);

    const hostHeader =
      request.headers.get("x-forwarded-host") ?? request.headers.get("host");

    let expectedHost = expected.host;
    let expectedHostname = expected.hostname;
    let expectedProtocol = expected.protocol;

    if (hostHeader) {
      expectedHost = hostHeader;
      expectedHostname = hostHeader.split(":")[0];
    } else if (process.env.LMSPILOT_PUBLIC_URL) {
      try {
        const pub = new URL(process.env.LMSPILOT_PUBLIC_URL);
        expectedHost = pub.host;
        expectedHostname = pub.hostname;
        expectedProtocol = pub.protocol;
      } catch {}
    }

    if (supplied.protocol !== expectedProtocol && supplied.protocol !== expected.protocol) {
      if (process.env.NODE_ENV === "production" && process.env.LMSPILOT_COOKIE_SECURE === "true") {
        return false;
      }
    }

    if (isLocalhost(supplied.hostname) && isLocalhost(expectedHostname)) {
      return true;
    }

    return supplied.host === expectedHost || supplied.host === expected.host;
  } catch {
    return false;
  }
}

