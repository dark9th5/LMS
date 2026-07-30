"use client";

import { useState } from "react";
import { landingForRoles } from "@/lib/role";
import type { PortalUser } from "@/lib/types";

export function LoginForm({ demoEnabled = false, demoPassword = "" }: { demoEnabled?: boolean; demoPassword?: string }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!username.trim() || !password) { setError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu."); return; }
    setLoading(true); setError("");
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: username.trim(), password }),
        cache: "no-store",
      });
      const data = await response.json().catch(() => ({ message: "Đăng nhập thất bại" })) as { message?: string; user?: PortalUser };
      if (!response.ok || !data.user) { setError(data.message ?? "Đăng nhập thất bại"); return; }
      // Full navigation guarantees the new role cookie is read by the server layout,
      // preventing a previous account's cached portal from being reused.
      window.location.replace(landingForRoles(data.user.roles));
    } catch { setError("Không kết nối được dịch vụ xác thực trong mạng nội bộ."); }
    finally { setLoading(false); }
  }

  return <div className="login-card">
    <span className="login-kicker">CHÀO MỪNG TRỞ LẠI</span>
    <h2>Đăng nhập LMSPilot</h2>
    <p>Sử dụng tài khoản nội bộ được quản trị viên cấp.</p>
    <form onSubmit={submit}>
      <label>Tên đăng nhập<input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required autoFocus placeholder="Nhập tên đăng nhập" /></label>
      <label>Mật khẩu<input value={password} onChange={(event) => setPassword(event.target.value)} type="password" autoComplete="current-password" required placeholder="Nhập mật khẩu" /></label>
      {error && <div className="form-error" role="alert">{error}</div>}
      <button className="login-button" disabled={loading}>{loading ? <><span className="spinner"/>Đang xác thực...</> : "Đăng nhập"}</button>
    </form>
    <div className="secure-note"><span>◆</span>Kết nối được bảo vệ trong mạng nội bộ</div>
    {demoEnabled && <details><summary>Tài khoản trình diễn</summary><code>admin · instructor · student</code><code>Mật khẩu: {demoPassword || "được cấu hình trong .env"}</code></details>}
  </div>;
}
