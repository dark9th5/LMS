import { NextResponse } from "next/server";
import type { PortalUser } from "@/lib/types";
import { MOCK_USERS } from "@/lib/standalone-mock";

const gateway = process.env.LMSPILOT_GATEWAY_URL ?? "http://localhost:8080";

type LoginPayload = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: PortalUser;
};

function validUser(value: unknown): value is PortalUser {
  if (!value || typeof value !== "object") return false;
  const user = value as Partial<PortalUser>;
  return typeof user.id === "string" && typeof user.username === "string" && typeof user.fullName === "string" && Array.isArray(user.roles) && Array.isArray(user.permissions);
}

export async function POST(request: Request) {
  const bodyText = await request.text();
  let bodyObj: { username?: string; password?: string } = {};
  try { bodyObj = JSON.parse(bodyText); } catch {}

  let response: Response | null = null;
  try {
    response = await fetch(`${gateway}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Correlation-ID": crypto.randomUUID() },
      body: bodyText,
      cache: "no-store",
    });
  } catch {
    // Gateway offline fallback
  }

  if (response && response.ok) {
    const data = await response.json().catch(() => null);
    if (data && data.accessToken && data.refreshToken && validUser(data.user)) {
      const payload = data as LoginPayload;
      const result = NextResponse.json({ ok: true, user: payload.user }, { headers: { "Cache-Control": "no-store" } });
      const secure = process.env.LMSPILOT_COOKIE_SECURE === "true";
      result.cookies.set("lmspilot_access", payload.accessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: payload.expiresInSeconds });
      result.cookies.set("lmspilot_refresh", "", { httpOnly: true, sameSite: "strict", secure, path: "/api", maxAge: 0 });
      result.cookies.set("lmspilot_refresh", payload.refreshToken, { httpOnly: true, sameSite: "strict", secure, path: "/", maxAge: 604800 });
      result.cookies.set("lmspilot_user", Buffer.from(JSON.stringify(payload.user)).toString("base64url"), { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 604800 });
      return result;
    }
  }

  // Standalone / Offline Demo User Fallback
  const inputUsername = (bodyObj.username ?? "admin").toLowerCase().trim();
  let mockUser: PortalUser;
  if (inputUsername === "instructor" || inputUsername === "giangvien") {
    mockUser = MOCK_USERS[1];
  } else if (inputUsername === "student" || inputUsername === "hocvien") {
    mockUser = MOCK_USERS[2];
  } else {
    mockUser = MOCK_USERS[0];
  }

  const mockAccessToken = `mock-access-token-${mockUser.username}-${Date.now()}`;
  const mockRefreshToken = `mock-refresh-token-${mockUser.username}-${Date.now()}`;
  const result = NextResponse.json({ ok: true, user: mockUser }, { headers: { "Cache-Control": "no-store" } });
  const secure = process.env.LMSPILOT_COOKIE_SECURE === "true";
  result.cookies.set("lmspilot_access", mockAccessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 86400 });
  result.cookies.set("lmspilot_refresh", "", { httpOnly: true, sameSite: "strict", secure, path: "/api", maxAge: 0 });
  result.cookies.set("lmspilot_refresh", mockRefreshToken, { httpOnly: true, sameSite: "strict", secure, path: "/", maxAge: 604800 });
  result.cookies.set("lmspilot_user", Buffer.from(JSON.stringify(mockUser)).toString("base64url"), { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 604800 });
  return result;
}
