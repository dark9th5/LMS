import { redirect } from "next/navigation";
import { ChangePasswordForm } from "./ChangePasswordForm";
import { getUser } from "@/lib/session";
import { MysticBackdrop } from "@/components/MysticBackdrop";

export const dynamic = "force-dynamic";

export default async function ChangePasswordPage() {
  const user = await getUser();
  if (!user) redirect("/login");
  return (
    <main className="password-gate">
      <MysticBackdrop />
      <section className="password-gate-layout">
        <aside className="password-gate-visual">
          <span className="password-coordinate">IDENTITY RITUAL · 01</span>
          <div className="password-shield" aria-hidden="true">
            <i />
            <i />
            <i />
            <span>✦</span>
          </div>
          <h2>Danh tính mạnh mở ra một học viện an toàn.</h2>
          <p>
            Mật khẩu riêng bảo vệ tiến độ, bài thi, điểm số và toàn bộ quyền hạn
            được trao cho bạn.
          </p>
          <div className="password-rules">
            <span>
              <i>01</i>Tối thiểu 12 ký tự
            </span>
            <span>
              <i>02</i>Đủ bốn nhóm ký tự
            </span>
            <span>
              <i>03</i>Không dùng lại mật khẩu tạm
            </span>
          </div>
        </aside>
        <div className="password-gate-card">
          <div className="login-card-ornament" aria-hidden="true">
            <i />
            <span>✦</span>
            <i />
          </div>
          <span className="login-kicker">BẢO VỆ TÀI KHOẢN</span>
          <h1>
            Đổi mật khẩu <em>tạm thời</em>
          </h1>
          <p>
            Xin chào <strong>{user.fullName}</strong>. Hãy đặt mật khẩu riêng
            trước khi bước vào học viện.
          </p>
          <ChangePasswordForm />
        </div>
      </section>
    </main>
  );
}
