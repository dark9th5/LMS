import { UserImportWizard } from "@/components/UserImportWizard";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await requireAuthenticatedUser();
  requireRole(user, "ADMIN");
  return <UserImportWizard />;
}
