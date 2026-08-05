import { redirect } from "next/navigation";
import { getPublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { getUser } from "@/lib/session";
import { Icon } from "@/components/Icon";
import { LoginForm } from "./LoginForm";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function Login({
  searchParams,
}: {
  searchParams: Promise<{ passwordChanged?: string }>;
}) {
  const [{ passwordChanged }, user, branding] = await Promise.all([
    searchParams,
    getUser(),
    getPublicBranding(),
  ]);
  if (user) redirect(landingForUser(user));
  const demoEnabled = process.env.LMSPILOT_SEED_DEMO === "true";
  const demoPassword = demoEnabled
    ? (process.env.LMSPILOT_DEFAULT_ADMIN_PASSWORD ?? "")
    : "";

  return (
    <main className="auth-page">
      <section
        className={`auth-showcase ${branding.backgroundUrl ? "has-custom-background" : ""}`}
        aria-labelledby="login-system-name"
        style={branding.backgroundUrl ? { backgroundImage: `linear-gradient(145deg, rgba(18, 25, 60, .84), rgba(62, 49, 151, .72)), url(${branding.backgroundUrl})` } : undefined}
      >
        <div className="auth-brand">
          {branding.logoUrl ? (
            <img className="auth-logo" src={branding.logoUrl} alt="" />
          ) : (
            <span className="auth-brand-mark" aria-hidden="true">L</span>
          )}
          <div>
            <strong id="login-system-name">{branding.systemName}</strong>
            <small>Không gian học tập của tổ chức</small>
          </div>
        </div>

        <div className="auth-showcase-copy">
          <span className="auth-pill"><Icon name="learn" size={16} /> Học tập liền mạch</span>
          <h2>Mỗi ngày tiến thêm một bước.</h2>
          <p>
            {branding.introduction ||
              "Học, kiểm tra và theo dõi tiến độ trong một không gian rõ ràng, tập trung."}
          </p>
        </div>

        <div className="auth-preview" aria-hidden="true">
          <article className="auth-course-card auth-course-primary">
            <span className="auth-course-icon"><Icon name="book" size={22} /></span>
            <div><small>Đang học</small><strong>Kỹ năng số nền tảng</strong></div>
            <span className="auth-progress-value">72%</span>
            <div className="auth-progress-track"><i /></div>
          </article>
          <article className="auth-course-card auth-course-secondary">
            <span className="auth-course-icon"><Icon name="exam" size={22} /></span>
            <div><small>Sắp tới</small><strong>Bài kiểm tra cuối khóa</strong></div>
            <span className="auth-date">09:30</span>
          </article>
          <span className="auth-float auth-float-a"><Icon name="check" size={18} /></span>
          <span className="auth-float auth-float-b"><Icon name="learn" size={18} /></span>
        </div>

        <p className="auth-privacy">
          <Icon name="lock" size={16} /> Dữ liệu được bảo vệ trong hệ thống của tổ chức.
        </p>
      </section>

      <section className="auth-access" aria-label="Biểu mẫu đăng nhập">
        <LoginForm
          demoEnabled={demoEnabled}
          demoPassword={demoPassword}
          passwordChanged={passwordChanged === "1"}
        />
      </section>
    </main>
  );
}
