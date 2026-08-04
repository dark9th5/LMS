import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { getPublicBranding } from "@/lib/branding";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function PortalLayout({ children }: { children: React.ReactNode }) {
  const [user, branding] = await Promise.all([getUser(), getPublicBranding()]);
  if (!user) redirect("/login");
  if (user.mustChangePassword) redirect("/change-password");
  return <PortalShell user={user} branding={branding}>{children}</PortalShell>;
}
