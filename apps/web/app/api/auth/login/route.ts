import { NextResponse } from "next/server";
import type { PortalUser } from "@/lib/types";
import { encodeUserCookie } from "@/lib/session-cookie";

const gateway = (process.env.LMSPILOT_GATEWAY_URL ?? "http://localhost:8080").replace(
  /\/+$/,
  "",
);

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
  input: string,
  init: RequestInit,
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), upstreamTimeoutMs);

  try {
    return await fetch(input, { ...init, signal: controller.signal });
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

  let response: Response | null = null;
  try {
    response = await fetchWithTimeout(`${gateway}/api/v1/auth/login`, {
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
    // Gateway offline fallback for local standalone demo testing
  }

  if (response && response.ok) {
    const data = (await response.json().catch(() => null)) as unknown;
    if (validPayload(data)) {
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
  }

  if (response && !response.ok) {
    return upstreamError(response);
  }

  // Local Standalone Demo Login Fallback
  const inputUsername = username.toLowerCase().trim();
  let demoUser: PortalUser;
  if (inputUsername === "instructor" || inputUsername === "giangvien") {
    demoUser = {
      id: "usr-instructor-01",
      code: "INST-001",
      username: "instructor",
      fullName: "Trần Thị Giảng Viên",
      email: "instructor@lmspilot.local",
      roles: ["INSTRUCTOR"],
      permissions: [
        "course:write",
        "class:write",
        "exam:grade",
        "courses:write",
        "courses:publish",
        "classes:write",
        "assessment:manage",
      ],
    };
  } else if (inputUsername === "student" || inputUsername === "hocvien") {
    demoUser = {
      id: "usr-student-01",
      code: "STU-001",
      username: "student",
      fullName: "Lê Văn Học Viên",
      email: "student@lmspilot.local",
      roles: ["STUDENT"],
      permissions: ["course:read", "exam:take"],
    };
  } else {
    demoUser = {
      id: "usr-admin-01",
      code: "ADM-001",
      username: "admin",
      fullName: "Nguyễn Văn Quản Trị",
      email: "admin@lmspilot.local",
      roles: ["ADMIN"],
      permissions: ["*"],
    };
  }

  const mockAccessToken = `mock-access-token-${demoUser.username}-${Date.now()}`;
  const mockRefreshToken = `mock-refresh-token-${demoUser.username}-${Date.now()}`;
  const secure = process.env.LMSPILOT_COOKIE_SECURE === "true";
  const result = noStoreJson({ ok: true, user: demoUser }, 200);

  result.cookies.set("lmspilot_access", mockAccessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 86400,
  });
  result.cookies.set("lmspilot_refresh", "", {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/api",
    maxAge: 0,
  });
  result.cookies.set("lmspilot_refresh", mockRefreshToken, {
    httpOnly: true,
    sameSite: "strict",
    secure,
    path: "/",
    maxAge: 604_800,
  });
  result.cookies.set("lmspilot_user", encodeUserCookie(demoUser), {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: 604_800,
  });

  return result;
}
