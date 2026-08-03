import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { encodeUserCookie } from "@/lib/session-cookie";
import type { PortalUser } from "@/lib/types";

function getGatewayUrl(): string {
  const envUrl = process.env.LMSPILOT_GATEWAY_URL;
  if (envUrl && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/+$/, "");
  }
  return "http://localhost:8080";
}

const upstreamTimeoutMs = positiveInteger(
  process.env.LMSPILOT_UPSTREAM_TIMEOUT_MS,
  15_000,
);

type RouteContext = { params: Promise<{ path: string[] }> };
type TokenPayload = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: PortalUser;
};

type RefreshResult =
  | { kind: "ok"; session: TokenPayload }
  | { kind: "rejected" }
  | { kind: "unavailable" };

const refreshInFlight = new Map<string, Promise<RefreshResult>>();

const requestHopByHopHeaders = [
  "connection",
  "cookie",
  "host",
  "content-length",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
];

const responseHopByHopHeaders = [
  "connection",
  "content-length",
  "content-encoding",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "set-cookie",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
];

function positiveInteger(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function cookieSecure(): boolean {
  return process.env.LMSPILOT_COOKIE_SECURE === "true";
}

function validTokenPayload(value: unknown): value is TokenPayload {
  if (!value || typeof value !== "object") return false;
  const payload = value as Partial<TokenPayload>;
  return (
    typeof payload.accessToken === "string" &&
    payload.accessToken.length > 0 &&
    typeof payload.refreshToken === "string" &&
    payload.refreshToken.length > 0 &&
    typeof payload.expiresInSeconds === "number" &&
    Number.isFinite(payload.expiresInSeconds) &&
    payload.expiresInSeconds > 0 &&
    validUser(payload.user)
  );
}

function validUser(value: unknown): value is PortalUser {
  if (!value || typeof value !== "object") return false;
  const user = value as Partial<PortalUser>;
  return (
    typeof user.id === "string" &&
    typeof user.username === "string" &&
    typeof user.fullName === "string" &&
    Array.isArray(user.roles) &&
    user.roles.every((role) => typeof role === "string") &&
    Array.isArray(user.permissions) &&
    user.permissions.every((permission) => typeof permission === "string")
  );
}

function applySessionCookies(response: NextResponse, session: TokenPayload) {
  const secure = cookieSecure();

  response.cookies.set("lmspilot_access", session.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: Math.max(1, Math.floor(session.expiresInSeconds)),
  });
  response.cookies.set("lmspilot_refresh", session.refreshToken, {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/",
    maxAge: 604_800,
  });
  response.cookies.set("lmspilot_user", encodeUserCookie(session.user), {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 604_800,
  });
}

function clearSessionCookies(response: NextResponse) {
  const secure = cookieSecure();
  response.cookies.set("lmspilot_access", "", {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 0,
  });
  response.cookies.set("lmspilot_user", "", {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 0,
  });
  response.cookies.set("lmspilot_refresh", "", {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/",
    maxAge: 0,
  });
  response.cookies.set("lmspilot_refresh", "", {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/api",
    maxAge: 0,
  });
}

async function fetchWithTimeout(
  input: string | URL,
  init: RequestInit,
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), upstreamTimeoutMs);

  const primaryGateway = getGatewayUrl();
  let rawPathOrUrl = typeof input === "string" ? input : input.toString();
  let path = rawPathOrUrl;
  if (rawPathOrUrl.startsWith("http://") || rawPathOrUrl.startsWith("https://")) {
    const parsed = new URL(rawPathOrUrl);
    path = parsed.pathname + parsed.search;
  }
  if (!path.startsWith("/")) path = "/" + path;

  const urlsToTry = Array.from(
    new Set([
      `${primaryGateway}${path}`,
      `http://127.0.0.1:8080${path}`,
      `http://localhost:8080${path}`,
      `http://api-gateway:8080${path}`,
    ]),
  );

  let lastError: unknown;
  try {
    for (const url of urlsToTry) {
      try {
        return await fetch(url, { ...init, signal: controller.signal });
      } catch (err) {
        lastError = err;
      }
    }
    throw lastError;
  } finally {
    clearTimeout(timer);
  }
}

function refreshSession(refreshToken: string): Promise<RefreshResult> {
  const existing = refreshInFlight.get(refreshToken);
  if (existing) return existing;

  const pending = (async (): Promise<RefreshResult> => {
    let response: Response;
    try {
      response = await fetchWithTimeout("/api/v1/auth/refresh", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Correlation-ID": crypto.randomUUID(),
        },
        body: JSON.stringify({ refreshToken }),
        cache: "no-store",
      });
    } catch {
      return { kind: "unavailable" };
    }

    if (!response.ok) return { kind: "rejected" };

    const data = (await response.json().catch(() => null)) as unknown;
    return validTokenPayload(data)
      ? { kind: "ok", session: data }
      : { kind: "rejected" };
  })();

  refreshInFlight.set(refreshToken, pending);
  void pending.finally(() => {
    setTimeout(() => refreshInFlight.delete(refreshToken), 5_000);
  });
  return pending;
}

function validPath(path: unknown): path is string[] {
  return (
    Array.isArray(path) &&
    path.length > 0 &&
    path.every(
      (segment) =>
        typeof segment === "string" &&
        segment.length > 0 &&
        segment !== "." &&
        segment !== ".." &&
        !segment.includes("/") &&
        !segment.includes("\\"),
    )
  );
}

function unauthorized(message = "Phiên đăng nhập đã hết hạn"): NextResponse {
  const response = NextResponse.json(
    { ok: false, code: "UNAUTHORIZED", message },
    { status: 401, headers: { "Cache-Control": "no-store" } },
  );
  clearSessionCookies(response);
  return response;
}

function unavailable(): NextResponse {
  return NextResponse.json(
    {
      ok: false,
      code: "GATEWAY_UNAVAILABLE",
      message:
        "Dịch vụ nghiệp vụ đang không khả dụng. Yêu cầu không được chuyển sang dữ liệu giả.",
    },
    { status: 503, headers: { "Cache-Control": "no-store" } },
  );
}

async function callGateway(
  req: NextRequest,
  url: URL,
  token: string,
  body?: ArrayBuffer,
): Promise<Response> {
  const headers = new Headers(req.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.set(
    "X-Correlation-ID",
    req.headers.get("X-Correlation-ID") ?? crypto.randomUUID(),
  );
  requestHopByHopHeaders.forEach((name) => headers.delete(name));

  const init: RequestInit & { duplex?: "half" } = {
    method: req.method,
    headers,
    body: ["GET", "HEAD"].includes(req.method) ? undefined : body,
    cache: "no-store",
  };
  if (init.body) init.duplex = "half";

  return fetchWithTimeout(url, init);
}

async function proxy(req: NextRequest, { params }: RouteContext) {
  const jar = await cookies();
  let access = jar.get("lmspilot_access")?.value;
  const refresh = jar.get("lmspilot_refresh")?.value;
  let refreshedSession: TokenPayload | null = null;

  if (!access && refresh) {
    const refreshResult = await refreshSession(refresh);
    if (refreshResult.kind === "unavailable") return unavailable();
    if (refreshResult.kind === "rejected") return unauthorized();

    refreshedSession = refreshResult.session;
    access = refreshedSession.accessToken;
  }

  if (!access) return unauthorized();

  const { path } = await params;
  if (!validPath(path)) {
    return NextResponse.json(
      {
        ok: false,
        code: "INVALID_GATEWAY_PATH",
        message: "Đường dẫn API không hợp lệ",
      },
      { status: 400, headers: { "Cache-Control": "no-store" } },
    );
  }

  const encodedPath = path.map((segment) => encodeURIComponent(segment)).join("/");
  const url = new URL(encodedPath, `${getGatewayUrl()}/`);
  req.nextUrl.searchParams.forEach((value, key) => {
    url.searchParams.append(key, value);
  });

  const body = ["GET", "HEAD"].includes(req.method)
    ? undefined
    : await req.arrayBuffer();

  let upstream: Response;
  try {
    upstream = await callGateway(req, url, access, body);
  } catch {
    return unavailable();
  }

  if (upstream.status === 401 && refresh && !refreshedSession) {
    const refreshResult = await refreshSession(refresh);
    if (refreshResult.kind === "unavailable") return unavailable();

    if (refreshResult.kind === "ok") {
      refreshedSession = refreshResult.session;
      try {
        upstream = await callGateway(
          req,
          url,
          refreshedSession.accessToken,
          body,
        );
      } catch {
        return unavailable();
      }
    }
  }

  const headers = new Headers(upstream.headers);
  responseHopByHopHeaders.forEach((name) => headers.delete(name));
  headers.set("Cache-Control", headers.get("Cache-Control") ?? "no-store");

  const response = new NextResponse(upstream.body, {
    status: upstream.status,
    headers,
  });

  if (upstream.status === 401) {
    clearSessionCookies(response);
  } else if (refreshedSession) {
    applySessionCookies(response, refreshedSession);
  }

  return response;
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
