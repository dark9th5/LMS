import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { isSameOriginMutation } from "@/lib/request-origin";
import { fetchGateway } from "@/lib/upstream-fetch";

export async function POST(request: Request) {
  if (!isSameOriginMutation(request)) {
    return NextResponse.json(
      { code: "CROSS_SITE_REQUEST", message: "Yêu cầu khác nguồn đã bị từ chối" },
      { status: 403, headers: { "Cache-Control": "no-store" } },
    );
  }
  const jar = await cookies();
  const access = jar.get("lmspilot_access")?.value;
  if (!access) return NextResponse.json({ message: "Phiên đăng nhập đã hết hạn" }, { status: 401 });
  const body = await request.text();
  const upstream = await fetchGateway("/api/v1/auth/change-password", { method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${access}` }, body, cache: "no-store" }).catch(() => null);
  if (!upstream) return NextResponse.json({ message: "Dịch vụ xác thực không khả dụng" }, { status: 503 });
  if (!upstream.ok) {
    const error = await upstream.json().catch(() => ({ message: "Không thể đổi mật khẩu" }));
    return NextResponse.json(error, { status: upstream.status });
  }
  const response = NextResponse.json({ ok: true });
  for (const name of ["lmspilot_access", "lmspilot_refresh", "lmspilot_user"]) response.cookies.set(name, "", { path: "/", maxAge: 0 });
  response.cookies.set("lmspilot_refresh", "", { path: "/api", maxAge: 0 });
  return response;
}
