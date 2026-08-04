"use client";

import { useState } from "react";
import { landingForUser } from "@/lib/authorization";
import type { PortalUser } from "@/lib/types";

export function LoginForm({
  demoEnabled = false,
  demoPassword = "",
}: {
  demoEnabled?: boolean;
  demoPassword?: string;
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
        setError(data.message ?? "Đăng nhập thất bại");
        return;
      }
      window.location.replace(
        data.user.mustChangePassword
          ? "/change-password"
          : landingForUser(data.user),
      );
    } catch {
      setError("Không kết nối được dịch vụ xác thực trong mạng nội bộ.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="cosmic-login-card">
      <div className="login-card-colorbar" aria-hidden="true">
        <i />
        <i />
        <i />
        <i />
      </div>
      <header>
        <span className="access-chip">MEMBER ACCESS</span>
        <span className="access-lock" aria-hidden="true">
          ●
        </span>
      </header>
      <h2>
        Chào mừng
        <br />
        trở lại.
      </h2>
      <p>Đăng nhập để tiếp tục đúng công việc, khóa học và dữ liệu của bạn.</p>
      <form onSubmit={submit}>
        <label>
          <span>
            <b>01</b> Tên đăng nhập
          </span>
          <span className="cosmic-input">
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
              autoFocus
              placeholder="VD: nguyenvana"
            />
            <i>@</i>
          </span>
        </label>
        <label>
          <span>
            <b>02</b> Mật khẩu
          </span>
          <span className="cosmic-input">
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type={showPassword ? "text" : "password"}
              autoComplete="current-password"
              required
              placeholder="Nhập mật khẩu"
            />
            <button
              type="button"
              className="reveal-password"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            >
              {showPassword ? "Ẩn" : "Hiện"}
            </button>
          </span>
        </label>
        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}
        <button className="cosmic-login-button" disabled={loading}>
          <span>{loading ? "Đang xác thực..." : "Vào không gian học tập"}</span>
          <b aria-hidden="true">↗</b>
        </button>
      </form>
      <div className="login-security-row">
        <span>
          <i>✓</i> Phiên được ký
        </span>
        <span>
          <i>✓</i> Quyền động
        </span>
        <span>
          <i>✓</i> Fail-closed
        </span>
      </div>
      {demoEnabled && (
        <details className="demo-access">
          <summary>Tài khoản trình diễn</summary>
          <code>admin · instructor · learner</code>
          <code>Mật khẩu: {demoPassword || "được cấu hình trong .env"}</code>
        </details>
      )}
    </div>
  );
}
