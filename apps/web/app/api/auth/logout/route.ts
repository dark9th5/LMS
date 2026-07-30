import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const gateway = process.env.LMSPILOT_GATEWAY_URL ?? "http://localhost:8080";

export async function POST(request: NextRequest) {
  const jar = await cookies();
  const refresh = jar.get("lmspilot_refresh")?.value;
  if (refresh) {
    await fetch(`${gateway}/api/v1/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: refresh }),
      cache: "no-store",
    }).catch(() => null);
  }

  // Preserve the LAN hostname/IP used by the browser without trusting a client-supplied redirect origin.
  const response = NextResponse.redirect(new URL("/login", request.nextUrl.origin), 303);
  response.cookies.set("lmspilot_access", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_user", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/api", maxAge: 0 });
  return response;
}
