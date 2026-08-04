import { NextResponse } from "next/server";
import type { PortalUser } from "@/lib/types";
import { encodeUserCookie } from "@/lib/session-cookie";
import { isSameOriginMutation } from "@/lib/request-origin";

function getGatewayUrl(): string {
  const envUrl = process.env.LMSPILOT_GATEWAY_URL;
  if (envUrl && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/+$/, "");
  }
  return "http://localhost:8080";
}

const upstreamTimeoutMs = positiveInteger(
  process.env.LMSPILOT_UPSTREAM_TIMEOUT_MS,
  10_000,
);

type LoginPayload = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: PortalUser;
};

function positiveInteger(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
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

function validPayload(value: unknown): value is LoginPayload {
  if (!value || typeof value !== "object") return false;
  const payload = value as Partial<LoginPayload>;
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

async function fetchWithTimeout(
  path: string,
  init: RequestInit,
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), upstreamTimeoutMs);
  const primaryGateway = getGatewayUrl();

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
        const res = await fetch(url, { ...init, signal: controller.signal });
        if (res.ok || res.status === 400 || res.status === 401) {
          return res;
        }
        lastError = new Error(`HTTP ${res.status} from ${url}`);
      } catch (err) {
        lastError = err;
      }
    }
    throw lastError;
  } finally {
    clearTimeout(timer);
  }
}

function noStoreJson(
  body: Record<string, unknown>,
  status: number,
): NextResponse {
  return NextResponse.json(body, {
    status,
    headers: { "Cache-Control": "no-store" },
  });
}

async function upstreamError(response: Response): Promise<NextResponse> {
  const contentType = response.headers.get("content-type") ?? "";
  let message = "Tên đăng nhập hoặc mật khẩu không đúng";
  let code = "LOGIN_REJECTED";

  if (contentType.includes("application/json")) {
    const data = (await response.json().catch(() => null)) as
      | { message?: unknown; code?: unknown }
      | null;
    if (typeof data?.message === "string" && data.message.trim()) {
      message = data.message;
    }
    if (typeof data?.code === "string" && data.code.trim()) {
      code = data.code;
    }
  }

  const status =
    response.status >= 400 && response.status <= 599 ? response.status : 502;
  return noStoreJson({ ok: false, code, message }, status);
}

export async function POST(request: Request) {
  if (!isSameOriginMutation(request)) {
    return noStoreJson(
      {
        ok: false,
        code: "CROSS_SITE_REQUEST",
        message: "Yêu cầu khác nguồn đã bị từ chối",
      },
      403,
    );
  }

  let credentials: { username?: unknown; password?: unknown };

  try {
    credentials = (await request.json()) as {
      username?: unknown;
      password?: unknown;
    };
  } catch {
    return noStoreJson(
      {
        ok: false,
        code: "INVALID_JSON",
        message: "Dữ liệu đăng nhập không hợp lệ",
      },
      400,
    );
  }

  const username =
    typeof credentials.username === "string"
      ? credentials.username.trim()
      : "";
  const password =
    typeof credentials.password === "string" ? credentials.password : "";

  if (
    username.length < 1 ||
    username.length > 128 ||
    password.length < 1 ||
    password.length > 1024
  ) {
    return noStoreJson(
      {
        ok: false,
        code: "INVALID_CREDENTIALS",
        message: "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu",
      },
      400,
    );
  }

  let response: Response;
  try {
    response = await fetchWithTimeout("/api/v1/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Correlation-ID":
          request.headers.get("X-Correlation-ID") ?? crypto.randomUUID(),
      },
      body: JSON.stringify({ username, password }),
      cache: "no-store",
    });
  } catch {
    return noStoreJson(
      {
        ok: false,
        code: "AUTH_SERVICE_UNAVAILABLE",
        message:
          "Dịch vụ đăng nhập đang không khả dụng. Hệ thống không chuyển sang tài khoản giả.",
      },
      503,
    );
  }

  if (!response.ok) {
    return upstreamError(response);
  }

  const data = (await response.json().catch(() => null)) as unknown;
  if (!validPayload(data)) {
    return noStoreJson(
      {
        ok: false,
        code: "INVALID_AUTH_RESPONSE",
        message: "Dịch vụ đăng nhập trả về dữ liệu không hợp lệ",
      },
      502,
    );
  }

  const secure = process.env.LMSPILOT_COOKIE_SECURE === "true";
  const accessMaxAge = Math.max(1, Math.floor(data.expiresInSeconds));
  const result = noStoreJson({ ok: true, user: data.user }, 200);

  result.cookies.set("lmspilot_access", data.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: accessMaxAge,
  });
  result.cookies.set("lmspilot_refresh", "", {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/api",
    maxAge: 0,
  });
  result.cookies.set("lmspilot_refresh", data.refreshToken, {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/",
    maxAge: 604_800,
  });
  result.cookies.set("lmspilot_user", encodeUserCookie(data.user), {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 604_800,
  });

  return result;
}
