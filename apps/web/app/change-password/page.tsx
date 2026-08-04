import { redirect } from "next/navigation";
import { getUser } from "@/lib/session";
import { ChangePasswordForm } from "./ChangePasswordForm";

export const dynamic = "force-dynamic";

export default async function ChangePasswordPage() {
  const user = await getUser();
  if (!user) redirect("/login");
  return (
    <main className="password-gate unified-password-page">
      <section className="password-gate-card unified-password-card">
        <header>
          <span className="simple-brand-mark" aria-hidden="true">L</span>
          <div>
            <small>Bảo mật tài khoản</small>
            <h1>Đổi mật khẩu lần đầu</h1>
          </div>
        </header>
        <p>
          Xin chào <strong>{user.fullName}</strong>. Hãy thay mật khẩu tạm trước
          khi tiếp tục sử dụng hệ thống.
        </p>
        <ul className="password-requirements" aria-label="Yêu cầu mật khẩu">
          <li>Tối thiểu 12 ký tự</li>
          <li>Có chữ hoa, chữ thường, số và ký tự đặc biệt</li>
          <li>Không dùng lại mật khẩu tạm</li>
        </ul>
        <ChangePasswordForm />
      </section>
    </main>
  );
}
