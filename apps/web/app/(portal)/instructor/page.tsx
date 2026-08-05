import { RoleDashboard } from "@/components/RoleDashboard";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function Page() {
  const user = await requireAuthenticatedUser();
  requireRole(user, "INSTRUCTOR");
  return <RoleDashboard user={user} />;
}
