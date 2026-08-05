"use client";

import { useState } from "react";
import { Icon } from "@/components/Icon";

export function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (newPassword !== confirm) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/auth/change-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPassword, newPassword }),
        cache: "no-store",
      });
      const data = (await response
        .json()
        .catch(() => ({ message: "Không thể đổi mật khẩu" }))) as {
        message?: string;
      };
      if (!response.ok) {
        setError(data.message ?? "Không thể đổi mật khẩu");
        return;
      }
      window.location.replace("/login?passwordChanged=1");
    } catch {
      setError("Không thể kết nối máy chủ. Vui lòng thử lại.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="auth-form password-change-form" onSubmit={submit}>
      <label className="field-group">
        <span>Mật khẩu tạm thời</span>
        <span className="input-shell">
          <Icon name="lock" size={19} />
          <input
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            required
          />
        </span>
      </label>
      <label className="field-group">
        <span>Mật khẩu mới</span>
        <span className="input-shell">
          <Icon name="lock" size={19} />
          <input
            type="password"
            autoComplete="new-password"
            minLength={12}
            maxLength={128}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            required
          />
        </span>
        <small>Ít nhất 12 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</small>
      </label>
      <label className="field-group">
        <span>Xác nhận mật khẩu</span>
        <span className="input-shell">
          <Icon name="check" size={19} />
          <input
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(event) => setConfirm(event.target.value)}
            required
          />
        </span>
      </label>
      {error && (
        <div className="form-message error" role="alert">
          <Icon name="warning" size={18} />
          <span>{error}</span>
        </div>
      )}
      <button className="button primary auth-submit" disabled={busy}>
        {busy ? "Đang cập nhật…" : "Đổi mật khẩu và đăng nhập lại"}
        {!busy && <Icon name="arrow" size={18} />}
      </button>
    </form>
  );
}
