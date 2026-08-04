import { redirect } from "next/navigation";
import { CosmicField } from "@/components/CosmicField";
import { getUser } from "@/lib/session";
import { ChangePasswordForm } from "./ChangePasswordForm";

export const dynamic = "force-dynamic";

export default async function ChangePasswordPage() {
  const user = await getUser();
  if (!user) redirect("/login");
  return (
    <main className="password-gate">
      <CosmicField />
      <section className="password-gate-layout">
        <aside className="password-gate-visual">
          <span className="password-coordinate">
            IDENTITY UPDATE · REQUIRED
          </span>
          <div className="password-shield" aria-hidden="true">
            <i />
            <i />
            <i />
            <span>✓</span>
          </div>
          <h2>
            Mật khẩu mới.
            <br />
            Khởi đầu an toàn hơn.
          </h2>
          <p>
            Hành động này bảo vệ tiến độ, điểm số và toàn bộ phạm vi quyền của
            bạn.
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
          <div className="login-card-colorbar" aria-hidden="true">
            <i />
            <i />
            <i />
            <i />
          </div>
          <span className="access-chip">SECURITY CHECKPOINT</span>
          <h1>
            Đổi mật khẩu
            <br />
            <em>tạm thời.</em>
          </h1>
          <p>
            Xin chào <strong>{user.fullName}</strong>. Chỉ còn một bước để tiếp
            tục.
          </p>
          <ChangePasswordForm />
        </div>
      </section>
    </main>
  );
}
