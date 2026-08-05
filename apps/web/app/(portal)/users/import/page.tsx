import { redirect } from "next/navigation";
import { PORTAL_PATHS } from "@/lib/portal-paths";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await requireAuthenticatedUser();
  requireRole(user, "ADMIN");
  redirect(PORTAL_PATHS.ADMIN.userImport);
}
