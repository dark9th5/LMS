import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { isSameOriginMutation } from "@/lib/request-origin";

const gateway = process.env.LMSPILOT_GATEWAY_URL ?? "http://localhost:8080";

export async function POST(request: NextRequest) {
  if (!isSameOriginMutation(request)) {
    return NextResponse.json(
      { code: "CROSS_SITE_REQUEST", message: "Yêu cầu khác nguồn đã bị từ chối" },
      { status: 403, headers: { "Cache-Control": "no-store" } },
    );
  }
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
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
  const proto = request.headers.get("x-forwarded-proto") ?? "http";
  const baseUrl = host ? `${proto}://${host}` : (process.env.LMSPILOT_PUBLIC_URL ?? request.nextUrl.origin);
  const response = NextResponse.redirect(new URL("/login", baseUrl), 303);
  response.cookies.set("lmspilot_access", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_user", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/api", maxAge: 0 });
  return response;
}
