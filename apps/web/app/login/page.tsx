import { redirect } from "next/navigation";
import { getUser } from "@/lib/session";
import { landingForUser } from "@/lib/authorization";
import { MysticBackdrop } from "@/components/MysticBackdrop";
import { LoginForm } from "./LoginForm";
import { getPublicBranding } from "@/lib/branding";

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
    <div className="login-page mystic-login-page">
      <MysticBackdrop />
      <section className="login-visual mystic-login-visual">
        <header className="login-visual-head">
          <div className="login-brand">
            <span className="brand-mark large rune-mark">
              <i>L</i>
              <b aria-hidden="true" />
            </span>
            <span>
              <b>{branding.systemName}</b>
              <small>HỌC VIỆN HUYỀN TRI · CLS</small>
            </span>
          </div>
          <div className="login-edition">
            <span>PRIVATE REALM</span>
            <b>MMXXVI</b>
          </div>
        </header>
        <div className="login-visual-frame">
          <div className="visual-copy">
            <div className="login-coordinate">
              <i /> REALM 07 · KNOWLEDGE ASCENDS <b />
            </div>
            <span className="eyebrow light">
              NƠI TRI THỨC TRỞ THÀNH SỨC MẠNH
            </span>
            <h1>
              Mỗi khóa học là một cánh cổng.
              <em>Mỗi thành tựu là một vì sao.</em>
            </h1>
            <p>
              {branding.introduction ||
                "Một không gian học tập sâu, tĩnh và cuốn hút—nơi tổ chức kiến tạo năng lực, kết nối con người và lưu giữ hành trình phát triển."}
            </p>
            <div className="login-manifesto">
              <span>✦</span>
              <p>
                “Kiến thức không chỉ được truyền đạt. Nó được khám phá, thử
                thách và khắc ghi.”
              </p>
            </div>
          </div>
          <div className="portal-observatory" aria-hidden="true">
            <div className="observatory-axis axis-x" />
            <div className="observatory-axis axis-y" />
            <div className="arcane-gate">
              <span className="gate-ring ring-one" />
              <span className="gate-ring ring-two" />
              <span className="gate-ring ring-three" />
              <span className="gate-ring ring-four" />
              <span className="gate-core">✦</span>
            </div>
            <span className="orbit-label label-one">LEARN</span>
            <span className="orbit-label label-two">CREATE</span>
            <span className="orbit-label label-three">ASCEND</span>
            <span className="observatory-code">
              AETHER NETWORK
              <br />
              NODE 001
            </span>
          </div>
          <div className="realm-features">
            <span>
              <i>01</i>
              <b>Học tập thích ứng</b>
              <small>Tiến độ, lộ trình và báo cáo cá nhân</small>
              <em>EXPLORE</em>
            </span>
            <span>
              <i>02</i>
              <b>Thử thách công bằng</b>
              <small>Kỳ thi, xếp hạng và phần thưởng</small>
              <em>CHALLENGE</em>
            </span>
            <span>
              <i>03</i>
              <b>Vận hành tự chủ</b>
              <small>Dữ liệu nằm trong hạ tầng của bạn</small>
              <em>CONTROL</em>
            </span>
          </div>
        </div>
        <footer className="visual-stats">
          <div>
            <small>01</small>
            <b>100%</b>
            <span>Dữ liệu On-Premise</span>
          </div>
          <div>
            <small>02</small>
            <b>AI</b>
            <span>Local hoặc API riêng</span>
          </div>
          <div>
            <small>03</small>
            <b>RBAC</b>
            <span>Quyền theo từng phạm vi</span>
          </div>
          <p>CLS · SECURE LEARNING INFRASTRUCTURE</p>
        </footer>
      </section>
      <aside className="login-side mystic-login-side">
        <div className="login-side-index">
          <span>ACCESS CHAMBER</span>
          <b>04 / 08</b>
        </div>
        <LoginForm demoEnabled={demoEnabled} demoPassword={demoPassword} />
        <p className="login-footer">
          <span>✦</span>
          {branding.systemName} · Học viện Huyền Tri · Phiên bản CLS
        </p>
      </aside>
    </div>
  );
}
