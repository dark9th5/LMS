import { redirect } from "next/navigation";
import { CosmicField } from "@/components/CosmicField";
import { getPublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { getUser } from "@/lib/session";
import { LoginForm } from "./LoginForm";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function Login() {
  const [user, branding] = await Promise.all([getUser(), getPublicBranding()]);
  if (user) redirect(landingForUser(user));
  const demoEnabled = process.env.LMSPILOT_SEED_DEMO === "true";
  const demoPassword = demoEnabled
    ? (process.env.LMSPILOT_DEFAULT_ADMIN_PASSWORD ?? "")
    : "";

  return (
    <main className="cosmic-login-page">
      <CosmicField />
      <section className="login-showcase">
        <header className="login-brandbar">
          <div className="login-brand">
            <span className="mission-mark" aria-hidden="true">
              <i />
              <b />
              <em />
            </span>
            <span>
              <b>{branding.systemName}</b>
              <small>LEARNING MANAGEMENT PLATFORM · 2026</small>
            </span>
          </div>
          <span className="login-live">
            <i /> SYSTEM ONLINE
          </span>
        </header>

        <div className="login-hero-copy">
          <span className="login-kicker">WELCOME TO YOUR LEARNING SPACE</span>
          <h1>
            <span>HỌC HỎI.</span>
            <span>PHÁT TRIỂN.</span>
            <span>BỨT PHÁ.</span>
          </h1>
          <p>
            {branding.introduction ||
              "Không gian học tập và phát triển dành cho mọi thành viên trong tổ chức."}
          </p>
        </div>

        <div className="learning-sculpture" aria-hidden="true">
          <div className="sculpture-core">
            <span>72%</span>
            <small>PROGRESS</small>
          </div>
          <i className="sculpture-orbit orbit-one" />
          <i className="sculpture-orbit orbit-two" />
          <i className="sculpture-orbit orbit-three" />
          <span className="floating-card card-course">
            <b>12</b>
            <small>MODULES</small>
          </span>
          <span className="floating-card card-streak">
            <b>08</b>
            <small>ACTIVE DAYS</small>
          </span>
          <span className="floating-card card-skill">
            <b>AI</b>
            <small>RESEARCH LAB</small>
          </span>
        </div>

        <div className="login-feature-ticker" aria-hidden="true">
          <div>
            <span>KNOWLEDGE MODULES</span>
            <i>◉</i>
            <span>TRAINING CLASSES</span>
            <i>◉</i>
            <span>AI ASSISTANT</span>
            <i>◉</i>
            <span>LIVE LEARNING</span>
            <i>◉</i>
            <span>KNOWLEDGE MODULES</span>
            <i>◉</i>
          </div>
        </div>

        <footer className="login-proof">
          <div>
            <b>100%</b>
            <span>Dữ liệu nội bộ</span>
          </div>
          <div>
            <b>19</b>
            <span>Dịch vụ nghiệp vụ</span>
          </div>
          <div>
            <b>RBAC</b>
            <span>Quyền theo phạm vi</span>
          </div>
          <p>LEARN · PRACTICE · GROW ↗</p>
        </footer>
      </section>

      <aside className="login-access">
        <div className="login-access-index">
          <span>SECURE ACCESS</span>
          <b>01 / 01</b>
        </div>
        <LoginForm demoEnabled={demoEnabled} demoPassword={demoPassword} />
        <p className="login-access-footer">
          <span>●</span> Kết nối được mã hóa · Không đăng ký công khai
        </p>
      </aside>
    </main>
  );
}
