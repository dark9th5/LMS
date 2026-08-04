import { redirect } from "next/navigation";
import { ClassesPage } from "@/components/ClassesPage";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["classes:read", "classes:manage", "live-sessions:join"])) redirect("/learning");
  return <ClassesPage user={user}/>;
}
