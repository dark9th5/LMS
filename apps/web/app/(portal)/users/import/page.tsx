import { redirect } from "next/navigation";
import { UserImportWizard } from "@/components/UserImportWizard";
import { hasAnyPermission } from "@/lib/authorization";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";

export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["users:bulk-manage", "users:write"])) redirect("/users");
  return <UserImportWizard />;
}
