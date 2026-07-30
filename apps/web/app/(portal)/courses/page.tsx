import { redirect } from "next/navigation";
import { CoursesPage } from "@/components/CoursesPage";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.some((role) => role === "ADMIN" || role === "INSTRUCTOR")) redirect("/learning");
  return <CoursesPage user={user}/>;
}
