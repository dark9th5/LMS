"use client";

import { useState } from "react";
import { landingForUser } from "@/lib/authorization";
import type { PortalUser } from "@/lib/types";

export function LoginForm({
  demoEnabled = false,
  demoPassword = "",
  passwordChanged = false,
}: {
  demoEnabled?: boolean;
  demoPassword?: string;
  passwordChanged?: boolean;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: username.trim(), password }),
        cache: "no-store",
      });
      const data = (await response
        .json()
        .catch(() => ({ message: "Đăng nhập thất bại" }))) as {
        message?: string;
        user?: PortalUser;
      };
      if (!response.ok || !data.user) {
        setError(data.message ?? "Tên đăng nhập hoặc mật khẩu chưa đúng.");
        return;
      }
      window.location.replace(
        data.user.mustChangePassword
          ? "/change-password"
          : landingForUser(data.user),
      );
    } catch {
      setError("Không thể kết nối máy chủ. Vui lòng thử lại hoặc liên hệ quản trị viên.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="cosmic-login-card unified-login-card">
      <header className="login-form-heading">
        <h1>Đăng nhập</h1>
        <p>Nhập tài khoản được tổ chức cấp để tiếp tục.</p>
      </header>
      {passwordChanged && (
        <div className="form-success" role="status">
          Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.
        </div>
      )}
      <form onSubmit={submit} noValidate>
        <label className="field-label" htmlFor="username">Tên đăng nhập</label>
        <div className="cosmic-input">
          <input
            id="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
            autoFocus
            spellCheck={false}
            placeholder="Tên đăng nhập hoặc email"
            aria-invalid={Boolean(error)}
            aria-describedby={error ? "login-error" : undefined}
          />
        </div>

        <label className="field-label" htmlFor="password">Mật khẩu</label>
        <div className="cosmic-input password-input">
          <input
            id="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            required
            placeholder="Nhập mật khẩu"
            aria-invalid={Boolean(error)}
            aria-describedby={error ? "login-error" : undefined}
          />
          <button
            type="button"
            className="reveal-password"
            onClick={() => setShowPassword((value) => !value)}
            aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            aria-pressed={showPassword}
          >
            {showPassword ? "Ẩn" : "Hiện"}
          </button>
        </div>

        {error && <div id="login-error" className="form-error" role="alert">{error}</div>}
        <button className="cosmic-login-button" disabled={loading}>
          {loading ? "Đang đăng nhập…" : "Đăng nhập"}
        </button>
      </form>
      <p className="login-help">Không đăng nhập được? Liên hệ quản trị viên của tổ chức.</p>
      {demoEnabled && (
        <details className="demo-access">
          <summary>Tài khoản trình diễn</summary>
          <code>admin</code>
          <code>Mật khẩu: {demoPassword || "được cấu hình trong .env"}</code>
        </details>
      )}
    </div>
  );
}
