import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { encodeUserCookie } from "@/lib/session-cookie";
import type { PortalUser } from "@/lib/types";
import { isSameOriginMutation } from "@/lib/request-origin";
import { fetchGateway, gatewayBaseUrl } from "@/lib/upstream-fetch";

type RouteContext = { params: Promise<{ path: string[] }> };
type TokenPayload = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: PortalUser;
};

type RefreshResult =
  | { kind: "ok"; session: TokenPayload }
  | { kind: "rejected"; code?: string }
  | { kind: "concurrent" }
  | { kind: "unavailable" };

const refreshInFlight = new Map<string, Promise<RefreshResult>>();
const terminalRefreshCodes = new Set([
  "EXPIRED_REFRESH_TOKEN",
  "INVALID_REFRESH_TOKEN",
  "REFRESH_TOKEN_REUSED",
  "ACCOUNT_NOT_ACTIVE",
]);

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
  "www-authenticate",
];

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
    user.roles.length >= 1 &&
    user.roles.every((role) =>
      ["ADMIN", "INSTRUCTOR", "STUDENT"].includes(String(role)),
    ) &&
    typeof user.primaryRole === "string" &&
    ["ADMIN", "INSTRUCTOR", "STUDENT"].includes(user.primaryRole) &&
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
  return fetchGateway(input, init);
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

    if (!response.ok) {
      const problem = (await response.json().catch(() => null)) as {
        code?: unknown;
      } | null;
      const code = typeof problem?.code === "string" ? problem.code : undefined;
      if (response.status === 409 && code === "REFRESH_TOKEN_ROTATED")
        return { kind: "concurrent" };
      // Only clear the browser session when Identity explicitly says the refresh
      // credential is terminal. A gateway/service 5xx or generic 401 is transient.
      if (
        (response.status === 401 || response.status === 403) &&
        code &&
        terminalRefreshCodes.has(code)
      ) {
        return { kind: "rejected", code };
      }
      return { kind: "unavailable" };
    }

    const data = (await response.json().catch(() => null)) as unknown;
    return validTokenPayload(data)
      ? { kind: "ok", session: data }
      : { kind: "unavailable" };
  })();

  refreshInFlight.set(refreshToken, pending);
  void pending.finally(() => {
    setTimeout(() => refreshInFlight.delete(refreshToken), 5_000);
  });
  return pending;
}

function accessTokenExpiresSoon(token: string, skewSeconds = 45): boolean {
  try {
    const payload = token.split(".")[1];
    if (!payload) return true;
    const decoded = JSON.parse(
      Buffer.from(payload, "base64url").toString("utf8"),
    ) as { exp?: unknown };
    return (
      typeof decoded.exp !== "number" ||
      decoded.exp * 1000 <= Date.now() + skewSeconds * 1000
    );
  } catch {
    return true;
  }
}

function refreshInProgress(): NextResponse {
  return NextResponse.json(
    {
      ok: false,
      code: "SESSION_REFRESH_IN_PROGRESS",
      message: "Phiên đăng nhập đang được làm mới. Vui lòng thử lại yêu cầu.",
    },
    {
      status: 409,
      headers: { "Cache-Control": "no-store", "Retry-After": "1" },
    },
  );
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
    { ok: false, code: "SESSION_EXPIRED", message },
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
  body?: BodyInit | null,
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
  if (!isSameOriginMutation(req)) {
    return NextResponse.json(
      {
        ok: false,
        code: "CROSS_SITE_REQUEST",
        message: "Yêu cầu khác nguồn đã bị từ chối",
      },
      { status: 403, headers: { "Cache-Control": "no-store" } },
    );
  }

  const jar = await cookies();
  let access = jar.get("lmspilot_access")?.value;
  const refresh = jar.get("lmspilot_refresh")?.value;
  let refreshedSession: TokenPayload | null = null;

  if (!access && refresh) {
    const refreshResult = await refreshSession(refresh);
    if (refreshResult.kind === "unavailable") return unavailable();
    if (refreshResult.kind === "concurrent") return refreshInProgress();
    if (refreshResult.kind === "rejected") return unauthorized();

    refreshedSession = refreshResult.session;
    access = refreshedSession.accessToken;
  }

  if (
    access &&
    refresh &&
    !refreshedSession &&
    accessTokenExpiresSoon(access)
  ) {
    const refreshResult = await refreshSession(refresh);
    if (refreshResult.kind === "ok") {
      refreshedSession = refreshResult.session;
      access = refreshedSession.accessToken;
    } else if (refreshResult.kind === "concurrent") {
      return refreshInProgress();
    } else if (refreshResult.kind === "rejected") {
      return unauthorized();
    }
    // If refresh service is temporarily unavailable, keep using a still-present access token.
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

  const encodedPath = path
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  const url = new URL(encodedPath, `${gatewayBaseUrl()}/`);
  req.nextUrl.searchParams.forEach((value, key) => {
    url.searchParams.append(key, value);
  });

  const hasBody = !["GET", "HEAD"].includes(req.method);
  const contentType = req.headers.get("content-type") ?? "";
  const contentLength = Number(req.headers.get("content-length") ?? "0");
  const streamBody =
    hasBody &&
    (contentType.includes("multipart/form-data") || contentLength > 1_048_576);
  const body: BodyInit | null | undefined = !hasBody
    ? undefined
    : streamBody
      ? req.body
      : await req.arrayBuffer();

  let upstream: Response;
  try {
    upstream = await callGateway(req, url, access, body);
  } catch {
    return unavailable();
  }

  if (upstream.status === 401 && refresh && !refreshedSession && !streamBody) {
    const refreshResult = await refreshSession(refresh);
    if (refreshResult.kind === "unavailable") return unavailable();
    if (refreshResult.kind === "concurrent") return refreshInProgress();

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
  headers.delete("www-authenticate");
  headers.delete("WWW-Authenticate");
  headers.set("Cache-Control", headers.get("Cache-Control") ?? "no-store");

  const response = new NextResponse(upstream.body, {
    status: upstream.status,
    headers,
  });

  if (refreshedSession) {
    applySessionCookies(response, refreshedSession);
  } else if (upstream.status === 401 && !refresh) {
    clearSessionCookies(response);
  }

  return response;
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
