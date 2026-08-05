import { SectionPage } from "@/components/SectionPage";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  return <SectionPage section="results" user={user} />;
}
