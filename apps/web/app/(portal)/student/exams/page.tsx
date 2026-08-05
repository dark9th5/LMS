import { ExamsPage } from "@/components/ExamsPage";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  return <ExamsPage user={user} standaloneOnly />;
}
