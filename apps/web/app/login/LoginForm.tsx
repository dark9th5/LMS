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
    <div className="login-card mystic-login-card">
      <div className="login-card-ornament" aria-hidden="true">
        <i />
        <span>✦</span>
        <i />
      </div>
      <header className="login-card-head">
        <div className="login-seal" aria-hidden="true">
          <span>L</span>
          <i />
        </div>
        <div>
          <span className="login-kicker">CỔNG VÀO HỌC VIỆN</span>
          <small>IDENTITY GATEWAY · SECURE</small>
        </div>
      </header>
      <h2>
        Đánh thức hành trình <em>tri thức</em>
      </h2>
      <p>
        Danh tính của bạn là chìa khóa mở đúng không gian, dữ liệu và quyền hạn.
      </p>
      <form onSubmit={submit}>
        <label>
          <span className="field-title">
            <b>Tên đăng nhập</b>
            <small>ACCOUNT ID</small>
          </span>
          <span className="mystic-input">
            <span className="input-sigil" aria-hidden="true">
              ✦
            </span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
              autoFocus
              placeholder="Nhập tên đăng nhập"
            />
            <i aria-hidden="true" />
          </span>
        </label>
        <label>
          <span className="field-title">
            <b>Mật khẩu</b>
            <small>SECURITY KEY</small>
          </span>
          <span className="mystic-input">
            <span className="input-sigil" aria-hidden="true">
              ◆
            </span>
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
            <i aria-hidden="true" />
          </span>
        </label>
        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}
        <button className="login-button portal-button" disabled={loading}>
          {loading ? (
            <>
              <span className="spinner" />
              Đang xác thực danh tính...
            </>
          ) : (
            <>
              <span>
                <small>ENTER THE REALM</small>Tiến vào học viện
              </span>
              <b aria-hidden="true">→</b>
            </>
          )}
        </button>
      </form>
      <div className="login-trust-grid">
        <span>
          <i>✓</i>
          <b>Kết nối nội bộ</b>
          <small>Không chuyển dữ liệu ra ngoài</small>
        </span>
        <span>
          <i>✓</i>
          <b>Phân quyền động</b>
          <small>Chỉ mở đúng phạm vi</small>
        </span>
      </div>
      <div className="secure-note">
        <span>✧</span>
        <div>
          <b>Phiên đăng nhập được bảo vệ</b>
          <small>Dữ liệu nằm trong hạ tầng của tổ chức</small>
        </div>
        <i>ENCRYPTED</i>
      </div>
      {demoEnabled && (
        <details>
          <summary>Tài khoản trình diễn</summary>
          <code>admin · instructor · learner</code>
          <code>Mật khẩu: {demoPassword || "được cấu hình trong .env"}</code>
        </details>
      )}
    </div>
  );
}
