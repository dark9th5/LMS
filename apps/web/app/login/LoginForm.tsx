"use client";

import { useRef, useState } from "react";
import { landingForUser } from "@/lib/authorization";
import type { PortalUser } from "@/lib/types";
import { Icon } from "@/components/Icon";

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
  const submitting = useRef(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (submitting.current) return;
    if (!username.trim() || !password) {
      setError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
      return;
    }
    submitting.current = true;
    setLoading(true);
    setError("");
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 10_000);
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: username.trim(), password }),
        cache: "no-store",
        signal: controller.signal,
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
    } catch (cause) {
      setError(
        cause instanceof DOMException && cause.name === "AbortError"
          ? "Máy chủ phản hồi quá chậm. Hãy kiểm tra trạng thái dịch vụ rồi thử lại."
          : "Không thể kết nối máy chủ. Vui lòng thử lại hoặc liên hệ quản trị viên.",
      );
    } finally {
      window.clearTimeout(timeout);
      submitting.current = false;
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">
      <header className="auth-card-heading">
        <span className="auth-card-icon">
          <Icon name="lock" size={22} />
        </span>
        <div>
          <p className="auth-eyebrow">Chào mừng trở lại</p>
          <h1>Đăng nhập</h1>
          <p>Truy cập khóa học, bài kiểm tra và công việc được giao.</p>
        </div>
      </header>

      {passwordChanged && (
        <div className="form-message success" role="status">
          <Icon name="check" size={18} />
          <span>Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.</span>
        </div>
      )}

      <form className="auth-form" onSubmit={submit} noValidate>
        <label className="field-group" htmlFor="username">
          <span>Tên đăng nhập hoặc email</span>
          <span className="input-shell">
            <Icon name="users" size={19} />
            <input
              id="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
              autoFocus
              spellCheck={false}
              placeholder="vd: nguyenvana"
              aria-invalid={Boolean(error)}
              aria-describedby={error ? "login-error" : undefined}
            />
          </span>
        </label>

        <label className="field-group" htmlFor="password">
          <span>Mật khẩu</span>
          <span className="input-shell password-shell">
            <Icon name="lock" size={19} />
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
              className="password-visibility"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              aria-pressed={showPassword}
            >
              {showPassword ? "Ẩn" : "Hiện"}
            </button>
          </span>
        </label>

        {error && (
          <div id="login-error" className="form-message error" role="alert">
            <Icon name="warning" size={18} />
            <span>{error}</span>
          </div>
        )}

        <button className="button primary auth-submit" disabled={loading}>
          {loading ? (
            <>
              <span className="button-spinner" />
              Đang đăng nhập…
            </>
          ) : (
            <>
              Đăng nhập
              <Icon name="arrow" size={18} />
            </>
          )}
        </button>
      </form>

      <p className="auth-help">
        Không đăng nhập được? Liên hệ quản trị viên của tổ chức để được hỗ trợ.
      </p>

      {demoEnabled && (
        <details className="demo-access">
          <summary>Ba tài khoản trình diễn tách biệt</summary>
          <div className="demo-role-grid">
            {[
              ["admin", "Quản trị viên"],
              ["instructor", "Giảng viên"],
              ["student", "Học viên"],
            ].map(([account, label]) => (
              <button
                type="button"
                key={account}
                onClick={() => {
                  setUsername(account);
                  if (demoPassword) setPassword(demoPassword);
                }}
              >
                <strong>{label}</strong>
                <code>{account}</code>
              </button>
            ))}
          </div>
          <p>
            Mật khẩu: <code>{demoPassword || "Được cấu hình trong .env"}</code>
          </p>
        </details>
      )}
    </div>
  );
}
