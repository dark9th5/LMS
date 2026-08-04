import { redirect } from "next/navigation";
import { CoursesPage } from "@/components/CoursesPage";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["courses:read", "courses:create", "courses:update", "courses:learn"])) redirect("/learning");
  return <CoursesPage user={user}/>;
}
