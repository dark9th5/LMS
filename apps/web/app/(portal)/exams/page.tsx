import { redirect } from "next/navigation";
import { resolvePortalRole } from "@/lib/role";
import { standaloneExamPath } from "@/lib/portal-paths";
import { requireAuthenticatedUser } from "@/lib/route-access";

export const dynamic = "force-dynamic";

export default async function Page() {
  const user = await requireAuthenticatedUser();
  const role = resolvePortalRole(user);
  if (role === "ADMIN") redirect("/admin");
  redirect(standaloneExamPath(role));
}
