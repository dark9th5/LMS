import { redirect } from "next/navigation";
import type { ReactNode } from "react";
import { AppShell } from "@/components/AppShell";
import { getPublicBranding } from "@/lib/branding";
import { requireAuthenticatedUser } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function PortalLayout({
  children,
}: {
  children: ReactNode;
}) {
  const [user, branding] = await Promise.all([
    requireAuthenticatedUser(),
    getPublicBranding(),
  ]);
  if (user.mustChangePassword) redirect("/change-password");
  return (
    <AppShell user={user} branding={branding}>
      {children}
    </AppShell>
  );
}
