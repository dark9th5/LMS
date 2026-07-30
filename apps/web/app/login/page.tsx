import { redirect } from "next/navigation";
import { getUser } from "@/lib/session";
import { LoginForm } from "./LoginForm";
import { landingForRoles } from "@/lib/role";

export const dynamic = "force-dynamic";
export const revalidate = 0;
export default async function Login() {
  const user = await getUser();
  if (user) redirect(landingForRoles(user.roles));
  const demoEnabled = process.env.LMSPILOT_SEED_DEMO === "true";
  const demoPassword = demoEnabled ? (process.env.LMSPILOT_DEFAULT_ADMIN_PASSWORD ?? "") : "";
  return <div className="login-page">
    <div className="login-visual">
      <div className="login-brand"><span className="brand-mark large">L</span><span><b>LMSPilot</b><small>Learning Operating System</small></span></div>
      <div className="visual-copy"><span className="eyebrow light">ĐÀO TẠO NỘI BỘ · AN TOÀN · CHỦ ĐỘNG</span><h1>Biến tri thức tổ chức thành năng lực thực tế.</h1><p>Quản lý trọn vẹn hành trình học tập trên hạ tầng của chính doanh nghiệp.</p></div>
      <div className="visual-stats"><div><b>100%</b><span>Dữ liệu On-Premise</span></div><div><b>LAN</b><span>Không phụ thuộc Internet</span></div><div><b>Local</b><span>Vận hành tại đơn vị</span></div></div>
      <div className="visual-grid" />
    </div>
    <div className="login-side"><LoginForm demoEnabled={demoEnabled} demoPassword={demoPassword} /><p className="login-footer">LMSPilot v0.4 · Trung tâm Công nghệ và Giải pháp</p></div>
  </div>;
}
