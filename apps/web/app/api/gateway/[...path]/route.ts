import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { handleMockGatewayRequest } from "@/lib/standalone-mock";

const gateway = process.env.LMSPILOT_GATEWAY_URL ?? "http://localhost:8080";
type RouteContext = { params: Promise<{ path: string[] }> };
type TokenPayload = { accessToken: string; refreshToken: string; expiresInSeconds: number; user: unknown };

const refreshInFlight = new Map<string, Promise<TokenPayload | null>>();

function cookieSecure(): boolean {
  return process.env.LMSPILOT_COOKIE_SECURE === "true";
}

function applySessionCookies(response: NextResponse, session: TokenPayload) {
  const secure = cookieSecure();
  response.cookies.set("lmspilot_access", session.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: session.expiresInSeconds,
  });
  response.cookies.set("lmspilot_refresh", session.refreshToken, {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/",
    maxAge: 604800,
  });
  response.cookies.set("lmspilot_user", Buffer.from(JSON.stringify(session.user)).toString("base64url"), {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 604800,
  });
}

function clearSessionCookies(response: NextResponse) {
  response.cookies.set("lmspilot_access", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_user", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/api", maxAge: 0 });
}

function refreshSession(refreshToken: string): Promise<TokenPayload | null> {
  const existing = refreshInFlight.get(refreshToken);
  if (existing) return existing;

  const pending = (async () => {
    try {
      const response = await fetch(`${gateway}/api/v1/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Correlation-ID": crypto.randomUUID() },
        body: JSON.stringify({ refreshToken }),
        cache: "no-store",
      });
      if (!response.ok) return null;
      return (await response.json()) as TokenPayload;
    } catch {
      return null;
    }
  })();

  refreshInFlight.set(refreshToken, pending);
  void pending.finally(() => setTimeout(() => refreshInFlight.delete(refreshToken), 5000));
  return pending;
}

async function callGateway(req: NextRequest, url: URL, token: string, body?: ArrayBuffer) {
  const headers = new Headers(req.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.set("X-Correlation-ID", req.headers.get("X-Correlation-ID") ?? crypto.randomUUID());
  headers.delete("host");
  headers.delete("cookie");
  headers.delete("content-length");
  const init: RequestInit & { duplex?: "half" } = {
    method: req.method,
    headers,
    body: ["GET", "HEAD"].includes(req.method) ? undefined : body,
    cache: "no-store",
  };
  if (init.body) init.duplex = "half";
  return fetch(url, init);
}

async function proxy(req: NextRequest, { params }: RouteContext) {
  const jar = await cookies();
  let access = jar.get("lmspilot_access")?.value;
  const refresh = jar.get("lmspilot_refresh")?.value;
  let refreshed: TokenPayload | null = null;

  if (!access && refresh) {
    refreshed = await refreshSession(refresh);
    access = refreshed?.accessToken;
  }

  // Allow mock access token or fallback if session cookie exists
  if (!access && !refresh) {
    const response = NextResponse.json({ message: "Phiên đăng nhập đã hết hạn" }, { status: 401 });
    clearSessionCookies(response);
    return response;
  }

  const { path } = await params;
  const url = new URL(`${gateway}/${path.join("/")}`);
  req.nextUrl.searchParams.forEach((value, key) => url.searchParams.append(key, value));
  const body = ["GET", "HEAD"].includes(req.method) ? undefined : await req.arrayBuffer();
  const bodyText = body ? new TextDecoder().decode(body) : undefined;

  let upstream: Response | null = null;
  try {
    upstream = await callGateway(req, url, access ?? "mock-token", body);
  } catch {
    // Gateway is unreachable -> Return Standalone Mock Response
    return handleMockGatewayRequest(path, req.method, bodyText);
  }

  if (upstream.status === 401 && refresh) {
    refreshed = await refreshSession(refresh);
    if (refreshed) {
      try {
        upstream = await callGateway(req, url, refreshed.accessToken, body);
      } catch {
        return handleMockGatewayRequest(path, req.method, bodyText);
      }
    }
  }

  if (!upstream) {
    return handleMockGatewayRequest(path, req.method, bodyText);
  }

  const headers = new Headers(upstream.headers);
  headers.delete("content-length");
  headers.delete("content-encoding");
  const response = new NextResponse(upstream.body, { status: upstream.status, headers });

  if (upstream.status === 401) clearSessionCookies(response);
  else if (refreshed) applySessionCookies(response, refreshed);

  return response;
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
