import { redirect } from "next/navigation";
import { getUser } from "@/lib/session";
import { Icon } from "@/components/Icon";
import { ChangePasswordForm } from "./ChangePasswordForm";

export const dynamic = "force-dynamic";

export default async function ChangePasswordPage() {
  const user = await getUser();
  if (!user) redirect("/login");
  return (
    <main className="auth-page password-page">
      <section className="password-intro">
        <span className="auth-pill"><Icon name="lock" size={16} /> Bảo mật tài khoản</span>
        <h1>Tạo mật khẩu riêng của bạn</h1>
        <p>
          Đây là bước bắt buộc khi đăng nhập lần đầu. Mật khẩu mới giúp bảo vệ
          dữ liệu học tập và công việc của bạn.
        </p>
        <div className="password-guide">
          <span><Icon name="check" size={18} /> Tối thiểu 12 ký tự</span>
          <span><Icon name="check" size={18} /> Có chữ hoa, chữ thường và số</span>
          <span><Icon name="check" size={18} /> Có ít nhất một ký tự đặc biệt</span>
        </div>
      </section>
      <section className="auth-access">
        <div className="auth-card password-card">
          <header className="auth-card-heading">
            <span className="auth-card-icon"><Icon name="users" size={22} /></span>
            <div>
              <p className="auth-eyebrow">Xin chào, {user.fullName}</p>
              <h2>Đổi mật khẩu lần đầu</h2>
              <p>Nhập mật khẩu tạm và thiết lập mật khẩu mới.</p>
            </div>
          </header>
          <ChangePasswordForm />
        </div>
      </section>
    </main>
  );
}
