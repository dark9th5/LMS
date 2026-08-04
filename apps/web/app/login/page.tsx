import { redirect } from "next/navigation";
import { getPublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { getUser } from "@/lib/session";
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
    <main className="cosmic-login-page unified-login-page">
      <section className="login-showcase" aria-labelledby="login-system-name">
        <div className="login-brand">
          {branding.logoUrl ? (
            <img className="login-logo" src={branding.logoUrl} alt="" />
          ) : (
            <span className="simple-brand-mark" aria-hidden="true">L</span>
          )}
          <div>
            <strong id="login-system-name">{branding.systemName}</strong>
            <small>Hệ thống quản lý học tập</small>
          </div>
        </div>
        <div className="login-welcome">
          <span>Học tập tập trung</span>
          <h2>Tiếp tục công việc và bài học của bạn.</h2>
          <p>{branding.introduction || "Không gian học tập nội bộ của tổ chức."}</p>
        </div>
        <p className="login-privacy">Dữ liệu được quản lý trong hệ thống của tổ chức.</p>
      </section>
      <section className="login-access" aria-label="Biểu mẫu đăng nhập">
        <LoginForm
          demoEnabled={demoEnabled}
          demoPassword={demoPassword}
          passwordChanged={passwordChanged === "1"}
        />
      </section>
    </main>
  );
}
