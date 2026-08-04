"use client";

import { useState } from "react";

export function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (newPassword !== confirm) { setError("Mật khẩu xác nhận không khớp."); return; }
    setBusy(true); setError("");
    const response = await fetch("/api/auth/change-password", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ currentPassword, newPassword }), cache: "no-store" });
    const data = await response.json().catch(() => ({ message: "Không thể đổi mật khẩu" })) as { message?: string };
    if (!response.ok) { setError(data.message ?? "Không thể đổi mật khẩu"); setBusy(false); return; }
    window.location.replace("/login?passwordChanged=1");
  }

  return <form className="password-change-form" onSubmit={submit}>
    <label>Mật khẩu tạm thời<input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
    <label>Mật khẩu mới<input type="password" autoComplete="new-password" minLength={12} maxLength={128} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required /><small>Ít nhất 12 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</small></label>
    <label>Xác nhận mật khẩu<input type="password" autoComplete="new-password" value={confirm} onChange={(event) => setConfirm(event.target.value)} required /></label>
    {error && <div className="form-error" role="alert">{error}</div>}
    <button className="login-button" disabled={busy}>{busy ? "Đang bảo vệ tài khoản..." : "Đổi mật khẩu và đăng nhập lại"}</button>
  </form>;
}
